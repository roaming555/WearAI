package com.foggland.wearai.ui.chat

import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.foggland.wearai.core.vm.ChatViewModel
import com.foggland.wearai.ui.components.AppBottomBar
import com.foggland.wearai.ui.components.AppCapsuleElevation
import com.foggland.wearai.ui.components.AppIcons
import com.foggland.wearai.ui.components.AppTopBar
import com.foggland.wearai.ui.components.LocalFloatingBars
import com.foggland.wearai.ui.components.capsuleBorderStroke
import com.foggland.wearai.ui.components.capsuleHeight
import com.foggland.wearai.ui.components.capsuleShape
import com.foggland.wearai.ui.components.floatingBottomReserve
import com.foggland.wearai.ui.components.floatingTopReserve
import com.foggland.wearai.ui.components.glassColor
import com.foggland.wearai.ui.theme.LocalSurfaceTrio

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
    var imagePath by androidx.compose.runtime.saveable.rememberSaveable {
        androidx.compose.runtime.mutableStateOf<String?>(null)
    }
    val appContext = androidx.compose.ui.platform.LocalContext.current
    val multimodalEnabled = activeModel?.multimodal == true
    var showImageSourceDialog by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }
    var pendingCameraUri by androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf<android.net.Uri?>(null)
    }

    fun applyPickedImage(uri: android.net.Uri?) {
        if (uri == null) return
        when (val result = ImageStore.import(appContext, uri)) {
            is ImageStore.ImportResult.Success -> imagePath = result.path
            is ImageStore.ImportResult.Failure ->
                android.widget.Toast.makeText(appContext, result.reason, android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    // 相册选图
    val galleryPicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent(),
    ) { uri -> applyPickedImage(uri) }

    // 拍照：先创建临时文件，拍完读取该 Uri
    val cameraLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.TakePicture(),
    ) { success ->
        if (success) applyPickedImage(pendingCameraUri)
        pendingCameraUri = null
    }
    val trio = LocalSurfaceTrio.current

    // 新消息或切换会话时滚动到底部；流式增长由 reverseLayout 锚定底部，不再打断手动上滑
    LaunchedEffect(conversation?.id, messages.size) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(0)
        }
    }

    val floating = settings.floatingBars
    val topReserve = floatingTopReserve()
    val bottomReserve = floatingBottomReserve()

    val header: @Composable (Modifier) -> Unit = { m ->
        AppTopBar(
            modifier = m,
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
                            color = trio.onTopBar.copy(alpha = 0.72f),
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
    }


    if (showImageSourceDialog) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showImageSourceDialog = false },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        ) {
            com.foggland.wearai.ui.components.ScaledDialogContent(settings.uiScale) {
                androidx.compose.material3.Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.86f)
                        .widthIn(max = 360.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("添加图片", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "选择图片来源",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    showImageSourceDialog = false
                                    galleryPicker.launch("image/*")
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("从相册选择") }
                            androidx.compose.material3.OutlinedButton(
                                onClick = {
                                    showImageSourceDialog = false
                                    val dir = java.io.File(appContext.cacheDir, "camera").apply { mkdirs() }
                                    val file = java.io.File(dir, "shot_" + System.currentTimeMillis() + ".jpg")
                                    val uri = androidx.core.content.FileProvider.getUriForFile(
                                        appContext,
                                        appContext.packageName + ".fileprovider",
                                        file,
                                    )
                                    pendingCameraUri = uri
                                    cameraLauncher.launch(uri)
                                },
                                modifier = Modifier.weight(1f),
                            ) { Text("拍照") }
                        }
                    }
                }
            }
        }
    }

    val inputBar: @Composable (Modifier) -> Unit = { m ->
        AppBottomBar(modifier = m) {
            ChatInputBar(
                value = input,
                onValueChange = { input = it },
                isGenerating = isGenerating,
                multimodalEnabled = multimodalEnabled,
                imagePath = imagePath,
                onPickImage = { showImageSourceDialog = true },
                onClearImage = { imagePath = null },
                onSend = {
                    viewModel.send(input, imagePath)
                    input = ""
                    imagePath = null
                },
                onStop = { viewModel.stop() },
            )
        }
    }

    val messageList: @Composable (Modifier, androidx.compose.foundation.layout.PaddingValues) -> Unit =
        { m, padding ->
            LazyColumn(
                state = listState,
                modifier = m,
                reverseLayout = true,
                contentPadding = padding,
            ) {
                items(messages.asReversed()) { message ->
                    MessageBubble(message)
                }
            }
        }

    if (floating) {
        CompositionLocalProvider(LocalContentColor provides trio.onBackground) {
            Box(modifier = Modifier.fillMaxSize()) {
                messageList(
                    Modifier.fillMaxSize().imePadding(),
                    androidx.compose.foundation.layout.PaddingValues(
                        start = 8.dp,
                        end = 8.dp,
                        top = topReserve + 8.dp,
                        bottom = bottomReserve + 8.dp,
                    ),
                )
                header(Modifier.align(Alignment.TopCenter))
                inputBar(Modifier.align(Alignment.BottomCenter))
            }
        }
    } else {
        Scaffold(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = trio.onBackground,
            topBar = { header(Modifier) },
            bottomBar = { inputBar(Modifier) },
        ) { innerPadding ->
            messageList(
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                androidx.compose.foundation.layout.PaddingValues(vertical = 8.dp),
            )
        }
    }
}

