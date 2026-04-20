let currentUser=null, currentSubjectId=null, currentChapterId=null, currentTopicId=null;
let testType=null, testQuestions=[], testAnswers={}, testTimer=null, testStartTime=0, testDurSec=0, lastReq=null;

window.onload = () => {
    const saved = localStorage.getItem('user');
    if (saved && localStorage.getItem('token')) {
        currentUser = JSON.parse(saved);
        authToken = localStorage.getItem('token');
        enterApp();
        // Agar test paytida sahifa yangilangan bo'lsa
        const activeTest = localStorage.getItem('activeTest');
        if (activeTest) {
            try {
                const t = JSON.parse(activeTest);
                const elapsed = Math.floor((Date.now() - t.testStartTime) / 1000);
                const remaining = t.testDurSec - elapsed;
                if (remaining > 0) {
                    // Testni tiklash
                    setTimeout(() => {
                        if (confirm("Test davom ettirilsinmi? (" + Math.floor(remaining/60) + " daqiqa qolgan)")) {
                            testType = t.testType;
                            testDurSec = remaining;
                            testStartTime = Date.now();
                            lastReq = t.lastReq;
                            testQuestions = t.questions;
                            testAnswers = {};
                            el('testTitle').textContent = t.testType === 'topic' ? 'Mavzu testi' : 'Bob testi';
                            el('testProgress').textContent = t.questions.length + ' ta savol';
                            el('testQuestions').innerHTML = t.questions.map((q,i) => `
                <div class="question-card">
                  <div class="question-num">Savol ${i+1}</div>
                  <div class="question-text">${esc(q.questionText)}</div>
                  <div class="options-list">
                    ${(q.options||[]).map((o,oi) => `
                      <button class="option-btn" id="opt-${q.id}-${oi}" onclick="pick(${q.id},${oi})">
                        <span class="option-letter">${'ABCD'[oi]}</span>${esc(o)}
                      </button>`).join('')}
                  </div>
                </div>`).join('');
                            startTimer();
                            showSection('test');
                        } else {
                            localStorage.removeItem('activeTest');
                        }
                    }, 500);
                } else {
                    localStorage.removeItem('activeTest');
                }
            } catch(e) { localStorage.removeItem('activeTest'); }
        }
    }
};

