package com.valentin.gestionfacil.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.valentin.gestionfacil.data.entity.Movimiento
import com.valentin.gestionfacil.data.entity.Recurrente
import kotlinx.coroutines.flow.Flow

/**
 * Proyección que une un recurrente con los datos de su categoría
 * (para la lista de la pantalla de recurrentes).
 */
data class RecurrenteConCategoria(
    @Embedded val recurrente: Recurrente,
    val categoriaNombre: String,
    val categoriaIcono: String,
    val categoriaColor: String
)

@Dao
abstract class RecurrenteDao {

    @Query("""
        SELECT r.*, c.nombre AS categoriaNombre, c.icono AS categoriaIcono, c.color AS categoriaColor
        FROM recurrentes r
        INNER JOIN categorias c ON c.id = r.categoriaId
        ORDER BY r.activo DESC, r.id DESC
    """)
    abstract fun observarTodos(): Flow<List<RecurrenteConCategoria>>

    @Query("SELECT * FROM recurrentes WHERE activo = 1")
    abstract suspend fun obtenerActivos(): List<Recurrente>

    @Insert
    abstract suspend fun insertar(recurrente: Recurrente): Long

    @Update
    abstract suspend fun actualizar(recurrente: Recurrente)

    @Delete
    abstract suspend fun eliminar(recurrente: Recurrente)

    @Insert
    abstract suspend fun insertarMovimiento(movimiento: Movimiento): Long

    @Insert
    abstract suspend fun insertarMovimientos(movimientos: List<Movimiento>)

    /** Crea el primer movimiento y la regla de repetición a la vez (todo o nada). */
    @Transaction
    open suspend fun crearConPrimerMovimiento(movimiento: Movimiento, recurrente: Recurrente) {
        insertarMovimiento(movimiento)
        insertar(recurrente)
    }

    /**
     * Guarda los movimientos generados y apunta hasta qué fecha se han creado.
     * Va en una transacción para que nunca se dupliquen ni se pierdan.
     */
    @Transaction
    open suspend fun registrarGeneracion(recurrenteActualizado: Recurrente, movimientos: List<Movimiento>) {
        insertarMovimientos(movimientos)
        actualizar(recurrenteActualizado)
    }
}