package com.card.terminal.db.dao

import androidx.room.*
import com.card.terminal.db.entity.Person

@Dao
interface PersonDao {
    @Query("SELECT * FROM Person")
    fun getAll(): List<Person>

    @Query("SELECT * FROM Person WHERE uid = :uid AND class_type = :classType")
    fun get(uid: Int, classType: String): Person

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg person: Person)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(person: List<Person>)

    @Query("DELETE FROM Person WHERE uid = :uid")
    fun deleteOne(vararg uid: Int): Int

    @Delete
    fun deleteMany(list: List<Person>)
}