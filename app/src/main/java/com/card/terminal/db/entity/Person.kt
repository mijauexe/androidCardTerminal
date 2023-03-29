package com.card.terminal.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity
@Serializable
class Person(
    @Serializable @PrimaryKey @ColumnInfo(name = "uid") val uid: Int,
    @Serializable  @ColumnInfo(name = "first_name") val firstName: String,
    @Serializable @ColumnInfo(name = "last_name") val lastName: String,
//    @Ignore val picture: Bitmap?
)