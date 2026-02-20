# UI视觉统一优化说明

## 本次目标
- 解决“界面风格混乱、颜色杂”的问题。
- 统一全局为单一蓝系主题，减少页面各自为政的硬编码颜色。

## 主要调整
- 统一色板与暗色模式：`values/colors.xml`、`values-night/colors.xml`。
- 统一圆角/间距体系：`values/dimens.xml`。
- 统一主题基础项：`values/themes.xml`（状态栏透明、导航栏背景、全局字体族）。
- 统一卡片与容器背景：
  - `drawable/bg_card_rounded.xml`
  - `drawable/bg_settings_group.xml`
  - `drawable/bg_home_quick_btn.xml`
  - `drawable/bg_love_card.xml`
- 统一底部导航选中/未选中配色：
  - `layout/activity_main.xml`
  - `color/nav_icon_color.xml`
  - `color/nav_text_color.xml`
- 修复情话弹窗视觉与文案乱码：
  - `layout/dialog_love_popup.xml`
  - `layout/fragment_love_letter.xml`（底部提示色）

## 效果说明
- 页面主色、强调色、文字层级、卡片边界风格保持一致。
- 去除多处白底/粉色硬编码，避免同屏出现多套视觉语言。
- 底部导航恢复“选中高亮、未选中弱化”的正常层次。

## 验证
- 已执行：`gradlew.bat :app:assembleDebug`
- 结果：`BUILD SUCCESSFUL`
