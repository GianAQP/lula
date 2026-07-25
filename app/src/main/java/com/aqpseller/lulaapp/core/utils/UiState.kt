package com.aqpseller.lulaapp.core.utils

/** Envoltorio genérico de estado de carga para ViewModels que exponen datos vía Flow. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
}
