package com.example.repsgrams.ui.components

import androidx.annotation.DrawableRes
import com.example.repsgrams.R

@DrawableRes
fun exerciseImageResource(imageAssetName: String?): Int = when (imageAssetName) {
    "ex_arm_circles" -> R.drawable.ex_arm_circles
    "ex_band_face_pull" -> R.drawable.ex_band_face_pull
    "ex_band_lateral_raise" -> R.drawable.ex_band_lateral_raise
    "ex_band_pull_aparts" -> R.drawable.ex_band_pull_aparts
    "ex_bodyweight_squats" -> R.drawable.ex_bodyweight_squats
    "ex_bulgarian_split_squat" -> R.drawable.ex_bulgarian_split_squat
    "ex_chin_ups" -> R.drawable.ex_chin_ups
    "ex_db_bicep_curl" -> R.drawable.ex_db_bicep_curl
    "ex_db_floor_press" -> R.drawable.ex_db_floor_press
    "ex_db_overhead_press" -> R.drawable.ex_db_overhead_press
    "ex_db_overhead_triceps_extension" -> R.drawable.ex_db_overhead_triceps_extension
    "ex_db_romanian_deadlift" -> R.drawable.ex_db_romanian_deadlift
    "ex_easy_push_ups" -> R.drawable.ex_easy_push_ups
    "ex_hanging_knee_raise" -> R.drawable.ex_hanging_knee_raise
    "ex_pull_ups" -> R.drawable.ex_pull_ups
    "ex_push_ups" -> R.drawable.ex_push_ups
    "ex_single_arm_db_row" -> R.drawable.ex_single_arm_db_row
    else -> 0
}
