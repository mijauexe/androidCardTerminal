package com.card.terminal.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
data class AllowedAccess(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    @ColumnInfo(name = "card_number") val cardNumber: String?,
    @ColumnInfo(name = "allowed") val allowed: Boolean?
)