package com.card.terminal.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
class Event (
    @Serializable @PrimaryKey(autoGenerate = true) val uid: Int,
    @Serializable @ColumnInfo(name = "event_code") val eventCode: Int,
    @Serializable @ColumnInfo(name = "event_code2") val eventCode2: Int,
    @Serializable @ColumnInfo(name = "card_number") val cardNumber: Int,
    @Serializable @ColumnInfo(name = "date_time") val dateTime: String,
    @Serializable @ColumnInfo(name = "published") val published: Boolean,
)

