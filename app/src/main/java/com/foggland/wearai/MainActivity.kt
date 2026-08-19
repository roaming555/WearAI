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
            ScaledDensity(scale = settings.uiScale) {
                WearAITheme(forceDark = settings.darkMode) {
                    WearAIApp(chatViewModel)
                }
            }
        }
    }
}
