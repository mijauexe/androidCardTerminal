package com.card.terminal.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import kotlinx.serialization.Serializable

@Entity
    (primaryKeys = ["uid", "class_type"])
@Serializable
class AccessLevel(
    @Serializable @ColumnInfo(name = "uid") val uid: Int,
    @Serializable @ColumnInfo(name = "class_type") val classType: String,
    @Serializable @ColumnInfo(name = "access_level") val accessLevel: Int,
)