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

const sendVerifyCode = d => api('POST','/auth/send-code',d);
const authRegister   = d => api('POST','/auth/register',d);
const authLogin    = d => api('POST','/auth/login',d);
const getSubjects  = () => api('GET','/subjects');
const getChapters  = id => api('GET',`/subjects/${id}/chapters`);
const getTopics    = id => api('GET',`/chapters/${id}/topics`);
const getTopicQ    = id => api('GET',`/topics/${id}/questions`);
const getChapterQ  = id => api('GET',`/chapters/${id}/questions`);
const submitTopicT = d  => api('POST','/tests/topic/submit',d);
const submitChapT  = d  => api('POST','/tests/chapter/submit',d);
const getMyResults = () => api('GET','/tests/my-results');
const saveProgress = (id,d) => api('POST',`/video/progress/${id}`,d);

const adminCreateSubject  = d   => api('POST','/admin/subjects',d);
const adminDeleteSubject  = id  => api('DELETE',`/admin/subjects/${id}`);
const adminCreateChapter  = d   => api('POST','/admin/chapters',d);
const adminDeleteChapter  = id  => api('DELETE',`/admin/chapters/${id}`);
const adminCreateTopic    = d   => api('POST','/admin/topics',d);
const adminUpdateTopic    = (id,d) => api('PUT',`/admin/topics/${id}`,d);
const adminDeleteTopic    = id  => api('DELETE',`/admin/topics/${id}`);
const adminGetResults     = ()  => api('GET','/admin/results/masked');
const adminGetUsers       = ()  => api('GET','/admin/users');
async function adminImportTopicQ(topicId, file) { const fd=new FormData(); fd.append('file',file); return api('POST',`/admin/import/topic-questions/${topicId}`,fd,true); }
async function adminImportChapterQ(chapterId, file) { const fd=new FormData(); fd.append('file',file); return api('POST',`/admin/import/chapter-questions/${chapterId}`,fd,true); }

const getAiAnalysis = () => api('GET','/ai/analyze');
const getAiVideos   = () => api('GET','/ai/video-recommendations');

const getMyProfile   = ()     => api('GET', '/profile/me');
const deleteAvatar   = ()     => api('DELETE', '/profile/avatar');
async function uploadAvatar(file) {
    const fd = new FormData();
    fd.append('file', file);
    return api('POST', '/profile/avatar', fd, true);
}

// Location
const getRegions      = ()         => api('GET', '/location/regions');
const getDistricts    = id         => api('GET', `/location/districts/${id}`);
const getSchools      = id         => api('GET', `/location/schools/${id}`);
const selectSchool    = d          => api('POST', '/location/select-school', d);
const getMySchool     = ()         => api('GET', '/location/my-school');
const getStudyRecs    = ()         => api('GET', '/ai/study-recommendations');

// Admin location
const adminAddRegion    = d        => api('POST', '/admin/location/regions', d);
const adminAddDistrict  = d        => api('POST', '/admin/location/districts', d);
const adminAddSchool    = d        => api('POST', '/admin/location/schools', d);
const adminDelRegion    = id       => api('DELETE', `/admin/location/regions/${id}`);
const adminDelDistrict  = id       => api('DELETE', `/admin/location/districts/${id}`);
const adminDelSchool    = id       => api('DELETE', `/admin/location/schools/${id}`);
const adminGetRegions   = ()       => api('GET', '/admin/location/regions');
const adminGetDistricts = id       => api('GET', `/admin/location/districts/${id}`);
const adminGetSchools   = id       => api('GET', `/admin/location/schools/${id}`);