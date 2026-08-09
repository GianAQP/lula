package com.aqpseller.lulaapp.domain.repository

interface PrivacidadRepository {
    suspend fun estaConfigurada(): Boolean
    suspend fun configurarPin(pin: String)
    suspend fun verificarPin(pin: String): Boolean
}
