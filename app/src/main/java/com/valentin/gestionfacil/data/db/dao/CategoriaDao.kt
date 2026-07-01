package com.valentin.gestionfacil.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.valentin.gestionfacil.data.entity.Categoria
import kotlinx.coroutines.flow.Flow

/**
 * DAO para la tabla de categorías.
 * Las consultas de lectura devuelven Flow para actualización reactiva de la UI.
 */
@Dao
interface CategoriaDao {

    @Query("SELECT * FROM categorias ORDER BY nombre ASC")
    fun observarTodas(): Flow<List<Categoria>>

    @Query("SELECT * FROM categorias WHERE id = :id")
    suspend fun obtenerPorId(id: Long): Categoria?

    @Query("SELECT COUNT(*) FROM categorias")
    suspend fun contar(): Int

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertar(categoria: Categoria): Long

    @Update
    suspend fun actualizar(categoria: Categoria)

    @Delete
    suspend fun eliminar(categoria: Categoria)

    /** Cuántos movimientos usan esta categoría — usado para impedir borrado si tiene datos. */
    @Query("SELECT COUNT(*) FROM movimientos WHERE categoriaId = :categoriaId")
    suspend fun contarMovimientosDeCategoria(categoriaId: Long): Int
}