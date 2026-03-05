const express = require('express');
const path = require('path');
const cors = require('cors');
const session = require('express-session');
const fs = require('fs');
const http = require('http');
const { WebSocketServer } = require('ws');
const apiRoutes = require('./routes/api');

// 确保 uploads 目录存在
const uploadsDir = path.join(__dirname, 'uploads');
if (!fs.existsSync(uploadsDir)) fs.mkdirSync(uploadsDir);

const app = express();
const PORT = process.env.PORT || 5000;

// 中间件
app.use(cors({ origin: true, credentials: true }));
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// Session 配置
app.use(session({
  secret: 'xiaozhanben-secret-2025',
  resave: false,
  saveUninitialized: false,
  cookie: { maxAge: 7 * 24 * 60 * 60 * 1000 } // 7天
}));

// 静态文件
app.use(express.static(path.join(__dirname, 'public')));
app.use('/uploads', express.static(path.join(__dirname, 'uploads')));

// 认证中间件
function authMiddleware(req, res, next) {
  if (req.path === '/api/auth/login' || req.path === '/api/auth/check' || req.path === '/api/auth/logout') {
    return next();
  }
  if (req.path === '/api/sync/upload' || req.path.startsWith('/api/location') || req.path.startsWith('/api/device') || req.path.startsWith('/api/notification')) {
    return next();
  }
  if (req.path === '/api/app/check-update' || req.path === '/api/app/latest' || req.path === '/api/app/check-push') {
    return next();
  }
  if (req.path.startsWith('/api/') && !req.session.userId) {
    return res.status(401).json({ success: false, message: '未登录' });
  }
  next();
}

app.use(authMiddleware);
app.use('/api', apiRoutes);

app.get('/', (req, res) => res.sendFile(path.join(__dirname, 'public', 'index.html')));

// 用 http.createServer 以便共享端口给 WebSocket
const server = http.createServer(app);

// ========== WebSocket 服务 ==========
const wss = new WebSocketServer({ server, path: '/ws' });
const wsClients = new Set();

wss.on('connection', (ws, req) => {
  wsClients.add(ws);
  console.log(`📱 WebSocket 客户端已连接 (在线: ${wsClients.size})`);

  ws.on('close', () => {
    wsClients.delete(ws);
    console.log(`📱 WebSocket 客户端断开 (在线: ${wsClients.size})`);
  });

  ws.on('message', (data) => {
    // 心跳响应
    try {
      const msg = JSON.parse(data);
      if (msg.type === 'ping') ws.send(JSON.stringify({ type: 'pong' }));
    } catch (_) {}
  });

  ws.on('error', () => wsClients.delete(ws));
});

// 广播函数 - 供 api.js 使用
function wsBroadcast(message) {
  const data = JSON.stringify(message);
  wsClients.forEach(ws => {
    try { if (ws.readyState === 1) ws.send(data); } catch (_) {}
  });
}

// 挂到 app 上让 api.js 能访问
app.set('wsBroadcast', wsBroadcast);
app.set('wsClients', wsClients);

// 启动
server.listen(PORT, '0.0.0.0', () => {
  console.log(`\n💕 我们的小账本 - Web后台管理系统`);
  console.log(`📍 本地访问: http://localhost:${PORT}`);
  console.log(`🌐 公网访问: https://resistive-diotic-jolie.ngrok-free.dev`);
  console.log(`📡 API 地址: http://localhost:${PORT}/api`);
  console.log(`🔌 WebSocket: ws://localhost:${PORT}/ws\n`);
});
