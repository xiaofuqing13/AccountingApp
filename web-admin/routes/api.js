const express = require('express');
const router = express.Router();
const db = require('../db');
const crypto = require('crypto');

/* ========== 工具函数 ========== */
function formatDate(d) {
  if (!d) return '';
  const dt = new Date(d);
  if (isNaN(dt.getTime())) return d;
  return dt.toISOString().slice(0, 10);
}

function hashPassword(password) {
  return crypto.createHash('sha256').update(password).digest('hex');
}

async function log(action, module, detail, ip) {
  try {
    await db.query(
      'INSERT INTO operation_logs (action, module, detail, ip_address) VALUES (?,?,?,?)',
      [action, module, detail, ip || '']
    );
  } catch (e) { /* silent */ }
}

/* ========== 认证 ========== */
router.post('/auth/login', async (req, res) => {
  try {
    const { username, password } = req.body;
    if (!username || !password) {
      return res.status(400).json({ success: false, message: '请输入用户名和密码' });
    }
    const hashedPwd = hashPassword(password);
    const [rows] = await db.query(
      'SELECT id, username FROM admin_users WHERE username = ? AND password = ?',
      [username, hashedPwd]
    );
    if (rows.length === 0) {
      await log('LOGIN_FAIL', 'auth', `登录失败: ${username}`, req.ip);
      return res.status(401).json({ success: false, message: '用户名或密码错误' });
    }
    req.session.userId = rows[0].id;
    req.session.username = rows[0].username;
    await log('LOGIN', 'auth', `登录成功: ${username}`, req.ip);
    res.json({ success: true, username: rows[0].username });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

router.post('/auth/logout', (req, res) => {
  req.session.destroy();
  res.json({ success: true });
});

router.get('/auth/check', (req, res) => {
  if (req.session.userId) {
    res.json({ success: true, loggedIn: true, username: req.session.username });
  } else {
    res.json({ success: true, loggedIn: false });
  }
});

router.put('/auth/password', async (req, res) => {
  try {
    const { oldPassword, newPassword } = req.body;
    if (!oldPassword || !newPassword) {
      return res.status(400).json({ success: false, message: '请填写完整' });
    }
    const oldHash = hashPassword(oldPassword);
    const [rows] = await db.query(
      'SELECT id FROM admin_users WHERE id = ? AND password = ?',
      [req.session.userId, oldHash]
    );
    if (rows.length === 0) {
      return res.status(400).json({ success: false, message: '原密码错误' });
    }
    const newHash = hashPassword(newPassword);
    await db.query('UPDATE admin_users SET password = ? WHERE id = ?', [newHash, req.session.userId]);
    await log('UPDATE', 'auth', '修改密码', req.ip);
    res.json({ success: true });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

/* ========== 记账 CRUD ========== */
router.get('/accounts', async (req, res) => {
  try {
    const { month, type, category, search, page = 1, limit = 50 } = req.query;
    let sql = 'SELECT * FROM accounts WHERE 1=1';
    const params = [];
    if (month) { sql += ' AND DATE_FORMAT(date, "%Y-%m") = ?'; params.push(month); }
    if (type) { sql += ' AND type = ?'; params.push(type); }
    if (category) { sql += ' AND category = ?'; params.push(category); }
    if (search) { sql += ' AND (note LIKE ? OR category LIKE ?)'; params.push(`%${search}%`, `%${search}%`); }
    
    // 总数
    const [countRows] = await db.query(sql.replace('SELECT *', 'SELECT COUNT(*) as total'), params);
    const total = countRows[0].total;
    
    sql += ' ORDER BY date DESC, id DESC LIMIT ? OFFSET ?';
    params.push(Number(limit), (Number(page) - 1) * Number(limit));
    const [rows] = await db.query(sql, params);
    rows.forEach(r => r.date = formatDate(r.date));
    res.json({ success: true, data: rows, total, page: Number(page), limit: Number(limit) });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

router.get('/accounts/stats', async (req, res) => {
  try {
    const { month } = req.query;
    let where = '';
    const params = [];
    if (month) { where = 'WHERE DATE_FORMAT(date, "%Y-%m") = ?'; params.push(month); }
    
    const [summary] = await db.query(
      `SELECT type, SUM(amount) as total, COUNT(*) as count FROM accounts ${where} GROUP BY type`, params
    );
    const [byCategory] = await db.query(
      `SELECT category, type, SUM(amount) as total, COUNT(*) as count FROM accounts ${where} GROUP BY category, type ORDER BY total DESC`, params
    );
    const [monthly] = await db.query(
      `SELECT DATE_FORMAT(date, '%Y-%m') as month, type, SUM(amount) as total FROM accounts GROUP BY month, type ORDER BY month DESC LIMIT 24`
    );
    
    let income = 0, expense = 0;
    summary.forEach(r => { if (r.type === '收入') income = Number(r.total); else expense = Number(r.total); });
    
    res.json({ success: true, data: { income, expense, balance: income - expense, byCategory, monthly } });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

router.post('/accounts', async (req, res) => {
  try {
    const { date, type, category, amount, note = '', location = '' } = req.body;
    if (!date || !type || !category || amount === undefined) {
      return res.status(400).json({ success: false, message: '缺少必填字段' });
    }
    // 去重：同日期+类型+分类+金额+备注
    const [exist] = await db.query(
      'SELECT id FROM accounts WHERE date=? AND type=? AND category=? AND amount=? AND note=? LIMIT 1',
      [date, type, category, amount, note]
    );
    if (exist.length > 0) {
      return res.json({ success: true, id: exist[0].id, message: '记录已存在(去重)' });
    }
    const [result] = await db.query(
      'INSERT INTO accounts (date,type,category,amount,note,location) VALUES (?,?,?,?,?,?)',
      [date, type, category, amount, note, location]
    );
    await log('CREATE', 'account', `新增${type} ${category} ¥${amount}`, req.ip);
    res.json({ success: true, id: result.insertId });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

router.put('/accounts/:id', async (req, res) => {
  try {
    const { date, type, category, amount, note, location } = req.body;
    await db.query(
      'UPDATE accounts SET date=?,type=?,category=?,amount=?,note=?,location=? WHERE id=?',
      [date, type, category, amount, note || '', location || '', req.params.id]
    );
    await log('UPDATE', 'account', `修改记账#${req.params.id}`, req.ip);
    res.json({ success: true });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

router.delete('/accounts/:id', async (req, res) => {
  try {
    await db.query('DELETE FROM accounts WHERE id=?', [req.params.id]);
    await log('DELETE', 'account', `删除记账#${req.params.id}`, req.ip);
    res.json({ success: true });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

/* ========== 日记 CRUD ========== */
router.get('/diaries', async (req, res) => {
  try {
    const { search, mood, weather, page = 1, limit = 20 } = req.query;
    let sql = 'SELECT * FROM diaries WHERE 1=1';
    const params = [];
    if (search) { sql += ' AND (title LIKE ? OR content LIKE ? OR tags LIKE ?)'; params.push(`%${search}%`, `%${search}%`, `%${search}%`); }
    if (mood) { sql += ' AND mood = ?'; params.push(mood); }
    if (weather) { sql += ' AND weather = ?'; params.push(weather); }

    const [countRows] = await db.query(sql.replace('SELECT *', 'SELECT COUNT(*) as total'), params);
    const total = countRows[0].total;

    sql += ' ORDER BY date DESC, id DESC LIMIT ? OFFSET ?';
    params.push(Number(limit), (Number(page) - 1) * Number(limit));
    const [rows] = await db.query(sql, params);
    rows.forEach(r => r.date = formatDate(r.date));
    res.json({ success: true, data: rows, total, page: Number(page), limit: Number(limit) });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

router.post('/diaries', async (req, res) => {
  try {
    const { date, title, content = '', weather = '', mood = '', location = '', tags = '' } = req.body;
    if (!date || !title) return res.status(400).json({ success: false, message: '缺少必填字段' });
    // 去重：同日期+标题
    const [exist] = await db.query(
      'SELECT id FROM diaries WHERE date=? AND title=? LIMIT 1',
      [date, title]
    );
    if (exist.length > 0) {
      return res.json({ success: true, id: exist[0].id, message: '记录已存在(去重)' });
    }
    const [result] = await db.query(
      'INSERT INTO diaries (date,title,content,weather,mood,location,tags) VALUES (?,?,?,?,?,?,?)',
      [date, title, content, weather, mood, location, tags]
    );
    await log('CREATE', 'diary', `新增日记「${title}」`, req.ip);
    res.json({ success: true, id: result.insertId });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

router.put('/diaries/:id', async (req, res) => {
  try {
    const { date, title, content, weather, mood, location, tags } = req.body;
    await db.query(
      'UPDATE diaries SET date=?,title=?,content=?,weather=?,mood=?,location=?,tags=? WHERE id=?',
      [date, title, content || '', weather || '', mood || '', location || '', tags || '', req.params.id]
    );
    await log('UPDATE', 'diary', `修改日记#${req.params.id}`, req.ip);
    res.json({ success: true });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

router.delete('/diaries/:id', async (req, res) => {
  try {
    await db.query('DELETE FROM diaries WHERE id=?', [req.params.id]);
    await log('DELETE', 'diary', `删除日记#${req.params.id}`, req.ip);
    res.json({ success: true });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

/* ========== 会议纪要 CRUD ========== */
router.get('/meetings', async (req, res) => {
  try {
    const { search, tag, page = 1, limit = 20 } = req.query;
    let sql = 'SELECT * FROM meetings WHERE 1=1';
    const params = [];
    if (search) { sql += ' AND (topic LIKE ? OR content LIKE ? OR attendees LIKE ?)'; params.push(`%${search}%`, `%${search}%`, `%${search}%`); }
    if (tag) { sql += ' AND tags LIKE ?'; params.push(`%${tag}%`); }

    const [countRows] = await db.query(sql.replace('SELECT *', 'SELECT COUNT(*) as total'), params);
    const total = countRows[0].total;

    sql += ' ORDER BY date DESC, id DESC LIMIT ? OFFSET ?';
    params.push(Number(limit), (Number(page) - 1) * Number(limit));
    const [rows] = await db.query(sql, params);
    rows.forEach(r => r.date = formatDate(r.date));
    res.json({ success: true, data: rows, total, page: Number(page), limit: Number(limit) });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

router.post('/meetings', async (req, res) => {
  try {
    const { date, topic, start_time = '', end_time = '', location = '', attendees = '', content = '', todo_items = '', tags = '' } = req.body;
    if (!date || !topic) return res.status(400).json({ success: false, message: '缺少必填字段' });
    // 去重：同日期+主题
    const [exist] = await db.query(
      'SELECT id FROM meetings WHERE date=? AND topic=? LIMIT 1',
      [date, topic]
    );
    if (exist.length > 0) {
      return res.json({ success: true, id: exist[0].id, message: '记录已存在(去重)' });
    }
    const [result] = await db.query(
      'INSERT INTO meetings (date,topic,start_time,end_time,location,attendees,content,todo_items,tags) VALUES (?,?,?,?,?,?,?,?,?)',
      [date, topic, start_time, end_time, location, attendees, content, todo_items, tags]
    );
    await log('CREATE', 'meeting', `新增会议「${topic}」`, req.ip);
    res.json({ success: true, id: result.insertId });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

router.put('/meetings/:id', async (req, res) => {
  try {
    const { date, topic, start_time, end_time, location, attendees, content, todo_items, tags } = req.body;
    await db.query(
      'UPDATE meetings SET date=?,topic=?,start_time=?,end_time=?,location=?,attendees=?,content=?,todo_items=?,tags=? WHERE id=?',
      [date, topic, start_time || '', end_time || '', location || '', attendees || '', content || '', todo_items || '', tags || '', req.params.id]
    );
    await log('UPDATE', 'meeting', `修改会议#${req.params.id}`, req.ip);
    res.json({ success: true });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

router.delete('/meetings/:id', async (req, res) => {
  try {
    await db.query('DELETE FROM meetings WHERE id=?', [req.params.id]);
    await log('DELETE', 'meeting', `删除会议#${req.params.id}`, req.ip);
    res.json({ success: true });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

/* ========== Android 批量同步上传 ========== */
router.post('/sync/upload', async (req, res) => {
  try {
    const { accounts = [], diaries = [], meetings = [] } = req.body;
    let aCount = 0, dCount = 0, mCount = 0;
    let aSkip = 0, dSkip = 0, mSkip = 0;

    for (const a of accounts) {
      // 去重：同日期+类型+分类+金额视为重复
      const [exist] = await db.query(
        'SELECT id FROM accounts WHERE date=? AND type=? AND category=? AND amount=? LIMIT 1',
        [a.date, a.type, a.category, a.amount]
      );
      if (exist.length > 0) { aSkip++; continue; }
      await db.query(
        'INSERT INTO accounts (date,type,category,amount,note,location) VALUES (?,?,?,?,?,?)',
        [a.date, a.type, a.category, a.amount, a.note || '', a.location || '']
      );
      aCount++;
    }
    for (const d of diaries) {
      // 去重：同日期+标题视为重复
      const [exist] = await db.query(
        'SELECT id FROM diaries WHERE date=? AND title=? LIMIT 1',
        [d.date, d.title]
      );
      if (exist.length > 0) { dSkip++; continue; }
      await db.query(
        'INSERT INTO diaries (date,title,content,weather,mood,location,tags) VALUES (?,?,?,?,?,?,?)',
        [d.date, d.title, d.content || '', d.weather || '', d.mood || '', d.location || '', d.tags || '']
      );
      dCount++;
    }
    for (const m of meetings) {
      // 去重：同日期+主题视为重复
      const [exist] = await db.query(
        'SELECT id FROM meetings WHERE date=? AND topic=? LIMIT 1',
        [m.date, m.topic]
      );
      if (exist.length > 0) { mSkip++; continue; }
      await db.query(
        'INSERT INTO meetings (date,topic,start_time,end_time,location,attendees,content,todo_items,tags) VALUES (?,?,?,?,?,?,?,?,?)',
        [m.date, m.topic, m.start_time || '', m.end_time || '', m.location || '', m.attendees || '', m.content || '', m.todo_items || '', m.tags || '']
      );
      mCount++;
    }

    const skipTotal = aSkip + dSkip + mSkip;
    const detail = `Android同步: 新增 记账${aCount} 日记${dCount} 会议${mCount}` +
      (skipTotal > 0 ? `, 跳过重复 ${skipTotal}条` : '');
    await log('SYNC', 'all', detail, req.ip);
    res.json({ success: true, synced: { accounts: aCount, diaries: dCount, meetings: mCount }, skipped: { accounts: aSkip, diaries: dSkip, meetings: mSkip } });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

/* ========== 设置 ========== */
router.get('/settings', async (req, res) => {
  try {
    const [rows] = await db.query('SELECT * FROM settings ORDER BY id');
    const obj = {};
    rows.forEach(r => { obj[r.setting_key] = r.setting_value; });
    res.json({ success: true, data: obj });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

router.put('/settings', async (req, res) => {
  try {
    const settings = req.body;
    for (const [key, value] of Object.entries(settings)) {
      await db.query(
        'INSERT INTO settings (setting_key, setting_value) VALUES (?,?) ON DUPLICATE KEY UPDATE setting_value=?',
        [key, value, value]
      );
    }
    await log('UPDATE', 'settings', `更新设置: ${Object.keys(settings).join(', ')}`, req.ip);
    res.json({ success: true });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

// 一键清空所有业务数据
router.post('/data/clear', async (req, res) => {
  try {
    const tables = ['accounts', 'diaries', 'meetings', 'locations', 'operation_logs'];
    for (const t of tables) {
      await db.query(`TRUNCATE TABLE ${t}`);
    }
    await log('DELETE', 'settings', '一键清空所有业务数据', req.ip);
    res.json({ success: true, message: '已清空所有数据' });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

/* ========== 操作日志 ========== */
router.get('/logs', async (req, res) => {
  try {
    const { module, page = 1, limit = 50 } = req.query;
    let sql = 'SELECT * FROM operation_logs WHERE 1=1';
    const params = [];
    if (module) { sql += ' AND module = ?'; params.push(module); }

    const [countRows] = await db.query(sql.replace('SELECT *', 'SELECT COUNT(*) as total'), params);
    const total = countRows[0].total;

    sql += ' ORDER BY created_at DESC LIMIT ? OFFSET ?';
    params.push(Number(limit), (Number(page) - 1) * Number(limit));
    const [rows] = await db.query(sql, params);
    res.json({ success: true, data: rows, total });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

/* ========== 数据导出 (JSON) ========== */
router.get('/export', async (req, res) => {
  try {
    const [accounts] = await db.query('SELECT * FROM accounts ORDER BY date DESC');
    const [diaries] = await db.query('SELECT * FROM diaries ORDER BY date DESC');
    const [meetings] = await db.query('SELECT * FROM meetings ORDER BY date DESC');
    accounts.forEach(r => r.date = formatDate(r.date));
    diaries.forEach(r => r.date = formatDate(r.date));
    meetings.forEach(r => r.date = formatDate(r.date));
    await log('EXPORT', 'all', `导出全部数据`, req.ip);
    res.json({ success: true, data: { accounts, diaries, meetings }, exportDate: new Date().toISOString() });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

/* ========== 仪表盘概览 ========== */
router.get('/dashboard', async (req, res) => {
  try {
    const now = new Date();
    const thisMonth = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`;

    const [[{ total: totalAccounts }]] = await db.query('SELECT COUNT(*) as total FROM accounts');
    const [[{ total: totalDiaries }]] = await db.query('SELECT COUNT(*) as total FROM diaries');
    const [[{ total: totalMeetings }]] = await db.query('SELECT COUNT(*) as total FROM meetings');

    const [monthSummary] = await db.query(
      'SELECT type, SUM(amount) as total FROM accounts WHERE DATE_FORMAT(date, "%Y-%m") = ? GROUP BY type',
      [thisMonth]
    );
    let monthIncome = 0, monthExpense = 0;
    monthSummary.forEach(r => { if (r.type === '收入') monthIncome = Number(r.total); else monthExpense = Number(r.total); });

    const [recentAccounts] = await db.query('SELECT * FROM accounts ORDER BY date DESC, id DESC LIMIT 5');
    const [recentDiaries] = await db.query('SELECT id, date, title, mood FROM diaries ORDER BY date DESC, id DESC LIMIT 5');
    const [recentMeetings] = await db.query('SELECT id, date, topic FROM meetings ORDER BY date DESC, id DESC LIMIT 5');

    recentAccounts.forEach(r => r.date = formatDate(r.date));
    recentDiaries.forEach(r => r.date = formatDate(r.date));
    recentMeetings.forEach(r => r.date = formatDate(r.date));

    // 最近7天趋势
    const [daily] = await db.query(
      `SELECT DATE_FORMAT(date, '%m-%d') as day, type, SUM(amount) as total 
       FROM accounts WHERE date >= DATE_SUB(CURDATE(), INTERVAL 7 DAY) 
       GROUP BY day, type ORDER BY day`
    );

    res.json({
      success: true,
      data: {
        counts: { accounts: totalAccounts, diaries: totalDiaries, meetings: totalMeetings },
        month: { income: monthIncome, expense: monthExpense, balance: monthIncome - monthExpense },
        recent: { accounts: recentAccounts, diaries: recentDiaries, meetings: recentMeetings },
        daily
      }
    });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

/* ========== 位置追踪 ========== */
// 手机端上报位置（不需要登录认证）
router.post('/location', async (req, res) => {
  try {
    const { latitude, longitude, address = '', device_name = '' } = req.body;
    if (!latitude || !longitude) {
      return res.status(400).json({ success: false, message: '缺少经纬度' });
    }
    // 去重：5分钟内同设备同地址不重复记录
    const [exist] = await db.query(
      'SELECT id FROM locations WHERE device_name=? AND latitude=? AND longitude=? AND created_at > DATE_SUB(NOW(), INTERVAL 5 MINUTE) LIMIT 1',
      [device_name, latitude, longitude]
    );
    if (exist.length > 0) {
      return res.json({ success: true, message: '位置已记录(去重)' });
    }
    await db.query(
      'INSERT INTO locations (latitude, longitude, address, device_name) VALUES (?, ?, ?, ?)',
      [latitude, longitude, address, device_name]
    );
    await log('UPLOAD', 'location', `手机定位上报: ${device_name || '未知设备'} | ${address || '无地址'} (${latitude}, ${longitude})`, req.ip);
    res.json({ success: true });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

// Web 端查询最近位置（支持设备筛选）
router.get('/locations', async (req, res) => {
  try {
    const { limit = 50, device_name } = req.query;
    let sql = 'SELECT * FROM locations';
    const params = [];
    if (device_name) {
      sql += ' WHERE device_name = ?';
      params.push(device_name);
    }
    sql += ' ORDER BY created_at DESC LIMIT ?';
    params.push(Number(limit));
    const [rows] = await db.query(sql, params);
    const latest = rows.length > 0 ? rows[0] : null;
    // 获取所有不同设备名
    const [devRows] = await db.query('SELECT DISTINCT device_name FROM locations WHERE device_name != "" ORDER BY device_name');
    const devices = devRows.map(r => r.device_name);
    res.json({ success: true, data: rows, latest, devices });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

// 导出全部位置记录为 CSV
router.get('/locations/export', async (req, res) => {
  try {
    const [rows] = await db.query('SELECT * FROM locations ORDER BY created_at DESC');
    let csv = '\uFEFF时间,设备,地址,经度,纬度\n';
    rows.forEach(r => {
      const time = r.created_at ? new Date(r.created_at).toLocaleString('zh-CN', { timeZone: 'Asia/Shanghai' }) : '';
      csv += `"${time}","${r.device_name || ''}","${(r.address || '').replace(/"/g, '""')}",${r.longitude},${r.latitude}\n`;
    });
    res.setHeader('Content-Type', 'text/csv; charset=utf-8');
    res.setHeader('Content-Disposition', 'attachment; filename=locations.csv');
    res.send(csv);
    await log('EXPORT', 'location', `导出位置记录 ${rows.length} 条`, req.ip);
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

/* ========== 手动请求定位 ========== */
let locationRequestPending = false;
// Web 端点击"立即定位"
router.post('/location/request', async (req, res) => {
  locationRequestPending = true;
  await log('REQUEST', 'location', 'Web端手动发起定位请求', req.ip);
  res.json({ success: true, message: '已发送定位请求，等待手机响应' });
});
// 手机端轮询检查
router.get('/location/pending', (req, res) => {
  const pending = locationRequestPending;
  if (pending) locationRequestPending = false;
  res.json({ pending });
});

/* ========== 推送更新 ========== */
let updatePushPending = false;
// Web 端点击"推送更新"
router.post('/app/push-update', async (req, res) => {
  updatePushPending = true;
  await log('PUSH', 'app', 'Web端推送应用更新通知', req.ip);
  res.json({ success: true, message: '已推送更新通知，等待手机响应' });
});
// 手机端轮询检查是否有推送更新
router.get('/app/check-push', (req, res) => {
  const pending = updatePushPending;
  if (pending) updatePushPending = false;
  res.json({ pending });
});

/* ========== 设备在线管理 ========== */
const devicesMap = new Map(); // deviceId -> { info, lastSeen }

// 手机端心跳上报（每30秒）
router.post('/device/heartbeat', (req, res) => {
  const { device_id, brand, model, android_version, battery, app_version, screen, network } = req.body;
  if (!device_id) return res.status(400).json({ success: false, message: '缺少device_id' });
  devicesMap.set(device_id, {
    device_id, brand: brand || '', model: model || '', android_version: android_version || '',
    battery: battery || 0, app_version: app_version || '', screen: screen || '', network: network || '',
    ip: req.ip || '', lastSeen: Date.now()
  });
  res.json({ success: true });
});

// Web 端查询设备列表
router.get('/devices', (req, res) => {
  const now = Date.now();
  const list = [];
  devicesMap.forEach(d => {
    list.push({ ...d, online: (now - d.lastSeen) < 90000 }); // 90秒无心跳=离线
  });
  list.sort((a, b) => b.lastSeen - a.lastSeen);
  res.json({ success: true, data: list });
});

/* ========== APK 版本管理 ========== */
const multer = require('multer');
const path = require('path');
const fs = require('fs');

const apkStorage = multer.diskStorage({
  destination: (req, file, cb) => {
    const dir = path.join(__dirname, '..', 'uploads');
    if (!fs.existsSync(dir)) fs.mkdirSync(dir);
    cb(null, dir);
  },
  filename: (req, file, cb) => {
    cb(null, `app_v${req.body.versionCode || 'unknown'}_${Date.now()}.apk`);
  }
});
const apkUpload = multer({
  storage: apkStorage,
  fileFilter: (req, file, cb) => {
    if (file.originalname.endsWith('.apk')) cb(null, true);
    else cb(new Error('只能上传 APK 文件'));
  },
  limits: { fileSize: 200 * 1024 * 1024 } // 200MB
});

// 上传 APK
router.post('/app/upload', apkUpload.single('apk'), async (req, res) => {
  try {
    if (!req.file) return res.status(400).json({ success: false, message: '请选择 APK 文件' });
    const { versionCode, versionName, changelog = '' } = req.body;
    if (!versionCode || !versionName) {
      fs.unlinkSync(req.file.path);
      return res.status(400).json({ success: false, message: '请填写版本号' });
    }
    await db.query(
      'INSERT INTO app_versions (version_code, version_name, changelog, filename) VALUES (?,?,?,?)',
      [Number(versionCode), versionName, changelog, req.file.filename]
    );
    await log('CREATE', 'app', `上传APK v${versionName}(${versionCode})`, req.ip);
    res.json({ success: true, message: `v${versionName} 上传成功` });
  } catch (e) {
    if (req.file) try { fs.unlinkSync(req.file.path); } catch (_) {}
    res.status(500).json({ success: false, message: e.message });
  }
});

// Android 检查更新
router.get('/app/check-update', async (req, res) => {
  try {
    const currentCode = Number(req.query.versionCode) || 0;
    const [rows] = await db.query(
      'SELECT * FROM app_versions ORDER BY version_code DESC LIMIT 1'
    );
    if (rows.length === 0 || rows[0].version_code <= currentCode) {
      return res.json({ success: true, hasUpdate: false });
    }
    const latest = rows[0];
    res.json({
      success: true,
      hasUpdate: true,
      versionCode: latest.version_code,
      versionName: latest.version_name,
      changelog: latest.changelog,
      downloadUrl: `/api/app/latest`
    });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

// 下载最新 APK
router.get('/app/latest', async (req, res) => {
  try {
    const [rows] = await db.query(
      'SELECT filename FROM app_versions ORDER BY version_code DESC LIMIT 1'
    );
    if (rows.length === 0) return res.status(404).json({ success: false, message: '暂无可用版本' });
    const filePath = path.join(__dirname, '..', 'uploads', rows[0].filename);
    if (!fs.existsSync(filePath)) return res.status(404).json({ success: false, message: '文件不存在' });
    res.download(filePath, `小账本_latest.apk`);
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

// 查询版本列表（Web端用）
router.get('/app/versions', async (req, res) => {
  try {
    const [rows] = await db.query('SELECT * FROM app_versions ORDER BY version_code DESC LIMIT 20');
    res.json({ success: true, data: rows });
  } catch (e) { res.status(500).json({ success: false, message: e.message }); }
});

module.exports = router;
