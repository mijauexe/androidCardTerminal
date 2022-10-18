package com.card.terminal.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity
data class ReadInfo (
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "card_number") val cardNumber: String?,
    @ColumnInfo(name = "timestamp") val timestamp: LocalDateTime?
)
