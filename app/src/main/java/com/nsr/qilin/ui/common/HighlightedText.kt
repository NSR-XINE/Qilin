package com.nsr.qilin.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

/**
 * Builds an [AnnotatedString] where every case-insensitive occurrence of
 * [query] inside [text] is wrapped in [style]. Falls back to plain [text]
 * when [query] is blank or no match is found.
 */
fun highlightText(
    text: String,
    query: String,
    style: SpanStyle
): AnnotatedString {
    val q = query.trim()
    if (q.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        var start = 0
        val lower = text.lowercase()
        val lowerQ = q.lowercase()
        while (start < text.length) {
            val idx = lower.indexOf(lowerQ, startIndex = start)
            if (idx < 0) {
                append(text.substring(start))
                break
            }
            append(text.substring(start, idx))
            withStyle(style) {
                append(text.substring(idx, idx + q.length))
            }
            start = idx + q.length
        }
    }
}

/**
 * [Text] with every case-insensitive match of [query] styled with
 * [highlightColor] and [highlightWeight]. Used by search results so matched
 * substrings are easy to spot.
 */
@Composable
fun HighlightedText(
    text: String,
    query: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    highlightColor: Color = MaterialTheme.colorScheme.primary,
    highlightWeight: FontWeight = FontWeight.Bold,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip
) {
    val highlighted = remember(text, query, highlightColor, highlightWeight) {
        highlightText(
            text = text,
            query = query,
            style = SpanStyle(color = highlightColor, fontWeight = highlightWeight)
        )
    }
    Text(
        text = highlighted,
        style = style,
        modifier = modifier,
        maxLines = maxLines,
        overflow = overflow
    )
}
