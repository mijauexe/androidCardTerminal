package com.card.terminal.db.entity

import android.graphics.Bitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
    (primaryKeys = ["uid", "class_type"])
@Serializable
class Person(
    @Serializable @ColumnInfo(name = "uid") val uid: Int,
    @Serializable @ColumnInfo(name = "class_type") val classType: String,
    @Serializable @ColumnInfo(name = "first_name") val firstName: String,
    @Serializable @ColumnInfo(name = "last_name") val lastName: String,
    @Serializable @ColumnInfo(name = "image") val image: String,
)