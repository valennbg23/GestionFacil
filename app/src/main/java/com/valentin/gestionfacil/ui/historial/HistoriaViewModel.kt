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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.text.Normalizer
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

enum class FiltroTipo { TODOS, GASTOS, INGRESOS }

/** Cuánto se gastó un día comparado con la media del mes (para el puntito del calendario). */
enum class NivelDia { BAJO, MEDIO, ALTO, INGRESO }

data class FiltrosHistorial(
    val tipo: FiltroTipo = FiltroTipo.TODOS,
    val mes: YearMonth = YearMonth.now(),
    val dia: LocalDate? = null,
    val categoriaId: Long? = null,
    val busqueda: String = ""
) {
    /** true mientras el usuario tiene algo escrito en el buscador. */
    val buscando: Boolean get() = busqueda.isNotBlank()
}

data class HistorialUiState(
    val movimientos: List<MovimientoConCategoria> = emptyList(),
    val categorias: List<Categoria> = emptyList(),
    val filtros: FiltrosHistorial = FiltrosHistorial(),
    /** Día del mes (1..31) → nivel de gasto, para pintar el calendario. */
    val nivelesPorDia: Map<Int, NivelDia> = emptyMap()
) {
    val esMesActual: Boolean get() = filtros.mes == YearMonth.now()
}

class HistorialViewModel(
    private val repository: FinanzasRepository
) : ViewModel() {

    private val _filtros = MutableStateFlow(FiltrosHistorial())

    val uiState: StateFlow<HistorialUiState> = combine(
        repository.observarTodosLosMovimientos(),
        repository.observarCategorias(),
        _filtros
    ) { movs, cats, f ->
        val textoBuscado = normalizar(f.busqueda)

        val filtrados = movs.filter { mov ->
            val fecha = runCatching { LocalDate.parse(mov.fecha) }.getOrNull()
            val tipoOk = cumpleTipo(mov, f.tipo)
            val catOk = f.categoriaId == null || mov.categoriaId == f.categoriaId
            // Mientras se busca, se busca en todos los meses (no solo en el seleccionado)
            val fechaOk = when {
                f.buscando -> true
                fecha == null -> false
                f.dia != null -> fecha == f.dia
                else -> YearMonth.from(fecha) == f.mes
            }
            val textoOk = !f.buscando || coincide(mov, textoBuscado)
            tipoOk && catOk && fechaOk && textoOk
        }

        HistorialUiState(
            movimientos = filtrados,
            categorias = cats,
            filtros = f,
            nivelesPorDia = calcularNiveles(movs, f)
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HistorialUiState()
    )

    fun setTipo(t: FiltroTipo) = _filtros.update { it.copy(tipo = t) }
    fun setCategoria(id: Long?) = _filtros.update { it.copy(categoriaId = id) }
    fun setBusqueda(texto: String) = _filtros.update { it.copy(busqueda = texto) }

    /** Al cambiar de mes se quita el día seleccionado. */
    fun mesAnterior() = _filtros.update { it.copy(mes = it.mes.minusMonths(1), dia = null) }

    fun mesSiguiente() = _filtros.update {
        if (it.mes.isBefore(YearMonth.now())) it.copy(mes = it.mes.plusMonths(1), dia = null) else it
    }

    /** Toca un día: filtra por él. Tocar el mismo día otra vez vuelve a todo el mes. */
    fun seleccionarDia(dia: LocalDate) = _filtros.update {
        it.copy(dia = if (it.dia == dia) null else dia)
    }
}

// ── Lógica de filtros ──────────────────────────────────────────────────

private fun cumpleTipo(mov: MovimientoConCategoria, tipo: FiltroTipo): Boolean = when (tipo) {
    FiltroTipo.TODOS -> true
    FiltroTipo.GASTOS -> mov.tipo == TipoMovimiento.GASTO
    FiltroTipo.INGRESOS -> mov.tipo == TipoMovimiento.INGRESO
}

/**
 * Nivel de cada día del mes para el calendario:
 * - Con "Ingresos" seleccionado, marca los días en que hubo ingresos.
 * - Si no, compara el gasto de cada día con la media de los días con gasto:
 *   menos del 75% → bajo, hasta 1,5 veces → medio, más → alto.
 * Respeta el filtro de categoría, pero no el de día (el calendario siempre enseña el mes entero).
 */
private fun calcularNiveles(
    movs: List<MovimientoConCategoria>,
    f: FiltrosHistorial
): Map<Int, NivelDia> {
    val tipoBuscado =
        if (f.tipo == FiltroTipo.INGRESOS) TipoMovimiento.INGRESO else TipoMovimiento.GASTO

    val totalPorDia = movs
        .filter { it.tipo == tipoBuscado }
        .filter { f.categoriaId == null || it.categoriaId == f.categoriaId }
        .mapNotNull { mov ->
            val fecha = runCatching { LocalDate.parse(mov.fecha) }.getOrNull()
            if (fecha != null && YearMonth.from(fecha) == f.mes) fecha.dayOfMonth to mov.importe else null
        }
        .groupBy({ it.first }, { it.second })
        .mapValues { (_, importes) -> importes.sum() }
        .filterValues { it > 0 }

    if (totalPorDia.isEmpty()) return emptyMap()

    if (tipoBuscado == TipoMovimiento.INGRESO) {
        return totalPorDia.mapValues { NivelDia.INGRESO }
    }

    val media = totalPorDia.values.average()
    return totalPorDia.mapValues { (_, total) ->
        val ratio = total / media
        when {
            ratio < 0.75 -> NivelDia.BAJO
            ratio <= 1.5 -> NivelDia.MEDIO
            else -> NivelDia.ALTO
        }
    }
}

private val ES = Locale("es", "ES")

/**
 * Pasa el texto a minúsculas y sin tildes, para que "nomina" encuentre "Nómina"
 * y "CAFE" encuentre "café".
 */
private fun normalizar(texto: String): String =
    Normalizer.normalize(texto, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
        .lowercase(ES)
        .trim()

/** Busca el texto en la descripción, en la categoría y en el importe. */
private fun coincide(mov: MovimientoConCategoria, textoBuscado: String): Boolean {
    if (normalizar(mov.descripcion).contains(textoBuscado)) return true
    if (normalizar(mov.categoriaNombre).contains(textoBuscado)) return true

    // Importe: "12,5" o "12.5" encuentran 12,50 €
    val importe = String.format(ES, "%.2f", mov.importe)
    return importe.contains(textoBuscado.replace('.', ','))
}