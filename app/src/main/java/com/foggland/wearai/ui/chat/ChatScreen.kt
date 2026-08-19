package com.foggland.wearai.ui.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.foggland.wearai.core.vm.ChatViewModel
import com.foggland.wearai.ui.components.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onOpenSettings: () -> Unit,
    onOpenConversations: () -> Unit,
) {
    val messages by viewModel.messages.collectAsState()
    val isGenerating by viewModel.isGenerating.collectAsState()
    val activeModel by viewModel.activeModel.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val conversation by viewModel.currentConversation.collectAsState()
    val listState = rememberLazyListState()
    var input by rememberSaveable { mutableStateOf("") }

    // 新消息或切换会话时滚动到底部；流式增长由 reverseLayout 锚定底部，不再打断手动上滑
    LaunchedEffect(conversation?.id, messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = conversation?.title ?: "WearAI",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        val subtitle = buildList {
                            activeModel?.name?.let { add(it) }
                            if (settings.deepThinking) add("深度思考")
                            if (settings.streaming) add("流式")
                        }
                        if (subtitle.isNotEmpty()) {
                            Text(
                                text = subtitle.joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.newConversation() }) {
                        Icon(AppIcons.Chat, contentDescription = "新对话")
                    }
                    IconButton(onClick = onOpenConversations) {
                        Icon(AppIcons.History, contentDescription = "对话记录")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(AppIcons.Settings, contentDescription = "设置")
                    }
                },
            )
        },
        bottomBar = {
            ChatInputBar(
                value = input,
                onValueChange = { input = it },
                isGenerating = isGenerating,
                onSend = {
                    viewModel.send(input)
                    input = ""
                },
                onStop = { viewModel.stop() },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (messages.isNotEmpty()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    reverseLayout = true,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
                ) {
                    items(messages.asReversed()) { message ->
                        MessageBubble(message)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isGenerating: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Surface(
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息…") },
                shape = RoundedCornerShape(24.dp),
                maxLines = 5,
            )
            Spacer(Modifier.size(8.dp))
            if (isGenerating) {
                IconButton(onClick = onStop) {
                    Icon(AppIcons.Stop, contentDescription = "停止生成")
                }
            } else {
                IconButton(
                    onClick = onSend,
                    enabled = value.isNotBlank(),
                ) {
                    Icon(AppIcons.Send, contentDescription = "发送")
                }
            }
        }
    }
}


