package com.valentin.gestionfacil.ui.ajustes

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valentin.gestionfacil.data.backup.BackupException
import com.valentin.gestionfacil.data.backup.BackupManager
import com.valentin.gestionfacil.data.backup.ResumenBackup
import com.valentin.gestionfacil.data.db.dao.TotalPorCategoria
import com.valentin.gestionfacil.data.entity.Categoria
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import com.valentin.gestionfacil.data.repository.FinanzasRepository
import com.valentin.gestionfacil.utils.DatosResumenMes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AjustesUiState(
    val mensaje: String? = null,
    val error: String? = null,
    val procesandoCopia: Boolean = false
)

class AjustesViewModel(
    private val repository: FinanzasRepository,
    private val backupManager: BackupManager
) : ViewModel() {

    private val _ui = MutableStateFlow(AjustesUiState())
    val ui: StateFlow<AjustesUiState> = _ui.asStateFlow()

    val categorias: StateFlow<List<Categoria>> = repository.observarCategorias()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun insertarCategoria(nombre: String, color: String) {
        viewModelScope.launch {
            try {
                repository.insertarCategoria(
                    Categoria(
                        nombre = nombre,
                        icono = "Category",
                        color = color,
                        esPredefinida = false
                    )
                )
                _ui.update { it.copy(mensaje = "Categoría creada") }
            } catch (e: Exception) {
                _ui.update { it.copy(error = "No se pudo crear: ${e.message}") }
            }
        }
    }

    fun actualizarCategoria(cat: Categoria) {
        viewModelScope.launch {
            repository.actualizarCategoria(cat)
        }
    }

    fun eliminarCategoria(cat: Categoria) {
        viewModelScope.launch {
            val usada = repository.contarMovimientosDe(cat.id)
            if (usada > 0) {
                _ui.update { it.copy(error = "No se puede eliminar: tiene $usada movimientos") }
                return@launch
            }
            try {
                repository.eliminarCategoria(cat)
                _ui.update { it.copy(mensaje = "Categoría eliminada") }
            } catch (e: Exception) {
                _ui.update { it.copy(error = e.message ?: "Error al eliminar") }
            }
        }
    }

    fun limpiarMensaje() = _ui.update { it.copy(mensaje = null, error = null) }

    // ── Copia de seguridad ─────────────────────────────────────────────

    /** Guarda todos los datos en el archivo que ha elegido el usuario. */
    fun exportarCopia(uri: Uri) {
        if (_ui.value.procesandoCopia) return
        viewModelScope.launch {
            _ui.update { it.copy(procesandoCopia = true) }
            try {
                val resumen = backupManager.exportar(uri)
                _ui.update {
                    it.copy(
                        procesandoCopia = false,
                        mensaje = "Copia guardada: ${resumen.texto()}"
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        procesandoCopia = false,
                        error = mensajeDeError(e, "No se pudo guardar la copia")
                    )
                }
            }
        }
    }

    /** Sustituye todos los datos por los de la copia elegida. */
    fun restaurarCopia(uri: Uri) {
        if (_ui.value.procesandoCopia) return
        viewModelScope.launch {
            _ui.update { it.copy(procesandoCopia = true) }
            try {
                val resumen = backupManager.restaurar(uri)
                _ui.update {
                    it.copy(
                        procesandoCopia = false,
                        mensaje = "Copia restaurada: ${resumen.texto()}"
                    )
                }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        procesandoCopia = false,
                        error = mensajeDeError(e, "No se pudo restaurar la copia")
                    )
                }
            }
        }
    }

    private fun mensajeDeError(e: Exception, porDefecto: String): String =
        if (e is BackupException) e.message ?: porDefecto else porDefecto

    private fun ResumenBackup.texto(): String =
        "${plural(movimientos, "movimiento", "movimientos")}, " +
                "${plural(metas, "meta", "metas")} y " +
                plural(presupuestos, "presupuesto", "presupuestos")

    private fun plural(n: Int, singular: String, plural: String): String =
        "$n ${if (n == 1) singular else plural}"

    // ── PDF ────────────────────────────────────────────────────────────

    /** Obtiene los datos del mes actual para el PDF. */
    suspend fun obtenerDatosResumenActual(): DatosResumenMes {
        val hoy = LocalDate.now()
        val anio = hoy.year
        val mes = hoy.monthValue
        val anioStr = anio.toString()
        val mesStr = "%02d".format(mes)

        val ingresos = repository.observarTotalMes(TipoMovimiento.INGRESO, anioStr, mesStr).first()
        val gastos = repository.observarTotalMes(TipoMovimiento.GASTO, anioStr, mesStr).first()
        val totales: List<TotalPorCategoria> =
            repository.observarGastosPorCategoria(anioStr, mesStr).first()

        return DatosResumenMes(
            anio = anio,
            mes = mes,
            ingresos = ingresos,
            gastos = gastos,
            totalesPorCategoria = totales
        )
    }
}