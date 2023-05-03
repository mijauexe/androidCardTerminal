package com.card.terminal.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
class Device(
    @Serializable @PrimaryKey @ColumnInfo(name = "uid") val uid: Int,
    @Serializable @ColumnInfo(name = "local_id") val localId: Int,
    @Serializable @ColumnInfo(name = "control_in") val controlIn: Int,
    @Serializable @ColumnInfo(name = "control_out") val controlOut: Int,
    @Serializable @ColumnInfo(name = "description") val description: String,
)