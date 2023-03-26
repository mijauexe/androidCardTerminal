package com.card.terminal.db.entity

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
data class PersonWithCards(
    @Embedded val person: Person,
    @Relation(
        parentColumn = "uid",
        entityColumn = "owner"
    )
    val cards: List<Card>
)