function switchTab(tab, btn) {
    document.querySelectorAll('.tab-btn').forEach(b=>b.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById('loginForm').classList.toggle('hidden', tab!=='login');
    document.getElementById('registerForm').classList.toggle('hidden', tab!=='register');
}

async function login() {
    const email=v('loginEmail'), password=v('loginPassword');
    if(!email||!password){showErr('loginError',"Maydonlarni to'ldiring");return;}
    try{ loading(true); const r=await authLogin({email,password}); saveAuth(r); enterApp(); }
    catch(e){ showErr('loginError',"Email yoki parol noto'g'ri"); }
    finally{ loading(false); }
}

// Ro'yxat 1-bosqich: Kod yuborish
async function sendCode() {
    const fullName=v('regName'), email=v('regEmail'), password=v('regPassword');
    if(!fullName||!email||!password){showErr('regError',"Barcha maydonlarni to'ldiring");return;}
    if(!email.includes('@')){showErr('regError',"Email manzil noto'g'ri");return;}
    if(password.length<6){showErr('regError',"Parol kamida 6 ta belgi");return;}
    try{
        loading(true);
        await sendVerifyCode({email});
        // 2-bosqichga o'tish
        el('registerForm').classList.add('hidden');
        el('verifyForm').classList.remove('hidden');
        el('verifyEmailText').textContent = email;
        el('verifyCode').focus();
        toast('Kod yuborildi! Emailingizni tekshiring 📧','success');
    }catch(e){ showErr('regError','Xatolik: '+e.message); }
    finally{ loading(false); }
}

// Ro'yxat 2-bosqich: Tasdiqlash
async function verifyAndRegister() {
    const email=v('regEmail'), code=v('verifyCode'),
        fullName=v('regName'), password=v('regPassword');
    if(code.length!==6){showErr('verifyError',"6 raqamli kod kiriting");return;}
    try{
        loading(true);
        const r=await authRegister({email,password,fullName,code});
        saveAuth(r); enterApp();
    }catch(e){ showErr('verifyError','Xatolik: '+e.message); }
    finally{ loading(false); }
}

async function resendCode() {
    const email=v('regEmail');
    try{
        loading(true);
        await sendVerifyCode({email});
        toast('Kod qayta yuborildi 📧','success');
        el('verifyCode').value='';
    }catch(e){ toast('Xatolik: '+e.message,'error'); }
    finally{ loading(false); }
}

function backToRegister() {
    el('verifyForm').classList.add('hidden');
    el('registerForm').classList.remove('hidden');
}

async function register() { await sendCode(); }

function saveAuth(r){ authToken=r.token; currentUser=r; localStorage.setItem('token',r.token); localStorage.setItem('user',JSON.stringify(r)); }
function logout(){ localStorage.clear(); authToken=null; currentUser=null; clearTimer(); location.reload(); }

function enterApp(){
    if(currentUser.role==='ADMIN'){ show('adminDashboard'); adminInit(); }
    else { show('studentDashboard'); studentInit(); }
}

// ===== STUDENT =====
async function studentInit(){
    const i=(currentUser.fullName||currentUser.email).charAt(0).toUpperCase();
    el('sidebarAvatar').textContent=i;
    el('sidebarName').textContent=currentUser.fullName||'';
    el('sidebarEmail').textContent=currentUser.email;
    // Sidebar avatar rasmini yuklash
    try {
        const profile = await getMyProfile();
        if (profile.avatarUrl) {
            el('sidebarAvatar').innerHTML = `<img src="https://edu-platform-1.onrender.com${profile.avatarUrl}" style="width:100%;height:100%;object-fit:cover;border-radius:50%">`;
        }
    } catch(e) {}
    showSection('subjects');
}

async function loadSubjects(){
    try{
        const list=await getSubjects();
        const icons=['📐','⚗️','🌍','📖','🔢','🎨','🏛️','💻','🌱','🗺️'];
        el('subjectsList').innerHTML=list.map((s,i)=>`
      <div class="subject-card" onclick="openSubject(${s.id},'${esc(s.name)}')">
        <div class="subject-icon">${s.icon||icons[i%icons.length]}</div>
        <div class="subject-name">${esc(s.name)}</div>
        <div class="subject-desc">${esc(s.description||'')}</div>
      </div>`).join('')||'<p class="empty">Fanlar yo\'q</p>';
    }catch(e){toast('Fanlar yuklanmadi','error');}
}

async function openSubject(id,name){
    currentSubjectId=id; el('subjectTitle').textContent=name;
    try{ loading(true);
        const list=await getChapters(id);
        el('chaptersList').innerHTML=list.map((c,i)=>`
      <div class="chapter-card" onclick="openChapter(${c.id},'${esc(c.name)}')">
        <div class="chapter-num">${i+1}</div>
        <div class="chapter-info"><div class="chapter-name">${esc(c.name)}</div><div class="chapter-meta">⏱️ Test: ${c.testDurationMinutes} daqiqa</div></div>
        <div class="chapter-arrow">›</div>
      </div>`).join('')||'<p class="empty">Boblar yo\'q</p>';
        showSection('chapters');
    }catch(e){toast('Boblar yuklanmadi','error');}
    finally{loading(false);}
}

async function openChapter(id,name){
    currentChapterId=id; el('chapterTitle').textContent=name;
    try{ loading(true);
        const list=await getTopics(id);
        el('topicsList').innerHTML=list.map((t,i)=>`
      <div class="topic-card" onclick="openTopic(${t.id},'${esc(t.name)}','${esc(t.videoUrl||'')}',${t.testDurationMinutes||30})">
        <span class="topic-status">📄</span>
        <span class="topic-name">${i+1}. ${esc(t.name)}</span>
        <span class="topic-badge">${t.testDurationMinutes||30} min</span>
      </div>`).join('')||'<p class="empty">Mavzular yo\'q</p>';
        el('chapterTestBanner').dataset.chapterid=id;
        showSection('topics');
    }catch(e){toast('Mavzular yuklanmadi','error');}
    finally{loading(false);}
}

function openTopic(id,name,videoUrl,dur){
    currentTopicId=id; window._dur=dur;
    el('topicTitle').textContent=name;
    el('topicDurText').textContent=`⏱️ ${dur} daqiqa`;
    const wrap=el('videoWrapper');
    if(videoUrl&&videoUrl.trim()){
        const embed=toEmbed(videoUrl);
        wrap.innerHTML=`<iframe src="${embed}" allowfullscreen style="position:absolute;inset:0;width:100%;height:100%;border:none"></iframe>`;
    } else {
        wrap.innerHTML='<div class="video-placeholder"><span>🎬</span><p>Video qo\'shilmagan</p></div>';
    }
    showSection('topicLearn');
}

function toEmbed(url){
    const m=url.match(/(?:youtube\.com\/watch\?v=|youtu\.be\/)([^&\s]+)/);
    return m?`https://www.youtube.com/embed/${m[1]}`:url;
}

function markWatched(){ saveProgress(currentTopicId,{watched:true,watchedSeconds:999}).then(()=>toast('Video ko\'rilgan deb belgilandi ✅','success')).catch(()=>{}); }

async function startTopicTest(){
    testType='topic'; testDurSec=(window._dur||30)*60;
    await loadTest(`/topics/${currentTopicId}/questions`,currentTopicId,null,'Mavzu testi');
}
async function startChapterTest(){
    testType='chapter'; testDurSec=60*60;
    const id=el('chapterTestBanner').dataset.chapterid||currentChapterId;
    await loadTest(`/chapters/${id}/questions`,null,id,'Bob testi');
}

async function loadTest(path,topicId,chapterId,title){
    try{ loading(true);
        const q=await api('GET',path);
        if(!q||!q.length){toast('Savollar yo\'q','error');return;}
        testQuestions=q; testAnswers={}; testStartTime=Date.now(); lastReq={topicId: topicId?Number(topicId):null, chapterId: chapterId?Number(chapterId):null};
        el('testTitle').textContent=title;
        el('testProgress').textContent=q.length+' ta savol';
        el('testQuestions').innerHTML=q.map((q,i)=>`
      <div class="question-card">
        <div class="question-num">Savol ${i+1}</div>
        <div class="question-text">${esc(q.questionText)}</div>
        <div class="options-list">
          ${(q.options||[]).map((o,oi)=>`
            <button class="option-btn" id="opt-${q.id}-${oi}" onclick="pick(${q.id},${oi})">
              <span class="option-letter">${'ABCD'[oi]}</span>${esc(o)}
            </button>`).join('')}
        </div>
      </div>`).join('');
        startTimer();
        showSection('test');
        // Test holatini localStorage ga saqlash
        localStorage.setItem('activeTest', JSON.stringify({
            testType, testDurSec, lastReq, testStartTime,
            questions: testQuestions.map(q => ({id:q.id, questionText:q.questionText, options:q.options}))
        }));
    }catch(e){toast('Test yuklanmadi: '+e.message,'error');}
    finally{loading(false);}
}

function pick(qId,oi){
    testAnswers[Number(qId)] = Number(oi);
    testQuestions.find(q=>q.id===Number(qId))?.options.forEach((_,i)=>{
        el(`opt-${qId}-${i}`)?.classList.toggle('selected',i===oi);
    });
}

function startTimer(){
    clearTimer(); let rem=testDurSec; updateTimer(rem);
    testTimer=setInterval(()=>{
        rem--; updateTimer(rem);
        if(rem<=300) el('timer').classList.add('warning');
        if(rem<=0){clearTimer();toast('Vaqt tugadi!','info');submitTest(true);}
    },1000);
}
function updateTimer(s){ const m=String(Math.floor(s/60)).padStart(2,'0'),sec=String(s%60).padStart(2,'0'); const t=el('timer'); if(t)t.textContent=`⏱️ ${m}:${sec}`; }
function clearTimer(){ if(testTimer){clearInterval(testTimer);testTimer=null;} }

async function submitTest(){
    clearTimer();
    localStorage.removeItem('activeTest');
    const timeTaken=Math.floor((Date.now()-testStartTime)/1000);
    const answers={};
    Object.keys(testAnswers).forEach(k=>{ answers[parseInt(k)] = parseInt(testAnswers[k]); });
    const req={answers,timeTakenSeconds:timeTaken,...lastReq};
    try{ loading(true);
        const r=testType==='topic'?await submitTopicT(req):await submitChapT(req);
        el('resultIcon').textContent=r.passed?'🎉':'😔';
        el('resultTitle').textContent=r.passed?'Tabriklaymiz!':'Harakat qiling!';
        el('resultPercent').textContent=r.percentage.toFixed(1)+'%';
        el('resultCorrect').textContent=`${r.correctAnswers}/${r.totalQuestions}`;
        el('resultStatus').textContent=r.passed?'✅ O\'tdi':'❌ O\'tmadi';
        el('resultStatus').style.color=r.passed?'var(--success)':'var(--danger)';
        el('resultMsg').textContent=r.passed?'60% dan yuqori ball!':'60% dan past. Qayta urinib ko\'ring.'+(testType==='chapter'?' Natija emailingizga yuborildi.':'');
        showSection('result');
        // AI tahlilni yuklash
        loadAiFeedback(r.id);
    }catch(e){toast('Xatolik: '+e.message,'error');}
    finally{loading(false);}
}

function retakeTest(){ testType==='topic'?startTopicTest():startChapterTest(); }

async function loadMyResults(){
    try{ loading(true);
        const r=await getMyResults();
        el('myResultsList').innerHTML=r&&r.length?`
      <table class="results-table"><thead><tr><th>Tur</th><th>To'g'ri</th><th>Foiz</th><th>Natija</th><th>Sana</th></tr></thead>
      <tbody>${r.map(x=>`<tr>
        <td><span class="${x.testType==='TOPIC'?'badge-topic':'badge-chapter'}">${x.testType==='TOPIC'?'Mavzu':'Bob'}</span></td>
        <td>${x.correctAnswers}/${x.totalQuestions}</td>
        <td><b>${x.percentage.toFixed(1)}%</b></td>
        <td><span class="${x.passed?'badge-pass':'badge-fail'}">${x.passed?"O'tdi":"O'tmadi"}</span></td>
        <td>${fmtDate(x.completedAt)}</td>
      </tr>`).join('')}</tbody></table>`:'<p class="empty">Hali test topshirilmagan</p>';
    }catch(e){toast('Yuklanmadi','error');}
    finally{loading(false);}
}

async function loadProfile(){
    try {
        const profile = await getMyProfile();
        // Avatar
        const imgWrap = el('profileAvatarImg');
        const avatarDiv = el('profileAvatar');
        const deleteBtn = el('deleteAvatarBtn');
        if (profile.avatarUrl) {
            imgWrap.innerHTML = `<img src="https://edu-platform-1.onrender.com${profile.avatarUrl}" class="profile-avatar-photo" alt="Avatar">`;
            if(deleteBtn) deleteBtn.style.display = 'inline-flex';
        } else {
            imgWrap.innerHTML = `<div class="profile-avatar">${(profile.fullName||profile.email).charAt(0).toUpperCase()}</div>`;
            if(deleteBtn) deleteBtn.style.display = 'none';
        }
        el('profileName').textContent = profile.fullName||'';
        el('profileEmail').textContent = profile.email;
        // Kirish vaqtlari
        el('profileLoginInfo').innerHTML = `
      <div class="login-info-row"><span>📅 Ro'yxat sanasi:</span><b>${profile.createdAt||'-'}</b></div>
      <div class="login-info-row"><span>🕐 Oxirgi kirish:</span><b>${profile.lastLoginAt||'-'}</b></div>`;
        // Statistika
        const r = await getMyResults();
        const total=r.length, passed=r.filter(x=>x.passed).length;
        el('profileStats').innerHTML=`
      <div class="stat-box"><span>${total}</span><label>Jami</label></div>
      <div class="stat-box"><span>${passed}</span><label>O'tdi</label></div>
      <div class="stat-box"><span>${total?Math.round(passed/total*100):0}%</span><label>O'tish</label></div>`;
    } catch(e) {
        el('profileName').textContent = currentUser.fullName||'';
        el('profileEmail').textContent = currentUser.email;
    }
}

async function handleAvatarUpload(input) {
    const file = input.files[0];
    if (!file) return;
    if (file.size > 5*1024*1024) { toast("Fayl 5MB dan katta!", 'error'); return; }
    try {
        loading(true);
        await uploadAvatar(file);
        toast('Rasm yuklandi ✅', 'success');
        // Sidebar avatarini ham yangilash
        const reader = new FileReader();
        reader.onload = e => {
            el('sidebarAvatar').innerHTML = `<img src="${e.target.result}" style="width:100%;height:100%;object-fit:cover;border-radius:50%">`;
        };
        reader.readAsDataURL(file);
        await loadProfile();
    } catch(e) { toast('Xatolik: ' + e.message, 'error'); }
    finally { loading(false); input.value=''; }
}

async function handleAvatarDelete() {
    if (!confirm("Rasmni o'chirishni tasdiqlaysizmi?")) return;
    try {
        loading(true);
        await deleteAvatar();
        toast("Rasm o'chirildi", 'success');
        el('sidebarAvatar').textContent = (currentUser.fullName||currentUser.email).charAt(0).toUpperCase();
        el('sidebarAvatar').innerHTML = '';
        el('sidebarAvatar').textContent = (currentUser.fullName||currentUser.email).charAt(0).toUpperCase();
        await loadProfile();
    } catch(e) { toast('Xatolik', 'error'); }
    finally { loading(false); }
}

function showSection(name){
    // Test paytida boshqa bo'limga o'tishni bloklash
    if (testTimer !== null && name !== 'test' && name !== 'result') {
        toast("⚠️ Avval testni yakunlang!", 'error');
        return;
    }
    ['subjects','chapters','topics','topicLearn','test','result','myResults','profile','aiAdvice','school'].forEach(s=>{
        el(s+'Section')?.classList.add('hidden');
    });
    el(name+'Section')?.classList.remove('hidden');
    if(name==='subjects') loadSubjects();
    if(name==='myResults') loadMyResults();
    if(name==='profile') loadProfile();
    if(name==='aiAdvice') loadAiPage();
    if(name==='school') loadSchoolSection();
}

function setActive(btn){ document.querySelectorAll('#studentDashboard .nav-item').forEach(b=>b.classList.remove('active')); btn.classList.add('active'); }

// ===== ADMIN =====
let adminSubId=null, adminChapId=null;

function adminInit(){ adminShowSection('adminSubjects'); adminLoadSubjects(); }

async function adminLoadSubjects(){
    try{
        const list=await getSubjects();
        el('adminSubjectsList').innerHTML=list.map(s=>`
      <div class="admin-item">
        <div class="admin-item-info" onclick="adminOpenSubject(${s.id},'${esc(s.name)}')">
          <div class="admin-item-title">${esc(s.icon||'📚')} ${esc(s.name)}</div>
          <div class="admin-item-meta">${esc(s.description||'')}</div>
        </div>
        <div class="admin-actions">
          <button class="btn-sm btn-delete" onclick="event.stopPropagation();adminDelSubject(${s.id})">🗑️</button>
        </div>
      </div>`).join('')||'<p class="empty">Fan yo\'q</p>';
    }catch(e){toast('Yuklanmadi','error');}
}

async function adminOpenSubject(id,name){
    adminSubId=id; el('adminSubjectTitle').textContent=`📁 ${name} - Boblar`;
    try{ loading(true);
        const list=await getChapters(id);
        el('adminChaptersList').innerHTML=list.map(c=>`
      <div class="admin-item">
        <div class="admin-item-info" onclick="adminOpenChapter(${c.id},'${esc(c.name)}')">
          <div class="admin-item-title">${esc(c.name)}</div>
          <div class="admin-item-meta">Test: ${c.testDurationMinutes} daqiqa</div>
        </div>
        <div class="admin-actions"><button class="btn-sm btn-delete" onclick="event.stopPropagation();adminDelChapter(${c.id})">🗑️</button></div>
      </div>`).join('')||'<p class="empty">Bob yo\'q</p>';
        adminShowSection('adminChapters');
    }catch(e){toast('Yuklanmadi','error');}
    finally{loading(false);}
}

async function adminOpenChapter(id,name){
    adminChapId=id; el('adminChapterTitle').textContent=`📋 ${name} - Mavzular`;
    try{ loading(true);
        const list=await getTopics(id);
        el('adminTopicsList').innerHTML=list.map(t=>`
      <div class="admin-item">
        <div class="admin-item-info">
          <div class="admin-item-title">${esc(t.name)}</div>
          <div class="admin-item-meta">Video: ${t.videoUrl?'✅':'❌'} | Test: ${t.testDurationMinutes} daqiqa</div>
        </div>
        <div class="admin-actions">
          <button class="btn-sm btn-import" onclick="openImportModal(${t.id})">📤 Excel</button>
          <button class="btn-sm btn-edit" onclick="openEditTopic(${t.id},'${esc(t.name)}','${esc(t.videoUrl||'')}',${t.testDurationMinutes})">✏️</button>
          <button class="btn-sm btn-delete" onclick="adminDelTopic(${t.id})">🗑️</button>
        </div>
      </div>`).join('')||'<p class="empty">Mavzu yo\'q</p>';
        adminShowSection('adminTopics');
    }catch(e){toast('Yuklanmadi','error');}
    finally{loading(false);}
}

function adminShowSection(name){
    ['adminSubjects','adminChapters','adminTopics','adminResults','adminUsers','adminSchools'].forEach(s=>el(s+'Section')?.classList.add('hidden'));
    el(name+'Section')?.classList.remove('hidden');
    if(name==='adminSubjects') adminLoadSubjects();
    if(name==='adminResults') adminLoadResults();
    if(name==='adminUsers') adminLoadUsers();
    if(name==='adminSchools') adminLoadSchools();
}
function setAdminActive(btn){ document.querySelectorAll('#adminDashboard .nav-item').forEach(b=>b.classList.remove('active')); btn.classList.add('active'); }
function adminBackToChapters(){ adminOpenSubject(adminSubId, el('adminSubjectTitle').textContent); }

// Forms
function addSubjectForm(){ return `
  <div class="form-group"><label>Fan nomi</label><input type="text" id="m_sName" placeholder="Matematika"></div>
  <div class="form-group"><label>Tavsif</label><input type="text" id="m_sDesc" placeholder="Fan haqida"></div>
  <div class="form-group"><label>Ikon (emoji)</label><input type="text" id="m_sIcon" placeholder="📐" maxlength="5"></div>
  <button class="btn-primary" onclick="adminSaveSubject()">Saqlash</button>`; }

function addChapterForm(){ return `
  <div class="form-group"><label>Bob nomi</label><input type="text" id="m_cName" placeholder="1-bob: Natural sonlar"></div>
  <div class="form-group"><label>Tartib raqami</label><input type="number" id="m_cOrder" value="1" min="1"></div>
  <div class="form-group"><label>Test muddati (daqiqa)</label><input type="number" id="m_cDur" value="60" min="10"></div>
  <button class="btn-primary" onclick="adminSaveChapter()">Saqlash</button>`; }

function addTopicForm(){ return `
  <div class="form-group"><label>Mavzu nomi</label><input type="text" id="m_tName" placeholder="1.1 Mavzu"></div>
  <div class="form-group"><label>Tartib raqami</label><input type="number" id="m_tOrder" value="1" min="1"></div>
  <div class="form-group"><label>YouTube video URL</label><input type="text" id="m_tVideo" placeholder="https://youtube.com/watch?v=..."></div>
  <div class="form-group"><label>Test muddati (daqiqa)</label><input type="number" id="m_tDur" value="30" min="5"></div>
  <button class="btn-primary" onclick="adminSaveTopic()">Saqlash</button>`; }

async function adminSaveSubject(){
    const name=v('m_sName'); if(!name){toast('Nom kiriting','error');return;}
    try{ loading(true); await adminCreateSubject({name,description:v('m_sDesc'),icon:v('m_sIcon')}); closeModal(); adminLoadSubjects(); toast('Fan qo\'shildi ✅','success'); }
    catch(e){toast('Xatolik: '+e.message,'error');}
    finally{loading(false);}
}
async function adminSaveChapter(){
    const name=v('m_cName'); if(!name){toast('Nom kiriting','error');return;}
    try{ loading(true);
        await adminCreateChapter({name,orderNum:parseInt(v('m_cOrder'))||1,testDurationMinutes:parseInt(v('m_cDur'))||60,subjectId:adminSubId});
        closeModal(); adminOpenSubject(adminSubId,''); toast('Bob qo\'shildi ✅','success');
    }catch(e){toast('Xatolik: '+e.message,'error');}
    finally{loading(false);}
}
async function adminSaveTopic(){
    const name=v('m_tName'); if(!name){toast('Nom kiriting','error');return;}
    try{ loading(true);
        await adminCreateTopic({name,orderNum:parseInt(v('m_tOrder'))||1,videoUrl:v('m_tVideo'),testDurationMinutes:parseInt(v('m_tDur'))||30,chapterId:adminChapId});
        closeModal(); adminOpenChapter(adminChapId,''); toast('Mavzu qo\'shildi ✅','success');
    }catch(e){toast('Xatolik: '+e.message,'error');}
    finally{loading(false);}
}

function openEditTopic(id,name,video,dur){
    openModal('Mavzuni tahrirlash',`
    <div class="form-group"><label>Mavzu nomi</label><input type="text" id="m_etName" value="${esc(name)}"></div>
    <div class="form-group"><label>Video URL</label><input type="text" id="m_etVideo" value="${esc(video)}"></div>
    <div class="form-group"><label>Test muddati</label><input type="number" id="m_etDur" value="${dur}"></div>
    <button class="btn-primary" onclick="adminUpdateT(${id})">Saqlash</button>`);
}
async function adminUpdateT(id){
    try{ loading(true);
        await adminUpdateTopic(id,{name:v('m_etName'),videoUrl:v('m_etVideo'),testDurationMinutes:parseInt(v('m_etDur'))||30});
        closeModal(); adminOpenChapter(adminChapId,''); toast('Yangilandi ✅','success');
    }catch(e){toast('Xatolik','error');}
    finally{loading(false);}
}

function openImportModal(topicId){
    openModal('Savol yuklash (Excel)',`
    <div class="import-hint">A=Savol | B-E=Variantlar | F=To'g'ri javob (1-4)</div>
    <input type="file" id="m_tFile" accept=".xlsx,.xls" style="width:100%;padding:10px;border:2px dashed var(--border);border-radius:8px;margin:12px 0">
    <button class="btn-primary" onclick="importTopicQ(${topicId})">📤 Yuklash</button>`);
}
async function importTopicQ(topicId){
    const file=document.getElementById('m_tFile')?.files[0]; if(!file){toast('Fayl tanlang','error');return;}
    try{ loading(true); const r=await adminImportTopicQ(topicId,file); closeModal(); toast(r.message||'Yuklandi ✅','success'); }
    catch(e){toast('Xatolik: '+e.message,'error');}
    finally{loading(false);}
}
async function importChapterQ(){
    const file=document.getElementById('chapterExcelFile')?.files[0]; if(!file){toast('Fayl tanlang','error');return;}
    try{ loading(true); const r=await adminImportChapterQ(adminChapId,file); toast(r.message||'Yuklandi ✅','success'); }
    catch(e){toast('Xatolik: '+e.message,'error');}
    finally{loading(false);}
}

async function adminDelSubject(id){ if(!confirm("O'chirishni tasdiqlaysizmi?"))return; await adminDeleteSubject(id); adminLoadSubjects(); toast("O'chirildi",'success'); }
async function adminDelChapter(id){ if(!confirm("O'chirishni tasdiqlaysizmi?"))return; await adminDeleteChapter(id); adminOpenSubject(adminSubId,''); toast("O'chirildi",'success'); }
async function adminDelTopic(id){ if(!confirm("O'chirishni tasdiqlaysizmi?"))return; await adminDeleteTopic(id); adminOpenChapter(adminChapId,''); toast("O'chirildi",'success'); }

async function adminLoadResults(){
    try{ loading(true);
        const r=await adminGetResults();
        el('adminResultsList').innerHTML=r&&r.length?`
      <table class="results-table"><thead><tr><th>Email</th><th>Ism</th><th>Tur</th><th>To'g'ri</th><th>Foiz</th><th>Natija</th><th>Sana</th></tr></thead>
      <tbody>${r.map(x=>`<tr>
        <td><code>${esc(x.email)}</code></td>
        <td>${esc(x.fullName||'-')}</td>
        <td><span class="${x.testType==='TOPIC'?'badge-topic':'badge-chapter'}">${x.testType==='TOPIC'?'Mavzu':'Bob'}</span></td>
        <td>${x.correctAnswers}/${x.totalQuestions}</td>
        <td><b>${x.percentage.toFixed(1)}%</b></td>
        <td><span class="${x.passed?'badge-pass':'badge-fail'}">${x.passed?"O'tdi":"O'tmadi"}</span></td>
        <td>${fmtDate(x.completedAt)}</td>
      </tr>`).join('')}</tbody></table>`:'<p class="empty">Natijalar yo\'q</p>';
    }catch(e){toast('Yuklanmadi','error');}
    finally{loading(false);}
}
async function adminLoadUsers(){
    try{ loading(true);
        const r=await adminGetUsers();
        el('adminUsersList').innerHTML=`
      <table class="results-table"><thead><tr><th>#</th><th>Rasm</th><th>Ism</th><th>Email</th><th>Rol</th><th>Ro'yxat sanasi</th><th>Oxirgi kirish</th></tr></thead>
      <tbody>${r.map((u,i)=>`<tr>
        <td>${i+1}</td>
        <td><div class="user-mini-avatar">${u.avatarUrl?`<img src="https://edu-platform-1.onrender.com${esc(u.avatarUrl)}" style="width:100%;height:100%;object-fit:cover;border-radius:50%">`:esc((u.fullName||u.email).charAt(0).toUpperCase())}</div></td>
        <td><b>${esc(u.fullName||'-')}</b></td>
        <td>${esc(u.email)}</td>
        <td><span class="${u.role==='ADMIN'?'badge-fail':'badge-pass'}">${u.role}</span></td>
        <td>${fmtDate(u.createdAt)}</td>
        <td>${u.lastLoginAt?fmtDate(u.lastLoginAt):'<span style="color:var(--text-muted)">Kirmagan</span>'}</td>
      </tr>`).join('')}</tbody></table>`;
    }catch(e){toast('Yuklanmadi','error');}
    finally{loading(false);}
}

// ===== UTILS =====
const el  = id => document.getElementById(id);
const v   = id => el(id)?.value?.trim()||'';
const esc = s  => s?String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;').replace(/'/g,'&#39;'):'';

