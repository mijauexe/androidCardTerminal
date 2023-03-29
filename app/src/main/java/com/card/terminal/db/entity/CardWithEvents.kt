package com.card.terminal.db.entity

import androidx.room.Embedded
import androidx.room.Relation
import kotlinx.serialization.Serializable

@Serializable
data class CardWithEvents(
    @Embedded val card: Card,
    @Serializable
    @Relation(
        parentColumn = "card_number",
        entityColumn = "card_number"
    )
    val events: List<Event>
)