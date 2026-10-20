package com.oem.car.core.common

import android.os.Trace
import android.util.Log

object CarLogger {
    private const val DEFAULT_TAG = "AAOS_Cockpit"

    fun d(tag: String = DEFAULT_TAG, message: String) {
        Log.d(tag, "[DEBUG] $message")
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        Log.i(tag, "[INFO] $message")
    }

    fun w(tag: String = DEFAULT_TAG, message: String) {
        Log.w(tag, "[WARN] $message")
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        Log.e(tag, "[ERROR] $message", throwable)
    }

    inline fun <T> traceSection(sectionName: String, block: () -> T): T {
        Trace.beginSection(sectionName)
        return try {
            block()
        } finally {
            Trace.endSection()
        }
    }
}
