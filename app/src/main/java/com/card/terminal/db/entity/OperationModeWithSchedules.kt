package com.card.terminal.db.entity

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class OperationModeWithSchedules(
    @Embedded val mode: OperationMode,
    @Relation(
        parentColumn = "uid",
        entityColumn = "scheduleId",
        associateBy = Junction(ModeScheduleCrossRef::class)
    )
    val schedules: List<OperationSchedule>
)