package com.seguimiento.clases.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.seguimiento.clases.ui.theme.parseColor

@Composable
fun SubjectBadge(
    code: String,
    colorHex: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    isArchived: Boolean = false
) {
    val backgroundColor = if (isArchived) Color(0xFF64748B) else parseColor(colorHex)
    val luminance = 0.299f * backgroundColor.red + 0.587f * backgroundColor.green + 0.114f * backgroundColor.blue
    val textColor = if (luminance > 0.55f) Color(0xFF0F172A) else Color.White

    // Tipografía proporcional al círculo. Se calcula desde dp para que no crezca con el tamaño
    // de letra del sistema: la insignia es un icono y el nombre completo ya aparece al lado.
    val fontSize = with(LocalDensity.current) {
        when {
            code.length <= 2 -> (size * 0.38f).toSp()
            code.length == 3 -> (size * 0.30f).toSp()
            else -> (size * 0.24f).toSp()
        }
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = code,
            color = textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}
