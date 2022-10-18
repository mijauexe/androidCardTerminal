package com.card.terminal.db

import androidx.room.TypeConverter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class Converters {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")

    @TypeConverter
    fun fromLocalDateTime(value: LocalDateTime?): String? {
        return value?.let { value.format(formatter) }
    }

    @TypeConverter
    fun toLocalDateTime(value: String?): LocalDateTime? {
        return value?.let { LocalDateTime.parse(value, formatter) }
    }
}