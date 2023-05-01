package com.card.terminal.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
class Card(
    @Serializable @PrimaryKey @ColumnInfo(name = "card_number") val cardNumber: String,
    @Serializable @ColumnInfo(name = "owner") val owner: Int,
    @Serializable @ColumnInfo(name = "class_type") val classType: String,
    @Serializable @ColumnInfo(name = "activation_date") val activationDate: String,
    @Serializable @ColumnInfo(name = "expiration_date") val expirationDate: String,
//    @Serializable @ColumnInfo(name = "access_level") val accessLevel: Int,
)