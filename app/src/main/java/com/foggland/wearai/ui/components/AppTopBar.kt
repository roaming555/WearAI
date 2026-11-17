package com.foggland.wearai.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.foggland.wearai.ui.theme.AppCapsuleRadius
import com.foggland.wearai.ui.theme.LocalSurfaceTrio

/** 是否启用「悬浮胶囊」样式（灵动岛风格）。由 MainActivity 根据设置提供。 */
val LocalFloatingBars = staticCompositionLocalOf { true }

/** 悬浮胶囊的圆角半径，可在设置里用滑块调整。 */
val LocalCapsuleRadius = staticCompositionLocalOf { AppCapsuleRadius }

/** 是否给悬浮胶囊描边（深色模式下描边为浅色，提升辨识度）。 */
val LocalCapsuleBorder = staticCompositionLocalOf { false }

/** 显示栏的普通半透明开关。 */
val LocalTranslucentBars = staticCompositionLocalOf { false }

/** 显示栏的普通半透明度。 */
val LocalTranslucentAlpha = staticCompositionLocalOf { 0.72f }

/** 栏底色：开启半透明时按不透明度变透，否则保持原色。 */
@Composable
fun barSurfaceColor(base: Color): Color = when {
    LocalTranslucentBars.current -> base.copy(alpha = LocalTranslucentAlpha.current)
    else -> base
}

/** 兼容旧调用名。 */
@Composable
fun glassColor(base: Color): Color = barSurfaceColor(base)

/** 胶囊投影高度。 */
val AppCapsuleElevation: Dp = 18.dp

/** 胶囊栏固定高度。 */
val CapsuleBarHeight: Dp = 56.dp

/** 当前胶囊高度。 */
@Composable
fun capsuleHeight(): Dp = CapsuleBarHeight

/** 当前胶囊圆角形状。 */
@Composable
fun capsuleShape(): RoundedCornerShape = RoundedCornerShape(LocalCapsuleRadius.current)

/** 胶囊描边。 */
@Composable
fun capsuleBorderStroke(contentColor: Color): BorderStroke? =
    if (LocalCapsuleBorder.current) BorderStroke(1.dp, contentColor.copy(alpha = 0.5f)) else null

private val BarVerticalMargin = 6.dp
private val BarHorizontalMargin = 10.dp

/**
 * 悬浮顶栏需要给内容预留的顶部空间。
 * 关闭悬浮样式时返回 0。
 */
@Composable
fun floatingTopReserve(): Dp {
    if (!LocalFloatingBars.current) return 0.dp
    val statusBar = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    return statusBar + BarVerticalMargin * 2 + capsuleHeight()
}

/** 悬浮底栏需要给内容预留的底部空间。关闭悬浮样式时返回 0。 */
@Composable
fun floatingBottomReserve(): Dp {
    if (!LocalFloatingBars.current) return 0.dp
    val navBar = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return navBar + BarVerticalMargin * 2 + capsuleHeight()
}

/**
 * 应用顶栏。
 *
 * 悬浮模式下是一颗真正浮在内容之上的胶囊（内容可以滚动到它下面），
 * 关闭悬浮时是常见的通栏顶栏。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val trio = LocalSurfaceTrio.current
    val floating = LocalFloatingBars.current

    if (floating) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = BarHorizontalMargin, vertical = BarVerticalMargin),
        ) {
            Surface(
                shape = capsuleShape(),
                color = glassColor(trio.topBar),
                contentColor = trio.onTopBar,
                border = capsuleBorderStroke(trio.onTopBar),
                shadowElevation = AppCapsuleElevation,
                tonalElevation = 0.dp,
            ) {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(capsuleHeight())
                            .padding(start = 12.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        navigationIcon()
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            title()
                        }
                        actions()
                    }
                }
            }
        }
    } else {
        TopAppBar(
            modifier = modifier,
            title = title,
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = trio.topBar,
                titleContentColor = trio.onTopBar,
                actionIconContentColor = trio.onTopBar,
                navigationIconContentColor = trio.onTopBar,
            ),
            navigationIcon = navigationIcon,
            actions = actions,
        )
    }
}

/**
 * 页面骨架：统一处理两种布局，并保证内容区域始终有正确的文字颜色
 * （悬浮模式用的是纯 Box 覆盖层，必须手动提供 LocalContentColor，
 * 否则深色模式下未显式指定颜色的文字会变成黑色而看不清）。
 */
@Composable
fun AppScreenLayout(
    topBar: @Composable (Modifier) -> Unit,
    content: @Composable (Modifier) -> Unit,
) {
    val trio = LocalSurfaceTrio.current
    val floating = LocalFloatingBars.current
    if (floating) {
        CompositionLocalProvider(LocalContentColor provides trio.onBackground) {
            Box(modifier = Modifier.fillMaxSize()) {
                content(Modifier.fillMaxSize())
                topBar(Modifier.align(Alignment.TopCenter))
            }
        }
    } else {
        Scaffold(
            containerColor = Color.Transparent,
            contentColor = trio.onBackground,
            topBar = { topBar(Modifier) },
        ) { innerPadding ->
            content(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        }
    }
}

/**
 * 应用底部区域容器。
 *
 * 悬浮模式下它是**透明的**：具体的胶囊（输入框、发送键）由内容自己绘制；
 * 关闭悬浮时恢复为通栏底栏，并留出正常内边距。
 */
@Composable
fun AppBottomBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val trio = LocalSurfaceTrio.current
    val floating = LocalFloatingBars.current
    if (floating) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = BarHorizontalMargin, vertical = BarVerticalMargin),
        ) {
            content()
        }
    } else {
        Surface(
            modifier = modifier,
            color = glassColor(trio.input),
            contentColor = trio.onInput,
        ) {
            Box(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            ) {
                content()
            }
        }
    }
}
