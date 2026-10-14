package com.valentin.gestionfacil.ui.addmovimiento

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valentin.gestionfacil.data.entity.Categoria
import com.valentin.gestionfacil.data.entity.Frecuencia
import com.valentin.gestionfacil.data.entity.Movimiento
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import com.valentin.gestionfacil.data.repository.FinanzasRepository
import com.valentin.gestionfacil.utils.AnalizadorTicket
import com.valentin.gestionfacil.utils.AvisosPresupuesto
import com.valentin.gestionfacil.utils.DatosTicket
import com.valentin.gestionfacil.utils.LectorTickets
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

data class AddMovimientoUiState(
    val esEdicion: Boolean = false,
    val tipo: TipoMovimiento = TipoMovimiento.GASTO,
    val importeStr: String = "",
    val categoriaId: Long? = null,
    val fecha: LocalDate = LocalDate.now(),
    val descripcion: String = "",
    val repetir: Boolean = false,
    val frecuencia: Frecuencia = Frecuencia.MENSUAL,
    val categorias: List<Categoria> = emptyList(),
    val guardando: Boolean = false,
    val guardado: Boolean = false,
    val error: String? = null,
    /** true mientras se lee la foto del ticket */
    val escaneando: Boolean = false,
    /** Mensaje tras escanear: qué se ha rellenado o qué ha fallado */
    val avisoTicket: String? = null,
    val avisoTicketEsError: Boolean = false
) {
    val importe: Double get() = importeStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val puedeGuardar: Boolean get() = importe > 0.0 && categoriaId != null && !guardando
}

class AddMovimientoViewModel(
    private val repository: FinanzasRepository,
    private val avisos: AvisosPresupuesto
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
    fun setRepetir(repetir: Boolean) = _ui.update { it.copy(repetir = repetir) }
    fun setFrecuencia(frecuencia: Frecuencia) = _ui.update { it.copy(frecuencia = frecuencia) }

    /**
     * Lee la foto de un ticket y rellena el formulario con lo que encuentre:
     * importe total, fecha, tienda (en la descripción) y una categoría sugerida.
     */
    fun escanearTicket(context: Context, uri: Uri) {
        if (_ui.value.escaneando) return
        val appContext = context.applicationContext
        _ui.update { it.copy(escaneando = true, avisoTicket = null) }

        viewModelScope.launch {
            try {
                val datos = LectorTickets.leer(appContext, uri)
                val categoria = AnalizadorTicket.sugerirCategoria(datos, _ui.value.categorias)

                _ui.update { estado ->
                    estado.copy(
                        escaneando = false,
                        tipo = TipoMovimiento.GASTO,
                        importeStr = datos.importe?.let { formatearImporte(it) } ?: estado.importeStr,
                        fecha = datos.fecha ?: estado.fecha,
                        descripcion = datos.comercio ?: estado.descripcion,
                        categoriaId = categoria ?: estado.categoriaId,
                        avisoTicket = mensajeEscaneo(datos, categoria != null),
                        avisoTicketEsError = datos.importe == null
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("GestionFacil", "Error al leer el ticket", e)
                _ui.update {
                    it.copy(
                        escaneando = false,
                        avisoTicket = "No se ha podido leer la foto. Prueba otra vez.",
                        avisoTicketEsError = true
                    )
                }
            }
        }
    }

    /** "45,67" (con coma, como se escribe en España) */
    private fun formatearImporte(importe: Double): String =
        String.format(Locale("es", "ES"), "%.2f", importe)

    private fun mensajeEscaneo(datos: DatosTicket, hayCategoria: Boolean): String {
        if (datos.vacio) {
            return "No he podido leer el ticket. Prueba con más luz y con el ticket bien estirado."
        }
        val rellenado = listOfNotNull(
            "importe".takeIf { datos.importe != null },
            "fecha".takeIf { datos.fecha != null },
            "tienda".takeIf { datos.comercio != null },
            "categoría".takeIf { hayCategoria }
        )
        val lista = if (rellenado.size == 1) rellenado[0]
        else rellenado.dropLast(1).joinToString(", ") + " y " + rellenado.last()

        return if (datos.importe == null)
            "No he encontrado el total: escríbelo a mano. Rellenado: $lista."
        else
            "Rellenado: $lista. Revísalo antes de guardar."
    }

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
                when {
                    estado.esEdicion -> repository.actualizarMovimiento(mov)
                    estado.repetir -> {
                        repository.crearMovimientoRecurrente(mov, estado.frecuencia)
                        // Si la fecha de inicio es antigua, crea ya las repeticiones que faltan
                        repository.generarRecurrentesPendientes()
                    }
                    else -> repository.insertarMovimiento(mov)
                }

                // Si es un gasto, mira si con él se llega al 80% o al 100% de algún presupuesto.
                // Va en su propio try: un fallo del aviso no debe impedir guardar el movimiento.
                if (estado.tipo == TipoMovimiento.GASTO) {
                    try {
                        avisos.comprobarMesActual()
                    } catch (e: Exception) {
                        // Sin aviso, pero el movimiento ya está guardado
                    }
                }

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