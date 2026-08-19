package com.foggland.wearai.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foggland.wearai.core.model.AppSettings
import com.foggland.wearai.core.util.roundTo
import com.foggland.wearai.core.vm.ChatViewModel
import com.foggland.wearai.ui.components.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenModels: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()
    val models by viewModel.models.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            item(key = "section_model") { SectionTitle("模型") }

            items(models, key = { "model_${it.id}" }) { model ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.selectModel(model.id) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = settings.activeModelId == model.id,
                        onClick = null,
                    )
                    Spacer(Modifier.size(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(model.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            model.callingName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item(key = "manage_models") {
                TextButton(onClick = onOpenModels) {
                    Icon(AppIcons.Settings, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("管理模型（可单独配置 API Key / 接入地址）")
                }
            }

            item(key = "section_title_model") { SectionTitle("标题总结模型") }

            items(models, key = { "title_model_${it.id}" }) { model ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setTitleModel(model.id) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = settings.titleModelId == model.id,
                        onClick = null,
                    )
                    Spacer(Modifier.size(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text(model.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            model.callingName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item(key = "section_generate") { SectionTitle("生成") }

            item(key = "switch_thinking") {
                SwitchRow(
                    title = "深度思考",
                    subtitle = "开启后模型会先输出推理过程，再给出答案",
                    checked = settings.deepThinking,
                    onCheckedChange = viewModel::setDeepThinking,
                )
            }
            item(key = "switch_stream") {
                SwitchRow(
                    title = "流式输出",
                    subtitle = "逐字显示模型回复（SSE）",
                    checked = settings.streaming,
                    onCheckedChange = viewModel::setStreaming,
                )
            }

            item(key = "section_advanced") { SectionTitle("高级参数") }

            item(key = "slider_temperature") {
                SliderRow(
                    title = "temperature 采样温度",
                    value = settings.temperature.toFloat(),
                    valueRange = 0f..1f,
                    display = "%.2f".format(settings.temperature),
                ) { v ->
                    viewModel.updateSettings { it.copy(temperature = v.toDouble().roundTo(2)) }
                }
            }
            item(key = "slider_top_p") {
                SliderRow(
                    title = "top_p 核采样",
                    value = settings.topP.toFloat(),
                    valueRange = 0f..1f,
                    display = "%.2f".format(settings.topP),
                ) { v ->
                    viewModel.updateSettings { it.copy(topP = v.toDouble().roundTo(2)) }
                }
            }
            item(key = "slider_max_tokens") {
                SliderRow(
                    title = "max_tokens 最大输出",
                    value = settings.maxTokens.toFloat(),
                    valueRange = 256f..8192f,
                    display = settings.maxTokens.toString(),
                ) { v ->
                    viewModel.updateSettings { it.copy(maxTokens = v.toInt()) }
                }
            }

            item(key = "section_display") { SectionTitle("显示") }

            item(key = "switch_dark_mode") {
                SwitchRow(
                    title = "深色模式",
                    subtitle = "开启后强制深色，关闭时跟随系统",
                    checked = settings.darkMode,
                    onCheckedChange = viewModel::setDarkMode,
                )
            }

            item(key = "slider_ui_scale") {
                SliderRow(
                    title = "界面尺寸（全局缩放）",
                    value = settings.uiScale,
                    valueRange = AppSettings.MIN_UI_SCALE..AppSettings.MAX_UI_SCALE,
                    display = "×%.1f".format(settings.uiScale),
                ) { v ->
                    viewModel.updateSettings { it.copy(uiScale = v.roundTo(2)) }
                }
            }

            item(key = "section_prompt") { SectionTitle("提示词") }

            item(key = "field_system_prompt") {
                OutlinedTextField(
                    value = settings.systemPrompt,
                    onValueChange = { v -> viewModel.updateSettings { it.copy(systemPrompt = v) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("系统提示词") },
                    minLines = 3,
                )
            }

            item(key = "bottom_spacer") { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
    )
    HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SliderRow(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    display: String,
    onValueChange: (Float) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
            )
            Text(
                text = display,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
        )
    }
}
