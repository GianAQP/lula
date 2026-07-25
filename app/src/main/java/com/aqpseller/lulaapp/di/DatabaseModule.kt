package com.aqpseller.lulaapp.di

import android.content.Context
import androidx.room.Room
import com.aqpseller.lulaapp.core.database.LulaDatabase
import com.aqpseller.lulaapp.data.local.dao.AreaDeVidaDao
import com.aqpseller.lulaapp.data.local.dao.ActividadDao
import com.aqpseller.lulaapp.data.local.dao.CitaDetalleDao
import com.aqpseller.lulaapp.data.local.dao.EntradaDiarioDao
import com.aqpseller.lulaapp.data.local.dao.EspacioDao
import com.aqpseller.lulaapp.data.local.dao.EspacioMiembroDao
import com.aqpseller.lulaapp.data.local.dao.FechaImportanteDetalleDao
import com.aqpseller.lulaapp.data.local.dao.FinanzasDao
import com.aqpseller.lulaapp.data.local.dao.HabitoDetalleDao
import com.aqpseller.lulaapp.data.local.dao.HistorialCambiosDao
import com.aqpseller.lulaapp.data.local.dao.MedicamentoDetalleDao
import com.aqpseller.lulaapp.data.local.dao.MetaDao
import com.aqpseller.lulaapp.data.local.dao.RegistroDiarioDao
import com.aqpseller.lulaapp.data.local.dao.RegistroSemanalDao
import com.aqpseller.lulaapp.data.local.dao.RetoFamiliarDao
import com.aqpseller.lulaapp.data.local.dao.RutinaDetalleDao
import com.aqpseller.lulaapp.data.local.dao.SolicitudCompartirDao
import com.aqpseller.lulaapp.data.local.dao.TareaDetalleDao
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
        Room.databaseBuilder(context, LulaDatabase::class.java, LulaDatabase.NOMBRE_BD).build()

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
    @Provides fun provideMetaDao(db: LulaDatabase): MetaDao = db.metaDao()
    @Provides fun provideFinanzasDao(db: LulaDatabase): FinanzasDao = db.finanzasDao()
    @Provides fun provideEntradaDiarioDao(db: LulaDatabase): EntradaDiarioDao = db.entradaDiarioDao()
    @Provides fun provideRegistroDiarioDao(db: LulaDatabase): RegistroDiarioDao = db.registroDiarioDao()
    @Provides fun provideRegistroSemanalDao(db: LulaDatabase): RegistroSemanalDao = db.registroSemanalDao()
    @Provides fun provideRetoFamiliarDao(db: LulaDatabase): RetoFamiliarDao = db.retoFamiliarDao()
    @Provides fun provideSolicitudCompartirDao(db: LulaDatabase): SolicitudCompartirDao = db.solicitudCompartirDao()
    @Provides fun provideHistorialCambiosDao(db: LulaDatabase): HistorialCambiosDao = db.historialCambiosDao()
}