function show(id){ ['authPage','studentDashboard','adminDashboard'].forEach(p=>el(p)?.classList.toggle('hidden',p!==id)); }
function openModal(title,body){ el('modalTitle').textContent=title; el('modalBody').innerHTML=body; el('modalOverlay').classList.remove('hidden'); }
function closeModal(){ el('modalOverlay').classList.add('hidden'); }
function loading(show){ el('loadingOverlay').classList.toggle('hidden',!show); }
function showErr(id,msg){ const e=el(id); e.textContent=msg; e.classList.remove('hidden'); setTimeout(()=>e.classList.add('hidden'),4000); }
function toast(msg,type='info'){ const d=document.createElement('div'); d.className=`toast ${type}`; d.textContent=msg; el('toastContainer').appendChild(d); setTimeout(()=>d.remove(),3500); }
function fmtDate(s){ try{return new Date(s).toLocaleString('uz-UZ',{dateStyle:'short',timeStyle:'short'});}catch{return s||'-';} }
document.addEventListener('keydown',e=>{ if(e.key==='Escape')closeModal(); });

// Test paytida sahifadan chiqishni bloklash
window.addEventListener('beforeunload', e => {
    if (testTimer !== null) {
        e.preventDefault();
        e.returnValue = "Test hali tugalanmagan! Sahifani yangilasangiz test yo'qoladi.";
        return e.returnValue;
    }
});

