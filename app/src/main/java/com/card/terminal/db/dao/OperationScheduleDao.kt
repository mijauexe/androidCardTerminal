package com.card.terminal.db.dao

import androidx.room.*
import com.card.terminal.db.entity.OperationSchedule

@Dao
interface OperationScheduleDao {
//    @Transaction
//    @Query("SELECT * FROM OperationSchedule")
//    fun getSchedulesWithModes(): List<OperationScheduleWithModes>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<OperationSchedule>)

    @Query("SELECT * FROM OperationSchedule")
    fun getAll(): List<OperationSchedule>?

    @Query("SELECT * FROM OperationSchedule WHERE uid = :uid")
    fun getById(uid: Int): OperationSchedule??

    @Query("SELECT * FROM OperationSchedule WHERE description = :description")
    fun getByDescription(description: String): OperationSchedule?

    @Query("SELECT * FROM OperationSchedule WHERE timeFrom = :timeFrom")
    fun getByTimeFrom(timeFrom: String): List<OperationSchedule>?

    @Query("SELECT * FROM OperationSchedule WHERE timeTo = :timeTo")
    fun getByTimeTo(timeTo: String): List<OperationSchedule>?
    @Query("DELETE FROM OperationSchedule")
    fun deleteAll()
}