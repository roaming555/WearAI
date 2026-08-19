package com.foggland.wearai.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle

/** 一个 Markdown 块。 */
sealed class MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class MathBlock(val latex: String) : MarkdownBlock()
    data object HorizontalRule : MarkdownBlock()
}

/** 段落内的一个片段：普通文本或行内公式。 */
sealed class InlineSegment {
    data class Text(val value: String) : InlineSegment()
    data class Math(val latex: String) : InlineSegment()
}

private val INLINE_PATTERN = Regex(
    """\*\*([^*]+)\*\*|\*([^*\s][^*]*)\*|`([^`]+)`|\[([^\]]+)]\(([^)]+)\)"""
)
private val INLINE_MATH_PATTERN = Regex("""(?<!\$)\$([^$\n]+?)\$(?!\$)""")
private val LIST_PATTERN = Regex("""[-*+] .+""")
private val ORDERED_PATTERN = Regex("""\d+\. .+""")

/**
 * 将 Markdown 解析为块列表，供 [MarkdownContent] 渲染。
 */
fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = markdown.split("\n")
    var i = 0
    while (i < lines.size) {
        val raw = lines[i]
        val trimmed = raw.trim()
        when {
            trimmed.startsWith("```") -> {
                val language = trimmed.removePrefix("```").trim()
                val codeLines = mutableListOf<String>()
                i++
                while (i < lines.size && !lines[i].trim().startsWith("```")) {
                    codeLines.add(lines[i])
                    i++
                }
                blocks.add(MarkdownBlock.CodeBlock(language = language, code = codeLines.joinToString("\n")))
                i++
            }

            trimmed.startsWith("$$") -> {
                val latex = if (trimmed.length > 2) {
                    trimmed.removePrefix("$$").removeSuffix("$$").trim()
                } else {
                    val sb = StringBuilder()
                    i++
                    while (i < lines.size) {
                        val line = lines[i]
                        if (line.trim().endsWith("$$")) {
                            sb.append(line.substringBeforeLast("$$"))
                            break
                        }
                        sb.append(line).append('\n')
                        i++
                    }
                    sb.toString().trim()
                }
                blocks.add(MarkdownBlock.MathBlock(latex))
                i++
            }

            trimmed.isEmpty() -> i++

            trimmed.startsWith("### ") -> {
                blocks.add(MarkdownBlock.Heading(3, trimmed.removePrefix("### ").trim()))
                i++
            }

            trimmed.startsWith("## ") -> {
                blocks.add(MarkdownBlock.Heading(2, trimmed.removePrefix("## ").trim()))
                i++
            }

            trimmed.startsWith("# ") -> {
                blocks.add(MarkdownBlock.Heading(1, trimmed.removePrefix("# ").trim()))
                i++
            }

            trimmed.startsWith("> ") -> {
                blocks.add(MarkdownBlock.Blockquote(trimmed.removePrefix("> ").trim()))
                i++
            }

            trimmed.matches(LIST_PATTERN) -> {
                blocks.add(MarkdownBlock.Paragraph("• " + trimmed.substring(2).trim()))
                i++
            }

            trimmed.matches(ORDERED_PATTERN) -> {
                val num = trimmed.substringBefore('.')
                blocks.add(MarkdownBlock.Paragraph(num + ". " + trimmed.substringAfter(". ").trim()))
                i++
            }

            trimmed == "---" || trimmed == "***" || trimmed == "___" -> {
                blocks.add(MarkdownBlock.HorizontalRule)
                i++
            }

            else -> {
                val sb = StringBuilder(raw)
                var j = i + 1
                while (j < lines.size) {
                    val next = lines[j]
                    val nt = next.trim()
                    if (isBlockBoundary(nt)) break
                    sb.append('\n').append(next)
                    j++
                }
                blocks.add(MarkdownBlock.Paragraph(sb.toString()))
                i = j
            }
        }
    }
    return blocks
}

private fun isBlockBoundary(trimmed: String): Boolean {
    return trimmed.isEmpty() ||
        trimmed.startsWith("```") ||
        trimmed.startsWith("$$") ||
        trimmed.startsWith("# ") ||
        trimmed.startsWith("## ") ||
        trimmed.startsWith("### ") ||
        trimmed.startsWith("> ") ||
        trimmed.matches(LIST_PATTERN) ||
        trimmed.matches(ORDERED_PATTERN) ||
        trimmed == "---" || trimmed == "***" || trimmed == "___"
}

/** 将文本按行内公式 `$...$` 切分为普通文本段与公式段。 */
fun splitInlineMath(text: String): List<InlineSegment> {
    val segments = mutableListOf<InlineSegment>()
    var last = 0
    for (m in INLINE_MATH_PATTERN.findAll(text)) {
        val start = m.range.first
        val end = m.range.last + 1
        if (start > last) segments.add(InlineSegment.Text(text.substring(last, start)))
        segments.add(InlineSegment.Math(m.groupValues[1]))
        last = end
    }
    if (last < text.length) segments.add(InlineSegment.Text(text.substring(last)))
    return segments
}

/** 向 [AnnotatedString.Builder] 追加带行内 Markdown（加粗/斜体/行内代码/链接）的文本。 */
fun AnnotatedString.Builder.appendInlineMarkdown(
    text: String,
    linkColor: Color,
    codeBackground: Color,
) {
    appendInline(text, linkColor, codeBackground)
}

private fun AnnotatedString.Builder.appendInline(
    text: String,
    linkColor: Color,
    codeBackground: Color,
) {
    var last = 0
    for (m in INLINE_PATTERN.findAll(text)) {
        val start = m.range.first
        val end = m.range.last + 1
        if (start > last) append(text.substring(last, start))
        when {
            m.groups[1] != null -> withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append(m.groups[1]!!.value)
            }

            m.groups[2] != null -> withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                append(m.groups[2]!!.value)
            }

            m.groups[3] != null -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = codeBackground)) {
                append(m.groups[3]!!.value)
            }

            m.groups[4] != null -> withStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)) {
                append(m.groups[4]!!.value)
            }
        }
        last = end
    }
    if (last < text.length) append(text.substring(last))
}
