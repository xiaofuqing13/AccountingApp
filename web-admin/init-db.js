const mysql = require('mysql2/promise');

async function initDB() {
  const conn = await mysql.createConnection({
    host: 'localhost',
    user: 'root',
    password: 'root123456',
    charset: 'utf8mb4'
  });

  console.log('🔗 已连接 MySQL');

  await conn.query('CREATE DATABASE IF NOT EXISTS accountbook CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci');
  console.log('📦 数据库 accountbook 已创建');

  await conn.query('USE accountbook');

  await conn.query(`
    CREATE TABLE IF NOT EXISTS accounts (
      id INT AUTO_INCREMENT PRIMARY KEY,
      date DATE NOT NULL,
      type VARCHAR(10) NOT NULL,
      category VARCHAR(50) NOT NULL,
      amount DECIMAL(10,2) NOT NULL,
      note TEXT DEFAULT NULL,
      location VARCHAR(200) DEFAULT '',
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      INDEX idx_date (date),
      INDEX idx_type (type),
      INDEX idx_category (category)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  console.log('✅ accounts 表已创建');

  await conn.query(`
    CREATE TABLE IF NOT EXISTS diaries (
      id INT AUTO_INCREMENT PRIMARY KEY,
      date DATE NOT NULL,
      title VARCHAR(200) NOT NULL,
      content TEXT,
      weather VARCHAR(50) DEFAULT '',
      mood VARCHAR(50) DEFAULT '',
      location VARCHAR(200) DEFAULT '',
      tags VARCHAR(500) DEFAULT '',
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      INDEX idx_date (date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  console.log('✅ diaries 表已创建');

  await conn.query(`
    CREATE TABLE IF NOT EXISTS meetings (
      id INT AUTO_INCREMENT PRIMARY KEY,
      date DATE NOT NULL,
      topic VARCHAR(200) NOT NULL,
      start_time VARCHAR(10) DEFAULT '',
      end_time VARCHAR(10) DEFAULT '',
      location VARCHAR(200) DEFAULT '',
      attendees TEXT DEFAULT NULL,
      content TEXT DEFAULT NULL,
      todo_items TEXT DEFAULT NULL,
      tags VARCHAR(500) DEFAULT '',
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
      INDEX idx_date (date)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  console.log('✅ meetings 表已创建');

  await conn.query(`
    CREATE TABLE IF NOT EXISTS settings (
      id INT AUTO_INCREMENT PRIMARY KEY,
      setting_key VARCHAR(100) UNIQUE NOT NULL,
      setting_value TEXT,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  console.log('✅ settings 表已创建');

  await conn.query(`
    CREATE TABLE IF NOT EXISTS operation_logs (
      id INT AUTO_INCREMENT PRIMARY KEY,
      action VARCHAR(50) NOT NULL,
      module VARCHAR(50) NOT NULL,
      detail TEXT,
      ip_address VARCHAR(50),
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      INDEX idx_module (module),
      INDEX idx_created (created_at)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  console.log('✅ operation_logs 表已创建');

  await conn.query(`
    CREATE TABLE IF NOT EXISTS admin_users (
      id INT AUTO_INCREMENT PRIMARY KEY,
      username VARCHAR(50) UNIQUE NOT NULL,
      password VARCHAR(128) NOT NULL,
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
  `);
  console.log('✅ admin_users 表已创建');

  // 插入默认管理员（用户名: 1, 密码: 1）
  const crypto = require('crypto');
  const defaultPwd = crypto.createHash('sha256').update('1').digest('hex');
  await conn.query(`
    CREATE TABLE IF NOT EXISTS locations (
      id INT AUTO_INCREMENT PRIMARY KEY,
      latitude DOUBLE NOT NULL,
      longitude DOUBLE NOT NULL,
      address VARCHAR(500) DEFAULT '',
      created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
      INDEX idx_created (created_at)
    )
  `);
  console.log('✅ locations 表已创建');

  await conn.query(
    'INSERT IGNORE INTO admin_users (username, password) VALUES (?, ?)',
    ['1', defaultPwd]
  );
  console.log('✅ 默认管理员已创建（用户名: 1, 密码: 1）');

  // 插入默认设置
  const defaults = [
    ['couple_name_1', '宝贝'],
    ['couple_name_2', '亲爱的'],
    ['anniversary_date', '2025-02-14'],
    ['theme_color', '#E8729A'],
    ['auto_backup', 'true'],
    ['app_name', '我们的小账本']
  ];
  for (const [key, value] of defaults) {
    await conn.query(
      'INSERT IGNORE INTO settings (setting_key, setting_value) VALUES (?, ?)',
      [key, value]
    );
  }
  console.log('✅ 默认设置已插入');

  await conn.end();
  console.log('🎉 数据库初始化完成！');
}

initDB().catch(err => {
  console.error('❌ 初始化失败:', err.message);
  process.exit(1);
});
