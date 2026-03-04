const express = require('express');
const path = require('path');
const cors = require('cors');
const apiRoutes = require('./routes/api');

const app = express();
const PORT = process.env.PORT || 5000;

// 中间件
app.use(cors());
app.use(express.json({ limit: '50mb' }));
app.use(express.urlencoded({ extended: true, limit: '50mb' }));

// 静态文件
app.use(express.static(path.join(__dirname, 'public')));

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
