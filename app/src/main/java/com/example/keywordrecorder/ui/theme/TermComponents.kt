package com.example.keywordrecorder.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ─[ TITLE ]──── style panel with border
@Composable
fun TermPanel(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.border(1.dp, MaterialTheme.colorScheme.outline)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "─[ ",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = " ]",
                color = MaterialTheme.colorScheme.outline,
                style = MaterialTheme.typography.titleSmall
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(12.dp),
            content = content
        )
    }
}

// ● STATUS  colored dot + label
@Composable
fun TermStatusDot(
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("●", color = color, style = MaterialTheme.typography.bodyMedium)
        Text(label, color = color, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}

// [■■■■░░░░░░░░] 42%  ASCII progress bar
@Composable
fun TermProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    barWidth: Int = 20,
    color: Color = TermPurpleBar,
    showPercent: Boolean = true
) {
    val clamped = progress.coerceIn(0f, 1f)
    val filled = (clamped * barWidth).toInt()
    val empty = barWidth - filled
    val pct = if (showPercent) " ${(clamped * 100).toInt()}%" else ""
    Text(
        text = "[" + "■".repeat(filled) + "░".repeat(empty) + "]$pct",
        color = color,
        style = MaterialTheme.typography.bodyMedium,
        modifier = modifier
    )
}

// KEY  description  — shortcut hint row
@Composable
fun TermKeyHint(
    key: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(key, color = TermYellow, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
        Text(description, color = MaterialTheme.colorScheme.outlineVariant, style = MaterialTheme.typography.labelSmall)
    }
}

// > label  terminal prompt prefix
@Composable
fun TermPrompt(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: Color = TermTextNormal
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(">", color = TermCyan, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = label.padEnd(16),
            color = MaterialTheme.colorScheme.outlineVariant,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(value, color = valueColor, style = MaterialTheme.typography.bodyMedium)
    }
}

// terminal-style action button  [LABEL]
@Composable
fun TermButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = TermCyan,
    enabled: Boolean = true
) {
    val resolvedColor = if (enabled) color else TermTextDim
    Text(
        text = "[$label]",
        color = resolvedColor,
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .border(1.dp, resolvedColor)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    )
}

// horizontal separator line
@Composable
fun TermDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = TermBorder, thickness = 1.dp)
}
