package com.valentin.gestionfacil.ui.addmovimiento

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valentin.gestionfacil.data.entity.Categoria
import com.valentin.gestionfacil.data.entity.Movimiento
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import com.valentin.gestionfacil.data.repository.FinanzasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class AddMovimientoUiState(
    val esEdicion: Boolean = false,
    val tipo: TipoMovimiento = TipoMovimiento.GASTO,
    val importeStr: String = "",
    val categoriaId: Long? = null,
    val fecha: LocalDate = LocalDate.now(),
    val descripcion: String = "",
    val categorias: List<Categoria> = emptyList(),
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val error: String? = null
) {
    val importe: Double get() = importeStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val puedeGuardar: Boolean get() = importe > 0.0 && categoriaId != null && !guardando
}

class AddMovimientoViewModel(
    private val repository: FinanzasRepository
) : ViewModel() {

    private val _ui = MutableStateFlow(AddMovimientoUiState())
    val ui: StateFlow<AddMovimientoUiState> = _ui.asStateFlow()

    private var movimientoEditandoId: Long? = null

    init {
        viewModelScope.launch {
            repository.observarCategorias().collect { cats ->
                _ui.update { it.copy(categorias = cats) }
            }
        }
    }

    fun cargarParaEditar(id: Long) {
        viewModelScope.launch {
            val mov = repository.obtenerMovimiento(id) ?: return@launch
            movimientoEditandoId = mov.id
            _ui.update {
                it.copy(
                    esEdicion = true,
                    tipo = mov.tipo,
                    importeStr = mov.importe.toString(),
                    categoriaId = mov.categoriaId,
                    fecha = mov.fecha,
                    descripcion = mov.descripcion
                )
            }
        }
    }

    fun setTipo(tipo: TipoMovimiento) = _ui.update { it.copy(tipo = tipo) }
    fun setImporte(texto: String) {
        val limpio = texto.filter { it.isDigit() || it == '.' || it == ',' }
        _ui.update { it.copy(importeStr = limpio) }
    }
    fun setCategoria(id: Long) = _ui.update { it.copy(categoriaId = id) }
    fun setFecha(fecha: LocalDate) = _ui.update { it.copy(fecha = fecha) }
    fun setDescripcion(texto: String) = _ui.update { it.copy(descripcion = texto) }

    fun guardar() {
        val estado = _ui.value
        if (!estado.puedeGuardar) return

        _ui.update { it.copy(guardando = true, error = null) }

        viewModelScope.launch {
            try {
                val mov = Movimiento(
                    id = movimientoEditandoId ?: 0L,
                    importe = estado.importe,
                    tipo = estado.tipo,
                    categoriaId = estado.categoriaId!!,
                    fecha = estado.fecha,
                    descripcion = estado.descripcion.trim()
                )
                if (estado.esEdicion) repository.actualizarMovimiento(mov)
                else repository.insertarMovimiento(mov)

                _ui.update { it.copy(guardando = false, guardado = true) }
            } catch (e: Exception) {
                _ui.update { it.copy(guardando = false, error = e.message ?: "Error al guardar") }
            }
        }
    }

    fun eliminar() {
        val id = movimientoEditandoId ?: return
        viewModelScope.launch {
            val mov = repository.obtenerMovimiento(id) ?: return@launch
            repository.eliminarMovimiento(mov)
            _ui.update { it.copy(guardado = true) }
        }
    }
}