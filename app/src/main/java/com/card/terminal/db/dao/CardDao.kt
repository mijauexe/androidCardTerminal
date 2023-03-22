package com.card.terminal.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.card.terminal.db.entity.Card

@Dao
interface CardDao {
    @Query("SELECT * FROM Card")
    fun getAll(): List<Card>

    @Query("SELECT * FROM Card WHERE card_number = :cardNumber")
    fun get(vararg cardNumber: Int): Card

    @Query("SELECT * FROM Card WHERE owner = :personId")
    fun getCardsByPersonId(personId: Int): List<Card>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg card: Card)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(cards: List<Card>)

    @Query("DELETE FROM Card WHERE owner = :ownerUid")
    fun deleteByOwnerId(vararg ownerUid: Int): Int

    @Query("DELETE FROM Card WHERE card_number = :cardNumber")
    fun deleteByCardNumber(vararg cardNumber: Int): Int
}