package com.card.terminal.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.card.terminal.db.entity.Button

@Dao
interface ButtonDao {
    @Query("SELECT * FROM Button")
    fun getAll(): List<Button>?

    @Query("SELECT * FROM Button WHERE classType = :classType")
    fun getAllByClassType(classType: String): List<Button>?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(button: Button)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(buttons: List<Button>)

    @Query("DELETE FROM Button")
    fun deleteAll()
}
