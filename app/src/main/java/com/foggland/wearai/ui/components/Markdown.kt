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

    /** Markdown 表格：表头 + 若干数据行（元素已去除两端的 | 与空格） */
    data class Table(
        val header: List<String>,
        val alignments: List<TableCellAlign>,
        val rows: List<List<String>>,
    ) : MarkdownBlock()
}

enum class TableCellAlign { START, CENTER, END }

/** 段落内的一个片段：普通文本或行内公式。 */
sealed class InlineSegment {
    data class Text(val value: String) : InlineSegment()
    data class Math(val latex: String) : InlineSegment()
}

private val INLINE_PATTERN = Regex(
    """\*\*([^*]+)\*\*|\*([^*\s][^*]*)\*|`([^`]+)`|\[([^\]]+)]\(([^)]+)\)|\*\*\*([^*]+)\*\*\*"""
)
private val INLINE_MATH_PATTERN = Regex("""(?<!\$)\$([^$\n]+?)\$(?!\$)""")
private val LIST_PATTERN = Regex("""[-*+] .+""")
private val ORDERED_PATTERN = Regex("""\d+\. .+""")

/** JLaTeXMath 能渲染的数学环境；其余 LaTeX 环境（itemize 等）视为文档结构，不按公式处理。 */
private val LATEX_MATH_ENVIRONMENTS = setOf(
    "matrix", "pmatrix", "bmatrix", "Bmatrix", "vmatrix", "Vmatrix",
    "smallmatrix", "array", "subarray", "cases", "aligned", "alignedat",
    "split", "gathered", "equation", "equation*", "align", "align*",
    "gather", "gather*", "eqnarray", "eqnarray*", "multline", "multline*",
)

/** LaTeX 文档结构命令 → Markdown，用于模型误输出整篇文档时的降级显示。
 *  注意：花括号必须转义。Android 5.x 的 ICU 正则实现不容忍未转义的 `}`，
 *  否则整份正则会在类初始化时抛异常导致崩溃。 */
private val LATEX_DOC_PATTERNS = listOf(
    Regex("""\\documentclass(\[[^\]]*\])?\{[^}]*\}""") to "",
    Regex("""\\usepackage(\[[^\]]*\])?\{[^}]*\}""") to "",
    Regex("""\\begin\{document\}""") to "",
    Regex("""\\end\{document\}""") to "",
    Regex("""\\begin\{(itemize|enumerate|description)\}""") to "",
    Regex("""\\end\{(itemize|enumerate|description)\}""") to "",
    Regex("""\\section\*?\{([^}]*)\}""") to "## \$1",
    Regex("""\\subsection\*?\{([^}]*)\}""") to "### \$1",
    Regex("""\\subsubsection\*?\{([^}]*)\}""") to "#### \$1",
    Regex("""\\item\s+""") to "- ",
    Regex("""\\textbf\{([^}]*)\}""") to "**\$1**",
    Regex("""\\textit\{([^}]*)\}""") to "*\$1*",
)

private fun latexEnvironmentOf(trimmed: String): String? =
    if (trimmed.startsWith("\\begin{")) {
        trimmed.removePrefix("\\begin{").substringBefore("}").trim()
    } else {
        null
    }

private fun isLatexMathEnvironment(trimmed: String): Boolean =
    latexEnvironmentOf(trimmed)?.let { it in LATEX_MATH_ENVIRONMENTS } == true

/** 把整篇 LaTeX 文档的骨架转成 Markdown，避免直接显示 \section、\begin{itemize} 等原始命令。 */
private fun normalizeLatexDocument(text: String): String {
    if (!text.contains("\\documentclass") &&
        !text.contains("\\begin{document}") &&
        !text.contains("\\section")
    ) {
        return text
    }
    val parts = text.split("```")
    if (parts.size == 1) return applyLatexDocPatterns(text)
    // 按 ``` 切分后，偶数段在代码块之外——只有这些段才做降级替换，
    // 否则会把代码块里的 LaTeX 源码也改掉。
    return buildString {
        parts.forEachIndexed { index, part ->
            if (index > 0) append("```")
            if (index % 2 == 0) append(applyLatexDocPatterns(part)) else append(part)
        }
    }
}

private fun applyLatexDocPatterns(text: String): String {
    var result = text
    for ((pattern, replacement) in LATEX_DOC_PATTERNS) {
        result = pattern.replace(result, replacement)
    }
    return result
}

