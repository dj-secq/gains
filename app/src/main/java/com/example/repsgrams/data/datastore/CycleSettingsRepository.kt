package com.example.repsgrams.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

import androidx.datastore.preferences.preferencesDataStore
import com.example.repsgrams.domain.progression.formatProgressionMap
import com.example.repsgrams.domain.progression.parseProgressionMap
import java.io.IOException
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

enum class ThemeMode { SYSTEM, LIGHT, DARK }
enum class UnitSystem { KG, LB }

const val DEFAULT_PLATES_KG = "25,20,15,10,5,2.5,1.25"
const val DEFAULT_PLATES_LB = "45,35,25,10,5,2.5"

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
    val keepScreenOn: Boolean = true,
    val progressionEnabled: Boolean = true,
    val progressionIncrementKg: Float = 2.5f,
    val progressionIncrementLb: Float = 5f,
    val rpeEnabled: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val barbellKg: Float = 20f,
    val barbellLb: Float = 45f,
    val platesKg: String = DEFAULT_PLATES_KG,
    val platesLb: String = DEFAULT_PLATES_LB,
    val defaultWarmupRestSeconds: Int = 90,
    val defaultWorkingRestSeconds: Int = 180,
    val defaultSupersetIntraRestSeconds: Int = 0,
    val progressionQualified: Map<Long, Long> = emptyMap(),
    val progressionSkipped: Map<Long, Long> = emptyMap(),
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
    suspend fun setKeepScreenOn(enabled: Boolean)
    suspend fun setProgressionEnabled(enabled: Boolean)
    suspend fun setProgressionIncrementKg(kilograms: Float)
    suspend fun setProgressionIncrementLb(pounds: Float)
    suspend fun setRpeEnabled(enabled: Boolean)
    suspend fun setHapticsEnabled(enabled: Boolean)
    suspend fun setBarbellKg(kilograms: Float)
    suspend fun setBarbellLb(pounds: Float)
    suspend fun setPlatesKg(stored: String)
    suspend fun setPlatesLb(stored: String)
    suspend fun setDefaultWarmupRestSeconds(seconds: Int)
    suspend fun setDefaultWorkingRestSeconds(seconds: Int)
    suspend fun setDefaultSupersetIntraRestSeconds(seconds: Int)
    suspend fun setProgressionQualified(qualified: Map<Long, Long>)
    suspend fun setProgressionSkipped(skipped: Map<Long, Long>)
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
            if (KEEP_SCREEN_ON !in preferences) preferences[KEEP_SCREEN_ON] = true
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
    override suspend fun setKeepScreenOn(enabled: Boolean) = update(KEEP_SCREEN_ON, enabled)
    override suspend fun setProgressionEnabled(enabled: Boolean) = update(PROGRESSION_ENABLED, enabled)
    override suspend fun setProgressionIncrementKg(kilograms: Float) {
        require(kilograms.isFinite() && kilograms > 0f)
        update(PROGRESSION_INCREMENT_KG, kilograms)
    }
    override suspend fun setProgressionIncrementLb(pounds: Float) {
        require(pounds.isFinite() && pounds > 0f)
        update(PROGRESSION_INCREMENT_LB, pounds)
    }
    override suspend fun setRpeEnabled(enabled: Boolean) = update(RPE_ENABLED, enabled)
    override suspend fun setHapticsEnabled(enabled: Boolean) = update(HAPTICS_ENABLED, enabled)
    override suspend fun setBarbellKg(kilograms: Float) {
        require(kilograms.isFinite() && kilograms > 0f)
        update(BARBELL_KG, kilograms)
    }
    override suspend fun setBarbellLb(pounds: Float) {
        require(pounds.isFinite() && pounds > 0f)
        update(BARBELL_LB, pounds)
    }
    override suspend fun setPlatesKg(stored: String) = update(PLATES_KG, canonicalPlates(stored))
    override suspend fun setPlatesLb(stored: String) = update(PLATES_LB, canonicalPlates(stored))
    override suspend fun setDefaultWarmupRestSeconds(seconds: Int) = update(DEFAULT_WARMUP_REST_SECONDS, restSeconds(seconds))
    override suspend fun setDefaultWorkingRestSeconds(seconds: Int) = update(DEFAULT_WORKING_REST_SECONDS, restSeconds(seconds))
    override suspend fun setDefaultSupersetIntraRestSeconds(seconds: Int) = update(DEFAULT_SUPERSET_INTRA_REST_SECONDS, restSeconds(seconds))
    override suspend fun setProgressionQualified(qualified: Map<Long, Long>) =
        update(PROGRESSION_QUALIFIED, formatProgressionMap(qualified))
    override suspend fun setProgressionSkipped(skipped: Map<Long, Long>) =
        update(PROGRESSION_SKIP, formatProgressionMap(skipped))

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
        keepScreenOn = preferences[KEEP_SCREEN_ON] ?: true,
        progressionEnabled = preferences[PROGRESSION_ENABLED] ?: true,
        progressionIncrementKg = preferences[PROGRESSION_INCREMENT_KG] ?: 2.5f,
        progressionIncrementLb = preferences[PROGRESSION_INCREMENT_LB] ?: 5f,
        rpeEnabled = preferences[RPE_ENABLED] ?: false,
        hapticsEnabled = preferences[HAPTICS_ENABLED] ?: true,
        barbellKg = preferences[BARBELL_KG] ?: 20f,
        barbellLb = preferences[BARBELL_LB] ?: 45f,
        platesKg = preferences[PLATES_KG]?.takeIf { parsePlateList(it).isNotEmpty() } ?: DEFAULT_PLATES_KG,
        platesLb = preferences[PLATES_LB]?.takeIf { parsePlateList(it).isNotEmpty() } ?: DEFAULT_PLATES_LB,
        defaultWarmupRestSeconds = preferences[DEFAULT_WARMUP_REST_SECONDS] ?: 90,
        defaultWorkingRestSeconds = preferences[DEFAULT_WORKING_REST_SECONDS] ?: 180,
        defaultSupersetIntraRestSeconds = preferences[DEFAULT_SUPERSET_INTRA_REST_SECONDS] ?: 0,
        progressionQualified = parseProgressionMap(preferences[PROGRESSION_QUALIFIED] ?: ""),
        progressionSkipped = parseProgressionMap(preferences[PROGRESSION_SKIP] ?: ""),
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
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val PROGRESSION_ENABLED = booleanPreferencesKey("progression_enabled")
        val PROGRESSION_INCREMENT_KG = floatPreferencesKey("progression_increment_kg")
        val PROGRESSION_INCREMENT_LB = floatPreferencesKey("progression_increment_lb")
        val RPE_ENABLED = booleanPreferencesKey("rpe_enabled")
        val HAPTICS_ENABLED = booleanPreferencesKey("haptics_enabled")
        val BARBELL_KG = floatPreferencesKey("barbell_kg")
        val BARBELL_LB = floatPreferencesKey("barbell_lb")
        val PLATES_KG = stringPreferencesKey("plates_kg")
        val PLATES_LB = stringPreferencesKey("plates_lb")
        val DEFAULT_WARMUP_REST_SECONDS = intPreferencesKey("default_warmup_rest_seconds")
        val DEFAULT_WORKING_REST_SECONDS = intPreferencesKey("default_working_rest_seconds")
        val DEFAULT_SUPERSET_INTRA_REST_SECONDS = intPreferencesKey("default_superset_intra_rest_seconds")
        val PROGRESSION_QUALIFIED = stringPreferencesKey("progression_qualified")
        val PROGRESSION_SKIP = stringPreferencesKey("progression_skip")
    }
}

fun parsePlateList(stored: String): List<Float> =
    stored.split(',')
        .mapNotNull { token -> token.trim().toFloatOrNull() }
        .filter { it.isFinite() && it > 0f }

fun formatPlateList(plates: List<Float>): String =
    plates.joinToString(",") { value ->
        String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    }

private fun canonicalPlates(stored: String): String {
    val plates = parsePlateList(stored).distinct().sortedDescending()
    require(plates.isNotEmpty()) { "Plate list needs one denomination" }
    return formatPlateList(plates)
}

private fun restSeconds(seconds: Int): Int {
    require(seconds >= 0) { "Rest seconds cannot be negative" }
    return seconds
}

private fun String?.toLocalTimeOrDefault(default: LocalTime): LocalTime =
    this?.let { runCatching { LocalTime.parse(it) }.getOrNull() } ?: default
