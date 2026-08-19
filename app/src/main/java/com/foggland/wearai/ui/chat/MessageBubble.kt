package com.foggland.wearai.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.foggland.wearai.core.model.ChatMessage
import com.foggland.wearai.core.model.ChatRole
import com.foggland.wearai.ui.components.MarkdownContent

/**
 * 单条消息气泡。用户消息靠右，助手消息靠左；助手可带“深度思考”卡片。
 * 文字可长按选中复制；助手正文实时渲染 Markdown。
 */
@Composable
fun MessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isUser = message.role == ChatRole.USER
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier.widthIn(max = 340.dp),
            horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        ) {
            if (!isUser && (message.reasoningContent.isNotBlank() || message.isStreaming)) {
                ReasoningCard(
                    reasoning = message.reasoningContent,
                    isStreaming = message.isStreaming,
                )
                Spacer(Modifier.height(6.dp))
            }
            Surface(
                shape = bubbleShape(isUser),
                color = if (isUser) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                },
                contentColor = if (isUser) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                if (message.content.isBlank() && message.isStreaming) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(14.dp),
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.size(8.dp))
                        Text(
                            text = "思考中…",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    MessageContent(message = message, isUser = isUser)
                }
            }
            if (message.isError) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "⚠ 生成被中断，可重新发送",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun MessageContent(message: ChatMessage, isUser: Boolean) {
    val padding = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
    if (isUser) {
        SelectionContainer {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyLarge,
                modifier = padding,
            )
        }
    } else {
        MarkdownContent(
            markdown = message.content,
            modifier = padding,
        )
    }
}

@Composable
private fun ReasoningCard(reasoning: String, isStreaming: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = if (isStreaming) "深度思考 · 进行中" else "深度思考",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f),
            )
            if (reasoning.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                MarkdownContent(
                    markdown = reasoning,
                    baseTextStyle = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

private fun bubbleShape(isUser: Boolean): RoundedCornerShape {
    return if (isUser) {
        RoundedCornerShape(18.dp, 18.dp, 4.dp, 18.dp)
    } else {
        RoundedCornerShape(18.dp, 18.dp, 18.dp, 4.dp)
    }
}
