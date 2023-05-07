package com.card.terminal.db.dao

import androidx.room.*
import com.card.terminal.db.entity.Calendar

@Dao
interface CalendarDao {
    @Query("SELECT * FROM Calendar")
    fun getAll(): List<Calendar>?

    @Query("SELECT * FROM Calendar WHERE uid = :id")
    fun get(id: Int): Calendar?

    @Query("SELECT * FROM calendar WHERE day = :day AND month = :month AND year = :year AND work_day = :workDay")
    fun getByDateAndWorkDay(day: Int, month: Int, year: Int, workDay: Boolean): List<Calendar>

    @Query("SELECT * FROM calendar WHERE day = :day AND month = :month AND year = :year")
    fun getByDate(day: Int, month: Int, year: Int): Calendar?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg calendar: Calendar)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<Calendar>)

    @Update
    fun update(calendar: Calendar)

    @Delete
    fun delete(calendar: Calendar)
}