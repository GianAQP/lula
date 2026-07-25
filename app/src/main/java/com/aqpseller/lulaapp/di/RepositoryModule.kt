package com.aqpseller.lulaapp.di

import com.aqpseller.lulaapp.data.repository.ActividadRepositoryImpl
import com.aqpseller.lulaapp.data.repository.AuthRepositoryLocalImpl
import com.aqpseller.lulaapp.data.repository.EspacioRepositoryImpl
import com.aqpseller.lulaapp.data.repository.FinanzasRepositoryImpl
import com.aqpseller.lulaapp.data.repository.RegistroDiarioRepositoryImpl
import com.aqpseller.lulaapp.data.repository.UsuarioRepositoryImpl
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.AuthRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.FinanzasRepository
import com.aqpseller.lulaapp.domain.repository.RegistroDiarioRepository
import com.aqpseller.lulaapp.domain.repository.UsuarioRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryLocalImpl): AuthRepository

    @Binds
    @Singleton
    abstract fun bindUsuarioRepository(impl: UsuarioRepositoryImpl): UsuarioRepository

    @Binds
    @Singleton
    abstract fun bindEspacioRepository(impl: EspacioRepositoryImpl): EspacioRepository

    @Binds
    @Singleton
    abstract fun bindActividadRepository(impl: ActividadRepositoryImpl): ActividadRepository

    @Binds
    @Singleton
    abstract fun bindFinanzasRepository(impl: FinanzasRepositoryImpl): FinanzasRepository

    @Binds
    @Singleton
    abstract fun bindRegistroDiarioRepository(impl: RegistroDiarioRepositoryImpl): RegistroDiarioRepository
}
