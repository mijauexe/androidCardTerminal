package com.card.terminal.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.card.terminal.db.entity.PersonWithCards

@Dao
interface PersonWithCardsDao {
    @Transaction
    @Query("SELECT * FROM Person")
    fun getAll(): List<PersonWithCards>
}