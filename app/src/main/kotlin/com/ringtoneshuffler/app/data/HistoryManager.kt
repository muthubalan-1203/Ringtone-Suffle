package com.ringtoneshuffler.app.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class HistoryItem(
    val uriString: String,
    val timestamp: Long
)

class HistoryManager(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _history = MutableStateFlow(getHistory())
    val history: StateFlow<List<HistoryItem>> = _history

    fun getHistory(): List<HistoryItem> {
        val raw = prefs.getString(KEY_HISTORY, "") ?: ""
        if (raw.isBlank()) return emptyList()

        return raw.split(DELIMITER).mapNotNull { itemString ->
            val parts = itemString.split(ITEM_DELIMITER)
            if (parts.size == 2) {
                val uri = parts[0]
                val timestamp = parts[1].toLongOrNull() ?: 0L
                HistoryItem(uri, timestamp)
            } else {
                null
            }
        }
    }

    fun addHistoryItem(uri: Uri) {
        val current = getHistory().toMutableList()
        // Add new item at the beginning
        current.add(0, HistoryItem(uri.toString(), System.currentTimeMillis()))
        
        // Keep only the last 50 items to avoid storing too much data
        if (current.size > 50) {
            current.subList(50, current.size).clear()
        }

        saveHistory(current)
    }

    fun clearHistory() {
        saveHistory(emptyList())
    }

    private fun saveHistory(historyList: List<HistoryItem>) {
        val serialized = historyList.joinToString(DELIMITER) { item ->
            "${item.uriString}$ITEM_DELIMITER${item.timestamp}"
        }
        prefs.edit().putString(KEY_HISTORY, serialized).apply()
        _history.value = historyList
    }

    companion object {
        const val PREFS_NAME = "ringtone_shuffler_history"
        const val KEY_HISTORY = "history_list"
        const val DELIMITER = "|;|"
        const val ITEM_DELIMITER = "|=|"
    }
}
