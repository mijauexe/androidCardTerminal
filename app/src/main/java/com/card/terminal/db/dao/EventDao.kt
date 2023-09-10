package com.card.terminal.db.dao

import androidx.room.*
import com.card.terminal.db.entity.Event

@Dao
interface EventDao {
    @Query("SELECT * FROM Event")
    fun getAll(): List<Event>?

    @Query("SELECT * FROM Event WHERE uid = :uid")
    fun get(uid: Int): Event?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg event: Event)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(events: List<Event>)

    @Update
    fun updateEvents(events: List<Event>)

    @Update
    fun update(vararg event: Event)

    @Query("SELECT * FROM Event WHERE card_number = :cardNumber")
    fun getEventsByCardNumber(cardNumber: Int): List<Event>?

    @Query("SELECT * FROM Event WHERE event_code = :eventCode")
    fun getEventsByEventCode(eventCode: Int): List<Event>?

    @Query("SELECT * FROM Event ORDER BY date_time DESC LIMIT 1")
    fun getLastScanEvent(): Event?

    @Query("SELECT * FROM Event WHERE card_number = :cardNumber ORDER BY date_time DESC LIMIT 1")
    fun getLastScanEventWithCardNumber(cardNumber: Int): Event?

    @Query("SELECT * FROM Event WHERE published = false")
    fun getUnpublishedEvents(): List<Event>?

    @Query("SELECT * FROM Event WHERE published = true")
    fun getPublishedEvents(): List<Event>?

    @Query("DELETE FROM Event WHERE image = :targetImage")
    fun deleteEventByImageUUID(targetImage: String)

    @Query("DELETE FROM Event")
    fun deleteAll()
}