/**
 * 将 Markdown 解析为块列表，供 [MarkdownContent] 渲染。
 */

/** 是否是表格分隔行（|---|---| 形式） */
private fun isTableSeparator(line: String): Boolean {
    val t = line.trim()
    if (!t.startsWith("|") || !t.endsWith("|")) return false
    val cells = t.trim('|').split("|")
    if (cells.isEmpty()) return false
    return cells.all { cell ->
        val c = cell.trim()
        c.isNotEmpty() && c.all { it == '-' || it == ':' } && c.count { it == '-' } >= 1
    }
}

/** 拆分表格行，去除两端的 | 并 trim；支持 \| 转义 */
private fun splitTableRow(line: String): List<String> {
    val t = line.trim().removePrefix("|").removeSuffix("|")
    val cells = mutableListOf<String>()
    val sb = StringBuilder()
    var escaped = false
    for (ch in t) {
        when {
            escaped -> { sb.append(ch); escaped = false }
            ch == '\\' -> escaped = true
            ch == '|' -> { cells.add(sb.toString().trim()); sb.clear() }
            else -> sb.append(ch)
        }
    }
    cells.add(sb.toString().trim())
    return cells
}

private fun parseTableAlignments(line: String): List<TableCellAlign> =
    line.trim().trim('|').split("|").map { cell ->
        val c = cell.trim()
        when {
            c.startsWith(":") && c.endsWith(":") -> TableCellAlign.CENTER
            c.endsWith(":") -> TableCellAlign.END
            else -> TableCellAlign.START
        }
    }

fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = normalizeLatexDocument(markdown).split("\n")
    var i = 0
    while (i < lines.size) {
        val raw = lines[i]
        val trimmed = raw.trim()
        when {
            // 表格：至少两行，第二行是 |---| 分隔行
            trimmed.startsWith("|") && i + 1 < lines.size && isTableSeparator(lines[i + 1]) -> {
                val header = splitTableRow(trimmed)
                val alignments = parseTableAlignments(lines[i + 1])
                i += 2
                val rows = mutableListOf<List<String>>()
                while (i < lines.size && lines[i].trim().startsWith("|")) {
                    rows.add(splitTableRow(lines[i].trim()))
                    i++
                }
                blocks.add(MarkdownBlock.Table(header, alignments, rows))
            }

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

            trimmed.startsWith("\\[") -> {
                // 标准 LaTeX 独立公式：\[...\]
                val latex = if (trimmed.endsWith("\\]") && trimmed.length > 4) {
                    trimmed.removePrefix("\\[").removeSuffix("\\]").trim()
                } else {
                    val sb = StringBuilder()
                    val rest = trimmed.removePrefix("\\[")
                    if (rest.isNotBlank()) sb.append(rest)
                    i++
                    while (i < lines.size) {
                        val line = lines[i]
                        if (line.trim().endsWith("\\]")) {
                            val before = line.substringBeforeLast("\\]")
                            if (before.isNotBlank()) {
                                if (sb.isNotEmpty()) sb.append('\n')
                                sb.append(before)
                            }
                            break
                        }
                        if (sb.isNotEmpty()) sb.append('\n')
                        sb.append(line)
                        i++
                    }
                    sb.toString().trim()
                }
                blocks.add(MarkdownBlock.MathBlock(latex))
                i++
            }

            isLatexMathEnvironment(trimmed) -> {
                // 环境式独立公式：\begin{matrix}...\end{matrix}、\begin{equation}... 等
                val env = latexEnvironmentOf(trimmed).orEmpty()
                val latex = if (trimmed.contains("\\end{$env}")) {
                    trimmed
                } else {
                    val content = mutableListOf(raw)
                    i++
                    while (i < lines.size) {
                        val line = lines[i]
                        content.add(line)
                        if (line.trim().contains("\\end{$env}")) break
                        i++
                    }
                    content.joinToString("\n").trim()
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
        trimmed.startsWith("\\[") ||
        isLatexMathEnvironment(trimmed) ||
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

            // 加粗 + 斜体
            m.groups[6] != null -> withStyle(
                SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic),
            ) {
                append(m.groups[6]!!.value)
            }
        }
        last = end
    }
    if (last < text.length) append(text.substring(last))
}
