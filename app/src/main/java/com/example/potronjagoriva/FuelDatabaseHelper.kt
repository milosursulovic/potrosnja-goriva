package com.example.potronjagoriva

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper


class FuelDatabaseHelper(context: Context) : SQLiteOpenHelper(context, "fuel_db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE fuel_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                liters REAL,
                kilometers REAL,
                consumption REAL,
                fuel_price INTEGER,
                timestamp INTEGER
            )
        """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS fuel_entries")
        onCreate(db)
    }

    fun insertEntry(entry: FuelEntry): Long {
        val db = writableDatabase
        val values = ContentValues().apply {
            put("liters", entry.liters)
            put("kilometers", entry.kilometers)
            put("consumption", entry.consumption)
            put("fuel_price", entry.fuelPrice)
            put("timestamp", entry.timestamp)
        }
        return db.insert("fuel_entries", null, values)
    }

    fun getAllEntries(): List<FuelEntry> {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM fuel_entries ORDER BY id DESC", null)
        val entries = mutableListOf<FuelEntry>()

        while (cursor.moveToNext()) {
            val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
            val liters = cursor.getDouble(cursor.getColumnIndexOrThrow("liters"))
            val kilometers = cursor.getDouble(cursor.getColumnIndexOrThrow("kilometers"))
            val consumption = cursor.getDouble(cursor.getColumnIndexOrThrow("consumption"))
            val fuelPrice = cursor.getInt(cursor.getColumnIndexOrThrow("fuel_price"))
            val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow("timestamp"))
            entries.add(FuelEntry(liters, kilometers, consumption, fuelPrice, timestamp, id))
        }

        cursor.close()
        return entries
    }

    fun deleteEntry(id: Long) {
        val db = writableDatabase
        db.delete("fuel_entries", "id = ?", arrayOf(id.toString()))
    }

    fun getTotalFuel(): Double {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT SUM(liters) FROM fuel_entries", null)
        var totalFuel = 0.0
        if (cursor.moveToFirst()) {
            totalFuel = cursor.getDouble(0)
        }
        cursor.close()
        return totalFuel
    }
}