package com.example.repsgrams.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.example.repsgrams.R

@OptIn(ExperimentalTextApi::class)
private val SpaceGrotesk = FontFamily(
    Font(
        R.font.space_grotesk,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.Setting("wght", 400f)),
    ),
    Font(
        R.font.space_grotesk,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.Setting("wght", 500f)),
    ),
)

val SpaceMono = FontFamily(
    Font(R.font.space_mono, weight = FontWeight.Normal),
)

@OptIn(ExperimentalTextApi::class)
private val Doto = FontFamily(
    Font(
        R.font.doto,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.Setting("wght", 500f),
            FontVariation.Setting("ROND", 100f),
        ),
    ),
)

/** Numerals only, 28sp or larger. Not for sentences. */
val DisplayNumeral = TextStyle(
    fontFamily = Doto,
    fontWeight = FontWeight.Medium,
    fontSize = 48.sp,
    lineHeight = 52.sp,
)

val MonoLabelStyle = TextStyle(
    fontFamily = SpaceMono,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.08.em,
)

val Typography = Typography(
    headlineLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 34.sp,
        lineHeight = 40.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = SpaceGrotesk,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),
    labelSmall = MonoLabelStyle,
)
