package com.seguimiento.clases.ui.screens.alarm.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
fun WheelTimePicker(
    selectedHour: Int,
    selectedMinute: Int,
    onTimeChanged: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = if (enabled) 0.35f else 0.15f),
        tonalElevation = if (enabled) 2.dp else 0.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 24.dp)
                .alpha(if (enabled) 1f else 0.38f),
            contentAlignment = Alignment.Center
        ) {
            // Fondo indicador de selección en el centro
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(56.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp)
                    )
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Rueda de Horas (00 - 23)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SingleWheel(
                        valueCount = 24,
                        selectedValue = selectedHour,
                        onValueSelected = { newHour ->
                            onTimeChanged(newHour, selectedMinute)
                        },
                        label = "horas",
                        enabled = enabled
                    )
                }

                // Dos puntos separadores ":"
                Text(
                    text = ":",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.padding(horizontal = 12.dp)
                )

                // Rueda de Minutos (00 - 59)
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    SingleWheel(
                        valueCount = 60,
                        selectedValue = selectedMinute,
                        onValueSelected = { newMinute ->
                            onTimeChanged(selectedHour, newMinute)
                        },
                        label = "min",
                        enabled = enabled
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SingleWheel(
    valueCount: Int,
    selectedValue: Int,
    onValueSelected: (Int) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 56.dp,
    enabled: Boolean = true
) {
    val haptic = LocalHapticFeedback.current

    // Usamos repetición circular para que el giro sea infinito y natural
    val repeatCount = 100
    val totalItems = valueCount * repeatCount
    val middleOffset = (repeatCount / 2) * valueCount
    val startIndex = middleOffset + (selectedValue % valueCount)

    val listState = rememberLazyListState(initialFirstVisibleItemIndex = startIndex)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    val coroutineScope = rememberCoroutineScope()

    // Detectar el elemento centrado
    val centerIndex by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val visibleItems = layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty()) {
                startIndex
            } else {
                val viewportCenter = (layoutInfo.viewportStartOffset + layoutInfo.viewportEndOffset) / 2
                visibleItems.minByOrNull { item ->
                    val itemCenter = item.offset + item.size / 2
                    abs(itemCenter - viewportCenter)
                }?.index ?: startIndex
            }
        }
    }

    // Notificar cambio cuando el elemento centrado cambia tras el scroll
    LaunchedEffect(centerIndex) {
        val actualValue = centerIndex % valueCount
        if (actualValue != selectedValue) {
            if (enabled) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            onValueSelected(actualValue)
        }
    }

    // Sincronizar si selectedValue cambia desde fuera (por ejemplo al entrar a la pantalla)
    LaunchedEffect(selectedValue) {
        val currentActual = centerIndex % valueCount
        if (currentActual != selectedValue) {
            val newIndex = (centerIndex / valueCount) * valueCount + selectedValue
            listState.scrollToItem(newIndex)
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .height(itemHeight * 3) // 3 elementos visibles: previo, actual, siguiente
                .width(90.dp),
            contentAlignment = Alignment.Center
        ) {
            LazyColumn(
                state = listState,
                flingBehavior = flingBehavior,
                userScrollEnabled = enabled,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(vertical = itemHeight),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                items(totalItems) { index ->
                    val value = index % valueCount
                    val isCenter = index == centerIndex
                    val distance = abs(index - centerIndex)

                    val alpha = when (distance) {
                        0 -> 1f
                        1 -> 0.35f
                        else -> 0.15f
                    }
                    val fontSize = when (distance) {
                        0 -> 34.sp
                        1 -> 24.sp
                        else -> 18.sp
                    }
                    val fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal
                    val textColor = if (isCenter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

                    Box(
                        modifier = Modifier
                            .height(itemHeight)
                            .fillMaxWidth()
                            .clickable {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = String.format("%02d", value),
                            fontSize = fontSize,
                            fontWeight = fontWeight,
                            color = textColor,
                            modifier = Modifier.alpha(alpha),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}
