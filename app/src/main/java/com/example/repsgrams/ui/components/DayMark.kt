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
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.repsgrams.domain.calendar.CalendarDay
import com.example.repsgrams.domain.calendar.DayMarkKind
import com.example.repsgrams.domain.calendar.dayMarkKind
import com.example.repsgrams.ui.theme.DoneGreen
import com.example.repsgrams.ui.theme.DoneGreenOnDark
import com.example.repsgrams.ui.theme.Ink
import com.example.repsgrams.ui.theme.LabelDark
import com.example.repsgrams.ui.theme.LabelLight
import com.example.repsgrams.ui.theme.LocalDarkTheme
import com.example.repsgrams.ui.theme.MonoLabelStyle
import com.example.repsgrams.ui.theme.PaperLight
import com.example.repsgrams.ui.theme.SignalRed

/**
 * Shared circle for the month and the week strip.
 * Trained is a green disk. A missed due day is a red ring. A personal record is signal red.
 * Today with nothing logged is the inverted disk: ink on the light board, paper on the dark board.
 */
@Composable
fun DayMark(
    day: CalendarDay,
    label: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    DayMark(kind = dayMarkKind(day), label = label, size = size, modifier = modifier)
}

@Composable
fun DayMark(
    kind: DayMarkKind,
    label: String,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val dark = LocalDarkTheme.current
    val fill = when (kind) {
        DayMarkKind.TRAINED -> if (dark) DoneGreenOnDark else DoneGreen
        DayMarkKind.PR -> SignalRed
        DayMarkKind.PENDING -> if (dark) PaperLight else Ink
        else -> Color.Transparent
    }
    val numeral = when (kind) {
        DayMarkKind.TRAINED -> if (dark) Ink else PaperLight
        DayMarkKind.PR -> PaperLight
        DayMarkKind.MISSED -> if (dark) PaperLight else Ink
        DayMarkKind.UPCOMING -> if (dark) LabelDark else LabelLight
        DayMarkKind.PENDING -> if (dark) Ink else PaperLight
        DayMarkKind.NUMERAL -> if (dark) LabelDark else LabelLight
    }
    val stroke = when (kind) {
        DayMarkKind.MISSED -> SignalRed
        DayMarkKind.UPCOMING -> if (dark) LabelDark else LabelLight
        else -> null
    }
    val figure = if (size < 32.dp) {
        MonoLabelStyle
    } else {
        MonoLabelStyle.copy(fontSize = 13.sp, lineHeight = 16.sp, letterSpacing = 0.em)
    }
    Box(
        modifier = modifier
            .size(size)
            .then(if (stroke != null) Modifier.border(1.dp, stroke, CircleShape) else Modifier)
            .clip(CircleShape)
            .background(fill),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = figure, color = numeral, maxLines = 1)
    }
}
