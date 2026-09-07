package com.fastek.notea.data.local

import androidx.room.TypeConverter
import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.data.local.entity.TypeNote
import java.time.LocalDate

/**
 * Convertisseurs Room. LocalDate nécessite le "core library desugaring" côté Gradle
 * pour être utilisable jusqu'à Android 7 (API 24) — voir NOTES.md.
 */
class Converters {

    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    fun toEpochDay(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    fun fromTypeNote(value: String?): TypeNote? = value?.let { TypeNote.valueOf(it) }

    @TypeConverter
    fun toTypeNote(type: TypeNote?): String? = type?.name

    @TypeConverter
    fun fromTypeEtablissement(value: String?): TypeEtablissement? =
        value?.let { TypeEtablissement.valueOf(it) }

    @TypeConverter
    fun toTypeEtablissement(type: TypeEtablissement?): String? = type?.name
}
