package com.foggland.wearai.core.repository

import com.foggland.wearai.core.model.AppSettings
import com.foggland.wearai.core.model.ChatMessage
import com.foggland.wearai.core.model.ChatRole
import com.foggland.wearai.core.network.StreamSummary
import com.foggland.wearai.core.network.ZhipuClient
import com.foggland.wearai.core.network.dto.ChatCompletionRequest
import com.foggland.wearai.core.network.dto.MessageDto
import com.foggland.wearai.core.network.dto.ThinkingDto
import com.foggland.wearai.core.util.roundTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 对话仓库：负责组装请求并调用网络层。
 */
class ChatRepository(
    private val client: ZhipuClient,
    private val settingsRepository: SettingsRepository,
) {

    suspend fun send(
        history: List<ChatMessage>,
        onDelta: (content: String, reasoning: String) -> Unit,
    ): StreamSummary = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.value
        val request = buildRequest(settings, history)
        client.chat(
            request,
            effectiveApiKey(settings, settings.activeModelId),
            effectiveEndpoint(settings, settings.activeModelId),
            onDelta,
        )
    }

    private fun buildRequest(settings: AppSettings, history: List<ChatMessage>): ChatCompletionRequest {
        val messages = mutableListOf<MessageDto>()
        if (settings.systemPrompt.isNotBlank()) {
            messages.add(MessageDto(role = ChatRole.SYSTEM.wireName, content = settings.systemPrompt))
        }
        history
            .filter { it.role != ChatRole.SYSTEM && !it.isError && it.content.isNotBlank() }
            .forEach { m ->
                messages.add(
                    MessageDto(
                        role = m.role.wireName,
                        content = m.content,
                        // 将上一次的思考内容回传给模型以保持上下文连续
                        reasoningContent = if (m.role == ChatRole.ASSISTANT && m.reasoningContent.isNotBlank()) {
                            m.reasoningContent
                        } else {
                            null
                        },
                    )
                )
            }
        return ChatCompletionRequest(
            model = modelCallingName(settings.activeModelId),
            messages = messages,
            stream = settings.streaming,
            temperature = settings.temperature.roundTo(2).coerceIn(0.0, 1.0),
            topP = settings.topP.roundTo(2),
            maxTokens = settings.maxTokens,
            thinking = ThinkingDto(if (settings.deepThinking) "enabled" else "disabled"),
        )
    }

    /**
     * 后台生成对话标题：用一次独立的非流式调用，让模型为对话总结一个简短标题。
     */
    suspend fun generateTitle(userMessage: String, assistantReply: String): String = withContext(Dispatchers.IO) {
        val settings = settingsRepository.settings.value
        val prompt = buildString {
            append("请为下面这段对话总结一个简短标题，直接输出标题本身，不要引号、不要标点、不要解释，不超过12个字。\n")
            append("用户：").append(userMessage.take(200)).append('\n')
            append("助手：").append(assistantReply.take(400))
        }
        val request = ChatCompletionRequest(
            model = modelCallingName(settings.titleModelId),
            messages = listOf(MessageDto(role = ChatRole.USER.wireName, content = prompt)),
            stream = false,
            temperature = 0.3,
            maxTokens = 64,
        )
        var title = ""
        client.chat(
            request,
            effectiveApiKey(settings, settings.titleModelId),
            effectiveEndpoint(settings, settings.titleModelId),
        ) { content, _ ->
            title += content
        }
        title.trim().trim('"', '“', '”', '「', '」', '《', '》', '。', '，', '、', '\n', ' ')
    }

    private fun effectiveApiKey(settings: AppSettings, modelId: String): String {
        val model = settingsRepository.models.value.firstOrNull { it.id == modelId }
        return model?.apiKey.orEmpty().ifBlank { settings.apiKey }
    }

    private fun effectiveEndpoint(settings: AppSettings, modelId: String): String {
        val model = settingsRepository.models.value.firstOrNull { it.id == modelId }
        return model?.endpointUrl.orEmpty().ifBlank { settings.endpointUrl }
    }

    /** 根据模型本地 id 解析实际调用名（model 字段应使用 callingName 而非本地 id）。 */
    private fun modelCallingName(modelId: String): String {
        val model = settingsRepository.models.value.firstOrNull { it.id == modelId }
        return model?.callingName ?: modelId
    }
}
