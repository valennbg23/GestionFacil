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
import java.util.Locale

enum class FiltroTipo { TODOS, GASTOS, INGRESOS }

data class FiltrosHistorial(
    val tipo: FiltroTipo = FiltroTipo.TODOS,
    val mes: Int? = null,
    val anio: Int? = null,
    val categoriaId: Long? = null,
    val busqueda: String = ""
) {
    /** true mientras el usuario tiene algo escrito en el buscador. */
    val buscando: Boolean get() = busqueda.isNotBlank()
}

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
        val textoBuscado = normalizar(f.busqueda)

        val filtrados = movs.filter { mov ->
            val tipoOk = when (f.tipo) {
                FiltroTipo.TODOS -> true
                FiltroTipo.GASTOS -> mov.tipo == TipoMovimiento.GASTO
                FiltroTipo.INGRESOS -> mov.tipo == TipoMovimiento.INGRESO
            }
            val fecha = runCatching { LocalDate.parse(mov.fecha) }.getOrNull()
            // Mientras se busca, se busca en todos los meses (no solo en el seleccionado)
            val mesOk = f.buscando || f.mes == null || fecha?.monthValue == f.mes
            val anioOk = f.buscando || f.anio == null || fecha?.year == f.anio
            val catOk = f.categoriaId == null || mov.categoriaId == f.categoriaId
            val textoOk = !f.buscando || coincide(mov, textoBuscado)
            tipoOk && mesOk && anioOk && catOk && textoOk
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
    fun setBusqueda(texto: String) = _filtros.update { it.copy(busqueda = texto) }
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