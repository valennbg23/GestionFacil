package com.valentin.gestionfacil.data.repository

import com.valentin.gestionfacil.data.db.dao.CategoriaDao
import com.valentin.gestionfacil.data.db.dao.MetaDao
import com.valentin.gestionfacil.data.db.dao.MovimientoConCategoria
import com.valentin.gestionfacil.data.db.dao.MovimientoDao
import com.valentin.gestionfacil.data.db.dao.PresupuestoConCategoria
import com.valentin.gestionfacil.data.db.dao.PresupuestoDao
import com.valentin.gestionfacil.data.db.dao.TotalPorCategoria
import com.valentin.gestionfacil.data.db.dao.TotalPorMes
import com.valentin.gestionfacil.data.entity.Categoria
import com.valentin.gestionfacil.data.entity.Meta
import com.valentin.gestionfacil.data.entity.Movimiento
import com.valentin.gestionfacil.data.entity.MovimientoMeta
import com.valentin.gestionfacil.data.entity.Presupuesto
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import kotlinx.coroutines.flow.Flow

/**
 * Fuente única de verdad para la capa de datos.
 */
class FinanzasRepository(
    private val categoriaDao: CategoriaDao,
    private val movimientoDao: MovimientoDao,
    private val presupuestoDao: PresupuestoDao,
    private val metaDao: MetaDao
) {

    // ── Categorías ─────────────────────────────────────────────────────

    fun observarCategorias(): Flow<List<Categoria>> = categoriaDao.observarTodas()
    suspend fun obtenerCategoria(id: Long): Categoria? = categoriaDao.obtenerPorId(id)
    suspend fun insertarCategoria(c: Categoria): Long = categoriaDao.insertar(c)
    suspend fun actualizarCategoria(c: Categoria) = categoriaDao.actualizar(c)
    suspend fun eliminarCategoria(c: Categoria) = categoriaDao.eliminar(c)
    suspend fun contarMovimientosDe(categoriaId: Long): Int =
        categoriaDao.contarMovimientosDeCategoria(categoriaId)

    // ── Movimientos ────────────────────────────────────────────────────

    suspend fun insertarMovimiento(m: Movimiento): Long = movimientoDao.insertar(m)
    suspend fun actualizarMovimiento(m: Movimiento) = movimientoDao.actualizar(m)
    suspend fun eliminarMovimiento(m: Movimiento) = movimientoDao.eliminar(m)
    suspend fun obtenerMovimiento(id: Long): Movimiento? = movimientoDao.obtenerPorId(id)

    fun observarTotalMes(tipo: TipoMovimiento, anio: String, mes: String): Flow<Double> =
        movimientoDao.observarTotalPorTipoYMes(tipo, anio, mes)

    fun observarUltimosMovimientos(limite: Int = 5): Flow<List<MovimientoConCategoria>> =
        movimientoDao.observarUltimos(limite)

    fun observarTodosLosMovimientos(): Flow<List<MovimientoConCategoria>> =
        movimientoDao.observarTodos()

    fun observarGastosPorCategoria(anio: String, mes: String): Flow<List<TotalPorCategoria>> =
        movimientoDao.observarTotalesPorCategoria(TipoMovimiento.GASTO, anio, mes)

    fun observarGastoCategoriaMes(categoriaId: Long, anio: String, mes: String): Flow<Double> =
        movimientoDao.observarGastoCategoriaMes(categoriaId, anio, mes)

    fun observarTotalesPorMes(desdeFecha: String): Flow<List<TotalPorMes>> =
        movimientoDao.observarTotalesPorMes(desdeFecha)

    // ── Presupuestos ───────────────────────────────────────────────────

    fun observarPresupuestosDelMes(mes: Int, anio: Int): Flow<List<PresupuestoConCategoria>> =
        presupuestoDao.observarPresupuestosDelMes(mes, anio)

    suspend fun guardarPresupuesto(p: Presupuesto): Long = presupuestoDao.insertarOReemplazar(p)
    suspend fun eliminarPresupuesto(p: Presupuesto) = presupuestoDao.eliminar(p)
    suspend fun obtenerPresupuesto(categoriaId: Long, mes: Int, anio: Int): Presupuesto? =
        presupuestoDao.obtenerPorCategoriaYMes(categoriaId, mes, anio)

    // ── Metas de ahorro ────────────────────────────────────────────────

    fun observarMetas(): Flow<List<Meta>> = metaDao.observarTodas()
    suspend fun obtenerMeta(id: Long): Meta? = metaDao.obtenerPorId(id)
    suspend fun insertarMeta(meta: Meta): Long = metaDao.insertar(meta)
    suspend fun actualizarMeta(meta: Meta) = metaDao.actualizar(meta)
    suspend fun eliminarMeta(meta: Meta) = metaDao.eliminar(meta)

    fun observarMovimientosDeMeta(metaId: Long): Flow<List<MovimientoMeta>> =
        metaDao.observarMovimientosDe(metaId)

    suspend fun insertarMovimientoMeta(mov: MovimientoMeta): Long =
        metaDao.insertarMovimiento(mov)

    suspend fun eliminarMovimientoMeta(mov: MovimientoMeta) =
        metaDao.eliminarMovimiento(mov)
}