package com.foggland.wearai.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.foggland.wearai.core.model.AppSettings
import com.foggland.wearai.core.util.roundTo
import com.foggland.wearai.core.vm.ChatViewModel
import com.foggland.wearai.ui.components.AppIcons
import com.foggland.wearai.ui.components.AppScreenLayout
import com.foggland.wearai.ui.components.AppTopBar
import com.foggland.wearai.ui.components.LocalFloatingBars
import com.foggland.wearai.ui.components.floatingTopReserve
import com.foggland.wearai.ui.theme.AppSmallRadius
import com.foggland.wearai.ui.theme.CUSTOM_SEED_PALETTE
import com.foggland.wearai.ui.theme.IMPRESSIONIST_PALETTES
import com.foggland.wearai.ui.theme.LocalSurfaceTrio
import com.foggland.wearai.ui.theme.SurfaceTrio
import com.foggland.wearai.ui.theme.dynamicColorSupported
import com.foggland.wearai.ui.theme.importBackgroundImage
import com.foggland.wearai.ui.theme.resolve

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    onOpenModels: () -> Unit,
    onOpenAbout: () -> Unit,
) {
    val settings by viewModel.settings.collectAsState()
    val models by viewModel.models.collectAsState()
    val context = LocalContext.current

    // 选择自定义背景图：复制到应用私有目录后保存路径，长期有效
    val backgroundPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        if (uri != null) {
            val saved = runCatching {
                context.contentResolver.openInputStream(uri)?.let { stream ->
                    importBackgroundImage(context.filesDir, stream)
                }
            }.getOrNull()
            if (saved != null) viewModel.setBackground(saved)
        }
    }

    val floating = LocalFloatingBars.current
    val topReserve = floatingTopReserve()
    // 提升到页面作用域，切换悬浮开关时布局分支会重建，此处可保留滚动位置
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    AppScreenLayout(
        topBar = { m ->
            AppTopBar(
                modifier = m,
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(AppIcons.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenAbout) {
                        Icon(AppIcons.Info, contentDescription = "关于")
                    }
                },
            )
        },
        content = { m ->
            LazyColumn(
                modifier = m,
                state = listState,
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = (if (floating) topReserve else 0.dp) + 4.dp,
                    bottom = 16.dp,
                ),
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

            item(key = "title_model_none") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.setTitleModel(AppSettings.TITLE_MODEL_NONE) }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(
                        selected = settings.titleModelId == AppSettings.TITLE_MODEL_NONE,
                        onClick = null,
                    )
                    Spacer(Modifier.size(4.dp))
                    Column(Modifier.weight(1f)) {
                        Text("无", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "不调用 AI，直接用「x月x日x时的对话」命名",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

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

            item(key = "switch_ui_scale_auto") {
                SwitchRow(
                    title = "自动适配屏幕",
                    subtitle = "根据屏幕宽度自动选择合适的界面尺寸；关闭后可手动调节",
                    checked = settings.uiScaleAuto,
                    onCheckedChange = viewModel::setUiScaleAuto,
                )
            }

            if (!settings.uiScaleAuto) {
            item(key = "slider_ui_scale") {
                SliderRow(
                    title = "界面尺寸（全局缩放）",
                    value = settings.uiScale,
                    valueRange = AppSettings.MIN_UI_SCALE..AppSettings.MAX_UI_SCALE,
                    display = "×%.1f".format(settings.uiScale),
                ) { v ->
                    viewModel.setUiScale(v)
                }
            }
            }

            item(key = "switch_floating_bars") {
                SwitchRow(
                    title = "圆角悬浮栏",
                    subtitle = "开启后工具栏与输入栏变为悬空的胶囊样式",
                    checked = settings.floatingBars,
                    onCheckedChange = viewModel::setFloatingBars,
                )
            }

            if (settings.floatingBars) {
                item(key = "slider_capsule_radius") {
                    SliderRow(
                        title = "胶囊圆角半径（R 角）",
                        value = settings.capsuleRadius,
                        valueRange = AppSettings.MIN_CAPSULE_RADIUS..AppSettings.MAX_CAPSULE_RADIUS,
                        display = "%.0f dp".format(settings.capsuleRadius),
                        onValueChange = viewModel::setCapsuleRadius,
                    )
                }

                item(key = "switch_capsule_border") {
                    SwitchRow(
                        title = "胶囊描边",
                        subtitle = "在胶囊外圈加细边（深色模式下为浅色），提升辨识度",
                        checked = settings.capsuleBorder,
                        onCheckedChange = viewModel::setCapsuleBorder,
                    )
                }
            }

            item(key = "switch_translucent_bars") {
                SwitchRow(
                    title = "半透明栏",
                    subtitle = "导航栏与输入区半透明，透出内容（不依赖背景图）",
                    checked = settings.translucentBars,
                    onCheckedChange = viewModel::setTranslucentBars,
                )
            }

            if (settings.translucentBars) {
                item(key = "slider_translucent_alpha") {
                    SliderRow(
                        title = "不透明度（越小越透）",
                        value = settings.translucentAlpha,
                        valueRange = AppSettings.MIN_GLASS_ALPHA..AppSettings.MAX_GLASS_ALPHA,
                        display = "%.0f%%".format(settings.translucentAlpha * 100),
                        onValueChange = viewModel::setTranslucentAlpha,
                    )
                }
            }

            item(key = "section_theme") { SectionTitle("主题") }

            item(key = "theme_presets") {
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Text("配色方案", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        text = "工具栏 · 消息 · 输入区 三色搭配，低对比邻近色，柔和耐看",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(8.dp))
                    ThemePresetPicker(
                        themeId = settings.themeId,
                        onSelect = viewModel::setThemeId,
                    )
                }
            }

            if (settings.themeId == AppSettings.THEME_CUSTOM) {
                item(key = "theme_custom_seed") {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        Text("自定义主色", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(8.dp))
                        CustomSeedPicker(
                            selected = settings.customSeed,
                            onSelect = viewModel::setCustomSeed,
                        )
                        Spacer(Modifier.height(12.dp))
                        RgbColorSliders(
                            argb = settings.customSeed,
                            onChange = viewModel::setCustomSeed,
                        )
                    }
                }
            }

            item(key = "theme_background") {
                ThemeBackgroundRow(
                    hasBackground = settings.backgroundPath != null,
                    onPick = { backgroundPicker.launch("image/*") },
                    onClear = { viewModel.setBackground(null) },
                )
            }

            if (settings.backgroundPath != null) {
                item(key = "slider_background_dim") {
                    SliderRow(
                        title = "背景蒙层（越大文字越清晰）",
                        value = settings.backgroundDim,
                        valueRange = 0f..0.9f,
                        display = "%.0f%%".format(settings.backgroundDim * 100),
                        onValueChange = viewModel::setBackgroundDim,
                    )
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
        },
    )
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

