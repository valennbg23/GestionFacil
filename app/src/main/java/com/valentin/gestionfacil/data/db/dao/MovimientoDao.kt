package com.valentin.gestionfacil.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.valentin.gestionfacil.data.entity.Movimiento
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import kotlinx.coroutines.flow.Flow

/**
 * Proyección que une un movimiento con los datos de su categoría.
 * Se usa en el historial y el dashboard para evitar joins manuales en el código.
 */
data class MovimientoConCategoria(
    val id: Long,
    val importe: Double,
    val tipo: TipoMovimiento,
    val categoriaId: Long,
    val fecha: String, // ISO (yyyy-MM-dd) — se parsea en el ViewModel
    val descripcion: String,
    val categoriaNombre: String,
    val categoriaIcono: String,
    val categoriaColor: String
)

/**
 * Proyección para la suma de gastos agrupada por categoría (gráfica de tarta).
 */
data class TotalPorCategoria(
    val categoriaId: Long,
    val categoriaNombre: String,
    val categoriaColor: String,
    val categoriaIcono: String,
    val total: Double
)

/**
 * Proyección para totales agrupados por mes y tipo (gráfica de evolución).
 */
data class TotalPorMes(
    val periodo: String,   // formato yyyy-MM
    val tipo: TipoMovimiento,
    val total: Double
)

@Dao
interface MovimientoDao {

    @Insert
    suspend fun insertar(movimiento: Movimiento): Long

    @Update
    suspend fun actualizar(movimiento: Movimiento)

    @Delete
    suspend fun eliminar(movimiento: Movimiento)

    @Query("SELECT * FROM movimientos WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Movimiento?

    /** Suma del importe para un tipo y un mes concreto (para balance mensual). */
    @Query("""
        SELECT COALESCE(SUM(importe), 0)
        FROM movimientos
        WHERE tipo = :tipo
          AND strftime('%Y', fecha) = :anio
          AND strftime('%m', fecha) = :mes
    """)
    fun observarTotalPorTipoYMes(tipo: TipoMovimiento, anio: String, mes: String): Flow<Double>

    /** Últimos N movimientos con su categoría (para el dashboard). */
    @Query("""
        SELECT m.id, m.importe, m.tipo, m.categoriaId, m.fecha, m.descripcion,
               c.nombre AS categoriaNombre, c.icono AS categoriaIcono, c.color AS categoriaColor
        FROM movimientos m
        INNER JOIN categorias c ON c.id = m.categoriaId
        ORDER BY m.fecha DESC, m.id DESC
        LIMIT :limite
    """)
    fun observarUltimos(limite: Int): Flow<List<MovimientoConCategoria>>

    /** Todos los movimientos con su categoría (para el historial). */
    @Query("""
        SELECT m.id, m.importe, m.tipo, m.categoriaId, m.fecha, m.descripcion,
               c.nombre AS categoriaNombre, c.icono AS categoriaIcono, c.color AS categoriaColor
        FROM movimientos m
        INNER JOIN categorias c ON c.id = m.categoriaId
        ORDER BY m.fecha DESC, m.id DESC
    """)
    fun observarTodos(): Flow<List<MovimientoConCategoria>>

    /** Totales por categoría filtrados por tipo y mes (para la gráfica de tarta). */
    @Query("""
        SELECT c.id AS categoriaId, c.nombre AS categoriaNombre,
               c.color AS categoriaColor, c.icono AS categoriaIcono,
               SUM(m.importe) AS total
        FROM movimientos m
        INNER JOIN categorias c ON c.id = m.categoriaId
        WHERE m.tipo = :tipo
          AND strftime('%Y', m.fecha) = :anio
          AND strftime('%m', m.fecha) = :mes
        GROUP BY c.id
        ORDER BY total DESC
    """)
    fun observarTotalesPorCategoria(
        tipo: TipoMovimiento, anio: String, mes: String
    ): Flow<List<TotalPorCategoria>>

    /** Suma de gastos de una categoría en un mes concreto (para presupuestos). */
    @Query("""
        SELECT COALESCE(SUM(importe), 0)
        FROM movimientos
        WHERE categoriaId = :categoriaId
          AND tipo = 'GASTO'
          AND strftime('%Y', fecha) = :anio
          AND strftime('%m', fecha) = :mes
    """)
    fun observarGastoCategoriaMes(categoriaId: Long, anio: String, mes: String): Flow<Double>

    /** Totales agrupados por mes y tipo, últimos N meses. Útil para la gráfica de línea. */
    @Query("""
        SELECT strftime('%Y-%m', fecha) AS periodo,
               tipo,
               COALESCE(SUM(importe), 0) AS total
        FROM movimientos
        WHERE fecha >= :desdeFecha
        GROUP BY periodo, tipo
        ORDER BY periodo ASC
    """)
    fun observarTotalesPorMes(desdeFecha: String): Flow<List<TotalPorMes>>
}