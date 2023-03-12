package com.card.terminal.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
class Card(
    @PrimaryKey @ColumnInfo(name = "card_number") val cardNumber: String,
    @ColumnInfo(name = "owner") val owner: Int,
    @ColumnInfo(name = "expiration_date") val expirationDate: String,
)