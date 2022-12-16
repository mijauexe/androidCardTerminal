package com.card.terminal.db.dao

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import com.card.terminal.db.entity.PinCode;

@Dao
interface PinCodeDao {

    @Query("SELECT * FROM pinCode")
    fun getAll(): List<PinCode>

    @Insert
    fun insertAll(vararg pinCode: PinCode)

}