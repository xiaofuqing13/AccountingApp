# iOS 备忘录滑动操作交互规范

本文档总结 iOS 原生应用（备忘录、邮件等）的滑动操作交互特性，为 Android 实现提供参考。

## 1. 滑动手势识别

### 1.1 触发条件

| 参数 | 值 | 说明 |
|------|-----|------|
| 最小滑动距离 | 10-15 dp | 超过此距离才识别为滑动手势 |
| 滑动角度容差 | ±30° | 水平方向 ±30° 范围内识别为左右滑动 |
| 速度阈值 | 500 dp/s | 快速滑动时可降低距离要求 |

### 1.2 手势优先级

```
垂直滚动 > 水平滑动 > 点击
```

- 当垂直位移 > 水平位移时，优先处理列表滚动
- 一旦确定为水平滑动，锁定方向，禁止垂直滚动
- 滑动过程中不响应点击事件

### 1.3 滑动方向

| 方向 | 操作类型 | 典型按钮 |
|------|---------|---------|
| 左滑 (Trailing) | 破坏性操作 | 删除、归档 |
| 右滑 (Leading) | 非破坏性操作 | 标记、置顶 |

## 2. 展开状态下的触摸处理

### 2.1 状态定义

```
关闭状态 (Closed)     → 偏移量 = 0
部分展开 (Partial)    → 0 < 偏移量 < 按钮总宽度
完全展开 (Expanded)   → 偏移量 = 按钮总宽度
全滑动 (Full Swipe)   → 偏移量 > 屏幕宽度 50%
```

### 2.2 触摸区域划分

展开状态下，触摸区域分为三部分：

```
┌─────────────────────────────────────────────────────┐
│                                                     │
│  ┌─────────────────────────┬──────────┬──────────┐  │
│  │      内容区域            │  编辑按钮 │  删除按钮 │  │
│  │   (点击关闭滑动)         │  (执行)   │  (执行)   │  │
│  └─────────────────────────┴──────────┴──────────┘  │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### 2.3 触摸响应规则

| 触摸位置 | 响应行为 |
|---------|---------|
| 内容区域 | 关闭当前展开的滑动菜单 |
| 操作按钮 | 执行对应操作 |
| 其他 Cell | 关闭当前展开项，不触发新滑动 |
| 列表空白区域 | 关闭当前展开项 |

### 2.4 关键行为：按钮点击优先

```
展开状态下：
  按钮区域触摸 → 直接执行按钮操作（不关闭菜单）
  内容区域触摸 → 关闭菜单（不执行其他操作）

滑动过程中：
  手指抬起在按钮区域 → 不执行按钮操作（视为滑动结束）
  需要明确的点击手势才能触发按钮
```

## 3. 按钮点击与滑动的区分

### 3.1 手势判定逻辑

```kotlin
fun onTouchEvent(event: MotionEvent): Boolean {
    when (event.action) {
        ACTION_DOWN -> {
            // 记录起始位置和时间
            startX = event.x
            startY = event.y
            startTime = System.currentTimeMillis()
            isDragging = false
        }
        ACTION_MOVE -> {
            val deltaX = abs(event.x - startX)
            val deltaY = abs(event.y - startY)

            // 判定为滑动的条件
            if (!isDragging && deltaX > touchSlop && deltaX > deltaY) {
                isDragging = true
            }
        }
        ACTION_UP -> {
            val deltaX = abs(event.x - startX)
            val duration = System.currentTimeMillis() - startTime

            if (!isDragging && deltaX < touchSlop && duration < 200) {
                // 判定为点击
                handleClick(event.x, event.y)
            } else {
                // 判定为滑动结束
                handleSwipeEnd()
            }
        }
    }
}
```

### 3.2 判定阈值

| 参数 | 推荐值 | 说明 |
|------|-------|------|
| touchSlop | 8 dp | 系统默认触摸容差 |
| 点击最大时长 | 200 ms | 超过视为长按或滑动 |
| 点击最大位移 | touchSlop | 超过视为滑动 |

### 3.3 展开状态下的点击处理

```kotlin
fun handleClickInExpandedState(x: Float, y: Float) {
    val buttonBounds = getButtonBounds()

    when {
        // 点击在按钮区域 - 执行按钮操作
        buttonBounds.contains(x, y) -> {
            executeButtonAction(getButtonAt(x, y))
            // 注意：iOS 中点击按钮后会自动关闭菜单
            closeSwipeMenu()
        }
        // 点击在内容区域 - 仅关闭菜单
        else -> {
            closeSwipeMenu()
        }
    }
}
```

## 4. 多项目滑动时的状态管理

### 4.1 单一展开原则

**核心规则：同一时刻只允许一个 Cell 处于展开状态**

```kotlin
class SwipeStateManager {
    private var expandedPosition: Int = -1

    fun onSwipeStart(position: Int) {
        // 关闭之前展开的项
        if (expandedPosition != -1 && expandedPosition != position) {
            closeItem(expandedPosition)
        }
        expandedPosition = position
    }

    fun onSwipeClose(position: Int) {
        if (expandedPosition == position) {
            expandedPosition = -1
        }
    }

