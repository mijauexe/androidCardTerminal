package com.card.terminal.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
data class PinCode(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "pin_code") val pinCode: String,
    //@ColumnInfo(name = "expiry_date") val expiryDate: String
)