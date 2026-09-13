package com.example.repsgrams.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class UnitSystem { KG, LB }

data class CycleSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val cycleStartDate: LocalDate, // Deprecated, unused by scheduling engine
    val adherenceGraceDays: Int = 1,
    val wheyServingGrams: Float,
    val proteinGoalMultiplierLow: Float,
    val proteinGoalMultiplierHigh: Float,
    val unitSystem: UnitSystem,
    val remindersEnabled: Boolean,
    val workoutReminderEnabled: Boolean,
    val creatineReminderEnabled: Boolean,
    val postWorkoutWheyReminderEnabled: Boolean,
    val workoutReminderTime: LocalTime,
    val creatineReminderTime: LocalTime,
    val postWorkoutWheyDelayMinutes: Int,
    val restTimerSound: String,
    val restTimerVibrationEnabled: Boolean,
    val restTimerAutoAdvance: Boolean,
    val defaultRestSeconds: Int = 90,
    val trackedMeasurements: Set<String>,
    val healthConnectEnabled: Boolean,
    val voiceCuesEnabled: Boolean,
)

interface CycleSettingsRepository {
    val settings: Flow<CycleSettings>
    suspend fun ensureInitialized()
    suspend fun setTrackedMeasurements(measurements: Set<String>)
    suspend fun setHealthConnectEnabled(enabled: Boolean)
    suspend fun setVoiceCuesEnabled(enabled: Boolean)
    suspend fun setCycleStartDate(date: LocalDate)
    suspend fun setAdherenceGraceDays(days: Int)
    suspend fun setWheyServingGrams(grams: Float)
    suspend fun setProteinGoalMultipliers(low: Float, high: Float)
    suspend fun setUnitSystem(unitSystem: UnitSystem)
    suspend fun setRemindersEnabled(enabled: Boolean)
    suspend fun setWorkoutReminderEnabled(enabled: Boolean)
    suspend fun setCreatineReminderEnabled(enabled: Boolean)
    suspend fun setPostWorkoutWheyReminderEnabled(enabled: Boolean)
    suspend fun setWorkoutReminderTime(time: LocalTime)
    suspend fun setCreatineReminderTime(time: LocalTime)
    suspend fun setPostWorkoutWheyDelayMinutes(minutes: Int)
    suspend fun setRestTimerSound(sound: String)
    suspend fun setRestTimerVibrationEnabled(enabled: Boolean)
    suspend fun setRestTimerAutoAdvance(enabled: Boolean)
    suspend fun setDefaultRestSeconds(seconds: Int)
    suspend fun setThemeMode(mode: ThemeMode)
}

val Context.cycleSettingsDataStore by preferencesDataStore(name = "cycle_settings")

