# 首页UI优化说明

## 本次优化目标
- 修复首页视觉“乱”和“丑”的观感问题。
- 保留原有功能逻辑与跳转，仅优化首屏样式和文案可读性。

## 具体改动
- 重做首页 Banner 背景与计时卡样式：
  - 新增 `app/src/main/res/drawable/bg_home_banner.xml`
  - 新增 `app/src/main/res/drawable/bg_counter_glass.xml`
- 优化首页布局 `app/src/main/res/layout/fragment_home.xml`：
  - 统一为粉色少女风（圆角、描边、柔和对比）
  - 快捷入口卡片统一描边+低阴影
  - 修复首页多处乱码文案（天/时/分/秒、今日情话、快捷入口文字等）
- 修复首页逻辑文案 `app/src/main/java/com/loveapp/accountbook/ui/home/HomeFragment.kt`：
  - 修复 Banner 彩蛋文案乱码
  - 修复导出路径 Toast 文案乱码

## 验证结果
- 编译验证：`gradlew.bat :app:assembleDebug` 通过
- 安装启动验证：APP 可正常启动进入首页，未出现 FATAL 崩溃
