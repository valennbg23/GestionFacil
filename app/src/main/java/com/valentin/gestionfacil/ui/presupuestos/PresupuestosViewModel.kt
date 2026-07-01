package com.valentin.gestionfacil.ui.presupuestos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valentin.gestionfacil.data.db.dao.PresupuestoConCategoria
import com.valentin.gestionfacil.data.entity.Categoria
import com.valentin.gestionfacil.data.entity.Presupuesto
import com.valentin.gestionfacil.data.repository.FinanzasRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class NivelAlerta { OK, CERCA, SUPERADO }

data class ItemPresupuesto(
    val presupuesto: PresupuestoConCategoria,
    val gastado: Double
) {
    val porcentaje: Float get() =
        if (presupuesto.importeMaximo > 0)
            (gastado / presupuesto.importeMaximo).toFloat().coerceAtMost(2f)
        else 0f

    val nivel: NivelAlerta get() = when {
        porcentaje >= 1f   -> NivelAlerta.SUPERADO
        porcentaje >= 0.8f -> NivelAlerta.CERCA
        else               -> NivelAlerta.OK
    }
}

data class PresupuestosUiState(
    val anio: Int,
    val mes: Int,
    val items: List<ItemPresupuesto> = emptyList(),
    val categorias: List<Categoria> = emptyList()
) {
    val esMesActual: Boolean get() {
        val hoy = LocalDate.now()
        return anio == hoy.year && mes == hoy.monthValue
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class PresupuestosViewModel(
    private val repository: FinanzasRepository
) : ViewModel() {

    private val hoy = LocalDate.now()

    private val _mesSeleccionado = MutableStateFlow(YearMonth.of(hoy.year, hoy.monthValue))

    val uiState: StateFlow<PresupuestosUiState> = _mesSeleccionado.flatMapLatest { ym ->
        val anio = ym.year
        val mes = ym.monthValue
        val anioStr = anio.toString()
        val mesStr = "%02d".format(mes)

        combine(
            repository.observarPresupuestosDelMes(mes, anio),
            repository.observarCategorias()
        ) { presupuestos, categorias ->
            Pair(presupuestos, categorias)
        }.flatMapLatest { (presupuestos, categorias) ->
            if (presupuestos.isEmpty()) {
                flowOf(
                    PresupuestosUiState(
                        anio = anio, mes = mes,
                        items = emptyList(), categorias = categorias
                    )
                )
            } else {
                val gastosFlows = presupuestos.map { p ->
                    repository.observarGastoCategoriaMes(p.categoriaId, anioStr, mesStr)
                        .map { gasto -> p to gasto }
                }
                combine(gastosFlows) { array ->
                    val items = array.map { (p, gasto) -> ItemPresupuesto(p, gasto) }
                    PresupuestosUiState(
                        anio = anio, mes = mes,
                        items = items, categorias = categorias
                    )
                }
            }
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        PresupuestosUiState(anio = hoy.year, mes = hoy.monthValue)
    )

    fun mesAnterior() {
        _mesSeleccionado.update { it.minusMonths(1) }
    }

    fun mesSiguiente() {
        val actual = YearMonth.of(hoy.year, hoy.monthValue)
        _mesSeleccionado.update { ym ->
            if (ym.isBefore(actual)) ym.plusMonths(1) else ym
        }
    }

    fun irAMesActual() {
        _mesSeleccionado.value = YearMonth.of(hoy.year, hoy.monthValue)
    }

    fun guardarPresupuesto(categoriaId: Long, importeMaximo: Double) {
        viewModelScope.launch {
            val ym = _mesSeleccionado.value
            val existente = repository.obtenerPresupuesto(categoriaId, ym.monthValue, ym.year)
            val p = Presupuesto(
                id = existente?.id ?: 0L,
                categoriaId = categoriaId,
                importeMaximo = importeMaximo,
                mes = ym.monthValue,
                anio = ym.year
            )
            repository.guardarPresupuesto(p)
        }
    }

    fun eliminarPresupuesto(categoriaId: Long) {
        viewModelScope.launch {
            val ym = _mesSeleccionado.value
            val existente = repository.obtenerPresupuesto(categoriaId, ym.monthValue, ym.year) ?: return@launch
            repository.eliminarPresupuesto(existente)
        }
    }
}