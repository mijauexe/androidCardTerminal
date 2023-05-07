package com.card.terminal.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.card.terminal.db.entity.PersonWithCards

@Dao
interface PersonWithCardsDao {
    @Transaction
    @Query("SELECT * FROM Person")
    fun getAll(): List<PersonWithCards>?

//    @Transaction
//    @Query("SELECT * FROM PersonWithCards where owner = :ownerId")
//    fun getPersonAndCards(vararg ownerId : Int): List<PersonWithCards>

    @Transaction
    @Query("DELETE FROM Card WHERE owner = :uid")
    fun deleteCard(vararg uid: Int): Int?
}