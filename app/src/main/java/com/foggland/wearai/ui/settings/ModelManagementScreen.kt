package com.foggland.wearai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import com.foggland.wearai.ui.components.ScaledDialogContent
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.foggland.wearai.core.model.ModelConfig
import com.foggland.wearai.core.vm.ChatViewModel
import com.foggland.wearai.ui.components.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagementScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
) {
    val models by viewModel.models.collectAsState()
    val settings by viewModel.settings.collectAsState()

    var showEditor by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var name by remember { mutableStateOf("") }
    var callingName by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    var endpointUrl by remember { mutableStateOf("") }
    var multimodal by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ModelConfig?>(null) }

    fun openAdd() {
        editingId = null
        name = ""
        callingName = ""
        apiKey = ""
        endpointUrl = ""
        multimodal = false
        showEditor = true
    }

    fun openEdit(model: ModelConfig) {
        editingId = model.id
        name = model.name
        callingName = model.callingName
        apiKey = model.apiKey
        endpointUrl = model.endpointUrl
        multimodal = model.multimodal
        showEditor = true
    }

    val trio = com.foggland.wearai.ui.theme.LocalSurfaceTrio.current
    val floating = com.foggland.wearai.ui.components.LocalFloatingBars.current
    val topReserve = com.foggland.wearai.ui.components.floatingTopReserve()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = trio.onBackground,
        contentWindowInsets = if (floating) {
            androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
        } else {
            androidx.compose.material3.ScaffoldDefaults.contentWindowInsets
        },
        topBar = {
            if (!floating) {
                com.foggland.wearai.ui.components.AppTopBar(
                    title = { Text("模型管理") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(AppIcons.ArrowBack, contentDescription = "返回")
                        }
                    },
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            state = listState,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = (if (floating) topReserve else 0.dp) + 4.dp,
                bottom = 24.dp,
            ),
        ) {
            item(key = "add_model_button") {
                androidx.compose.material3.Button(
                    onClick = ::openAdd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(AppIcons.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(8.dp))
                    Text("新增模型", style = MaterialTheme.typography.bodyLarge)
                }
            }

            items(models) { model ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                model.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            if (settings.activeModelId == model.id) {
                                Spacer(Modifier.size(6.dp))
                                Text(
                                    "●",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                        Text(
                            model.callingName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { openEdit(model) }) {
                        Icon(AppIcons.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { deleting = model }) {
                        Icon(
                            AppIcons.Delete,
                            contentDescription = "删除",
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant,
                )
            }
        }
    }
        if (floating) {
            com.foggland.wearai.ui.components.AppTopBar(
                modifier = Modifier.align(Alignment.TopCenter),
                title = { Text("模型管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        }
    }

    if (showEditor) {
        // 用 Dialog 自绘，让弹窗内容也套用全局界面缩放（LocalDensity 不会自动传入弹窗窗口）
        Dialog(
            onDismissRequest = { showEditor = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            ScaledDialogContent(settings) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .widthIn(max = 460.dp)
                        .fillMaxHeight(0.92f)
                        .heightIn(max = 560.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (editingId == null) "新增模型" else "编辑模型",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("显示名称") },
                                placeholder = { Text("如：GLM-4.7-Flash") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = callingName,
                                onValueChange = { callingName = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("调用名（model）") },
                                placeholder = { Text("如：glm-4.7-flash") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("API Key") },
                                singleLine = true,
                            )
                            OutlinedTextField(
                                value = endpointUrl,
                                onValueChange = { endpointUrl = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("接入地址（chat/completions）") },
                                placeholder = { Text("留空使用默认") },
                                singleLine = true,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text("多模态模型", style = MaterialTheme.typography.bodyLarge)
                                    Text(
                                        "开启后聊天框会出现上传图片按钮",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Switch(checked = multimodal, onCheckedChange = { multimodal = it })
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { showEditor = false }) {
                                Text("取消")
                            }
                            TextButton(
                                onClick = {
                                    val id = editingId
                                    if (id == null) {
                                        viewModel.addModel(name, callingName, apiKey, endpointUrl, multimodal)
                                    } else {
                                        viewModel.updateModel(id, name, callingName, apiKey, endpointUrl, multimodal)
                                    }
                                    showEditor = false
                                },
                                enabled = name.isNotBlank() && callingName.isNotBlank(),
                            ) {
                                Text("保存")
                            }
                        }
                    }
                }
            }
        }
    }

    deleting?.let { model ->
        Dialog(
            onDismissRequest = { deleting = null },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            ScaledDialogContent(settings) {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .widthIn(max = 420.dp),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("删除模型", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(12.dp))
                        Text("确定删除「${model.name}」（${model.callingName}）吗？")
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                        ) {
                            TextButton(onClick = { deleting = null }) {
                                Text("取消")
                            }
                            TextButton(
                                onClick = {
                                    viewModel.deleteModel(model.id)
                                    deleting = null
                                },
                            ) {
                                Text("删除", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}