@Composable
private fun ChatInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    isGenerating: Boolean,
    multimodalEnabled: Boolean,
    imagePath: String?,
    onPickImage: () -> Unit,
    onClearImage: () -> Unit,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    val trio = LocalSurfaceTrio.current
    val floating = LocalFloatingBars.current

    AppBottomBar {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 待发送附件预览（输入框上方）
            if (imagePath != null) {
                AttachmentPreview(path = imagePath, onRemove = onClearImage)
                Spacer(Modifier.size(6.dp))
            }
        if (floating) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (multimodalEnabled) {
                    Surface(
                        shape = capsuleShape(),
                        color = glassColor(trio.input),
                        contentColor = trio.onInput,
                        border = capsuleBorderStroke(trio.onInput),
                        shadowElevation = AppCapsuleElevation,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(capsuleHeight()),
                    ) {
                        Box(
                            modifier = Modifier.width(capsuleHeight()),
                            contentAlignment = Alignment.Center,
                        ) {
                            IconButton(onClick = onPickImage) {
                                Icon(AppIcons.Add, contentDescription = "添加图片")
                            }
                        }
                    }
                    Spacer(Modifier.size(8.dp))
                }
                // 输入框：自己就是一个胶囊，内部不再有第二层圆角
                Surface(
                    shape = capsuleShape(),
                    color = glassColor(trio.input),
                    contentColor = trio.onInput,
                    border = capsuleBorderStroke(trio.onInput),
                    shadowElevation = AppCapsuleElevation,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = capsuleHeight()),
                ) {
                    Box {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = capsuleHeight())
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            BasicTextField(
                                value = value,
                                onValueChange = onValueChange,
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyLarge.copy(color = trio.onInput),
                                cursorBrush = SolidColor(trio.accent),
                                maxLines = 6,
                                decorationBox = { inner ->
                                    if (value.isEmpty()) {
                                        Text(
                                            text = "输入消息…",
                                            style = MaterialTheme.typography.bodyLarge,
                                            color = trio.onInput.copy(alpha = 0.55f),
                                            maxLines = 1,
                                        )
                                    }
                                    inner()
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.size(8.dp))

                // 发送 / 停止：独立圆形按钮，与输入框分开
                Surface(
                    shape = capsuleShape(),
                    color = glassColor(trio.accent),
                    contentColor = trio.onAccent,
                    border = capsuleBorderStroke(trio.onAccent),
                    shadowElevation = AppCapsuleElevation,
                    tonalElevation = 0.dp,
                    modifier = Modifier.height(capsuleHeight()),
                ) {
                    Box(
                        modifier = Modifier.width(capsuleHeight()),
                        contentAlignment = Alignment.Center,
                    ) {
                        // 发送 / 停止 图标带淡入淡出与缩放的交叉动画
                        androidx.compose.animation.AnimatedContent(
                            targetState = isGenerating,
                            transitionSpec = {
                                (androidx.compose.animation.fadeIn(
                                    animationSpec = androidx.compose.animation.core.tween(180),
                                ) + androidx.compose.animation.scaleIn(
                                    initialScale = 0.7f,
                                    animationSpec = androidx.compose.animation.core.spring(
                                        dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                        stiffness = androidx.compose.animation.core.Spring.StiffnessMedium,
                                    ),
                                )).togetherWith(
                                    androidx.compose.animation.fadeOut(
                                        animationSpec = androidx.compose.animation.core.tween(120),
                                    ) + androidx.compose.animation.scaleOut(
                                        targetScale = 0.7f,
                                        animationSpec = androidx.compose.animation.core.tween(120),
                                    ),
                                )
                            },
                            label = "sendIcon",
                        ) { generating ->
                            if (generating) {
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
            }
        } else {
            // 通栏模式：圆润的胶囊输入框（细边）+ 朴素发送图标
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (multimodalEnabled) {
                    IconButton(onClick = onPickImage) {
                        Icon(AppIcons.Add, contentDescription = "添加图片")
                    }
                }
                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("输入消息…") },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
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
    }
}


/** 待发送图片的缩略图预览：72dp 圆角小图 + 右上角移除按钮 */
@Composable
private fun AttachmentPreview(path: String, onRemove: () -> Unit) {
    val thumb = androidx.compose.runtime.remember(path) { ImageStore.loadThumbnail(path, 256) }
    Box(
        modifier = Modifier
            .padding(start = 4.dp)
            .size(72.dp),
    ) {
        if (thumb != null) {
            androidx.compose.foundation.Image(
                bitmap = thumb.asImageBitmap(),
                contentDescription = "待发送图片",
                modifier = Modifier
                    .size(72.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .size(20.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.55f))
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                AppIcons.Close,
                contentDescription = "移除图片",
                tint = androidx.compose.ui.graphics.Color.White,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