// ===== AI TEST FEEDBACK =====
async function loadAiFeedback(testResultId) {
    const box = el('aiFeedbackBox');
    if (!box) return;
    box.classList.remove('hidden');
    box.innerHTML = `<div class="ai-feedback-loading"><div class="loader"></div><p>🤖 AI tahlil qilyapti...</p></div>`;
    try {
        const r = await api('GET', `/ai/result/${testResultId}`);
        let html = `<div class="ai-feedback-header">🤖 AI Tahlil</div>`;
        html += `<div class="ai-feedback-text">${esc(r.feedback).replace(/\n/g,'<br>')}</div>`;
        if (r.wrongDetails && r.wrongDetails.length > 0) {
            html += `<div class="ai-wrong-title">❌ Xato qilingan savollar (${r.wrongCount} ta)</div>`;
            r.wrongDetails.forEach((w, i) => {
                html += `<div class="ai-wrong-item">
          <div class="ai-wrong-q">${i+1}. ${esc(w.question)}</div>
          <div class="ai-wrong-ans">
            <span class="ai-wrong-user">❌ Sizning javob: ${esc(w.userAnswer)}</span>
          </div>
        </div>`;
            });
        }
        box.innerHTML = html;
    } catch(e) {
        box.innerHTML = `<div class="ai-feedback-header">🤖 AI Tahlil</div><p style="color:var(--text-muted);padding:12px">Tahlil yuklanmadi.</p>`;
    }
}

