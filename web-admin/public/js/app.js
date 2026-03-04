/* ====== 我们的小账本 - 前端逻辑 ====== */

const API = '/api';
let currentMonth = (() => { const d = new Date(); return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`; })();
let accountPage = 1, diaryPage = 1, meetingPage = 1, logPage = 1;
let confirmResolve = null;

const EXPENSE_CATS = ['餐饮','交通','购物','住房','通讯','娱乐','医疗','教育','服饰','更多'];
const INCOME_CATS = ['工资','奖金','理财','兼职','其他'];

/* ====== 导航 ====== */
document.querySelectorAll('.nav-item').forEach(item => {
  item.addEventListener('click', () => {
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    item.classList.add('active');
    const page = item.dataset.page;
    document.querySelectorAll('.page-section').forEach(s => s.classList.remove('active'));
    document.getElementById(`page-${page}`).classList.add('active');
    if (page === 'dashboard') loadDashboard();
    else if (page === 'accounts') loadAccounts();
    else if (page === 'diaries') loadDiaries();
    else if (page === 'meetings') loadMeetings();
    else if (page === 'settings') loadSettings();
    else if (page === 'logs') loadLogs();
    else if (page === 'locations') loadLocations();
    else if (page === 'appupdate') loadVersions();
  });
});

/* ====== API 请求 ====== */
async function api(path, options = {}) {
  const url = API + path;
  const config = { headers: { 'Content-Type': 'application/json' }, credentials: 'same-origin', ...options };
  if (config.body && typeof config.body === 'object') config.body = JSON.stringify(config.body);
  const res = await fetch(url, config);
  if (res.status === 401 && !path.startsWith('/auth/')) {
    showLoginPage();
    return { success: false, message: '未登录' };
  }
  return res.json();
}

/* ====== Toast 通知 ====== */
function toast(msg, type = 'success') {
  const icons = { success: '✅', error: '❌', info: '💕' };
  const c = document.getElementById('toast-container');
  const el = document.createElement('div');
  el.className = `toast toast-${type}`;
  el.innerHTML = `${icons[type] || ''} ${msg}`;
  c.appendChild(el);
  setTimeout(() => el.remove(), 3000);
}

/* ====== 防抖 ====== */
let debounceTimer;
function debounce(fn, delay) {
  return function(...args) {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => fn(...args), delay);
  };
}

/* ====== 模态框 ====== */
function openModal(id) { document.getElementById(id).classList.add('show'); }
function closeModal(id) { document.getElementById(id).classList.remove('show'); }

/* ====== 确认框 ====== */
function showConfirm(text) {
  return new Promise(resolve => {
    confirmResolve = resolve;
    document.getElementById('confirm-text').textContent = text;
    document.getElementById('confirm-dialog').classList.add('show');
  });
}
function closeConfirm(result) {
  document.getElementById('confirm-dialog').classList.remove('show');
  if (confirmResolve) { confirmResolve(result); confirmResolve = null; }
}

/* ====== 格式化 ====== */
function fmtMoney(n) { return '¥' + Number(n).toFixed(2); }
function fmtDate(d) { return d ? d.slice(0, 10) : ''; }
function truncate(s, len = 40) { return s && s.length > len ? s.slice(0, len) + '...' : (s || ''); }
function fmtTime(ts) {
  if (!ts) return '';
  const d = new Date(ts);
  return `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
}

/* ====================================================================
   仪表盘
   ==================================================================== */
async function loadDashboard() {
  const res = await api('/dashboard');
  if (!res.success) return;
  const d = res.data;

  document.getElementById('dashboard-stats').innerHTML = `
    <div class="stat-card">
      <div class="stat-icon">📒</div>
      <div class="stat-value">${d.counts.accounts}</div>
      <div class="stat-label">记账记录</div>
    </div>
    <div class="stat-card">
      <div class="stat-icon">📖</div>
      <div class="stat-value">${d.counts.diaries}</div>
      <div class="stat-label">日记篇数</div>
    </div>
    <div class="stat-card">
      <div class="stat-icon">📋</div>
      <div class="stat-value">${d.counts.meetings}</div>
      <div class="stat-label">会议纪要</div>
    </div>
    <div class="stat-card income">
      <div class="stat-icon">📈</div>
      <div class="stat-value">${fmtMoney(d.month.income)}</div>
      <div class="stat-label">本月收入</div>
    </div>
    <div class="stat-card expense">
      <div class="stat-icon">📉</div>
      <div class="stat-value">${fmtMoney(d.month.expense)}</div>
      <div class="stat-label">本月支出</div>
    </div>
    <div class="stat-card balance">
      <div class="stat-icon">💰</div>
      <div class="stat-value">${fmtMoney(d.month.balance)}</div>
      <div class="stat-label">本月结余</div>
    </div>
  `;

  // recent accounts
  const ra = document.getElementById('recent-accounts');
  if (d.recent.accounts.length === 0) {
    ra.innerHTML = '<div class="empty-state"><div class="empty-icon">💰</div><p>暂无记账记录</p></div>';
  } else {
    ra.innerHTML = d.recent.accounts.map(a => `
      <li class="recent-item">
        <div class="item-info">
          <div class="item-icon ${a.type === '收入' ? 'income-bg' : 'expense-bg'}">${a.type === '收入' ? '📈' : '📉'}</div>
          <div>
            <div class="item-title">${a.category}${a.note ? ' · ' + truncate(a.note, 15) : ''}</div>
            <div class="item-date">${fmtDate(a.date)}</div>
          </div>
        </div>
        <div class="item-amount ${a.type === '收入' ? 'amount-income' : 'amount-expense'}">${a.type === '收入' ? '+' : '-'}${fmtMoney(a.amount)}</div>
      </li>
    `).join('');
  }

  // recent diaries
  const rd = document.getElementById('recent-diaries');
  if (d.recent.diaries.length === 0) {
    rd.innerHTML = '<div class="empty-state"><div class="empty-icon">📖</div><p>暂无日记</p></div>';
  } else {
    rd.innerHTML = d.recent.diaries.map(di => `
      <li class="recent-item">
        <div class="item-info">
          <div class="item-icon diary-bg">📖</div>
          <div>
            <div class="item-title">${di.title}${di.mood ? ' ' + di.mood : ''}</div>
            <div class="item-date">${fmtDate(di.date)}</div>
          </div>
        </div>
      </li>
    `).join('');
  }

  // recent meetings
  const rm = document.getElementById('recent-meetings');
  if (d.recent.meetings.length === 0) {
    rm.innerHTML = '<div class="empty-state"><div class="empty-icon">📋</div><p>暂无会议</p></div>';
  } else {
    rm.innerHTML = d.recent.meetings.map(m => `
      <li class="recent-item">
        <div class="item-info">
          <div class="item-icon meeting-bg">📋</div>
          <div>
            <div class="item-title">${m.topic}</div>
            <div class="item-date">${fmtDate(m.date)}</div>
          </div>
        </div>
      </li>
    `).join('');
  }
}

/* ====================================================================
   记账管理
   ==================================================================== */
function updateMonthLabel() {
  document.getElementById('account-month').textContent = currentMonth;
}

function changeMonth(delta) {
  const [y, m] = currentMonth.split('-').map(Number);
  const d = new Date(y, m - 1 + delta, 1);
  currentMonth = `${d.getFullYear()}-${String(d.getMonth()+1).padStart(2,'0')}`;
  updateMonthLabel();
  loadAccounts();
}

async function loadAccounts() {
  updateMonthLabel();
  const search = document.getElementById('account-search').value;
  const type = document.getElementById('account-type-filter').value;
  const params = new URLSearchParams({ month: currentMonth, page: accountPage, limit: 20 });
  if (search) params.set('search', search);
  if (type) params.set('type', type);

  // load stats
  const statsRes = await api(`/accounts/stats?month=${currentMonth}`);
  if (statsRes.success) {
    const s = statsRes.data;
    document.getElementById('account-stats').innerHTML = `
      <div class="stat-card income"><div class="stat-icon">📈</div><div class="stat-value">${fmtMoney(s.income)}</div><div class="stat-label">本月收入</div></div>
      <div class="stat-card expense"><div class="stat-icon">📉</div><div class="stat-value">${fmtMoney(s.expense)}</div><div class="stat-label">本月支出</div></div>
      <div class="stat-card balance"><div class="stat-icon">💰</div><div class="stat-value">${fmtMoney(s.balance)}</div><div class="stat-label">本月结余</div></div>
    `;
  }

  const res = await api(`/accounts?${params}`);
  if (!res.success) return;
  const tbody = document.getElementById('accounts-tbody');
  if (res.data.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6"><div class="empty-state"><div class="empty-icon">💰</div><p>本月暂无记账记录</p></div></td></tr>`;
  } else {
    tbody.innerHTML = res.data.map(a => `
      <tr>
        <td>${fmtDate(a.date)}</td>
        <td><span class="tag ${a.type === '收入' ? 'tag-income' : 'tag-expense'}">${a.type}</span></td>
        <td>${a.category}</td>
        <td class="${a.type === '收入' ? 'amount-income' : 'amount-expense'}">${a.type === '收入' ? '+' : '-'}${fmtMoney(a.amount)}</td>
        <td>${truncate(a.note, 20)}</td>
        <td class="action-btns">
          <button class="btn btn-sm btn-outline" onclick="editAccount(${a.id})">✏️</button>
          <button class="btn btn-sm btn-danger" onclick="deleteAccount(${a.id})">🗑️</button>
        </td>
      </tr>
    `).join('');
  }
  renderPagination('accounts-pagination', res.total, accountPage, 20, p => { accountPage = p; loadAccounts(); });
}

function updateCategoryOptions() {
  const type = document.getElementById('account-type').value;
  const cats = type === '收入' ? INCOME_CATS : EXPENSE_CATS;
  document.getElementById('account-category').innerHTML = cats.map(c => `<option value="${c}">${c}</option>`).join('');
}

function showAccountModal(data = null) {
  document.getElementById('account-modal-title').textContent = data ? '编辑记账' : '新增记账';
  document.getElementById('account-edit-id').value = data ? data.id : '';
  document.getElementById('account-date').value = data ? fmtDate(data.date) : new Date().toISOString().slice(0,10);
  document.getElementById('account-type').value = data ? data.type : '支出';
  updateCategoryOptions();
  if (data) document.getElementById('account-category').value = data.category;
  document.getElementById('account-amount').value = data ? data.amount : '';
  document.getElementById('account-note').value = data ? (data.note || '') : '';
  openModal('account-modal');
}

async function editAccount(id) {
  const res = await api(`/accounts?page=1&limit=9999`);
  if (!res.success) return;
  const item = res.data.find(a => a.id === id);
  if (item) showAccountModal(item);
}

async function saveAccount() {
  const id = document.getElementById('account-edit-id').value;
  const body = {
    date: document.getElementById('account-date').value,
    type: document.getElementById('account-type').value,
    category: document.getElementById('account-category').value,
    amount: parseFloat(document.getElementById('account-amount').value) || 0,
    note: document.getElementById('account-note').value
  };
  if (!body.date || !body.category || body.amount <= 0) return toast('请填写完整信息', 'error');
  const res = id ? await api(`/accounts/${id}`, { method: 'PUT', body }) : await api('/accounts', { method: 'POST', body });
  if (res.success) { toast(id ? '修改成功' : '添加成功'); closeModal('account-modal'); loadAccounts(); }
  else toast(res.message, 'error');
}

async function deleteAccount(id) {
  if (await showConfirm('确定要删除这条记账记录吗？')) {
    const res = await api(`/accounts/${id}`, { method: 'DELETE' });
    if (res.success) { toast('删除成功'); loadAccounts(); } else toast(res.message, 'error');
  }
}

/* ====================================================================
   日记管理
   ==================================================================== */
async function loadDiaries() {
  const search = document.getElementById('diary-search').value;
  const params = new URLSearchParams({ page: diaryPage, limit: 20 });
  if (search) params.set('search', search);

  const res = await api(`/diaries?${params}`);
  if (!res.success) return;
  const tbody = document.getElementById('diaries-tbody');
  if (res.data.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7"><div class="empty-state"><div class="empty-icon">📖</div><p>暂无日记</p></div></td></tr>`;
  } else {
    tbody.innerHTML = res.data.map(d => `
      <tr>
        <td>${fmtDate(d.date)}</td>
        <td><strong>${d.title}</strong></td>
        <td>${d.weather || '-'}</td>
        <td>${d.mood || '-'}</td>
        <td>${truncate(d.content, 30)}</td>
        <td>${d.tags ? d.tags.split(',').map(t => `<span class="tag tag-default">${t.trim()}</span>`).join(' ') : '-'}</td>
        <td class="action-btns">
          <button class="btn btn-sm btn-outline" onclick="editDiary(${d.id})">✏️</button>
          <button class="btn btn-sm btn-danger" onclick="deleteDiary(${d.id})">🗑️</button>
        </td>
      </tr>
    `).join('');
  }
  renderPagination('diaries-pagination', res.total, diaryPage, 20, p => { diaryPage = p; loadDiaries(); });
}

function showDiaryModal(data = null) {
  document.getElementById('diary-modal-title').textContent = data ? '编辑日记' : '新增日记';
  document.getElementById('diary-edit-id').value = data ? data.id : '';
  document.getElementById('diary-date').value = data ? fmtDate(data.date) : new Date().toISOString().slice(0,10);
  document.getElementById('diary-title').value = data ? data.title : '';
  document.getElementById('diary-weather').value = data ? (data.weather || '') : '';
  document.getElementById('diary-mood').value = data ? (data.mood || '') : '';
  document.getElementById('diary-tags').value = data ? (data.tags || '') : '';
  document.getElementById('diary-content').value = data ? (data.content || '') : '';
  openModal('diary-modal');
}

async function editDiary(id) {
  const res = await api(`/diaries?page=1&limit=9999`);
  if (!res.success) return;
  const item = res.data.find(d => d.id === id);
  if (item) showDiaryModal(item);
}

async function saveDiary() {
  const id = document.getElementById('diary-edit-id').value;
  const body = {
    date: document.getElementById('diary-date').value,
    title: document.getElementById('diary-title').value,
    content: document.getElementById('diary-content').value,
    weather: document.getElementById('diary-weather').value,
    mood: document.getElementById('diary-mood').value,
    tags: document.getElementById('diary-tags').value
  };
  if (!body.date || !body.title) return toast('请填写日期和标题', 'error');
  const res = id ? await api(`/diaries/${id}`, { method: 'PUT', body }) : await api('/diaries', { method: 'POST', body });
  if (res.success) { toast(id ? '修改成功' : '添加成功'); closeModal('diary-modal'); loadDiaries(); }
  else toast(res.message, 'error');
}

async function deleteDiary(id) {
  if (await showConfirm('确定要删除这篇日记吗？')) {
    const res = await api(`/diaries/${id}`, { method: 'DELETE' });
    if (res.success) { toast('删除成功'); loadDiaries(); } else toast(res.message, 'error');
  }
}

/* ====================================================================
   会议管理
   ==================================================================== */
async function loadMeetings() {
  const search = document.getElementById('meeting-search').value;
  const params = new URLSearchParams({ page: meetingPage, limit: 20 });
  if (search) params.set('search', search);

  const res = await api(`/meetings?${params}`);
  if (!res.success) return;
  const tbody = document.getElementById('meetings-tbody');
  if (res.data.length === 0) {
    tbody.innerHTML = `<tr><td colspan="7"><div class="empty-state"><div class="empty-icon">📋</div><p>暂无会议纪要</p></div></td></tr>`;
  } else {
    tbody.innerHTML = res.data.map(m => `
      <tr>
        <td>${fmtDate(m.date)}</td>
        <td><strong>${m.topic}</strong></td>
        <td>${m.start_time || '-'}${m.end_time ? ' ~ ' + m.end_time : ''}</td>
        <td>${m.location || '-'}</td>
        <td>${truncate(m.attendees, 20) || '-'}</td>
        <td>${m.tags ? m.tags.split(',').map(t => `<span class="tag tag-purple">${t.trim()}</span>`).join(' ') : '-'}</td>
        <td class="action-btns">
          <button class="btn btn-sm btn-outline" onclick="editMeeting(${m.id})">✏️</button>
          <button class="btn btn-sm btn-danger" onclick="deleteMeeting(${m.id})">🗑️</button>
        </td>
      </tr>
    `).join('');
  }
  renderPagination('meetings-pagination', res.total, meetingPage, 20, p => { meetingPage = p; loadMeetings(); });
}

function showMeetingModal(data = null) {
  document.getElementById('meeting-modal-title').textContent = data ? '编辑会议' : '新增会议';
  document.getElementById('meeting-edit-id').value = data ? data.id : '';
  document.getElementById('meeting-date').value = data ? fmtDate(data.date) : new Date().toISOString().slice(0,10);
  document.getElementById('meeting-topic').value = data ? data.topic : '';
  document.getElementById('meeting-start').value = data ? (data.start_time || '') : '';
  document.getElementById('meeting-end').value = data ? (data.end_time || '') : '';
  document.getElementById('meeting-location').value = data ? (data.location || '') : '';
  document.getElementById('meeting-attendees').value = data ? (data.attendees || '') : '';
  document.getElementById('meeting-tags').value = data ? (data.tags || '') : '';
  document.getElementById('meeting-content').value = data ? (data.content || '') : '';
  document.getElementById('meeting-todos').value = data ? (data.todo_items || '') : '';
  openModal('meeting-modal');
}

async function editMeeting(id) {
  const res = await api(`/meetings?page=1&limit=9999`);
  if (!res.success) return;
  const item = res.data.find(m => m.id === id);
  if (item) showMeetingModal(item);
}

async function saveMeeting() {
  const id = document.getElementById('meeting-edit-id').value;
  const body = {
    date: document.getElementById('meeting-date').value,
    topic: document.getElementById('meeting-topic').value,
    start_time: document.getElementById('meeting-start').value,
    end_time: document.getElementById('meeting-end').value,
    location: document.getElementById('meeting-location').value,
    attendees: document.getElementById('meeting-attendees').value,
    content: document.getElementById('meeting-content').value,
    todo_items: document.getElementById('meeting-todos').value,
    tags: document.getElementById('meeting-tags').value
  };
  if (!body.date || !body.topic) return toast('请填写日期和主题', 'error');
  const res = id ? await api(`/meetings/${id}`, { method: 'PUT', body }) : await api('/meetings', { method: 'POST', body });
  if (res.success) { toast(id ? '修改成功' : '添加成功'); closeModal('meeting-modal'); loadMeetings(); }
  else toast(res.message, 'error');
}

async function deleteMeeting(id) {
  if (await showConfirm('确定要删除这条会议纪要吗？')) {
    const res = await api(`/meetings/${id}`, { method: 'DELETE' });
    if (res.success) { toast('删除成功'); loadMeetings(); } else toast(res.message, 'error');
  }
}

/* ====================================================================
   系统设置
   ==================================================================== */
async function loadSettings() {
  const res = await api('/settings');
  if (!res.success) return;
  const s = res.data;
  document.getElementById('settings-content').innerHTML = `
    <div class="settings-group">
      <h4>💕 情侣信息</h4>
      <div class="settings-item">
        <span class="setting-label">称呼 1</span>
        <div class="setting-value"><input type="text" data-key="couple_name_1" value="${s.couple_name_1 || ''}"></div>
      </div>
      <div class="settings-item">
        <span class="setting-label">称呼 2</span>
        <div class="setting-value"><input type="text" data-key="couple_name_2" value="${s.couple_name_2 || ''}"></div>
      </div>
      <div class="settings-item">
        <span class="setting-label">纪念日</span>
        <div class="setting-value"><input type="date" data-key="anniversary_date" value="${s.anniversary_date || ''}"></div>
      </div>
    </div>
    <div class="settings-group">
      <h4>🎨 应用设置</h4>
      <div class="settings-item">
        <span class="setting-label">应用名称</span>
        <div class="setting-value"><input type="text" data-key="app_name" value="${s.app_name || ''}"></div>
      </div>
      <div class="settings-item">
        <span class="setting-label">主题颜色</span>
        <div class="setting-value"><input type="color" data-key="theme_color" value="${s.theme_color || '#E8729A'}" style="width:60px;height:32px;padding:2px;border:1.5px solid #ddd;border-radius:6px;cursor:pointer"></div>
      </div>
      <div class="settings-item">
        <span class="setting-label">自动备份</span>
        <div class="setting-value">
          <select data-key="auto_backup" style="padding:6px 12px;border:1.5px solid #ddd;border-radius:6px;font-size:13px;font-family:inherit">
            <option value="true" ${s.auto_backup === 'true' ? 'selected' : ''}>开启</option>
            <option value="false" ${s.auto_backup !== 'true' ? 'selected' : ''}>关闭</option>
          </select>
        </div>
      </div>
    </div>
    <div class="settings-group">
      <h4>📡 同步信息</h4>
      <div class="settings-item">
        <span class="setting-label">API 地址</span>
        <div class="setting-value" style="font-size:13px;color:#9E9E9E">${window.location.origin}/api</div>
      </div>
      <div class="settings-item">
        <span class="setting-label">ngrok 域名</span>
        <div class="setting-value" style="font-size:13px;color:#9E9E9E">resistive-diotic-jolie.ngrok-free.dev</div>
      </div>
    </div>
    <div class="settings-group">
      <h4>🔐 账号安全</h4>
      <div class="settings-item">
        <span class="setting-label">修改密码</span>
        <div class="setting-value">
          <button class="btn btn-outline btn-sm" onclick="openModal('password-modal')">🔑 修改密码</button>
        </div>
      </div>
    </div>
  `;
}

async function saveSettings() {
  const inputs = document.querySelectorAll('#settings-content [data-key]');
  const body = {};
  inputs.forEach(el => {
    const key = el.dataset.key;
    body[key] = el.type === 'checkbox' ? (el.checked ? 'true' : 'false') : el.value;
  });
  const res = await api('/settings', { method: 'PUT', body });
  if (res.success) toast('设置已保存'); else toast(res.message, 'error');
}

/* ====== APK 版本管理 ====== */
async function uploadApk() {
  const fileInput = document.getElementById('apk-file');
  const versionCode = document.getElementById('apk-version-code').value;
  const versionName = document.getElementById('apk-version-name').value;
  const changelog = document.getElementById('apk-changelog').value;

  if (!fileInput.files.length) return toast('请选择 APK 文件', 'error');
  if (!versionCode || !versionName) return toast('请填写版本号', 'error');

  const formData = new FormData();
  formData.append('apk', fileInput.files[0]);
  formData.append('versionCode', versionCode);
  formData.append('versionName', versionName);
  formData.append('changelog', changelog);

  toast('正在上传...', 'info');
  try {
    const resp = await fetch(`${API}/app/upload`, { method: 'POST', body: formData, credentials: 'include' });
    const res = await resp.json();
    if (res.success) {
      toast(res.message || '上传成功');
      fileInput.value = '';
      document.getElementById('apk-version-code').value = '';
      document.getElementById('apk-version-name').value = '';
      document.getElementById('apk-changelog').value = '';
      loadVersions();
    } else {
      toast(res.message || '上传失败', 'error');
    }
  } catch (e) {
    toast('上传失败: ' + e.message, 'error');
  }
}

async function loadVersions() {
  try {
    const res = await api('/app/versions');
    if (!res.success) return;
    const tbody = document.getElementById('versions-tbody');
    if (!res.data || res.data.length === 0) {
      tbody.innerHTML = '<tr><td colspan="4" style="text-align:center;color:var(--text-hint);padding:20px">暂无版本</td></tr>';
      return;
    }
    tbody.innerHTML = res.data.map(v => `
      <tr>
        <td>v${v.version_name}</td>
        <td>${v.version_code}</td>
        <td>${v.changelog || '-'}</td>
        <td>${fmtTime(v.created_at)}</td>
      </tr>
    `).join('');
  } catch (e) { console.error('加载版本失败:', e); }
}

/* ====================================================================
   操作日志
   ==================================================================== */
async function loadLogs() {
  const module = document.getElementById('log-module-filter').value;
  const params = new URLSearchParams({ page: logPage, limit: 30 });
  if (module) params.set('module', module);

  const res = await api(`/logs?${params}`);
  if (!res.success) return;
  const tbody = document.getElementById('logs-tbody');
  if (res.data.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5"><div class="empty-state"><div class="empty-icon">📝</div><p>暂无操作日志</p></div></td></tr>`;
  } else {
    tbody.innerHTML = res.data.map(l => `
      <tr>
        <td style="white-space:nowrap">${fmtTime(l.created_at)}</td>
        <td><span class="log-action ${l.action}">${l.action}</span></td>
        <td>${l.module}</td>
        <td>${truncate(l.detail, 40)}</td>
        <td style="color:#9E9E9E;font-size:12px">${l.ip_address || '-'}</td>
      </tr>
    `).join('');
  }
  renderPagination('logs-pagination', res.total, logPage, 30, p => { logPage = p; loadLogs(); });
}

/* ====================================================================
   位置追踪
   ==================================================================== */
async function loadLocations() {
  try {
    const res = await api('/locations?limit=50');
    if (!res.success) return;

    // 最新位置卡片
    if (res.latest) {
      const l = res.latest;
      document.getElementById('latest-address').textContent = l.address || '未知位置';
      document.getElementById('latest-time').textContent = fmtTime(l.created_at);
      document.getElementById('latest-device').textContent = l.device_name || '-';
      document.getElementById('latest-coords').textContent = `${l.longitude.toFixed(4)}, ${l.latitude.toFixed(4)}`;
    } else {
      document.getElementById('latest-address').textContent = '暂无数据';
    }

    // 位置列表
    const tbody = document.getElementById('locations-tbody');
    tbody.innerHTML = res.data.map(r => `
      <tr>
        <td>${fmtTime(r.created_at)}</td>
        <td>${r.device_name || '-'}</td>
        <td>${r.address || '-'}</td>
        <td>${r.longitude.toFixed(6)}</td>
        <td>${r.latitude.toFixed(6)}</td>
      </tr>
    `).join('');

    if (res.data.length === 0) {
      tbody.innerHTML = '<tr><td colspan="5" style="text-align:center;color:var(--text-hint);padding:40px">暂无位置记录，等待手机上报...</td></tr>';
    }
  } catch (e) {
    console.error('加载位置失败:', e);
  }
}

async function requestLocation() {
  try {
    const res = await api('/location/request', 'POST');
    if (res.success) {
      alert('📡 定位请求已发送！手机将在30秒内响应，稍后点击刷新查看。');
    }
  } catch (e) { alert('发送失败: ' + e.message); }
}

/* ====================================================================
   导出数据
   ==================================================================== */
async function exportAllData() {
  toast('正在导出...', 'info');
  const res = await api('/export');
  if (!res.success) return toast(res.message, 'error');
  const blob = new Blob([JSON.stringify(res.data, null, 2)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = `小账本备份_${new Date().toISOString().slice(0,10)}.json`;
  a.click();
  URL.revokeObjectURL(url);
  toast('导出成功！');
}

/* ====================================================================
   分页组件
   ==================================================================== */
function renderPagination(containerId, total, current, pageSize, onChange) {
  const totalPages = Math.ceil(total / pageSize);
  if (totalPages <= 1) { document.getElementById(containerId).innerHTML = ''; return; }
  const container = document.getElementById(containerId);
  let html = `<button ${current <= 1 ? 'disabled' : ''} onclick="void(0)">◀</button>`;
  
  const start = Math.max(1, current - 2);
  const end = Math.min(totalPages, current + 2);
  
  if (start > 1) html += `<button onclick="void(0)">1</button>`;
  if (start > 2) html += `<span class="page-info">...</span>`;
  
  for (let i = start; i <= end; i++) {
    html += `<button class="${i === current ? 'active' : ''}" onclick="void(0)">${i}</button>`;
  }
  
  if (end < totalPages - 1) html += `<span class="page-info">...</span>`;
  if (end < totalPages) html += `<button onclick="void(0)">${totalPages}</button>`;
  
  html += `<button ${current >= totalPages ? 'disabled' : ''} onclick="void(0)">▶</button>`;
  html += `<span class="page-info">共 ${total} 条</span>`;
  
  container.innerHTML = html;
  container.querySelectorAll('button').forEach(btn => {
    btn.addEventListener('click', () => {
      const text = btn.textContent;
      if (text === '◀' && current > 1) onChange(current - 1);
      else if (text === '▶' && current < totalPages) onChange(current + 1);
      else if (!isNaN(Number(text))) onChange(Number(text));
    });
  });
}

/* ====== 登录相关 ====== */
function showLoginPage() {
  document.getElementById('login-overlay').style.display = 'flex';
  document.getElementById('app-container').style.display = 'none';
  document.getElementById('login-error').textContent = '';
  document.getElementById('login-username').value = '';
  document.getElementById('login-password').value = '';
}

function showAppPage() {
  const overlay = document.getElementById('login-overlay');
  overlay.classList.add('hiding');
  setTimeout(() => {
    overlay.style.display = 'none';
    overlay.classList.remove('hiding');
  }, 400);
  document.getElementById('app-container').style.display = '';
  loadDashboard();
}

async function doLogin() {
  const username = document.getElementById('login-username').value.trim();
  const password = document.getElementById('login-password').value;
  const errEl = document.getElementById('login-error');
  const btn = document.getElementById('login-btn');

  if (!username || !password) {
    errEl.textContent = '请输入用户名和密码';
    return;
  }

  errEl.textContent = '';
  btn.disabled = true;
  btn.querySelector('.login-btn-text').style.display = 'none';
  btn.querySelector('.login-btn-loading').style.display = '';

  try {
    const res = await api('/auth/login', { method: 'POST', body: { username, password } });
    if (res.success) {
      showAppPage();
    } else {
      errEl.textContent = res.message || '登录失败';
    }
  } catch (e) {
    errEl.textContent = '网络错误，请稍后重试';
  } finally {
    btn.disabled = false;
    btn.querySelector('.login-btn-text').style.display = '';
    btn.querySelector('.login-btn-loading').style.display = 'none';
  }
}

async function doLogout() {
  await api('/auth/logout', { method: 'POST' });
  showLoginPage();
  toast('已退出登录', 'info');
}

async function changePassword() {
  const oldPwd = document.getElementById('old-password').value;
  const newPwd = document.getElementById('new-password').value;
  const confirmPwd = document.getElementById('confirm-password').value;

  if (!oldPwd || !newPwd || !confirmPwd) return toast('请填写完整', 'error');
  if (newPwd !== confirmPwd) return toast('两次输入的新密码不一致', 'error');
  if (newPwd.length < 1) return toast('密码不能为空', 'error');

  const res = await api('/auth/password', { method: 'PUT', body: { oldPassword: oldPwd, newPassword: newPwd } });
  if (res.success) {
    toast('密码修改成功');
    closeModal('password-modal');
    document.getElementById('old-password').value = '';
    document.getElementById('new-password').value = '';
    document.getElementById('confirm-password').value = '';
  } else {
    toast(res.message || '修改失败', 'error');
  }
}

/* ====== 初始化 ====== */
document.addEventListener('DOMContentLoaded', async () => {
  // Enter 键登录
  document.getElementById('login-password').addEventListener('keydown', e => {
    if (e.key === 'Enter') doLogin();
  });
  document.getElementById('login-username').addEventListener('keydown', e => {
    if (e.key === 'Enter') document.getElementById('login-password').focus();
  });

  // 检查登录状态
  try {
    const res = await api('/auth/check');
    if (res.success && res.loggedIn) {
      showAppPage();
    } else {
      showLoginPage();
    }
  } catch (e) {
    showLoginPage();
  }
});
