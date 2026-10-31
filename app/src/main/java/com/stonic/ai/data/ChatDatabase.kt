package com.stonic.ai.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class ChatMessage(val id: Long, val role: String, val text: String, val timestamp: Long)

class ChatDatabase(context: Context) : SQLiteOpenHelper(context, "stonic_chat.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE messages (id INTEGER PRIMARY KEY AUTOINCREMENT, role TEXT NOT NULL, body TEXT NOT NULL, created_at INTEGER NOT NULL)")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}
    fun add(role: String, body: String) {
        val values = ContentValues().apply { put("role", role); put("body", body); put("created_at", System.currentTimeMillis()) }
        writableDatabase.insert("messages", null, values)
    }
    fun all(): List<ChatMessage> {
        val result = mutableListOf<ChatMessage>()
        readableDatabase.rawQuery("SELECT id, role, body, created_at FROM messages ORDER BY id ASC", null).use { c ->
            while (c.moveToNext()) result.add(ChatMessage(c.getLong(0), c.getString(1), c.getString(2), c.getLong(3)))
        }
        return result
    }
    fun clear() { writableDatabase.delete("messages", null, null) }
}
