package com.card.terminal.db.entity

import androidx.room.Entity

@Entity(primaryKeys = ["modeId", "scheduleId"])
data class ModeScheduleCrossRef(
    val modeId: Int,
    val scheduleId: Int
)