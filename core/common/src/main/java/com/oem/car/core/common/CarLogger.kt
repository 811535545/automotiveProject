package com.oem.car.core.common

import android.os.Trace
import android.util.Log

object CarLogger {
    private const val DEFAULT_TAG = "AAOS_Cockpit"

    fun d(tag: String = DEFAULT_TAG, message: String) {
        try {
            Log.d(tag, "[DEBUG] $message")
        } catch (e: Throwable) {
            println("[$tag][DEBUG] $message")
        }
    }

    fun i(tag: String = DEFAULT_TAG, message: String) {
        try {
            Log.i(tag, "[INFO] $message")
        } catch (e: Throwable) {
            println("[$tag][INFO] $message")
        }
    }

    fun w(tag: String = DEFAULT_TAG, message: String) {
        try {
            Log.w(tag, "[WARN] $message")
        } catch (e: Throwable) {
            println("[$tag][WARN] $message")
        }
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        try {
            Log.e(tag, "[ERROR] $message", throwable)
        } catch (e: Throwable) {
            println("[$tag][ERROR] $message ${throwable?.message ?: ""}")
        }
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
