package com.card.terminal.db.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class OperationScheduleWithModes(
    @Embedded val schedule: OperationSchedule,
    @Relation(
        parentColumn = "uid",
        entityColumn = "modeId",
        associateBy = Junction(ModeScheduleCrossRef::class)
    )
    val modes: List<OperationMode>
)