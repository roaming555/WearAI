package com.foggland.wearai.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.foggland.wearai.core.vm.ChatViewModel
import com.foggland.wearai.ui.chat.ChatScreen
import com.foggland.wearai.ui.conversations.ConversationsScreen
import com.foggland.wearai.ui.settings.ModelManagementScreen
import com.foggland.wearai.ui.settings.SettingsScreen

private const val SCREEN_CHAT = "chat"
private const val SCREEN_SETTINGS = "settings"
private const val SCREEN_MODELS = "models"
private const val SCREEN_CONVERSATIONS = "conversations"

/**
 * 手机端根导航：聊天 → 设置 → 模型管理 → 对话记录。
 */
@Composable
fun WearAIApp(viewModel: ChatViewModel) {
    var screen by rememberSaveable { mutableStateOf(SCREEN_CHAT) }

    // 拦截系统返回键：在子页面返回上一级，而不是直接退出应用
    BackHandler(enabled = screen != SCREEN_CHAT) {
        when (screen) {
            SCREEN_SETTINGS -> screen = SCREEN_CHAT
            SCREEN_MODELS -> screen = SCREEN_SETTINGS
            SCREEN_CONVERSATIONS -> screen = SCREEN_CHAT
        }
    }

    when (screen) {
        SCREEN_CHAT -> ChatScreen(
            viewModel = viewModel,
            onOpenSettings = { screen = SCREEN_SETTINGS },
            onOpenConversations = { screen = SCREEN_CONVERSATIONS },
        )

        SCREEN_SETTINGS -> SettingsScreen(
            viewModel = viewModel,
            onBack = { screen = SCREEN_CHAT },
            onOpenModels = { screen = SCREEN_MODELS },
        )

        SCREEN_MODELS -> ModelManagementScreen(
            viewModel = viewModel,
            onBack = { screen = SCREEN_SETTINGS },
        )

        SCREEN_CONVERSATIONS -> ConversationsScreen(
            viewModel = viewModel,
            onBack = { screen = SCREEN_CHAT },
            onOpenConversation = { screen = SCREEN_CHAT },
        )
    }
}
