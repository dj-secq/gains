package com.example.repsgrams.domain.progress

import com.example.repsgrams.data.datastore.UnitSystem

/** Shared with session logging. Room stores kilograms; pounds exist only at the boundary. */
const val POUNDS_PER_KILOGRAM = 2.2046226f

fun poundsToKilograms(pounds: Float): Float = pounds / POUNDS_PER_KILOGRAM

fun kilogramsToPounds(kilograms: Float): Float = kilograms * POUNDS_PER_KILOGRAM

fun bodyweightToKilograms(entered: Float, unitSystem: UnitSystem): Float =
    if (unitSystem == UnitSystem.LB) poundsToKilograms(entered) else entered

fun bodyweightToDisplay(kilograms: Float, unitSystem: UnitSystem): Float =
    if (unitSystem == UnitSystem.LB) kilogramsToPounds(kilograms) else kilograms
