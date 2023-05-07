package com.card.terminal.db.dao

import androidx.room.*
import com.card.terminal.db.entity.OperationMode

@Dao
interface OperationModeDao {
//    @Transaction
//    @Query("SELECT * FROM OperationMode")
//    fun getModesWithSchedules(): List<OperationModeWithSchedules>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<OperationMode>)

    @Query("SELECT * FROM OperationMode WHERE uid = :uid")
    fun getById(uid: Int): List<OperationMode>?

    @Query("SELECT * FROM OperationMode")
    fun getAll(): List<OperationMode>?

    @Query("DELETE FROM OperationMode")
    fun deleteAll()
}