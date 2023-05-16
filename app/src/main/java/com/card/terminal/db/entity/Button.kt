package com.card.terminal.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
class Button(
    @Serializable @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "uid") val uid: Int,
    @Serializable @ColumnInfo(name = "classType") val classType: String,
    @Serializable @ColumnInfo(name = "label") val label: String,
    @Serializable @ColumnInfo(name = "title") val title: String,
    @Serializable @ColumnInfo(name = "eCode2") val eCode2: Int,
    @Serializable @ColumnInfo(name = "eCode") val eCode: Int,
)