package com.card.terminal.db.entity

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
data class PersonWithAccessLevels(
    @Embedded val person: Person,
    @Serializable
    @Relation(
        parentColumn = "uid",
        entityColumn = "uid"
    )
    val uidAccessLevels: List<AccessLevel>,
    @Serializable
    @Relation(
        parentColumn = "class_type",
        entityColumn = "class_type"
    )
    val classTypeAccessLevels: List<AccessLevel>
)