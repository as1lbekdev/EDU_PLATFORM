const API_BASE = 'https://edu-platform-1.onrender.com';
let authToken = localStorage.getItem('token');

async function api(method, path, body = null, isForm = false) {
    const headers = {};
    if (authToken) headers['Authorization'] = `Bearer ${authToken}`;
    if (!isForm && body) headers['Content-Type'] = 'application/json';
    const opts = { method, headers, mode: 'cors', credentials: 'omit' };
    if (body) opts.body = isForm ? body : JSON.stringify(body);
    const res = await fetch(API_BASE + path, opts);
    if (!res.ok) { const t = await res.text(); throw new Error(t || `HTTP ${res.status}`); }
    const ct = res.headers.get('content-type');
    if (ct && ct.includes('application/json')) return res.json();
    return null;
}

const sendVerifyCode    = d => api('POST','/api/auth/send-code',d);
const forgotPassword    = d => api('POST','/api/auth/forgot-password',d);
const verifyResetCode   = d => api('POST','/api/auth/verify-reset-code',d);   // ✅ tuzatildi: api// → /api/
const resetPassword     = d => api('POST','/api/auth/reset-password',d);
const authRegister      = d => api('POST','/api/auth/register',d);
const authLogin         = d => api('POST','/api/auth/login',d);
const getSubjects       = () => api('GET','/api/subjects');                    // ✅ /api/ qo'shildi
const getChapters       = id => api('GET',`/api/subjects/${id}/chapters`);
const getTopics         = id => api('GET',`/api/chapters/${id}/topics`);
const getTopicQ         = id => api('GET',`/api/topics/${id}/questions`);
const getChapterQ       = id => api('GET',`/api/chapters/${id}/questions`);
const submitTopicT      = d  => api('POST','/api/tests/topic/submit',d);
const submitChapT       = d  => api('POST','/api/tests/chapter/submit',d);
const getMyResults      = () => api('GET','/api/tests/my-results');
const saveProgress      = (id,d) => api('POST',`/api/video/progress/${id}`,d);

const adminCreateSubject  = d      => api('POST','/api/admin/subjects',d);
const adminDeleteSubject  = id     => api('DELETE',`/api/admin/subjects/${id}`);
const adminCreateChapter  = d      => api('POST','/api/admin/chapters',d);
const adminDeleteChapter  = id     => api('DELETE',`/api/admin/chapters/${id}`);
const adminCreateTopic    = d      => api('POST','/api/admin/topics',d);
const adminUpdateTopic    = (id,d) => api('PUT',`/api/admin/topics/${id}`,d);
const adminDeleteTopic    = id     => api('DELETE',`/api/admin/topics/${id}`);
const adminGetResults     = ()     => api('GET','/api/admin/results/masked');
const adminGetUsers       = ()     => api('GET','/api/admin/users');
async function adminImportTopicQ(topicId, file) { const fd=new FormData(); fd.append('file',file); return api('POST',`/api/admin/import/topic-questions/${topicId}`,fd,true); }   // ✅
async function adminImportChapterQ(chapterId, file) { const fd=new FormData(); fd.append('file',file); return api('POST',`/api/admin/import/chapter-questions/${chapterId}`,fd,true); }  // ✅

const getAiAnalysis = () => api('GET','/api/ai/analyze');                      // ✅
const getAiVideos   = () => api('GET','/api/ai/video-recommendations');        // ✅

const getMyProfile  = ()  => api('GET','/api/profile/me');                     // ✅
const deleteAvatar  = ()  => api('DELETE','/api/profile/avatar');              // ✅
async function uploadAvatar(file) {
    const fd = new FormData();
    fd.append('file', file);
    return api('POST', '/api/profile/avatar', fd, true);                       // ✅
}

const getRegions      = ()    => api('GET','/api/location/regions');           // ✅
const getDistricts    = id    => api('GET',`/api/location/districts/${id}`);   // ✅
const getSchools      = id    => api('GET',`/api/location/schools/${id}`);     // ✅
const selectSchool    = d     => api('POST','/api/location/select-school',d);  // ✅
const getMySchool     = ()    => api('GET','/api/location/my-school');         // ✅
const getStudyRecs    = ()    => api('GET','/api/ai/study-recommendations');   // ✅

const adminAddRegion    = d   => api('POST','/api/admin/location/regions',d);
const adminAddDistrict  = d   => api('POST','/api/admin/location/districts',d);
const adminAddSchool    = d   => api('POST','/api/admin/location/schools',d);
const adminDelRegion    = id  => api('DELETE',`/api/admin/location/regions/${id}`);
const adminDelDistrict  = id  => api('DELETE',`/api/admin/location/districts/${id}`);
const adminDelSchool    = id  => api('DELETE',`/api/admin/location/schools/${id}`);
const adminGetRegions   = ()  => api('GET','/api/admin/location/regions');
const adminGetDistricts = id  => api('GET',`/api/admin/location/districts/${id}`);
const adminGetSchools   = id  => api('GET',`/api/admin/location/schools/${id}`);