    fun closeAll() {
        if (expandedPosition != -1) {
            closeItem(expandedPosition)
            expandedPosition = -1
        }
    }
}
```

### 4.2 自动关闭触发条件

| 触发事件 | 行为 |
|---------|------|
| 开始滑动另一个 Cell | 关闭当前展开项 |
| 点击任意非按钮区域 | 关闭当前展开项 |
| 开始列表滚动 | 关闭当前展开项 |
| 点击操作按钮 | 执行操作后关闭 |
| 数据刷新/重载 | 关闭所有展开项 |

### 4.3 RecyclerView 集成

```kotlin
recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
    override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
        if (newState == RecyclerView.SCROLL_STATE_DRAGGING) {
            // 开始滚动时关闭所有展开项
            swipeStateManager.closeAll()
        }
    }
})

recyclerView.setOnTouchListener { _, event ->
    if (event.action == MotionEvent.ACTION_DOWN) {
        // 触摸列表时检查是否需要关闭展开项
        val touchedPosition = findChildViewUnder(event.x, event.y)
        if (touchedPosition != expandedPosition) {
            swipeStateManager.closeAll()
        }
    }
    false
}
```

## 5. 动画效果

### 5.1 动画参数

| 动画类型 | 时长 | 插值器 | 说明 |
|---------|------|-------|------|
| 展开动画 | 250-300 ms | Spring (damping=0.7) | 带轻微回弹 |
| 关闭动画 | 200-250 ms | DecelerateInterpolator | 减速停止 |
| 按钮点击反馈 | 100 ms | LinearInterpolator | 快速响应 |
| 全滑动删除 | 300 ms | AccelerateInterpolator | 加速滑出 |

### 5.2 Spring 动画配置

```kotlin
// iOS 风格的弹簧动画
val springAnimation = SpringAnimation(view, DynamicAnimation.TRANSLATION_X)
    .setSpring(SpringForce()
        .setFinalPosition(targetPosition)
        .setDampingRatio(0.7f)      // 阻尼比：0.7 轻微回弹
        .setStiffness(800f))        // 刚度：800 中等速度

// 或使用 ObjectAnimator 模拟
ObjectAnimator.ofFloat(view, "translationX", targetX).apply {
    duration = 280
    interpolator = PathInterpolator(0.25f, 0.1f, 0.25f, 1f) // iOS 默认曲线
}
```

### 5.3 滑动跟手动画

```kotlin
fun updateSwipeOffset(deltaX: Float) {
    // 1:1 跟手，无阻尼
    if (offset >= 0 && offset <= maxOffset) {
        translationX = -offset
    }
    // 超出范围时添加阻尼
    else {
        val overscroll = offset - maxOffset
        val dampedOverscroll = overscroll * 0.3f  // 30% 阻尼
        translationX = -(maxOffset + dampedOverscroll)
    }
}
```

### 5.4 按钮显示动画

iOS 提供三种按钮显示风格：

#### Border 风格（默认）
```
按钮区域均分，同时显示
┌────────────────────────────────────────┐
│ Content          │ Edit │ Delete │
└────────────────────────────────────────┘
```

#### Drag 风格
```
按钮跟随内容拖动，逐个显示
滑动 50dp:  │ Content     │Del│
滑动 100dp: │ Content │Edit│Delete│
```

#### Reveal 风格
```
按钮固定在边缘，被内容遮挡后逐渐显示
```

### 5.5 全滑动 (Full Swipe) 动画

```kotlin
// 当滑动超过阈值时触发全滑动
val fullSwipeThreshold = itemWidth * 0.5f  // 50% 宽度

fun onSwipeEnd(velocity: Float, offset: Float) {
    when {
        // 高速滑动或超过阈值 → 执行全滑动操作
        velocity > 1000 || offset > fullSwipeThreshold -> {
            animateFullSwipe {
                executeFirstAction()  // 执行第一个操作（通常是删除）
            }
        }
        // 超过按钮宽度 → 保持展开
        offset > buttonWidth * 0.5f -> {
            animateToExpanded()
        }
        // 否则 → 关闭
        else -> {
            animateToClose()
        }
    }
}

fun animateFullSwipe(onComplete: () -> Unit) {
    ObjectAnimator.ofFloat(itemView, "translationX", -screenWidth).apply {
        duration = 300
        interpolator = AccelerateInterpolator()
        doOnEnd { onComplete() }
        start()
    }
}
```

## 6. 实现检查清单

### 6.1 手势识别
- [ ] 正确区分水平滑动和垂直滚动
- [ ] 滑动过程中锁定方向
- [ ] 支持快速滑动（velocity-based）判定

### 6.2 状态管理
- [ ] 同时只有一个 Cell 展开
- [ ] 滚动时自动关闭展开项
- [ ] 点击其他区域关闭展开项

### 6.3 触摸处理
- [ ] 展开状态下按钮可点击
- [ ] 点击内容区域关闭菜单
- [ ] 滑动结束不误触发按钮点击

### 6.4 动画效果
- [ ] 展开/关闭使用弹簧动画
- [ ] 滑动跟手流畅
- [ ] 超出范围有阻尼效果
- [ ] 支持全滑动操作（可选）

## 7. 参考资源

- [SwipeCellKit](https://github.com/SwipeCellKit/SwipeCellKit) - iOS 滑动 Cell 开源实现
- [objc.io - Interactive Animations](https://www.objc.io/issues/12-animations/interactive-animations/) - 交互式动画原理
- Apple UIKit - UISwipeActionsConfiguration
- Apple Human Interface Guidelines - Lists and Tables
