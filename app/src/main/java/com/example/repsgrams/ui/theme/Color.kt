package com.example.repsgrams.ui.theme

import androidx.compose.ui.graphics.Color

/** Light gray board. Paper is white. Ink is near-black. */
val CanvasLight = Color(0xFFE8E8EA)
val PaperLight = Color(0xFFFFFFFF)
val InsetLight = Color(0xFFE0E0E0)
val Ink = Color(0xFF111111)
val HairlineLight = Color(0xFFE0E0E0)
val LabelLight = Color(0xFF666666)

/** Dark board. Paper is a step above black so a hairline can separate a card. */
val CanvasDark = Color(0xFF000000)
val PaperDark = Color(0xFF1A1A1A)
val InsetDark = Color(0xFF111111)
val HairlineDark = Color(0xFF333333)
val LabelDark = Color(0xFF999999)

/** Live dot, rest at or after zero, a personal record, a missed or not-yet mark, and a destructive confirm. */
val SignalRed = Color(0xFFD71921)

/** Finished, taken, or trained. Dark text sits on the light green; white text sits on the light-board green. */
val DoneGreen = Color(0xFF128A42)
val DoneGreenOnDark = Color(0xFF3DDC84)
