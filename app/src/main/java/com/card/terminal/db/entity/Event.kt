package com.card.terminal.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
class Event (
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "event_code") val eventCode: Int,
    @ColumnInfo(name = "card_number") val cardNumber: Int,
    @ColumnInfo(name = "date_time") val dateTime: String,
    @ColumnInfo(name = "published") val published: Boolean,
)