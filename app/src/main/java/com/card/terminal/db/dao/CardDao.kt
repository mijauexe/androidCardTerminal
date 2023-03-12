package com.card.terminal.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.card.terminal.db.entity.Card

@Dao
interface CardDao {
    @Query("SELECT * FROM Card")
    fun getAll(): List<Card>

    @Query("SELECT * FROM Card WHERE owner = :personId")
    fun getCardsByPersonId(personId: Int): List<Card>

    @Insert
    fun insertAll(vararg card: Card)
}