/** 内置印象派配色 + 动态取色（支持时）+ 自定义。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ThemePresetPicker(themeId: String, onSelect: (String) -> Unit) {
    val isDark = MaterialTheme.colorScheme.background.hueIsDark()
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        IMPRESSIONIST_PALETTES.forEach { palette ->
            val trio = palette.resolve(isDark)
            FilterChip(
                selected = themeId == palette.id,
                onClick = { onSelect(palette.id) },
                label = { Text(palette.label) },
                shape = RoundedCornerShape(
                    com.foggland.wearai.ui.theme.LocalSmallRadius.current,
                ),
                leadingIcon = { TriColorSwatch(trio) },
            )
        }
        if (dynamicColorSupported) {
            FilterChip(
                selected = themeId == AppSettings.THEME_DYNAMIC,
                onClick = { onSelect(AppSettings.THEME_DYNAMIC) },
                label = { Text("动态取色") },
            )
        }
        FilterChip(
            selected = themeId == AppSettings.THEME_CUSTOM,
            onClick = { onSelect(AppSettings.THEME_CUSTOM) },
            label = { Text("自定义") },
        )
    }
}

/** 用工具栏/消息/输入区三色做渐变小圆点，直观预览一套主题。 */
@Composable
private fun TriColorSwatch(trio: SurfaceTrio) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(trio.topBar, trio.message, trio.input),
                ),
            ),
    )
}

private fun Color.hueIsDark(): Boolean =
    (0.299f * red + 0.587f * green + 0.114f * blue) < 0.5f

