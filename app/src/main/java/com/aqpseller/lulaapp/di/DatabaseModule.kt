package com.aqpseller.lulaapp.di

import android.content.Context
import androidx.room.Room
import com.aqpseller.lulaapp.core.database.LulaDatabase
import com.aqpseller.lulaapp.core.database.MIGRATION_15_16
import com.aqpseller.lulaapp.core.database.MIGRATION_16_17
import com.aqpseller.lulaapp.core.database.MIGRATION_17_18
import com.aqpseller.lulaapp.core.database.MIGRATION_18_19
import com.aqpseller.lulaapp.core.database.MIGRATION_19_20
import com.aqpseller.lulaapp.core.database.MIGRATION_20_21
import com.aqpseller.lulaapp.core.database.MIGRATION_21_22
import com.aqpseller.lulaapp.core.database.MIGRATION_22_23
import com.aqpseller.lulaapp.core.database.MIGRATION_23_24
import com.aqpseller.lulaapp.core.database.MIGRATION_24_25
import com.aqpseller.lulaapp.core.database.MIGRATION_25_26
import com.aqpseller.lulaapp.core.database.MIGRATION_26_27
import com.aqpseller.lulaapp.core.database.MIGRATION_27_28
import com.aqpseller.lulaapp.core.database.MIGRATION_28_29
import com.aqpseller.lulaapp.core.database.MIGRATION_29_30
import com.aqpseller.lulaapp.core.database.MIGRATION_30_31
import com.aqpseller.lulaapp.data.local.dao.AreaDeVidaDao
import com.aqpseller.lulaapp.data.local.dao.ActividadDao
import com.aqpseller.lulaapp.data.local.dao.CitaDetalleDao
import com.aqpseller.lulaapp.data.local.dao.ConexionDao
import com.aqpseller.lulaapp.data.local.dao.EntradaDiarioDao
import com.aqpseller.lulaapp.data.local.dao.EspacioDao
import com.aqpseller.lulaapp.data.local.dao.EspacioMiembroDao
import com.aqpseller.lulaapp.data.local.dao.FechaImportanteDetalleDao
import com.aqpseller.lulaapp.data.local.dao.FinanzasDao
import com.aqpseller.lulaapp.data.local.dao.HabitoDetalleDao
import com.aqpseller.lulaapp.data.local.dao.HistorialCambiosDao
import com.aqpseller.lulaapp.data.local.dao.ListaDao
import com.aqpseller.lulaapp.data.local.dao.ListaEjecucionDao
import com.aqpseller.lulaapp.data.local.dao.ListaItemDao
import com.aqpseller.lulaapp.data.local.dao.MedicamentoDetalleDao
import com.aqpseller.lulaapp.data.local.dao.MetaDao
import com.aqpseller.lulaapp.data.local.dao.NotaDao
import com.aqpseller.lulaapp.data.local.dao.PropositoPersonalDao
import com.aqpseller.lulaapp.data.local.dao.RegistroActividadDao
import com.aqpseller.lulaapp.data.local.dao.RegistroDiarioDao
import com.aqpseller.lulaapp.data.local.dao.RegistroSemanalDao
import com.aqpseller.lulaapp.data.local.dao.RetoFamiliarDao
import com.aqpseller.lulaapp.data.local.dao.RutinaDetalleDao
import com.aqpseller.lulaapp.data.local.dao.SesionCitaDao
import com.aqpseller.lulaapp.data.local.dao.SolicitudCompartirDao
import com.aqpseller.lulaapp.data.local.dao.TareaDetalleDao
import com.aqpseller.lulaapp.data.local.dao.TomaMedicamentoDao
import com.aqpseller.lulaapp.data.local.dao.UsuarioDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideLulaDatabase(@ApplicationContext context: Context): LulaDatabase =
        Room.databaseBuilder(context, LulaDatabase::class.java, LulaDatabase.NOMBRE_BD)
            .addMigrations(MIGRATION_15_16, MIGRATION_16_17, MIGRATION_17_18, MIGRATION_18_19, MIGRATION_19_20, MIGRATION_20_21, MIGRATION_21_22, MIGRATION_22_23, MIGRATION_23_24, MIGRATION_24_25, MIGRATION_25_26, MIGRATION_26_27, MIGRATION_27_28, MIGRATION_28_29, MIGRATION_29_30, MIGRATION_30_31)
            // Solo permite borrar y recrear si alguien tiene una versión de pruebas vieja
            // (1-14, ninguna con datos reales de un usuario de verdad) — de la 15 en adelante
            // ya hay migraciones reales registradas arriba, así que un cambio de esquema sin
            // su `Migration` correspondiente hace que la app truene al abrir, a propósito,
            // para no volver a depender en silencio del borrado. Ver
            // `Plan/08-decisiones-tecnicas.md`. Importante: Room no permite que un mismo
            // número de versión esté en `addMigrations` (como versión de inicio) Y en este
            // rango a la vez — se cae con "Inconsistency detected" en tiempo de ejecución, no
            // al compilar (ver crash real, 2026-08-01). El límite de acá SIEMPRE debe terminar
            // un número antes de la versión de inicio de la migración real más vieja.
            .fallbackToDestructiveMigrationFrom(dropAllTables = true, *(1..14).toList().toIntArray())
            .build()

    @Provides fun provideUsuarioDao(db: LulaDatabase): UsuarioDao = db.usuarioDao()
    @Provides fun provideEspacioDao(db: LulaDatabase): EspacioDao = db.espacioDao()
    @Provides fun provideEspacioMiembroDao(db: LulaDatabase): EspacioMiembroDao = db.espacioMiembroDao()
    @Provides fun provideAreaDeVidaDao(db: LulaDatabase): AreaDeVidaDao = db.areaDeVidaDao()
    @Provides fun provideActividadDao(db: LulaDatabase): ActividadDao = db.actividadDao()
    @Provides fun provideHabitoDetalleDao(db: LulaDatabase): HabitoDetalleDao = db.habitoDetalleDao()
    @Provides fun provideTareaDetalleDao(db: LulaDatabase): TareaDetalleDao = db.tareaDetalleDao()
    @Provides fun provideRutinaDetalleDao(db: LulaDatabase): RutinaDetalleDao = db.rutinaDetalleDao()
    @Provides fun provideMedicamentoDetalleDao(db: LulaDatabase): MedicamentoDetalleDao = db.medicamentoDetalleDao()
    @Provides fun provideCitaDetalleDao(db: LulaDatabase): CitaDetalleDao = db.citaDetalleDao()
    @Provides fun provideFechaImportanteDetalleDao(db: LulaDatabase): FechaImportanteDetalleDao = db.fechaImportanteDetalleDao()
    @Provides fun provideRegistroActividadDao(db: LulaDatabase): RegistroActividadDao = db.registroActividadDao()
    @Provides fun provideMetaDao(db: LulaDatabase): MetaDao = db.metaDao()
    @Provides fun provideFinanzasDao(db: LulaDatabase): FinanzasDao = db.finanzasDao()
    @Provides fun provideEntradaDiarioDao(db: LulaDatabase): EntradaDiarioDao = db.entradaDiarioDao()
    @Provides fun provideRegistroDiarioDao(db: LulaDatabase): RegistroDiarioDao = db.registroDiarioDao()
    @Provides fun provideRegistroSemanalDao(db: LulaDatabase): RegistroSemanalDao = db.registroSemanalDao()
    @Provides fun provideRetoFamiliarDao(db: LulaDatabase): RetoFamiliarDao = db.retoFamiliarDao()
    @Provides fun provideSolicitudCompartirDao(db: LulaDatabase): SolicitudCompartirDao = db.solicitudCompartirDao()
    @Provides fun provideHistorialCambiosDao(db: LulaDatabase): HistorialCambiosDao = db.historialCambiosDao()
    @Provides fun provideListaDao(db: LulaDatabase): ListaDao = db.listaDao()
    @Provides fun provideListaItemDao(db: LulaDatabase): ListaItemDao = db.listaItemDao()
    @Provides fun provideListaEjecucionDao(db: LulaDatabase): ListaEjecucionDao = db.listaEjecucionDao()
    @Provides fun provideTomaMedicamentoDao(db: LulaDatabase): TomaMedicamentoDao = db.tomaMedicamentoDao()
    @Provides fun provideNotaDao(db: LulaDatabase): NotaDao = db.notaDao()
    @Provides fun providePropositoPersonalDao(db: LulaDatabase): PropositoPersonalDao = db.propositoPersonalDao()
    @Provides fun provideConexionDao(db: LulaDatabase): ConexionDao = db.conexionDao()
    @Provides fun provideSesionCitaDao(db: LulaDatabase): SesionCitaDao = db.sesionCitaDao()
}
