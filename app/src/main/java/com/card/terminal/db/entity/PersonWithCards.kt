package com.card.terminal.db.entity

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
data class PersonWithCards(
    @Embedded val person: Person,
    @Serializable
    @Relation(
        parentColumn = "uid",
        entityColumn = "owner"
    )
    val cards: List<Card>,
    @Serializable
    @Relation(
        parentColumn = "class_type",
        entityColumn = "class_type"
    )
    val classTypeCards: List<Card>
)