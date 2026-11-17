package com.foggland.wearai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.foggland.wearai.core.di.ServiceLocator
import com.foggland.wearai.core.vm.ChatViewModel
import com.foggland.wearai.ui.WearAIApp
import com.foggland.wearai.ui.theme.AppBackground
import com.foggland.wearai.ui.theme.ScaledDensity
import com.foggland.wearai.ui.theme.WearAITheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val chatViewModel: ChatViewModel = viewModel(
                factory = ChatViewModel.factory(ServiceLocator.get(LocalContext.current))
            )
            val settings by chatViewModel.settings.collectAsState()
            val configuration = androidx.compose.ui.platform.LocalConfiguration.current
            // 自动适配开关打开时，按屏幕宽度推荐缩放；否则用用户手动值
            val effectiveScale = if (settings.uiScaleAuto) {
                com.foggland.wearai.ui.theme.UiScaleAuto.suggest(configuration)
            } else {
                settings.uiScale
            }
            ScaledDensity(scale = effectiveScale) {
                WearAITheme(
                    forceDark = settings.darkMode,
                    themeId = settings.themeId,
                    customSeed = settings.customSeed,
                ) {
                    androidx.compose.runtime.CompositionLocalProvider(
                        com.foggland.wearai.ui.components.LocalFloatingBars provides settings.floatingBars,
                        com.foggland.wearai.ui.components.LocalCapsuleRadius provides
                            androidx.compose.ui.unit.Dp(settings.capsuleRadius),
                        com.foggland.wearai.ui.components.LocalCapsuleBorder provides settings.capsuleBorder,
                        com.foggland.wearai.ui.components.LocalTranslucentBars provides settings.translucentBars,
                        com.foggland.wearai.ui.components.LocalTranslucentAlpha provides settings.translucentAlpha,
                    ) {
                        AppBackground(
                            backgroundPath = settings.backgroundPath,
                            dim = settings.backgroundDim,
                        ) {
                            WearAIApp(chatViewModel)
                        }
                    }
                }
            }
        }
    }

}