// ===== AI SAHIFA (sidebar) =====
async function loadAiPage() {
    const div = el('aiAnalysisDiv');
    if (!div) return;
    div.innerHTML = '<div class="ai-loading"><div class="loader"></div><p>Natijalar yuklanmoqda...</p></div>';

    try {
        const results = await getMyResults();
        if (!results || results.length === 0) {
            div.innerHTML = '<div class="ai-card"><p style="color:var(--text-muted)">Hali test topshirilmagan. Avval testlarni yechib ko\'ring!</p></div>';
            return;
        }

        // Oxirgi 5 ta natijani ko'rsatish va har biri uchun AI tahlil tugmasi
        const recent = results.slice(0, 5);
        let html = '<div class="ai-card">';
        html += '<h3>📋 Oxirgi test natijalari</h3>';
        html += '<p style="color:var(--text-muted);font-size:13px;margin-bottom:16px">Har bir test uchun AI tahlilni ko\'rish uchun tugmani bosing</p>';

        recent.forEach((r, i) => {
            const type = r.testType === 'TOPIC' ? 'Mavzu' : 'Bob';
            const date = fmtDate(r.completedAt);
            html += `<div class="ai-result-row">
        <div class="ai-result-info">
          <span class="${r.testType === 'TOPIC' ? 'badge-topic' : 'badge-chapter'}">${type}</span>
          <span class="ai-result-score" style="color:${r.passed ? 'var(--success)' : 'var(--danger)'}">${r.percentage.toFixed(1)}%</span>
          <span class="ai-result-date">${date}</span>
        </div>
        <button class="btn-sm btn-edit" onclick="showSingleFeedback(${r.id}, this)">🤖 AI Tahlil</button>
      </div>
      <div id="feedback-${r.id}" class="hidden"></div>`;
        });

        html += '</div>';
        div.innerHTML = html;
    } catch(e) {
        div.innerHTML = '<div class="ai-card"><p style="color:var(--danger)">Yuklanmadi.</p></div>';
    }
}

