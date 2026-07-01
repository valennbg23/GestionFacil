package com.valentin.gestionfacil.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.valentin.gestionfacil.data.entity.Presupuesto
import kotlinx.coroutines.flow.Flow

/**
 * Proyección que une un presupuesto con los datos de su categoría.
 */
data class PresupuestoConCategoria(
    val id: Long,
    val categoriaId: Long,
    val importeMaximo: Double,
    val mes: Int,
    val anio: Int,
    val categoriaNombre: String,
    val categoriaIcono: String,
    val categoriaColor: String
)

@Dao
interface PresupuestoDao {

    /** Guarda o reemplaza un presupuesto (un único presupuesto por categoría/mes/año). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarOReemplazar(presupuesto: Presupuesto): Long

    @Update
    suspend fun actualizar(presupuesto: Presupuesto)

    @Delete
    suspend fun eliminar(presupuesto: Presupuesto)

    @Query("SELECT * FROM presupuestos WHERE categoriaId = :categoriaId AND mes = :mes AND anio = :anio LIMIT 1")
    suspend fun obtenerPorCategoriaYMes(categoriaId: Long, mes: Int, anio: Int): Presupuesto?

    @Query("""
        SELECT p.id, p.categoriaId, p.importeMaximo, p.mes, p.anio,
               c.nombre AS categoriaNombre, c.icono AS categoriaIcono, c.color AS categoriaColor
        FROM presupuestos p
        INNER JOIN categorias c ON c.id = p.categoriaId
        WHERE p.mes = :mes AND p.anio = :anio
        ORDER BY c.nombre ASC
    """)
    fun observarPresupuestosDelMes(mes: Int, anio: Int): Flow<List<PresupuestoConCategoria>>
}