package com.oem.car.core.common

sealed interface CarResult<out T> {
    data class Success<T>(val data: T) : CarResult<T>
    data class Error(val exception: Throwable, val message: String? = null) : CarResult<Nothing>
    data object Loading : CarResult<Nothing>
}
