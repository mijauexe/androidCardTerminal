package com.card.terminal.db.dao

import androidx.room.*
import com.card.terminal.db.entity.OperationMode
import com.card.terminal.db.entity.OperationModeWithSchedules

@Dao
interface OperationModeDao {
    @Transaction
    @Query("SELECT * FROM OperationMode")
    fun getModesWithSchedules(): List<OperationModeWithSchedules>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<OperationMode>)
}