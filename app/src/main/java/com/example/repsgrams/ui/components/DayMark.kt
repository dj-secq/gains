package com.example.repsgrams.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.repsgrams.domain.calendar.CalendarDay
import com.example.repsgrams.domain.calendar.DayMarkKind
import com.example.repsgrams.domain.calendar.dayMarkKind
import com.example.repsgrams.ui.theme.CanvasDark
import com.example.repsgrams.ui.theme.CanvasLight
import com.example.repsgrams.ui.theme.Ink
import com.example.repsgrams.ui.theme.LabelDark
import com.example.repsgrams.ui.theme.LabelLight
import com.example.repsgrams.ui.theme.LocalDarkTheme
import com.example.repsgrams.ui.theme.MonoLabelStyle
import com.example.repsgrams.ui.theme.PaperLight
import com.example.repsgrams.ui.theme.SignalRed

/**
 * Shared circle for the month and the week strip.
 * Trained is a contrasting disk. Pending is an ink disk with a canvas-colored numeral.
 */
@Composable
fun DayMark(
    day: CalendarDay,
    label: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val dark = LocalDarkTheme.current
    val kind = dayMarkKind(day)
    val fill = when (kind) {
        DayMarkKind.TRAINED -> if (dark) PaperLight else Ink
        DayMarkKind.PR -> SignalRed
        DayMarkKind.PENDING -> Ink
        else -> Color.Transparent
    }
    val numeral = when (kind) {
        DayMarkKind.TRAINED -> if (dark) Ink else PaperLight
        DayMarkKind.PR -> PaperLight
        DayMarkKind.MISSED -> if (dark) PaperLight else Ink
        DayMarkKind.UPCOMING -> LabelLight
        DayMarkKind.PENDING -> if (dark) CanvasDark else CanvasLight
        DayMarkKind.NUMERAL -> if (dark) LabelDark else LabelLight
    }
    val stroke = when (kind) {
        DayMarkKind.MISSED -> if (dark) PaperLight else Ink
        DayMarkKind.UPCOMING -> LabelLight
        else -> null
    }
    Box(
        modifier = modifier
            .size(size)
            .then(if (stroke != null) Modifier.border(1.dp, stroke, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = MonoLabelStyle, color = numeral, maxLines = 1)
    }
}
