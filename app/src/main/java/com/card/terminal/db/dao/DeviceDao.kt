package com.card.terminal.db.dao

import androidx.room.*
import com.card.terminal.db.entity.Device

@Dao
interface DeviceDao {
    @Query("SELECT * FROM Device")
    fun getAll(): List<Device>?

    @Query("SELECT * FROM Device WHERE uid = :uid")
    fun get(uid: Int): Device?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg devices: Device)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(devices: List<Device>)

    @Delete
    fun delete(vararg devices: Device)

    @Query("DELETE FROM Device")
    fun deleteAll()

    @Delete
    fun deleteMany(list: List<Device>)
}