package com.aqpseller.lulaapp.di

import com.aqpseller.lulaapp.data.preferences.AjustesRepositoryImpl
import com.aqpseller.lulaapp.data.preferences.PrivacidadRepositoryImpl
import com.aqpseller.lulaapp.data.repository.ActividadRepositoryImpl
import com.aqpseller.lulaapp.data.repository.AuthRepositoryLocalImpl
import com.aqpseller.lulaapp.data.repository.ConexionRepositoryImpl
import com.aqpseller.lulaapp.data.repository.EntradaDiarioRepositoryImpl
import com.aqpseller.lulaapp.data.repository.EspacioRepositoryImpl
import com.aqpseller.lulaapp.data.repository.FinanzasRepositoryImpl
import com.aqpseller.lulaapp.data.repository.ListaRepositoryImpl
import com.aqpseller.lulaapp.data.repository.MetaRepositoryImpl
import com.aqpseller.lulaapp.data.repository.NotaRepositoryImpl
import com.aqpseller.lulaapp.data.repository.PropositoPersonalRepositoryImpl
import com.aqpseller.lulaapp.data.repository.RegistroDiarioRepositoryImpl
import com.aqpseller.lulaapp.data.repository.RegistroSemanalRepositoryImpl
import com.aqpseller.lulaapp.data.repository.RetoFamiliarRepositoryImpl
import com.aqpseller.lulaapp.data.repository.SolicitudCompartirRepositoryImpl
import com.aqpseller.lulaapp.data.repository.UsuarioRepositoryImpl
import com.aqpseller.lulaapp.domain.repository.ActividadRepository
import com.aqpseller.lulaapp.domain.repository.AjustesRepository
import com.aqpseller.lulaapp.domain.repository.AuthRepository
import com.aqpseller.lulaapp.domain.repository.ConexionRepository
import com.aqpseller.lulaapp.domain.repository.EntradaDiarioRepository
import com.aqpseller.lulaapp.domain.repository.EspacioRepository
import com.aqpseller.lulaapp.domain.repository.FinanzasRepository
import com.aqpseller.lulaapp.domain.repository.ListaRepository
import com.aqpseller.lulaapp.domain.repository.MetaRepository
import com.aqpseller.lulaapp.domain.repository.NotaRepository
import com.aqpseller.lulaapp.domain.repository.PrivacidadRepository
import com.aqpseller.lulaapp.domain.repository.PropositoPersonalRepository
import com.aqpseller.lulaapp.domain.repository.RegistroDiarioRepository
import com.aqpseller.lulaapp.domain.repository.RegistroSemanalRepository
import com.aqpseller.lulaapp.domain.repository.RetoFamiliarRepository
import com.aqpseller.lulaapp.domain.repository.SolicitudCompartirRepository
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

    @Binds
    @Singleton
    abstract fun bindPrivacidadRepository(impl: PrivacidadRepositoryImpl): PrivacidadRepository

    @Binds
    @Singleton
    abstract fun bindMetaRepository(impl: MetaRepositoryImpl): MetaRepository

    @Binds
    @Singleton
    abstract fun bindRegistroSemanalRepository(impl: RegistroSemanalRepositoryImpl): RegistroSemanalRepository

    @Binds
    @Singleton
    abstract fun bindAjustesRepository(impl: AjustesRepositoryImpl): AjustesRepository

    @Binds
    @Singleton
    abstract fun bindListaRepository(impl: ListaRepositoryImpl): ListaRepository

    @Binds
    @Singleton
    abstract fun bindSolicitudCompartirRepository(impl: SolicitudCompartirRepositoryImpl): SolicitudCompartirRepository

    @Binds
    @Singleton
    abstract fun bindNotaRepository(impl: NotaRepositoryImpl): NotaRepository

    @Binds
    @Singleton
    abstract fun bindEntradaDiarioRepository(impl: EntradaDiarioRepositoryImpl): EntradaDiarioRepository

    @Binds
    @Singleton
    abstract fun bindRetoFamiliarRepository(impl: RetoFamiliarRepositoryImpl): RetoFamiliarRepository

    @Binds
    @Singleton
    abstract fun bindPropositoPersonalRepository(impl: PropositoPersonalRepositoryImpl): PropositoPersonalRepository

    @Binds
    @Singleton
    abstract fun bindConexionRepository(impl: ConexionRepositoryImpl): ConexionRepository
}
