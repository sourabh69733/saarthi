package dev.saarthi.app

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class FuelEntry(
    val id: Long,
    val savedAtMs: Long,
    val amountRupees: Double,
    val litres: Double,
    val odometerKm: Int,
    val transcript: String,
)

class FuelStore(context: Context) : SQLiteOpenHelper(context, "saarthi.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE fuel_entries (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                saved_at_ms INTEGER NOT NULL,
                amount_rupees REAL NOT NULL,
                litres REAL NOT NULL,
                odometer_km INTEGER NOT NULL,
                transcript TEXT NOT NULL
            )
        """.trimIndent())
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit

    fun save(command: FuelCommand, transcript: String): Long {
        val values = ContentValues().apply {
            put("saved_at_ms", System.currentTimeMillis())
            put("amount_rupees", command.amountRupees)
            put("litres", command.litres)
            put("odometer_km", command.odometerKm)
            put("transcript", transcript)
        }
        return writableDatabase.insertOrThrow("fuel_entries", null, values)
    }

    fun recent(limit: Int = 20): List<FuelEntry> {
        val entries = mutableListOf<FuelEntry>()
        readableDatabase.query(
            "fuel_entries", null, null, null, null, null, "saved_at_ms DESC, id DESC", limit.toString()
        ).use { cursor ->
            while (cursor.moveToNext()) {
                entries += FuelEntry(
                    cursor.getLong(cursor.getColumnIndexOrThrow("id")),
                    cursor.getLong(cursor.getColumnIndexOrThrow("saved_at_ms")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("amount_rupees")),
                    cursor.getDouble(cursor.getColumnIndexOrThrow("litres")),
                    cursor.getInt(cursor.getColumnIndexOrThrow("odometer_km")),
                    cursor.getString(cursor.getColumnIndexOrThrow("transcript")),
                )
            }
        }
        return entries
    }
}
