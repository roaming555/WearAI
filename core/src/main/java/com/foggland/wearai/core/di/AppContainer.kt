package com.foggland.wearai.core.di

import android.content.Context
import com.foggland.wearai.core.network.ZhipuClient
import com.foggland.wearai.core.repository.ChatRepository
import com.foggland.wearai.core.repository.ConversationStore
import com.foggland.wearai.core.repository.SettingsRepository

/**
 * 极简手动依赖注入容器（避免引入额外 DI 框架）。
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val settingsRepository: SettingsRepository = SettingsRepository(appContext)
    val conversationStore: ConversationStore = ConversationStore(appContext)
    val zhipuClient: ZhipuClient = ZhipuClient()
    val chatRepository: ChatRepository = ChatRepository(zhipuClient, settingsRepository)
}

/**
 * 进程级单例，供 phone 与 wear 两个模块共享同一份数据层实例。
 */
object ServiceLocator {
    @Volatile
    private var instance: AppContainer? = null

    fun get(context: Context): AppContainer =
        instance ?: synchronized(this) {
            instance ?: AppContainer(context).also { instance = it }
        }
}
