package com.card.terminal.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
class OperationSchedule(
    @Serializable @PrimaryKey @ColumnInfo(name = "uid") val uid: Int,
    @Serializable @ColumnInfo(name = "description") val description: String,
    @Serializable @ColumnInfo(name = "timeFrom") val timeFrom: String,
    @Serializable @ColumnInfo(name = "timeTo") val timeTo: String,
)