package com.foggland.wearai.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * 以软件方式对整棵 UI 进行等比缩放。
 *
 * 通过重写 [LocalDensity]，让所有 dp 与 sp 尺寸统一乘以 [scale]，
 * 从而适配 dpi 较大、分辨率极小的普通安卓手表（如 320x360 @ 240dpi）。
 * 仅缩放 density，不缩放 fontScale，避免文字被二次放大。
 */
@Composable
fun ScaledDensity(
    scale: Float,
    content: @Composable () -> Unit,
) {
    val base = LocalDensity.current
    val scaled = remember(base, scale) {
        if (scale == 1f) base else Density(density = base.density * scale, fontScale = base.fontScale)
    }
    CompositionLocalProvider(LocalDensity provides scaled, content = content)
}
