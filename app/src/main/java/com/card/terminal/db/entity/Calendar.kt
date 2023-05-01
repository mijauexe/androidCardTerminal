package com.card.terminal.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
class Calendar(
    @Serializable @PrimaryKey @ColumnInfo(name = "uid") val uid: Int,
    @Serializable @ColumnInfo(name = "day") val day: Int,
    @Serializable @ColumnInfo(name = "month") val month: Int,
    @Serializable @ColumnInfo(name = "year") val year: Int,
    @Serializable @ColumnInfo(name = "work_day") val workDay: Boolean,
    @Serializable @ColumnInfo(name = "description") val description: String,
)