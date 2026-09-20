package com.moodnotes.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private enum class BlockKind { PARAGRAPH, HEADING, QUOTE, BULLET, ORDERED }

private data class MdBlock(val kind: BlockKind, val text: String, val level: Int = 0)

/**
 * 把 Markdown 原文按行解析成块：标题 / 引用 / 无序列表 / 有序列表 / 普通段落。
 * 行内强调（**加粗**、*斜体*、`代码`）由 [parseInline] 处理。
 */
private fun parseBlocks(text: String): List<MdBlock> {
    if (text.isBlank()) return emptyList()
    return text.split('\n').map { raw ->
        val line = raw.trimEnd()
        when {
            line.startsWith("### ") -> MdBlock(BlockKind.HEADING, line.substring(4), 3)
            line.startsWith("## ") -> MdBlock(BlockKind.HEADING, line.substring(3), 2)
            line.startsWith("# ") -> MdBlock(BlockKind.HEADING, line.substring(2), 1)
            line.startsWith("> ") -> MdBlock(BlockKind.QUOTE, line.substring(2))
            line.startsWith("- ") || line.startsWith("* ") || line.startsWith("+ ") ->
                MdBlock(BlockKind.BULLET, line.substring(2))
            isOrderedList(line) -> MdBlock(BlockKind.ORDERED, line.replace(listPrefix(), ""))
            else -> MdBlock(BlockKind.PARAGRAPH, line)
        }
    }
}

private fun isOrderedList(line: String): Boolean =
    Regex("""^\d+[.、]\s""").containsMatchIn(line)

private fun listPrefix(): Regex = Regex("""^\d+[.、]\s""")

/**
 * 行内 Markdown 解析：`**x**` / `__x__` 加粗，`*x*` / `_x_` 斜体，`` `x` `` 等宽代码。
 * 未闭合的标记以普通文本原样保留。
 */
private fun parseInline(text: String, base: SpanStyle): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    val n = text.length
    fun appendStyled(content: String, style: SpanStyle) {
        builder.pushStyle(style)
        builder.append(content)
        builder.pop()
    }
    while (i < n) {
        val c = text[i]
        when (c) {
            '*' -> {
                if (i + 1 < n && text[i + 1] == '*') {
                    val close = text.indexOf("**", i + 2)
                    if (close > i + 2) {
                        appendStyled(text.substring(i + 2, close), base.copy(fontWeight = FontWeight.Bold))
                        i = close + 2
                    } else { builder.append('*'); i++ }
                } else {
                    val close = text.indexOf('*', i + 1)
                    if (close > i + 1 && text.getOrNull(close + 1) != '*') {
                        appendStyled(text.substring(i + 1, close), base.copy(fontStyle = FontStyle.Italic))
                        i = close + 1
                    } else { builder.append('*'); i++ }
                }
            }
            '_' -> {
                if (i + 1 < n && text[i + 1] == '_') {
                    val close = text.indexOf("__", i + 2)
                    if (close > i + 2) {
                        appendStyled(text.substring(i + 2, close), base.copy(fontWeight = FontWeight.Bold))
                        i = close + 2
                    } else { builder.append('_'); i++ }
                } else {
                    val close = text.indexOf('_', i + 1)
                    if (close > i + 1) {
                        appendStyled(text.substring(i + 1, close), base.copy(fontStyle = FontStyle.Italic))
                        i = close + 1
                    } else { builder.append('_'); i++ }
                }
            }
            '`' -> {
                val close = text.indexOf('`', i + 1)
                if (close > i) {
                    appendStyled(
                        text.substring(i + 1, close),
                        base.copy(fontFamily = FontFamily.Monospace, background = Color.Transparent),
                    )
                    i = close + 1
                } else { builder.append('`'); i++ }
            }
            else -> { builder.append(c); i++ }
        }
    }
    return builder.toAnnotatedString()
}

/**
 * 查看模式下渲染日记正文。支持 Markdown 行内强调与常见块级语法，字体随全局字号设置。
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    color: Color = MaterialTheme.colorScheme.onSurface,
) {
    val blocks = remember(text) { parseBlocks(text) }
    Column(modifier = modifier) {
        blocks.forEach { block ->
            when (block.kind) {
                BlockKind.HEADING -> HeadingBlock(block, textStyle, color)
                BlockKind.QUOTE -> QuoteBlock(block, textStyle, color)
                BlockKind.BULLET -> ListItemBlock(block, textStyle, color, prefix = "•")
                BlockKind.ORDERED -> ListItemBlock(block, textStyle, color, prefix = "·")
                BlockKind.PARAGRAPH -> ParagraphBlock(block, textStyle, color)
            }
        }
    }
}

@Composable
private fun ParagraphBlock(block: MdBlock, textStyle: TextStyle, color: Color) {
    Text(
        text = parseInline(block.text, SpanStyle(color = color)),
        style = textStyle,
        color = color,
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
    )
}

@Composable
private fun HeadingBlock(block: MdBlock, _textStyle: TextStyle, color: Color) {
    val style = when (block.level) {
        1 -> MaterialTheme.typography.headlineSmall
        2 -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.titleMedium
    }.copy(fontWeight = FontWeight.Bold, color = color)
    Text(
        text = parseInline(block.text, SpanStyle(color = color)),
        style = style,
        color = color,
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 4.dp),
    )
}

@Composable
private fun QuoteBlock(block: MdBlock, textStyle: TextStyle, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.7f))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = parseInline(block.text, SpanStyle(color = color)),
            style = textStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ListItemBlock(block: MdBlock, textStyle: TextStyle, color: Color, prefix: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(
            text = prefix,
            style = textStyle,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = parseInline(block.text, SpanStyle(color = color)),
            style = textStyle,
            color = color,
            modifier = Modifier.weight(1f),
        )
    }
}
