package com.card.terminal.db.dao

import androidx.room.*
import com.card.terminal.db.entity.AccessLevel

@Dao
interface AccessLevelDao {
    @Query("SELECT * FROM AccessLevel WHERE uid = :uid")
    fun get(uid: Int): AccessLevel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg accessLevels: AccessLevel)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(person: List<AccessLevel>)

    @Delete
    fun delete(vararg accessLevels: AccessLevel)

    @Delete
    fun deleteMany(list: List<AccessLevel>)

    @Query("DELETE FROM AccessLevel")
    fun deleteAll()
}
