package com.valentin.gestionfacil.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.valentin.gestionfacil.data.entity.Categoria
import com.valentin.gestionfacil.data.entity.Meta
import com.valentin.gestionfacil.data.entity.Movimiento
import com.valentin.gestionfacil.data.entity.MovimientoMeta
import com.valentin.gestionfacil.data.entity.Presupuesto

/**
 * Todo el contenido de la base de datos de una vez.
 * Es lo que se guarda en el archivo de copia y lo que se restaura desde él.
 */
data class ContenidoBd(
    val categorias: List<Categoria>,
    val movimientos: List<Movimiento>,
    val presupuestos: List<Presupuesto>,
    val metas: List<Meta>,
    val movimientosMeta: List<MovimientoMeta>
)

/**
 * DAO exclusivo para la copia de seguridad.
 *
 * Lee todas las tablas dentro de una transacción (la foto es coherente) y
 * reemplaza todos los datos también en una sola transacción: si algo falla
 * a mitad, Room deshace todo y los datos actuales quedan intactos.
 */
@Dao
abstract class BackupDao {

    // ── Lectura completa ───────────────────────────────────────────────

    @Query("SELECT * FROM categorias ORDER BY id")
    abstract suspend fun obtenerCategorias(): List<Categoria>

    @Query("SELECT * FROM movimientos ORDER BY id")
    abstract suspend fun obtenerMovimientos(): List<Movimiento>

    @Query("SELECT * FROM presupuestos ORDER BY id")
    abstract suspend fun obtenerPresupuestos(): List<Presupuesto>

    @Query("SELECT * FROM metas ORDER BY id")
    abstract suspend fun obtenerMetas(): List<Meta>

    @Query("SELECT * FROM movimientos_meta ORDER BY id")
    abstract suspend fun obtenerMovimientosMeta(): List<MovimientoMeta>

    // ── Borrado (de hijas a padres, por las claves foráneas) ──────────

    @Query("DELETE FROM movimientos_meta")
    abstract suspend fun borrarMovimientosMeta()

    @Query("DELETE FROM metas")
    abstract suspend fun borrarMetas()

    @Query("DELETE FROM presupuestos")
    abstract suspend fun borrarPresupuestos()

    @Query("DELETE FROM movimientos")
    abstract suspend fun borrarMovimientos()

    @Query("DELETE FROM categorias")
    abstract suspend fun borrarCategorias()

    // ── Inserción conservando los id originales ────────────────────────

    @Insert
    abstract suspend fun insertarCategorias(lista: List<Categoria>)

    @Insert
    abstract suspend fun insertarMovimientos(lista: List<Movimiento>)

    @Insert
    abstract suspend fun insertarPresupuestos(lista: List<Presupuesto>)

    @Insert
    abstract suspend fun insertarMetas(lista: List<Meta>)

    @Insert
    abstract suspend fun insertarMovimientosMeta(lista: List<MovimientoMeta>)

    // ── Operaciones completas ──────────────────────────────────────────

    /** Lee todas las tablas en una única transacción. */
    @Transaction
    open suspend fun leerTodo(): ContenidoBd = ContenidoBd(
        categorias = obtenerCategorias(),
        movimientos = obtenerMovimientos(),
        presupuestos = obtenerPresupuestos(),
        metas = obtenerMetas(),
        movimientosMeta = obtenerMovimientosMeta()
    )

    /**
     * Borra todos los datos actuales e inserta los de la copia.
     * Todo o nada: si una inserción falla, no se borra nada.
     */
    @Transaction
    open suspend fun reemplazarTodo(contenido: ContenidoBd) {
        borrarMovimientosMeta()
        borrarMetas()
        borrarPresupuestos()
        borrarMovimientos()
        borrarCategorias()

        insertarCategorias(contenido.categorias)
        insertarMovimientos(contenido.movimientos)
        insertarPresupuestos(contenido.presupuestos)
        insertarMetas(contenido.metas)
        insertarMovimientosMeta(contenido.movimientosMeta)
    }
}