async function showSingleFeedback(resultId, btn) {
    const box = el(`feedback-${resultId}`);
    if (!box) return;
    if (!box.classList.contains('hidden')) {
        box.classList.add('hidden');
        btn.textContent = '🤖 AI Tahlil';
        return;
    }
    box.classList.remove('hidden');
    btn.textContent = '⏳ Yuklanmoqda...';
    btn.disabled = true;
    box.innerHTML = '<div class="ai-feedback-loading"><div class="loader"></div><p>AI tahlil qilyapti...</p></div>';

    try {
        const r = await api('GET', `/ai/result/${resultId}`);
        let html = `<div class="ai-feedback-text">${esc(r.feedback).replace(/\n/g,'<br>')}</div>`;
        if (r.wrongDetails && r.wrongDetails.length > 0) {
            html += `<div class="ai-wrong-title">❌ Xato savollar (${r.wrongCount} ta)</div>`;
            r.wrongDetails.forEach((w, i) => {
                html += `<div class="ai-wrong-item">
          <div class="ai-wrong-q">${i+1}. ${esc(w.question)}</div>
          <div class="ai-wrong-ans">
            <span class="ai-wrong-user">❌ Sizning javob: ${esc(w.userAnswer)}</span>
          </div>
        </div>`;
            });
        }
        box.innerHTML = html;
        btn.textContent = '✕ Yopish';
        btn.disabled = false;
    } catch(e) {
        box.innerHTML = '<p style="color:var(--danger);padding:8px">Tahlil yuklanmadi.</p>';
        btn.textContent = '🤖 AI Tahlil';
        btn.disabled = false;
    }
}

