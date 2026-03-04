const express = require('express');
const path = require('path');
const cors = require('cors');
const session = require('express-session');
const apiRoutes = require('./routes/api');

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

// 认证中间件 - API 路由保护（排除登录相关和同步上传接口）
function authMiddleware(req, res, next) {
  // 登录/登出/检查状态接口不需要认证
  if (req.path === '/api/auth/login' || req.path === '/api/auth/check' || req.path === '/api/auth/logout') {
    return next();
  }
  // Android 同步上传接口不需要 session 认证
  if (req.path === '/api/sync/upload') {
    return next();
  }
  // 其他 API 接口需要认证
  if (req.path.startsWith('/api/') && !req.session.userId) {
    return res.status(401).json({ success: false, message: '未登录' });
  }
  next();
}

app.use(authMiddleware);

// API 路由
app.use('/api', apiRoutes);

// 页面路由 - SPA 风格，所有页面由前端 JS 处理
app.get('/', (req, res) => res.sendFile(path.join(__dirname, 'public', 'index.html')));

// 启动
app.listen(PORT, '0.0.0.0', () => {
  console.log(`\n💕 我们的小账本 - Web后台管理系统`);
  console.log(`📍 本地访问: http://localhost:${PORT}`);
  console.log(`🌐 公网访问: https://resistive-diotic-jolie.ngrok-free.dev`);
  console.log(`📡 API 地址: http://localhost:${PORT}/api\n`);
});
