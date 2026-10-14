package com.valentin.gestionfacil.data.repository

import com.valentin.gestionfacil.data.db.dao.CategoriaDao
import com.valentin.gestionfacil.data.db.dao.MetaDao
import com.valentin.gestionfacil.data.db.dao.MovimientoConCategoria
import com.valentin.gestionfacil.data.db.dao.MovimientoDao
import com.valentin.gestionfacil.data.db.dao.PresupuestoConCategoria
import com.valentin.gestionfacil.data.db.dao.PresupuestoDao
import com.valentin.gestionfacil.data.db.dao.RecurrenteConCategoria
import com.valentin.gestionfacil.data.db.dao.RecurrenteDao
import com.valentin.gestionfacil.data.db.dao.TotalPorCategoria
import com.valentin.gestionfacil.data.db.dao.TotalPorMes
import com.valentin.gestionfacil.data.entity.Categoria
import com.valentin.gestionfacil.data.entity.Frecuencia
import com.valentin.gestionfacil.data.entity.Meta
import com.valentin.gestionfacil.data.entity.Movimiento
import com.valentin.gestionfacil.data.entity.MovimientoMeta
import com.valentin.gestionfacil.data.entity.Presupuesto
import com.valentin.gestionfacil.data.entity.Recurrente
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import com.valentin.gestionfacil.utils.CalendarioRecurrente
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.LocalDate

/**
 * Fuente única de verdad para la capa de datos.
 */
class FinanzasRepository(
    private val categoriaDao: CategoriaDao,
    private val movimientoDao: MovimientoDao,
    private val presupuestoDao: PresupuestoDao,
    private val metaDao: MetaDao,
    private val recurrenteDao: RecurrenteDao
) {

    /** Evita que dos generaciones de recurrentes a la vez dupliquen movimientos. */
    private val mutexRecurrentes = Mutex()

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

    // ── Movimientos recurrentes ────────────────────────────────────────

    fun observarRecurrentes(): Flow<List<RecurrenteConCategoria>> = recurrenteDao.observarTodos()

    /**
     * Guarda [movimiento] como primer movimiento y crea la regla para que se
     * repita solo con la [frecuencia] elegida.
     */
    suspend fun crearMovimientoRecurrente(movimiento: Movimiento, frecuencia: Frecuencia) {
        val recurrente = Recurrente(
            importe = movimiento.importe,
            tipo = movimiento.tipo,
            categoriaId = movimiento.categoriaId,
            descripcion = movimiento.descripcion,
            frecuencia = frecuencia,
            fechaInicio = movimiento.fecha,
            ultimaFecha = movimiento.fecha
        )
        recurrenteDao.crearConPrimerMovimiento(movimiento, recurrente)
    }

    /**
     * Pausa o reanuda un recurrente.
     * Al reanudarlo NO se crean los movimientos de las fechas que pasaron
     * mientras estaba en pausa: se continúa desde la próxima fecha.
     */
    suspend fun cambiarActivoRecurrente(
        recurrente: Recurrente,
        activo: Boolean,
        hoy: LocalDate = LocalDate.now()
    ) {
        val ultimaFecha = if (activo) {
            CalendarioRecurrente.fechasPendientes(
                inicio = recurrente.fechaInicio,
                frecuencia = recurrente.frecuencia,
                despuesDe = recurrente.ultimaFecha,
                hasta = hoy
            ).lastOrNull() ?: recurrente.ultimaFecha
        } else {
            recurrente.ultimaFecha
        }
        recurrenteDao.actualizar(recurrente.copy(activo = activo, ultimaFecha = ultimaFecha))
    }

    /** Borra la regla de repetición. Los movimientos ya creados se quedan. */
    suspend fun eliminarRecurrente(recurrente: Recurrente) = recurrenteDao.eliminar(recurrente)

    /**
     * Crea los movimientos de los recurrentes activos cuya fecha ya ha llegado.
     * Se llama al abrir la app y al crear un recurrente nuevo.
     *
     * @return cuántos movimientos se han creado
     */
    suspend fun generarRecurrentesPendientes(hoy: LocalDate = LocalDate.now()): Int =
        mutexRecurrentes.withLock {
            var creados = 0
            recurrenteDao.obtenerActivos().forEach { rec ->
                val fechas = CalendarioRecurrente.fechasPendientes(
                    inicio = rec.fechaInicio,
                    frecuencia = rec.frecuencia,
                    despuesDe = rec.ultimaFecha,
                    hasta = hoy
                )
                if (fechas.isNotEmpty()) {
                    val movimientos = fechas.map { fecha ->
                        Movimiento(
                            importe = rec.importe,
                            tipo = rec.tipo,
                            categoriaId = rec.categoriaId,
                            fecha = fecha,
                            descripcion = rec.descripcion
                        )
                    }
                    recurrenteDao.registrarGeneracion(
                        recurrenteActualizado = rec.copy(ultimaFecha = fechas.last()),
                        movimientos = movimientos
                    )
                    creados += movimientos.size
                }
            }
            creados
        }
}