// ===== MAKTAB TANLASH =====
let selectedRegionId = null, selectedDistrictId = null;

async function loadSchoolSection() {
    await loadMySchoolInfo();
    await loadRegionOptions();
}

async function loadMySchoolInfo() {
    const box = el('mySchoolInfo');
    const card = el('schoolSelectCard');
    try {
        const s = await getMySchool();
        if (s.hasSchool) {
            box.innerHTML = `
        <div class="school-current-card">
          <div class="school-current-icon">🏫</div>
          <div>
            <div class="school-current-name">${esc(s.schoolName)}${s.schoolNumber?' ('+esc(s.schoolNumber)+'-maktab)':''}</div>
            <div class="school-current-meta">📍 ${esc(s.regionName)} › ${esc(s.districtName)}</div>
            <div class="school-current-changes">
              ${s.changesRemaining > 0
                ? `O'zgartirish imkoniyati: <b>${s.changesRemaining} ta</b>`
                : '<span style="color:var(--danger)">O\'zgartirish imkoniyati tugagan</span>'}
            </div>
          </div>
        </div>`;
            if (s.changesRemaining === 0) card.style.display = 'none';
        } else {
            box.innerHTML = `<div class="school-warning">⚠️ Maktabingizni tanlamaguningizcha fanlar va testlar mavjud bo'lmaydi!</div>`;
        }
    } catch(e) {
        box.innerHTML = '';
    }
}

async function loadRegionOptions() {
    try {
        const regions = await getRegions();
        const sel = el('regionSelect');
        sel.innerHTML = '<option value="">-- Viloyatni tanlang --</option>';
        regions.forEach(r => {
            sel.innerHTML += `<option value="${r.id}">${esc(r.name)}</option>`;
        });
    } catch(e) {}
}

async function loadDistricts(regionId) {
    selectedRegionId = regionId;
    const dSel = el('districtSelect');
    const sSel = el('schoolSelect');
    dSel.innerHTML = '<option value="">Yuklanmoqda...</option>';
    dSel.disabled = true;
    sSel.innerHTML = '<option value="">-- Avval tuman tanlang --</option>';
    sSel.disabled = true;
    if (!regionId) return;
    try {
        const districts = await getDistricts(regionId);
        dSel.innerHTML = '<option value="">-- Tuman/Shahar tanlang --</option>';
        districts.forEach(d => {
            dSel.innerHTML += `<option value="${d.id}">${esc(d.name)}${d.city?' (Shahar)':''}</option>`;
        });
        dSel.disabled = false;
        dSel.onchange = e => loadSchools(e.target.value);
    } catch(e) { dSel.innerHTML = '<option value="">Xatolik</option>'; }
}

