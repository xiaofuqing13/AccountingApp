# 我们的小账本 - Android 开发说明

## 一、项目概览

基于原型设计 `design/prototype.html` 实现的完整Android应用，采用少女粉色系Material Design 3主题。

- **语言**：Kotlin
- **最低版本**：Android 7.0 (API 24)
- **架构**：MVVM (ViewModel + LiveData)
- **导航**：Jetpack Navigation + BottomNavigationView
- **数据存储**：Apache POI (.xlsx)
- **构建工具**：Gradle Kotlin DSL

## 二、项目结构

```
app/
├── build.gradle.kts          # 构建配置(插件+依赖+Android配置)
├── settings.gradle.kts       # 项目设置
├── gradle.properties         # Gradle属性
├── proguard-rules.pro        # 混淆规则
└── src/main/
    ├── AndroidManifest.xml
    ├── java/com/loveapp/accountbook/
    │   ├── MainActivity.kt              # 主Activity(底部导航)
    │   ├── data/
    │   │   ├── model/                   # 数据模型
    │   │   └── repository/ExcelRepository.kt
    │   ├── ui/
    │   │   ├── splash/SplashActivity.kt # 启动页
    │   │   ├── home/                    # 首页(计时器+问候)
    │   │   ├── account/                 # 记账(列表+添加+统计)
    │   │   ├── diary/                   # 日记(列表+添加)
    │   │   ├── meeting/                 # 会议(列表+添加)
    │   │   ├── settings/                # 设置
    │   │   ├── love/                    # 情书页
    │   │   └── adapter/                 # RecyclerView适配器
    │   └── util/
    │       ├── DateUtils.kt             # 日期/计时/问候工具
    │       └── EasterEggManager.kt      # 彩蛋系统
    └── res/
        ├── layout/       (17个布局文件)
        ├── navigation/   nav_graph.xml
        ├── menu/         bottom_nav_menu.xml
        ├── drawable/     渐变/圆角/按钮背景/图标
        ├── mipmap/       应用图标(爱心adaptive-icon)
        ├── values/       colors/strings/themes/dimens
        └── xml/          file_paths.xml
```

## 三、核心模块说明

### 3.1 数据存储 (ExcelRepository)
- 使用Apache POI 5.2.5读写.xlsx文件
- 单文件3个Sheet：记账/日记/会议纪要
- 文件存储在 `getExternalFilesDir()/DataManager/` 目录
- 支持导入导出操作
- 所有IO操作在协程Dispatchers.IO中执行

### 3.2 首页 (HomeFragment)
- **智能问候语**：根据时间自动切换（早/中/下午/晚/深夜）
- **实时计时器**：每秒刷新显示在一起的天:时:分:秒
- **统计卡片**：在一起天数/本月日记/本月记账
- **快捷操作**：6个快捷入口网格

### 3.3 记账模块
- **列表页**：月份切换器 + 收支汇总 + 按日期分组列表
- **添加页**：收入/支出Tab + 分类网格 + 备注 + 数字键盘
- **统计页**：总支出 + 日均 + 分类排行

### 3.4 日记模块
- **列表页**：搜索栏 + 日记卡片流（日期/天气/标题/预览/心情）
- **写日记页**：元数据标签 + 标题 + 内容 + 底部工具栏

### 3.5 会议模块
- **列表页**：会议卡片（日期框+主题+时间地点+参会人+标签）
- **添加页**：结构化表单（主题/时间/地点/参会人/内容/待办/标签）

### 3.6 设置模块
- 数据管理：导出/导入Excel、存储位置、自动备份
- 通用设置：主题颜色、深色模式、提醒通知、密码锁定
- 关于：版本信息、意见反馈

### 3.7 情书页
- 完整情书正文（信纸样式）
- 8条承诺卡片（Chip组件）

### 3.8 自动保存 (DraftManager)
- 使用SharedPreferences存储草稿，输入停止2秒后自动保存
- **记账页**：自动保存金额和备注草稿
- **日记页**：自动保存标题和内容草稿
- **会议页**：自动保存主题、地点、参会人、内容、待办5个字段草稿
- 进入编辑页时自动检测并恢复草稿，提示"已恢复上次编辑的草稿"
- 保存成功后自动清除对应草稿

## 四、彩蛋系统 (EasterEggManager)

继承原型中的15个彩蛋，在Android端通过 `EasterEggManager` 统一管理。
弹窗使用自定义 `dialog_love_popup.xml` 布局。

## 五、主要依赖

| 库 | 版本 | 用途 |
|----|------|------|
| Material Design 3 | 1.11.0 | UI组件 |
| Navigation | 2.7.6 | 页面导航 |
| Lifecycle | 2.7.0 | ViewModel/LiveData |
| Apache POI | 5.2.5 | Excel读写 |
| Coroutines | 1.7.3 | 异步操作 |
| RecyclerView | 1.3.2 | 列表展示 |
| CardView | 1.0.0 | 卡片样式 |

## 六、构建与运行

1. 用Android Studio打开 `app/` 目录
2. 等待Gradle同步完成
3. 连接Android设备或启动模拟器
4. 点击Run运行项目

## 七、设计规范

| 项目 | 规范 |
|------|------|
| 主色调 | #E8729A / #FF85A2 / #F4A0C0 |
| 收入色 | #66BB6A |
| 支出色 | #E8729A |
| 点缀色 | #D4A0E8 |
| 卡片圆角 | 12dp |
| 页面边距 | 16dp |
