package com.foggland.wearai.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
    var deleting by remember { mutableStateOf<ModelConfig?>(null) }

    fun openAdd() {
        editingId = null
        name = ""
        callingName = ""
        apiKey = ""
        endpointUrl = ""
        showEditor = true
    }

    fun openEdit(model: ModelConfig) {
        editingId = model.id
        name = model.name
        callingName = model.callingName
        apiKey = model.apiKey
        endpointUrl = model.endpointUrl
        showEditor = true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("模型管理") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = ::openAdd) {
                Icon(AppIcons.Add, contentDescription = "新增模型")
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp),
        ) {
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

    if (showEditor) {
        AlertDialog(
            onDismissRequest = { showEditor = false },
            title = { Text(if (editingId == null) "新增模型" else "编辑模型") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        placeholder = { Text("留空使用默认") },
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
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val id = editingId
                        if (id == null) {
                            viewModel.addModel(name, callingName, apiKey, endpointUrl)
                        } else {
                            viewModel.updateModel(id, name, callingName, apiKey, endpointUrl)
                        }
                        showEditor = false
                    },
                    enabled = name.isNotBlank() && callingName.isNotBlank(),
                ) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditor = false }) {
                    Text("取消")
                }
            },
        )
    }

    deleting?.let { model ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除模型") },
            text = { Text("确定删除「${model.name}」（${model.callingName}）吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteModel(model.id)
                        deleting = null
                    },
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleting = null }) {
                    Text("取消")
                }
            },
        )
    }
}
