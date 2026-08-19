package com.foggland.wearai.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * 渲染 Markdown：普通段落/标题/引用可选中，代码块渲染为带语言与复制按钮的代码框，
 * 并支持实时 LaTeX 数学公式（行内 `$...$` 与块级 `$$...$$`）。
 */
@Composable
fun MarkdownContent(
    markdown: String,
    modifier: Modifier = Modifier,
    baseTextStyle: TextStyle? = null,
) {
    val colorScheme = MaterialTheme.colorScheme
    val bodyStyle = baseTextStyle ?: MaterialTheme.typography.bodyLarge
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Paragraph -> SelectionContainer {
                    InlineMarkdownText(block.text, bodyStyle)
                }

                is MarkdownBlock.Heading -> SelectionContainer {
                    InlineMarkdownText(
                        block.text,
                        bodyStyle.copy(
                            fontSize = when (block.level) {
                                1 -> 21.sp
                                2 -> 19.sp
                                else -> 17.sp
                            },
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }

                is MarkdownBlock.Blockquote -> SelectionContainer {
                    InlineMarkdownText(
                        block.text,
                        bodyStyle.copy(
                            color = colorScheme.onSurfaceVariant,
                            fontStyle = FontStyle.Italic,
                        ),
                    )
                }

                is MarkdownBlock.CodeBlock -> CodeBox(language = block.language, code = block.code)

                is MarkdownBlock.MathBlock -> BlockMath(block.latex)

                is MarkdownBlock.HorizontalRule -> HorizontalDivider(color = colorScheme.outlineVariant)
            }
        }
    }
}

@Composable
private fun InlineMarkdownText(text: String, style: TextStyle) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val fontSizePx = with(density) {
        (if (style.fontSize.type == TextUnitType.Unspecified) 16.sp else style.fontSize)
            .toPx().toInt().coerceAtLeast(12)
    }
    val mathColor = LocalContentColor.current

    val segments = remember(text) { splitInlineMath(text) }

    val mathBitmaps = remember(segments, colorScheme, density) {
        segments.filterIsInstance<InlineSegment.Math>().map { seg ->
            renderLatex(seg.latex, fontSizePx, mathColor)
        }
    }

    val annotated = remember(segments, colorScheme) {
        buildAnnotatedString {
            var mathIndex = 0
            segments.forEach { seg ->
                when (seg) {
                    is InlineSegment.Text -> appendInlineMarkdown(
                        seg.value,
                        colorScheme.primary,
                        colorScheme.onSurfaceVariant.copy(alpha = 0.15f),
                    )

                    is InlineSegment.Math -> {
                        appendInlineContent("math_$mathIndex", seg.latex)
                        mathIndex++
                    }
                }
            }
        }
    }

    val inlineContent = remember(mathBitmaps, density) {
        val map = mutableMapOf<String, InlineTextContent>()
        mathBitmaps.forEachIndexed { i, bmp ->
            if (bmp != null) {
                map["math_$i"] = InlineTextContent(
                    Placeholder(
                        width = with(density) { (bmp.width / density.density).sp },
                        height = with(density) { (bmp.height / density.density).sp },
                        placeholderVerticalAlign = PlaceholderVerticalAlign.Center,
                    ),
                ) { Image(bitmap = bmp, contentDescription = "公式") }
            }
        }
        map
    }

    Text(text = annotated, style = style, inlineContent = inlineContent)
}

@Composable
private fun BlockMath(latex: String) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val mathColor = LocalContentColor.current
    val textSizePx = with(density) { 20.sp.toPx().toInt() }
    val bitmap = remember(latex, mathColor, density) {
        renderLatex(latex, textSizePx, mathColor)
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = "公式")
        } else {
            Text("$$latex$$", style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun CodeBox(language: String, code: String) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var copied by remember { mutableStateOf(false) }

    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 4.dp, top = 2.dp, bottom = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = language.ifBlank { "text" },
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clickable {
                            copyToClipboard(context, code)
                            copied = true
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (copied) AppIcons.Check else AppIcons.Copy,
                        contentDescription = if (copied) "已复制" else "复制代码",
                        tint = if (copied) colorScheme.primary else colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            HorizontalDivider(color = colorScheme.outlineVariant)
            SelectionContainer {
                Text(
                    text = code,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    color = colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("code", text))
}
