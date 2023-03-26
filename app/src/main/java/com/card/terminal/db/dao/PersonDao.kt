package com.card.terminal.db.dao

import androidx.room.*
import com.card.terminal.db.entity.Person

@Dao
interface PersonDao {
    @Query("SELECT * FROM Person")
    fun getAll(): List<Person>

    @Query("SELECT * FROM Person WHERE uid = :uid")
    fun get(uid: Int): Person

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg person: Person)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(person: List<Person>)


    @Query("DELETE FROM Person WHERE uid = :uid")
    fun deleteOne(vararg uid: Int): Int

//    @Query("DELETE FROM Person WHERE uid in :ids")
//    fun deleteMany(ids: List<Int>)

}