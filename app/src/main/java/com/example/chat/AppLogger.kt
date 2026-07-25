package com.example.chat

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {
    private val _logs = MutableStateFlow<List<String>>(emptyList())
    val logs: StateFlow<List<String>> = _logs.asStateFlow()
    private val dateFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        addLog("D/$tag: $message")
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e(tag, message, throwable)
        val trace = throwable?.stackTraceToString() ?: ""
        addLog("E/$tag: $message\n$trace")
    }

    private fun addLog(logMsg: String) {
        val time = dateFormat.format(Date())
        val fullMsg = "[$time] $logMsg"
        val current = _logs.value.toMutableList()
        current.add(fullMsg)
        if (current.size > 200) {
            current.removeAt(0)
        }
        _logs.value = current
    }
    
    fun clear() {
        _logs.value = emptyList()
    }
}
