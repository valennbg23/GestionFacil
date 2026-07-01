package com.valentin.gestionfacil.ui.historial

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valentin.gestionfacil.data.db.dao.MovimientoConCategoria
import com.valentin.gestionfacil.data.entity.Categoria
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import com.valentin.gestionfacil.data.repository.FinanzasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate

enum class FiltroTipo { TODOS, GASTOS, INGRESOS }

data class FiltrosHistorial(
    val tipo: FiltroTipo = FiltroTipo.TODOS,
    val mes: Int? = null,
    val anio: Int? = null,
    val categoriaId: Long? = null
)

data class HistorialUiState(
    val movimientos: List<MovimientoConCategoria> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val filtros: FiltrosHistorial = FiltrosHistorial()
)

class HistorialViewModel(
    private val repository: FinanzasRepository
) : ViewModel() {

    private val _filtros = MutableStateFlow(
        FiltrosHistorial(
            mes = LocalDate.now().monthValue,
            anio = LocalDate.now().year
        )
    )

    val uiState: StateFlow<HistorialUiState> = combine(
        repository.observarTodosLosMovimientos(),
        repository.observarCategorias(),
        _filtros
    ) { movs, cats, f ->
        val filtrados = movs.filter { mov ->
            val tipoOk = when (f.tipo) {
                FiltroTipo.TODOS -> true
                FiltroTipo.GASTOS -> mov.tipo == TipoMovimiento.GASTO
                FiltroTipo.INGRESOS -> mov.tipo == TipoMovimiento.INGRESO
            }
            val fecha = runCatching { LocalDate.parse(mov.fecha) }.getOrNull()
            val mesOk = f.mes == null || fecha?.monthValue == f.mes
            val anioOk = f.anio == null || fecha?.year == f.anio
            val catOk = f.categoriaId == null || mov.categoriaId == f.categoriaId
            tipoOk && mesOk && anioOk && catOk
        }
        HistorialUiState(movimientos = filtrados, categorias = cats, filtros = f)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HistorialUiState()
    )

    fun setTipo(t: FiltroTipo) = _filtros.update { it.copy(tipo = t) }
    fun setCategoria(id: Long?) = _filtros.update { it.copy(categoriaId = id) }
    fun setMes(mes: Int?) = _filtros.update { it.copy(mes = mes) }
}