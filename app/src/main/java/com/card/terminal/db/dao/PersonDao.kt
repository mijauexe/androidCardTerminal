package com.card.terminal.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.card.terminal.db.entity.Card
import com.card.terminal.db.entity.Person

@Dao
interface PersonDao {
    @Query("SELECT * FROM Person")
    fun getAll(): List<Person>

    @Insert
    fun insertAll(vararg person: Person)
}