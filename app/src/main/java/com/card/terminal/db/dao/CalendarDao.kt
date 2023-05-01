package com.card.terminal.db.dao

import androidx.room.*
import com.card.terminal.db.entity.Calendar

@Dao
interface CalendarDao {
    @Query("SELECT * FROM Calendar")
    fun getAll(): List<Calendar>

    @Query("SELECT * FROM Calendar WHERE uid = :id")
    fun get(id: Int): Calendar?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg calendar: Calendar)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<Calendar>)

    @Update
    fun update(calendar: Calendar)

    @Delete
    fun delete(calendar: Calendar)
}