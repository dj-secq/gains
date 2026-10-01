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
}
