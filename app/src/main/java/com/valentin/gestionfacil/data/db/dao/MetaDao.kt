package com.valentin.gestionfacil.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.valentin.gestionfacil.data.entity.Meta
import com.valentin.gestionfacil.data.entity.MovimientoMeta
import kotlinx.coroutines.flow.Flow

@Dao
interface MetaDao {

    // ── Metas ───────────────────────────────────────────────

    @Query("SELECT * FROM metas ORDER BY id DESC")
    fun observarTodas(): Flow<List<Meta>>

    @Query("SELECT * FROM metas WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Meta?

    @Insert
    suspend fun insertar(meta: Meta): Long

    @Update
    suspend fun actualizar(meta: Meta)

    @Delete
    suspend fun eliminar(meta: Meta)

    // ── Movimientos de meta ─────────────────────────────────

    @Insert
    suspend fun insertarMovimiento(mov: MovimientoMeta): Long

    @Delete
    suspend fun eliminarMovimiento(mov: MovimientoMeta)

    @Query("SELECT * FROM movimientos_meta WHERE metaId = :metaId ORDER BY fecha DESC, id DESC")
    fun observarMovimientosDe(metaId: Long): Flow<List<MovimientoMeta>>
}