class PreferencesCycleSettingsRepository(
    private val context: Context,
    private val clock: Clock,
) : CycleSettingsRepository {
    override val settings: Flow<CycleSettings> = context.cycleSettingsDataStore.data
        .catch { error ->
            if (error is IOException) emit(androidx.datastore.preferences.core.emptyPreferences()) else throw error
        }
        .map(::toSettings)

    override suspend fun setTrackedMeasurements(measurements: Set<String>) {
        context.cycleSettingsDataStore.edit { it[TRACKED_MEASUREMENTS] = measurements }
    }

    override suspend fun setHealthConnectEnabled(enabled: Boolean) {
        context.cycleSettingsDataStore.edit { it[HEALTH_CONNECT_ENABLED] = enabled }
    }

    override suspend fun setVoiceCuesEnabled(enabled: Boolean) {
        context.cycleSettingsDataStore.edit { it[VOICE_CUES_ENABLED] = enabled }
    }

    override suspend fun ensureInitialized() {
        context.cycleSettingsDataStore.edit { preferences ->
            if (CYCLE_START_DATE !in preferences) preferences[CYCLE_START_DATE] = LocalDate.now(clock).toString()
            if (ADHERENCE_GRACE_DAYS !in preferences) preferences[ADHERENCE_GRACE_DAYS] = 1
            if (WHEY_SERVING_GRAMS !in preferences) preferences[WHEY_SERVING_GRAMS] = 25f
            if (PROTEIN_MULTIPLIER_LOW !in preferences) preferences[PROTEIN_MULTIPLIER_LOW] = 1.6f
            if (PROTEIN_MULTIPLIER_HIGH !in preferences) preferences[PROTEIN_MULTIPLIER_HIGH] = 2f
            if (UNIT_SYSTEM !in preferences) preferences[UNIT_SYSTEM] = UnitSystem.KG.name
            if (REMINDERS_ENABLED !in preferences) preferences[REMINDERS_ENABLED] = false
            if (WORKOUT_REMINDER_ENABLED !in preferences) preferences[WORKOUT_REMINDER_ENABLED] = false
            if (CREATINE_REMINDER_ENABLED !in preferences) preferences[CREATINE_REMINDER_ENABLED] = false
            if (POST_WORKOUT_WHEY_REMINDER_ENABLED !in preferences) {
                preferences[POST_WORKOUT_WHEY_REMINDER_ENABLED] = false
            }
            if (WORKOUT_REMINDER_TIME !in preferences) preferences[WORKOUT_REMINDER_TIME] = "18:00"
            if (CREATINE_REMINDER_TIME !in preferences) preferences[CREATINE_REMINDER_TIME] = "20:00"
            if (POST_WORKOUT_WHEY_DELAY_MINUTES !in preferences) {
                preferences[POST_WORKOUT_WHEY_DELAY_MINUTES] = 25
            }
            if (REST_TIMER_SOUND !in preferences) preferences[REST_TIMER_SOUND] = "default"
            if (REST_TIMER_VIBRATION !in preferences) preferences[REST_TIMER_VIBRATION] = true
            if (REST_TIMER_AUTO_ADVANCE !in preferences) preferences[REST_TIMER_AUTO_ADVANCE] = true
        }
    }

    override suspend fun setCycleStartDate(date: LocalDate) = update(CYCLE_START_DATE, date.toString())
    override suspend fun setAdherenceGraceDays(days: Int) {
        require(days in 0..7) { "Adherence grace days must be between 0 and 7" }
        update(ADHERENCE_GRACE_DAYS, days)
    }
    override suspend fun setWheyServingGrams(grams: Float) {
        require(grams > 0) { "Whey serving grams must be positive" }
        update(WHEY_SERVING_GRAMS, grams)
    }

    override suspend fun setProteinGoalMultipliers(low: Float, high: Float) {
        require(low > 0 && high >= low) { "Protein goal multipliers must be positive and ordered" }
        context.cycleSettingsDataStore.edit {
            it[PROTEIN_MULTIPLIER_LOW] = low
            it[PROTEIN_MULTIPLIER_HIGH] = high
        }
    }

    override suspend fun setUnitSystem(unitSystem: UnitSystem) = update(UNIT_SYSTEM, unitSystem.name)
    override suspend fun setRemindersEnabled(enabled: Boolean) = update(REMINDERS_ENABLED, enabled)
    override suspend fun setWorkoutReminderEnabled(enabled: Boolean) = update(WORKOUT_REMINDER_ENABLED, enabled)
    override suspend fun setCreatineReminderEnabled(enabled: Boolean) = update(CREATINE_REMINDER_ENABLED, enabled)
    override suspend fun setPostWorkoutWheyReminderEnabled(enabled: Boolean) =
        update(POST_WORKOUT_WHEY_REMINDER_ENABLED, enabled)
    override suspend fun setWorkoutReminderTime(time: LocalTime) = update(WORKOUT_REMINDER_TIME, time.toString())
    override suspend fun setCreatineReminderTime(time: LocalTime) = update(CREATINE_REMINDER_TIME, time.toString())
    override suspend fun setPostWorkoutWheyDelayMinutes(minutes: Int) {
        require(minutes in 15..120) { "Whey reminder delay must be between 15 and 120 minutes" }
        update(POST_WORKOUT_WHEY_DELAY_MINUTES, minutes)
    }

    override suspend fun setRestTimerSound(sound: String) = update(REST_TIMER_SOUND, sound)
    override suspend fun setRestTimerVibrationEnabled(enabled: Boolean) = update(REST_TIMER_VIBRATION, enabled)
    override suspend fun setRestTimerAutoAdvance(enabled: Boolean) = update(REST_TIMER_AUTO_ADVANCE, enabled)
    override suspend fun setThemeMode(mode: ThemeMode) = update(THEME_MODE, mode.name)
    override suspend fun setDefaultRestSeconds(seconds: Int) = update(DEFAULT_REST_SECONDS, seconds)

    private fun toSettings(preferences: Preferences) = CycleSettings(
        themeMode = preferences[THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM,
        cycleStartDate = preferences[CYCLE_START_DATE]
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
            ?: LocalDate.now(clock),
        adherenceGraceDays = preferences[ADHERENCE_GRACE_DAYS] ?: 1,
        wheyServingGrams = preferences[WHEY_SERVING_GRAMS] ?: 25f,
        proteinGoalMultiplierLow = preferences[PROTEIN_MULTIPLIER_LOW] ?: 1.6f,
        proteinGoalMultiplierHigh = preferences[PROTEIN_MULTIPLIER_HIGH] ?: 2f,
        unitSystem = preferences[UNIT_SYSTEM]?.let { runCatching { UnitSystem.valueOf(it) }.getOrNull() } ?: UnitSystem.KG,
        remindersEnabled = preferences[REMINDERS_ENABLED] ?: false,
        workoutReminderEnabled = preferences[WORKOUT_REMINDER_ENABLED] ?: false,
        creatineReminderEnabled = preferences[CREATINE_REMINDER_ENABLED] ?: false,
        postWorkoutWheyReminderEnabled = preferences[POST_WORKOUT_WHEY_REMINDER_ENABLED] ?: false,
        workoutReminderTime = preferences[WORKOUT_REMINDER_TIME].toLocalTimeOrDefault(LocalTime.of(18, 0)),
        creatineReminderTime = preferences[CREATINE_REMINDER_TIME].toLocalTimeOrDefault(LocalTime.of(20, 0)),
        postWorkoutWheyDelayMinutes = preferences[POST_WORKOUT_WHEY_DELAY_MINUTES] ?: 25,
        restTimerSound = preferences[REST_TIMER_SOUND] ?: "default",
        restTimerVibrationEnabled = preferences[REST_TIMER_VIBRATION] ?: true,
        restTimerAutoAdvance = preferences[REST_TIMER_AUTO_ADVANCE] ?: true,
        defaultRestSeconds = preferences[DEFAULT_REST_SECONDS] ?: 90,
        trackedMeasurements = preferences[TRACKED_MEASUREMENTS] ?: emptySet(),
        healthConnectEnabled = preferences[HEALTH_CONNECT_ENABLED] ?: false,
        voiceCuesEnabled = preferences[VOICE_CUES_ENABLED] ?: false,
    )

    private suspend fun <T> update(key: Preferences.Key<T>, value: T) {
        context.cycleSettingsDataStore.edit { it[key] = value }
    }

    private companion object {
        val CYCLE_START_DATE = stringPreferencesKey("cycle_start_date")
        val ADHERENCE_GRACE_DAYS = androidx.datastore.preferences.core.intPreferencesKey("adherence_grace_days")
        val WHEY_SERVING_GRAMS = floatPreferencesKey("whey_serving_grams")
        val PROTEIN_MULTIPLIER_LOW = floatPreferencesKey("protein_multiplier_low")
        val PROTEIN_MULTIPLIER_HIGH = floatPreferencesKey("protein_multiplier_high")
        val UNIT_SYSTEM = stringPreferencesKey("unit_system")
        val REMINDERS_ENABLED = booleanPreferencesKey("reminders_enabled")
        val WORKOUT_REMINDER_ENABLED = booleanPreferencesKey("workout_reminder_enabled")
        val CREATINE_REMINDER_ENABLED = booleanPreferencesKey("creatine_reminder_enabled")
        val POST_WORKOUT_WHEY_REMINDER_ENABLED = booleanPreferencesKey("post_workout_whey_reminder_enabled")
        val WORKOUT_REMINDER_TIME = stringPreferencesKey("workout_reminder_time")
        val CREATINE_REMINDER_TIME = stringPreferencesKey("creatine_reminder_time")
        val POST_WORKOUT_WHEY_DELAY_MINUTES = androidx.datastore.preferences.core.intPreferencesKey(
            "post_workout_whey_delay_minutes",
        )
        val REST_TIMER_SOUND = stringPreferencesKey("rest_timer_sound")
        val REST_TIMER_VIBRATION = booleanPreferencesKey("rest_timer_vibration")
        val REST_TIMER_AUTO_ADVANCE = booleanPreferencesKey("rest_timer_auto_advance")
        val TRACKED_MEASUREMENTS = androidx.datastore.preferences.core.stringSetPreferencesKey("tracked_measurements")
        val HEALTH_CONNECT_ENABLED = booleanPreferencesKey("health_connect_enabled")
        val VOICE_CUES_ENABLED = booleanPreferencesKey("voice_cues_enabled")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DEFAULT_REST_SECONDS = androidx.datastore.preferences.core.intPreferencesKey("default_rest_seconds")
    }
}

private fun String?.toLocalTimeOrDefault(default: LocalTime): LocalTime =
    this?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: default
