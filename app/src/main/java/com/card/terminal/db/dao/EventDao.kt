package com.card.terminal.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.card.terminal.db.entity.Event

@Dao
interface EventDao {
    @Query("SELECT * FROM Event")
    fun getAll(): List<Event>

    @Query("SELECT * FROM Event WHERE uid = :uid")
    fun get(uid: Int): Event

    @Query("SELECT * FROM Event WHERE card_number = :cardNumber")
    fun getEventsByCardNumber(cardNumber: Int): List<Event>

    @Query("SELECT * FROM Event WHERE event_code = :eventCode")
    fun getEventsByEventCode(eventCode: Int): List<Event>
}