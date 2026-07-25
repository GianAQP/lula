package com.aqpseller.lulaapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
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
import com.aqpseller.lulaapp.data.local.entity.ActividadEntity
import com.aqpseller.lulaapp.data.local.entity.AreaDeVidaEntity
import com.aqpseller.lulaapp.data.local.entity.CitaDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.EntradaDiarioEntity
import com.aqpseller.lulaapp.data.local.entity.EspacioEntity
import com.aqpseller.lulaapp.data.local.entity.EspacioMiembroEntity
import com.aqpseller.lulaapp.data.local.entity.FechaImportanteDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.FinanzasEntity
import com.aqpseller.lulaapp.data.local.entity.HabitoDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.HistorialCambiosEntity
import com.aqpseller.lulaapp.data.local.entity.MedicamentoDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.MetaActividadCrossRef
import com.aqpseller.lulaapp.data.local.entity.MetaEntity
import com.aqpseller.lulaapp.data.local.entity.RegistroDiarioEntity
import com.aqpseller.lulaapp.data.local.entity.RegistroSemanalEntity
import com.aqpseller.lulaapp.data.local.entity.RetoFamiliarEntity
import com.aqpseller.lulaapp.data.local.entity.RetoFamiliarParticipanteEntity
import com.aqpseller.lulaapp.data.local.entity.RutinaDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.SolicitudCompartirEntity
import com.aqpseller.lulaapp.data.local.entity.TareaDetalleEntity
import com.aqpseller.lulaapp.data.local.entity.UsuarioEntity

@Database(
    entities = [
        UsuarioEntity::class,
        EspacioEntity::class,
        EspacioMiembroEntity::class,
        AreaDeVidaEntity::class,
        ActividadEntity::class,
        HabitoDetalleEntity::class,
        TareaDetalleEntity::class,
        RutinaDetalleEntity::class,
        MedicamentoDetalleEntity::class,
        CitaDetalleEntity::class,
        FechaImportanteDetalleEntity::class,
        MetaEntity::class,
        MetaActividadCrossRef::class,
        FinanzasEntity::class,
        EntradaDiarioEntity::class,
        RegistroDiarioEntity::class,
        RegistroSemanalEntity::class,
        RetoFamiliarEntity::class,
        RetoFamiliarParticipanteEntity::class,
        SolicitudCompartirEntity::class,
        HistorialCambiosEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class LulaDatabase : RoomDatabase() {

    abstract fun usuarioDao(): UsuarioDao
    abstract fun espacioDao(): EspacioDao
    abstract fun espacioMiembroDao(): EspacioMiembroDao
    abstract fun areaDeVidaDao(): AreaDeVidaDao
    abstract fun actividadDao(): ActividadDao
    abstract fun habitoDetalleDao(): HabitoDetalleDao
    abstract fun tareaDetalleDao(): TareaDetalleDao
    abstract fun rutinaDetalleDao(): RutinaDetalleDao
    abstract fun medicamentoDetalleDao(): MedicamentoDetalleDao
    abstract fun citaDetalleDao(): CitaDetalleDao
    abstract fun fechaImportanteDetalleDao(): FechaImportanteDetalleDao
    abstract fun metaDao(): MetaDao
    abstract fun finanzasDao(): FinanzasDao
    abstract fun entradaDiarioDao(): EntradaDiarioDao
    abstract fun registroDiarioDao(): RegistroDiarioDao
    abstract fun registroSemanalDao(): RegistroSemanalDao
    abstract fun retoFamiliarDao(): RetoFamiliarDao
    abstract fun solicitudCompartirDao(): SolicitudCompartirDao
    abstract fun historialCambiosDao(): HistorialCambiosDao

    companion object {
        const val NOMBRE_BD = "lula.db"
    }
}
