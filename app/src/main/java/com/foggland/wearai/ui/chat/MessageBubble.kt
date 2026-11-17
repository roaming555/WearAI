package com.foggland.wearai.ui.chat

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.foggland.wearai.core.model.ChatMessage
import com.foggland.wearai.core.model.ChatRole
import com.foggland.wearai.ui.components.MarkdownContent
import com.foggland.wearai.ui.theme.LocalSurfaceTrio

/**
 * 单条消息气泡。用户消息靠右，助手消息靠左；助手可带“深度思考”卡片。
 * 文字可长按选中复制；助手正文实时渲染 Markdown。
 */
@Composable
fun MessageBubble(message: ChatMessage, modifier: Modifier = Modifier) {
    val isUser = message.role == ChatRole.USER
    val trio = LocalSurfaceTrio.current
    // 入场：淡入 + 上滑，新消息不会突冗出现
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val enterAlpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(260),
        label = "bubbleAlpha",
    )
    val enterOffset by animateDpAsState(
        targetValue = if (appeared) 0.dp else 12.dp,
        animationSpec = tween(300),
        label = "bubbleOffset",
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .offset(y = enterOffset)
            .graphicsLayer { alpha = enterAlpha }
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
                modifier = Modifier.animateContentSize(),
                shape = bubbleShape(isUser),
                color = if (isUser) {
                    trio.userBubble
                } else {
                    trio.message
                },
                contentColor = if (isUser) {
                    trio.onUserBubble
                } else {
                    trio.onMessage
                },
            ) {
                // Surface 的内容默认是 Box（会叠在一起），
                // 这里明确用 Column，保证图片在上、文字在下
                Column(modifier = Modifier.fillMaxWidth()) {
                // 附带图片（多模态）
                message.imagePath?.let { path ->
                    val thumb = remember(path) { ImageStore.loadThumbnail(path) }
                    if (thumb != null) {
                        androidx.compose.foundation.Image(
                            bitmap = thumb.asImageBitmap(),
                            contentDescription = "图片",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .padding(6.dp)
                                .padding(bottom = 2.dp)
                                .clip(RoundedCornerShape(com.foggland.wearai.ui.theme.LocalSmallRadius.current)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        )
                    }
                }
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
    val trio = LocalSurfaceTrio.current
    Surface(
        shape = RoundedCornerShape(com.foggland.wearai.ui.theme.LocalSmallRadius.current),
        color = trio.input,
        contentColor = trio.onInput,
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = if (isStreaming) "深度思考 · 进行中" else "深度思考",
                style = MaterialTheme.typography.labelSmall,
                color = trio.onInput.copy(alpha = 0.7f),
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

@Composable
private fun bubbleShape(isUser: Boolean): RoundedCornerShape {
    // 使用全局 R 角，与卡片、代码框、对话框保持一致
    return RoundedCornerShape(com.foggland.wearai.ui.theme.LocalSmallRadius.current)
}
