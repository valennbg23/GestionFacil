package com.valentin.gestionfacil.ui.metas

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valentin.gestionfacil.data.entity.Meta
import com.valentin.gestionfacil.data.entity.MovimientoMeta
import com.valentin.gestionfacil.data.entity.TipoMovimientoMeta
import com.valentin.gestionfacil.data.repository.FinanzasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MetasUiState(
    val mensaje: String? = null,
    val error: String? = null
)

class MetasViewModel(
    private val repository: FinanzasRepository
) : ViewModel() {

    val metas: StateFlow<List<Meta>> = repository.observarMetas()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _ui = MutableStateFlow(MetasUiState())
    val ui: StateFlow<MetasUiState> = _ui.asStateFlow()

    fun crearMeta(
        nombre: String,
        objetivo: Double,
        icono: String,
        color: String,
        fechaLimite: LocalDate?
    ) {
        viewModelScope.launch {
            try {
                repository.insertarMeta(
                    Meta(
                        nombre = nombre,
                        icono = icono,
                        color = color,
                        objetivo = objetivo,
                        ahorrado = 0.0,
                        fechaLimite = fechaLimite
                    )
                )
                _ui.update { it.copy(mensaje = "Meta creada correctamente") }
            } catch (e: Exception) {
                _ui.update { it.copy(error = "No se pudo crear: ${e.message}") }
            }
        }
    }

    fun actualizarMeta(meta: Meta) {
        viewModelScope.launch {
            repository.actualizarMeta(meta)
            _ui.update { it.copy(mensaje = "Meta actualizada") }
        }
    }

    fun eliminarMeta(meta: Meta) {
        viewModelScope.launch {
            repository.eliminarMeta(meta)
            _ui.update { it.copy(mensaje = "Meta eliminada") }
        }
    }

    /** Aporta dinero a la meta. */
    fun aportar(meta: Meta, importe: Double, nota: String = "") {
        if (importe <= 0) return
        viewModelScope.launch {
            try {
                repository.insertarMovimientoMeta(
                    MovimientoMeta(
                        metaId = meta.id,
                        importe = importe,
                        tipo = TipoMovimientoMeta.APORTE,
                        fecha = LocalDate.now(),
                        nota = nota
                    )
                )
                repository.actualizarMeta(meta.copy(ahorrado = meta.ahorrado + importe))
                _ui.update { it.copy(mensaje = "Has aportado ${formatoSimple(importe)} a \"${meta.nombre}\"") }
            } catch (e: Exception) {
                _ui.update { it.copy(error = "Error al aportar: ${e.message}") }
            }
        }
    }

    /** Retira dinero de la meta. */
    fun retirar(meta: Meta, importe: Double, nota: String = "") {
        if (importe <= 0) return
        if (importe > meta.ahorrado) {
            _ui.update { it.copy(error = "No puedes retirar más de lo ahorrado") }
            return
        }
        viewModelScope.launch {
            try {
                repository.insertarMovimientoMeta(
                    MovimientoMeta(
                        metaId = meta.id,
                        importe = importe,
                        tipo = TipoMovimientoMeta.RETIRO,
                        fecha = LocalDate.now(),
                        nota = nota
                    )
                )
                repository.actualizarMeta(meta.copy(ahorrado = meta.ahorrado - importe))
                _ui.update { it.copy(mensaje = "Has retirado ${formatoSimple(importe)} de \"${meta.nombre}\"") }
            } catch (e: Exception) {
                _ui.update { it.copy(error = "Error al retirar: ${e.message}") }
            }
        }
    }

    fun limpiarMensaje() = _ui.update { it.copy(mensaje = null, error = null) }

    private fun formatoSimple(d: Double): String = "%.2f €".format(d)
}