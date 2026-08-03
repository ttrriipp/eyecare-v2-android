package com.eyecare.app.presentation.auth.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.eyecare.app.ui.theme.EyecareColors

@Composable
fun PolicyConsentRow(
    label: String,
    linkText: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onLinkClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var linkRange by remember(label, linkText) { mutableStateOf(0 to 0) }
    val linkColor = EyecareColors.current.accentText
    val annotatedText = remember(label, linkText, linkColor) {
        buildAnnotatedString {
            append(label)
            append(" ")
            val start = length
            withStyle(
                style = SpanStyle(
                    color = linkColor,
                    textDecoration = TextDecoration.Underline,
                ),
            ) {
                append(linkText)
            }
            linkRange = start to length
        }
    }
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Row(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
        )
        Text(
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium,
            onTextLayout = { layoutResult = it },
            modifier = Modifier
                .weight(1f)
                .padding(top = 12.dp)
                .pointerInput(enabled, checked) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        val position = layoutResult?.getOffsetForPosition(offset) ?: return@detectTapGestures
                        val (start, end) = linkRange
                        if (position in start until end) {
                            onLinkClick()
                        } else {
                            onCheckedChange(!checked)
                        }
                    }
                },
        )
    }
}
