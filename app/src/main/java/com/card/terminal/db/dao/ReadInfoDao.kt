package com.card.terminal.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.card.terminal.db.entity.ReadInfo


@Dao
interface ReadInfoDao {

    @Query("SELECT * FROM readInfo")
    fun getAll(): List<ReadInfo>

    @Insert
    fun insertAll(vararg readInfo: ReadInfo)

}