/** 自定义主色选择：一排可点选的色块。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomSeedPicker(selected: Int, onSelect: (Int) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CUSTOM_SEED_PALETTE.forEach { color ->
            val argb = color.toArgb()
            val isSelected = argb == selected
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.outlineVariant
                        },
                        shape = CircleShape,
                    )
                    .clickable { onSelect(argb) },
            )
        }
    }
}

/** 自定义主色：色板快选 + RGB 三通道精确调节。 */
@Composable
private fun RgbColorSliders(argb: Int, onChange: (Int) -> Unit) {
    val red = (argb shr 16) and 0xFF
    val green = (argb shr 8) and 0xFF
    val blue = argb and 0xFF

    fun emit(r: Int = red, g: Int = green, b: Int = blue) {
        onChange((0xFF shl 24) or (r shl 16) or (g shl 8) or b)
    }

    val hexValue = "%02X%02X%02X".format(red, green, blue)
    val hexState = androidx.compose.runtime.remember(hexValue) {
        androidx.compose.runtime.mutableStateOf(hexValue)
    }
    val errorState = androidx.compose.runtime.remember {
        androidx.compose.runtime.mutableStateOf(false)
    }

    val lightness = ((maxOf(red, green, blue) + minOf(red, green, blue)) / 2f) / 255f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(argb)),
            )
            Spacer(Modifier.size(10.dp))
            androidx.compose.material3.OutlinedTextField(
                value = hexState.value,
                onValueChange = { input ->
                    val filtered = input
                        .uppercase()
                        .filter { it.isDigit() || it in 'A'..'F' }
                        .take(6)
                    hexState.value = filtered
                    if (filtered.length == 6) {
                        val parsed = filtered.toIntOrNull(16)
                        errorState.value = parsed == null
                        if (parsed != null) {
                            onChange((0xFF shl 24) or parsed)
                        }
                    } else {
                        errorState.value = filtered.isNotEmpty() && filtered.length < 6
                    }
                },
                modifier = Modifier.weight(1f),
                singleLine = true,
                isError = errorState.value,
                label = { Text("十六进制 #RRGGBB") },
                textStyle = MaterialTheme.typography.bodyMedium,
                shape = RoundedCornerShape(AppSmallRadius),
            )
        }

        Spacer(Modifier.size(6.dp))

        RgbSliderRow("R", red, Color(0xFFE05B5B)) { emit(r = it) }
        RgbSliderRow("G", green, Color(0xFF4CA863)) { emit(g = it) }
        RgbSliderRow("B", blue, Color(0xFF4D7BD0)) { emit(b = it) }

        // 明度（HSL 中的 L）：保留色相，整体提亮或压暗
        RgbSliderRow(
            label = "L",
            value = (lightness * 100f).toInt(),
            tint = Color(argb),
            range = 0..100,
        ) { l ->
            val target = l / 100f
            val cur = lightness
            if (target >= cur) {
                val k = if (cur >= 1f) 0f else (target - cur) / (1f - cur)
                emit(
                    r = (red + (255 - red) * k).toInt().coerceIn(0, 255),
                    g = (green + (255 - green) * k).toInt().coerceIn(0, 255),
                    b = (blue + (255 - blue) * k).toInt().coerceIn(0, 255),
                )
            } else {
                val k = if (cur <= 0f) 0f else target / cur
                emit(
                    r = (red * k).toInt().coerceIn(0, 255),
                    g = (green * k).toInt().coerceIn(0, 255),
                    b = (blue * k).toInt().coerceIn(0, 255),
                )
            }
        }
    }
}

@Composable
private fun RgbSliderRow(
    label: String,
    value: Int,
    tint: Color,
    range: IntRange = 0..255,
    onValueChange: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(18.dp),
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(kotlin.math.round(it).toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            colors = androidx.compose.material3.SliderDefaults.colors(
                thumbColor = tint,
                activeTrackColor = tint,
            ),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp),
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(30.dp),
        )
    }
}

@Composable
private fun ThemeBackgroundRow(
    hasBackground: Boolean,
    onPick: () -> Unit,
    onClear: () -> Unit,
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
                .padding(end = 12.dp),
        ) {
            Text("背景图片", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (hasBackground) "已设置自定义背景" else "当前使用主题纯色背景",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(onClick = onPick) { Text(if (hasBackground) "更换" else "选择") }
        if (hasBackground) {
            TextButton(onClick = onClear) {
                Text("清除", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
