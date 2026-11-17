package com.foggland.wearai.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * 在弹窗内部重新提供按全局缩放换算过的 [LocalDensity]。
 *
 * Compose 的 `Dialog` 会使用独立的窗口与 Composition 上下文，
 * 页面级的 `LocalDensity` 覆盖**不会**传进去，因此每个弹窗内部都要重新提供一次，
 * 否则「界面尺寸」缩放对弹窗内容不生效。
 *
 * 用法：`Dialog(...) { ScaledDialogContent(settings.uiScale) { /* 弹窗内容 */ } }`
 */
@Composable
fun ScaledDialogContent(scale: Float, content: @Composable () -> Unit) {
    val base = LocalDensity.current
    CompositionLocalProvider(
        LocalDensity provides Density(base.density * scale, base.fontScale),
        content = content,
    )
}