async function loadSchools(districtId) {
    selectedDistrictId = districtId;
    const sSel = el('schoolSelect');
    sSel.innerHTML = '<option value="">Yuklanmoqda...</option>';
    sSel.disabled = true;
    if (!districtId) return;
    try {
        const schools = await getSchools(districtId);
        sSel.innerHTML = '<option value="">-- Maktabni tanlang --</option>';
        schools.forEach(s => {
            sSel.innerHTML += `<option value="${s.id}">${esc(s.name)}${s.number?' - '+esc(s.number)+'-maktab':''}</option>`;
        });
        sSel.disabled = false;
    } catch(e) { sSel.innerHTML = '<option value="">Xatolik</option>'; }
}

async function saveSchool() {
    const schoolId = el('schoolSelect').value;
    if (!schoolId) { toast("Maktabni tanlang!", 'error'); return; }
    try {
        loading(true);
        const r = await selectSchool({ schoolId: parseInt(schoolId) });
        toast(r.message + ` (Qolgan o'zgarish: ${r.changesRemaining})`, 'success');
        await loadSchoolSection();
        // Subjects ni ham qayta yuklash uchun
        loadSubjects();
    } catch(e) {
        toast(e.message || 'Xatolik', 'error');
    } finally { loading(false); }
}

// ===== ADMIN MAKTAB BOSHQARUVI =====
let adminSelRegionId = null, adminSelDistrictId = null;

async function adminLoadSchools() {
    try {
        const regions = await adminGetRegions();
        el('adminRegionsList').innerHTML = regions.length
            ? regions.map(r => `
          <div class="admin-item" onclick="adminLoadDistrictList(${r.id},'${esc(r.name)}')" style="cursor:pointer">
            <div class="admin-item-info"><div class="admin-item-title">📍 ${esc(r.name)}</div></div>
            <button class="btn-sm btn-delete" onclick="event.stopPropagation();adminDelReg(${r.id})">🗑️</button>
          </div>`).join('')
            : '<p class="empty">Viloyat yo\'q</p>';
    } catch(e) { toast('Yuklanmadi','error'); }
}

async function adminSaveRegion() {
    const name = el('newRegionName').value.trim();
    if (!name) { toast('Nom kiriting','error'); return; }
    try {
        loading(true);
        await adminAddRegion({ name });
        el('newRegionName').value = '';
        toast('Viloyat qo\'shildi ✅','success');
        await adminLoadSchools();
    } catch(e) { toast('Xatolik','error'); } finally { loading(false); }
}

async function adminLoadDistrictList(regionId, regionName) {
    adminSelRegionId = regionId;
    el('adminDistrictForm').classList.remove('hidden');
    el('adminSchoolForm').classList.add('hidden');
    el('adminSchoolsList').innerHTML = '<p class="empty">Tuman tanlang</p>';
    try {
        const districts = await adminGetDistricts(regionId);
        el('adminDistrictsList').innerHTML = districts.length
            ? districts.map(d => `
          <div class="admin-item" onclick="adminLoadSchoolList(${d.id})" style="cursor:pointer">
            <div class="admin-item-info">
              <div class="admin-item-title">${d.city?'🏙️':'🏘️'} ${esc(d.name)}</div>
            </div>
            <button class="btn-sm btn-delete" onclick="event.stopPropagation();adminDelDist(${d.id})">🗑️</button>
          </div>`).join('')
            : '<p class="empty">Tuman yo\'q</p>';
    } catch(e) {}
}

async function adminSaveDistrict() {
    if (!adminSelRegionId) { toast('Viloyat tanlang','error'); return; }
    const name = el('newDistrictName').value.trim();
    const isCity = el('districtIsCity').checked;
    if (!name) { toast('Nom kiriting','error'); return; }
    try {
        loading(true);
        await adminAddDistrict({ name, isCity, regionId: adminSelRegionId });
        el('newDistrictName').value = '';
        toast('Tuman qo\'shildi ✅','success');
        await adminLoadDistrictList(adminSelRegionId, '');
    } catch(e) { toast('Xatolik','error'); } finally { loading(false); }
}

async function adminLoadSchoolList(districtId) {
    adminSelDistrictId = districtId;
    el('adminSchoolForm').classList.remove('hidden');
    try {
        const schools = await adminGetSchools(districtId);
        el('adminSchoolsList').innerHTML = schools.length
            ? schools.map(s => `
          <div class="admin-item">
            <div class="admin-item-info">
              <div class="admin-item-title">🏫 ${esc(s.name)}${s.number?' ('+esc(s.number)+')':''}</div>
            </div>
            <button class="btn-sm btn-delete" onclick="adminDelSch(${s.id})">🗑️</button>
          </div>`).join('')
            : '<p class="empty">Maktab yo\'q</p>';
    } catch(e) {}
}

async function adminSaveSchool() {
    if (!adminSelDistrictId) { toast('Tuman tanlang','error'); return; }
    const name = el('newSchoolName').value.trim();
    const number = el('newSchoolNumber').value.trim();
    if (!name) { toast('Nom kiriting','error'); return; }
    try {
        loading(true);
        await adminAddSchool({ name, number, districtId: adminSelDistrictId });
        el('newSchoolName').value = ''; el('newSchoolNumber').value = '';
        toast('Maktab qo\'shildi ✅','success');
        await adminLoadSchoolList(adminSelDistrictId);
    } catch(e) { toast('Xatolik','error'); } finally { loading(false); }
}

async function adminDelReg(id) {
    if (!confirm("O'chirishni tasdiqlaysizmi? Barcha tumanlar ham o'chadi!")) return;
    await adminDelRegion(id); adminLoadSchools(); toast("O'chirildi",'success');
}
async function adminDelDist(id) {
    if (!confirm("O'chirishni tasdiqlaysizmi?")) return;
    await adminDelDistrict(id); adminLoadDistrictList(adminSelRegionId,''); toast("O'chirildi",'success');
}
async function adminDelSch(id) {
    if (!confirm("O'chirishni tasdiqlaysizmi?")) return;
    await adminDelSchool(id); adminLoadSchoolList(adminSelDistrictId); toast("O'chirildi",'success');
}