package com.example.repsgrams.data.db

import androidx.room.TypeConverter
import java.time.Instant
import java.time.LocalDate

class DatabaseConverters {
    @TypeConverter fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()
    @TypeConverter fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)
    @TypeConverter fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilli()
    @TypeConverter fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)
    @TypeConverter fun repTypeToString(value: RepType?): String? = value?.name
    @TypeConverter fun stringToRepType(value: String?): RepType? = value?.let(RepType::valueOf)
    @TypeConverter fun blockKindToString(value: BlockKind?): String? = value?.name
    @TypeConverter fun stringToBlockKind(value: String?): BlockKind? = value?.let(BlockKind::valueOf)
    @TypeConverter fun sessionKindToString(value: SessionKind?): String? = value?.name
    @TypeConverter fun stringToSessionKind(value: String?): SessionKind? = value?.let(SessionKind::valueOf)
    @TypeConverter fun scheduleModeToString(value: ScheduleMode?): String? = value?.name
    @TypeConverter fun stringToScheduleMode(value: String?): ScheduleMode? = value?.let(ScheduleMode::valueOf)
    @TypeConverter fun setTypeToString(value: SetType?): String? = value?.name
    @TypeConverter fun stringToSetType(value: String?): SetType? = value?.let(SetType::valueOf)
    @TypeConverter fun equipmentToString(value: Equipment?): String? = value?.name
    @TypeConverter fun stringToEquipment(value: String?): Equipment? = value?.let(Equipment::valueOf)
}
