package com.foggland.wearai.core.vm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.foggland.wearai.core.di.AppContainer
import com.foggland.wearai.core.model.AppSettings
import com.foggland.wearai.core.model.ChatMessage
import com.foggland.wearai.core.model.ChatRole
import com.foggland.wearai.core.model.Conversation
import com.foggland.wearai.core.model.ModelConfig
import com.foggland.wearai.core.model.Usage
import com.foggland.wearai.core.repository.ChatRepository
import com.foggland.wearai.core.repository.ConversationStore
import com.foggland.wearai.core.repository.SettingsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * 聊天 ViewModel：支持多会话本地存储与后台自动生成标题。
 */
class ChatViewModel(
    private val chatRepository: ChatRepository,
    private val settingsRepository: SettingsRepository,
    private val conversationStore: ConversationStore,
) : ViewModel() {

    private val _conversations = MutableStateFlow(conversationStore.loadAll())
    val conversations: StateFlow<List<Conversation>> = _conversations

    private val _currentId = MutableStateFlow(conversationStore.getCurrentId())

    val currentConversation: StateFlow<Conversation?> =
        combine(_conversations, _currentId) { list, id ->
            list.firstOrNull { it.id == id } ?: list.firstOrNull()
        }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val messages: StateFlow<List<ChatMessage>> = currentConversation
        .map { it?.messages ?: emptyList() }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val settings: StateFlow<AppSettings> = settingsRepository.settings
    val models: StateFlow<List<ModelConfig>> = settingsRepository.models

    val activeModel: StateFlow<ModelConfig?> = combine(settings, models) { s, ms ->
        ms.firstOrNull { it.id == s.activeModelId } ?: ms.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating

    private val _lastUsage = MutableStateFlow<Usage?>(null)
    val lastUsage: StateFlow<Usage?> = _lastUsage

    private var generationJob: Job? = null

    // 已发起过标题生成的会话 id，避免重复请求
    private val titleRequested = mutableSetOf<String>()

    /** 同步读取当前会话（避免派生 StateFlow 的异步滞后）。 */
    private fun currentConversationValue(): Conversation? {
        val id = _currentId.value
        return _conversations.value.firstOrNull { it.id == id }
            ?: _conversations.value.firstOrNull()
    }

    // ------------------------------------------------------------------
    // 会话管理
    // ------------------------------------------------------------------

    fun newConversation() {
        if (_isGenerating.value) stop()
        val current = currentConversationValue()
        if (current != null && current.messages.isEmpty() && current.title == Conversation.DEFAULT_TITLE) {
            return // 当前已是空的新会话，直接复用
        }
        val conv = Conversation(id = UUID.randomUUID().toString())
        _conversations.update { listOf(conv) + it }
        _currentId.value = conv.id
        conversationStore.setCurrentId(conv.id)
        _lastUsage.value = null
        persistConversations()
    }

    fun switchConversation(id: String) {
        if (_isGenerating.value) stop()
        _currentId.value = id
        conversationStore.setCurrentId(id)
        _lastUsage.value = null
    }

    fun deleteConversation(id: String) {
        if (_isGenerating.value && _currentId.value == id) stop()
        _conversations.update { list -> list.filterNot { it.id == id } }
        if (_currentId.value == id) {
            val next = _conversations.value.firstOrNull()?.id
            _currentId.value = next
            conversationStore.setCurrentId(next)
            _lastUsage.value = null
        }
        persistConversations()
    }

    // ------------------------------------------------------------------
    // 对话
    // ------------------------------------------------------------------

    fun send(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || _isGenerating.value) return
        var conv = currentConversationValue()
        if (conv == null) {
            newConversation()
            conv = currentConversationValue() ?: return
        }
        val newHistory = conv.messages + ChatMessage(role = ChatRole.USER, content = trimmed)
        updateCurrentConversation { it.copy(messages = newHistory) }
        startGeneration(newHistory)
    }

    private fun startGeneration(history: List<ChatMessage>) {
        generationJob = viewModelScope.launch {
            _isGenerating.value = true
            updateCurrentConversation { c ->
                c.copy(
                    messages = c.messages + ChatMessage(
                        role = ChatRole.ASSISTANT,
                        content = "",
                        reasoningContent = "",
                        isStreaming = true,
                    )
                )
            }
            try {
                val summary = chatRepository.send(history) { contentDelta, reasoningDelta ->
                    updateLastMessage { m ->
                        m.copy(
                            content = m.content + contentDelta,
                            reasoningContent = m.reasoningContent + reasoningDelta,
                        )
                    }
                }
                _lastUsage.value = summary.usage
                updateLastMessage { m -> m.copy(isStreaming = false, finishReason = summary.finishReason) }
                requestTitleIfNeeded(currentConversationValue()?.id ?: "")
            } catch (e: Exception) {
                updateLastMessage { m ->
                    val message = e.message ?: "未知错误"
                    if (m.content.isBlank() && m.reasoningContent.isBlank()) {
                        m.copy(isStreaming = false, isError = true, content = "请求失败：$message")
                    } else {
                        m.copy(isStreaming = false, isError = true)
                    }
                }
            } finally {
                _isGenerating.value = false
                persistConversations()
            }
        }
    }

    fun stop() {
        generationJob?.cancel()
        generationJob = null
        _isGenerating.value = false
        updateLastMessage { m -> if (m.isStreaming) m.copy(isStreaming = false) else m }
        persistConversations()
    }

    // ------------------------------------------------------------------
    // 标题自动生成
    // ------------------------------------------------------------------

    private fun requestTitleIfNeeded(convId: String) {
        if (convId.isEmpty()) return
        val conv = currentConversationValue() ?: return
        if (conv.id != convId) return
        if (conv.title != Conversation.DEFAULT_TITLE) return
        if (!titleRequested.add(convId)) return
        val firstUser = conv.messages.firstOrNull { it.role == ChatRole.USER && it.content.isNotBlank() } ?: return
        val firstAssistant = conv.messages.firstOrNull { it.role == ChatRole.ASSISTANT && it.content.isNotBlank() } ?: return

        viewModelScope.launch {
            val title = generateTitleWithRetry(firstUser.content, firstAssistant.content)
            if (title != null) {
                updateConversationById(convId) { it.copy(title = title) }
                persistConversations()
            } else {
                // 本次失败后移除标记，允许下一次回答后再尝试
                titleRequested.remove(convId)
            }
        }
    }

    /**
     * 带延迟与退避重试地生成标题。
     * 免费模型对连续请求存在限流（HTTP 429 / 错误码 1305），
     * 紧跟主对话请求立即调用极易被限流，因此先等待再重试。
     */
    private suspend fun generateTitleWithRetry(userMessage: String, assistantReply: String): String? {
        val waits = listOf(5000L, 10000L, 20000L, 30000L)
        for (wait in waits) {
            delay(wait)
            try {
                val title = chatRepository.generateTitle(userMessage, assistantReply)
                if (title.isNotBlank()) return title
            } catch (_: Exception) {
                // 忽略并继续重试
            }
        }
        return null
    }

    // ------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------

    private fun updateCurrentConversation(transform: (Conversation) -> Conversation) {
        val id = _currentId.value ?: return
        updateConversationById(id, transform)
    }

    private fun updateConversationById(id: String, transform: (Conversation) -> Conversation) {
        _conversations.update { list ->
            list.map { if (it.id == id) transform(it).copy(updatedAt = System.currentTimeMillis()) else it }
        }
    }

    private fun updateLastMessage(transform: (ChatMessage) -> ChatMessage) {
        updateCurrentConversation { conv ->
            val msgs = conv.messages
            if (msgs.isEmpty()) conv
            else {
                val mutable = msgs.toMutableList()
                mutable[mutable.size - 1] = transform(mutable[mutable.size - 1])
                conv.copy(messages = mutable)
            }
        }
    }

    private fun persistConversations() {
        conversationStore.saveAll(_conversations.value)
    }

    // ------------------------------------------------------------------
    // 设置与模型管理
    // ------------------------------------------------------------------

    fun updateSettings(transform: (AppSettings) -> AppSettings) = settingsRepository.update(transform)

    fun setDeepThinking(enabled: Boolean) = settingsRepository.update { it.copy(deepThinking = enabled) }

    fun setStreaming(enabled: Boolean) = settingsRepository.update { it.copy(streaming = enabled) }

    fun setDarkMode(enabled: Boolean) = settingsRepository.update { it.copy(darkMode = enabled) }

    fun selectModel(id: String) = settingsRepository.update { it.copy(activeModelId = id) }

    fun setTitleModel(id: String) = settingsRepository.update { it.copy(titleModelId = id) }

    fun addModel(name: String, callingName: String, apiKey: String, endpointUrl: String) =
        settingsRepository.addModel(name, callingName, apiKey, endpointUrl)

    fun updateModel(id: String, name: String, callingName: String, apiKey: String, endpointUrl: String) =
        settingsRepository.updateModel(id, name, callingName, apiKey, endpointUrl)

    fun deleteModel(id: String) = settingsRepository.deleteModel(id)

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChatViewModel(
                    chatRepository = container.chatRepository,
                    settingsRepository = container.settingsRepository,
                    conversationStore = container.conversationStore,
                ) as T
            }
        }
    }
}
