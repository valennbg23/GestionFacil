package com.valentin.gestionfacil.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valentin.gestionfacil.data.db.dao.MovimientoConCategoria
import com.valentin.gestionfacil.data.db.dao.PresupuestoConCategoria
import com.valentin.gestionfacil.data.db.dao.RecurrenteConCategoria
import com.valentin.gestionfacil.data.db.dao.TotalPorCategoria
import com.valentin.gestionfacil.data.db.dao.TotalPorMes
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import com.valentin.gestionfacil.data.repository.FinanzasRepository
import com.valentin.gestionfacil.utils.AsistenteConsejos
import com.valentin.gestionfacil.utils.Consejo
import com.valentin.gestionfacil.utils.EstadisticasRacha
import com.valentin.gestionfacil.utils.RachaCalculator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.YearMonth

data class DashboardUiState(
    val anio: Int,
    val mes: Int,
    val ingresos: Double = 0.0,
    val gastos: Double = 0.0,
    val gastosPorCategoria: List<TotalPorCategoria> = emptyList(),
    val ultimosMovimientos: List<MovimientoConCategoria> = emptyList(),
    val totalesPorMes: List<TotalPorMes> = emptyList(),
    val racha: EstadisticasRacha = EstadisticasRacha(0, 0, 0, ""),
    val consejos: List<Consejo> = emptyList()
) {
    val saldo: Double get() = ingresos - gastos
    val esMesActual: Boolean get() {
        val hoy = LocalDate.now()
        return anio == hoy.year && mes == hoy.monthValue
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModel(
    private val repository: FinanzasRepository
) : ViewModel() {

    private val hoy = LocalDate.now()

    private val _mesSeleccionado = MutableStateFlow(YearMonth.of(hoy.year, hoy.monthValue))

    val uiState: StateFlow<DashboardUiState> = _mesSeleccionado.flatMapLatest { ym ->
        val anioStr = ym.year.toString()
        val mesStr = "%02d".format(ym.monthValue)
        val desdeFecha = ym.minusMonths(5).atDay(1).toString()

        combine(
            repository.observarTotalMes(TipoMovimiento.INGRESO, anioStr, mesStr),
            repository.observarTotalMes(TipoMovimiento.GASTO, anioStr, mesStr),
            repository.observarGastosPorCategoria(anioStr, mesStr),
            repository.observarUltimosMovimientos(limite = 5),
            repository.observarTotalesPorMes(desdeFecha),
            repository.observarTodosLosMovimientos(),
            // Para los consejos: presupuestos del mes actual y movimientos recurrentes
            repository.observarPresupuestosDelMes(hoy.monthValue, hoy.year),
            repository.observarRecurrentes()
        ) { values ->
            @Suppress("UNCHECKED_CAST")
            val ingresos = values[0] as Double
            @Suppress("UNCHECKED_CAST")
            val gastos = values[1] as Double
            @Suppress("UNCHECKED_CAST")
            val porCategoria = values[2] as List<TotalPorCategoria>
            @Suppress("UNCHECKED_CAST")
            val ultimos = values[3] as List<MovimientoConCategoria>
            @Suppress("UNCHECKED_CAST")
            val porMes = values[4] as List<TotalPorMes>
            @Suppress("UNCHECKED_CAST")
            val todosMovimientos = values[5] as List<MovimientoConCategoria>
            @Suppress("UNCHECKED_CAST")
            val presupuestos = values[6] as List<PresupuestoConCategoria>
            @Suppress("UNCHECKED_CAST")
            val recurrentes = values[7] as List<RecurrenteConCategoria>

            val racha = RachaCalculator.calcular(todosMovimientos)

            // Los consejos solo tienen sentido para el mes en curso
            val esMesActual = ym == YearMonth.from(LocalDate.now())
            val consejos = if (esMesActual) {
                AsistenteConsejos.generar(
                    movimientos = todosMovimientos,
                    presupuestos = presupuestos,
                    recurrentes = recurrentes.map { it.recurrente }
                )
            } else {
                emptyList()
            }

            DashboardUiState(
                anio = ym.year,
                mes = ym.monthValue,
                ingresos = ingresos,
                gastos = gastos,
                gastosPorCategoria = porCategoria,
                ultimosMovimientos = ultimos,
                totalesPorMes = porMes,
                racha = racha,
                consejos = consejos
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState(anio = hoy.year, mes = hoy.monthValue)
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
}