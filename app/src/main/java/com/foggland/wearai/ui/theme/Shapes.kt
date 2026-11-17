package com.foggland.wearai.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 胶囊圆角半径：只用于「胶囊类」元素——悬浮顶栏、输入框、发送按钮。
 * 胶囊高度 = 半径 × 2，所以始终是完整胶囊。
 */
val AppCapsuleRadius = 28.dp

/**
 * 界面圆角半径（R 角）默认值。
 *
 * 这是**除胶囊之外所有元素**的圆角半径，用户可在设置里用滑块调节：
 *  - 0dp   → 直角
 *  - 4dp   → 轻微圆角（按钮、输入框）
 *  - 8dp   → 常用卡片
 *  - 16dp  → 大圆角弹窗
 *  - 等于元素宽/高的一半 → 变成胶囊
 */
val AppSmallRadius = 8.dp

/** 当前生效的 R 角（由设置注入），供各组件读取。 */
val LocalSmallRadius = staticCompositionLocalOf { AppSmallRadius }

/** Material3 形状全部跟随 R 角，保证对话框、卡片与气泡观感一致。 */
fun appShapes(radius: Dp = AppSmallRadius) = Shapes(
    extraSmall = RoundedCornerShape(radius),
    small = RoundedCornerShape(radius),
    medium = RoundedCornerShape(radius),
    large = RoundedCornerShape(radius),
    extraLarge = RoundedCornerShape(radius),
)
