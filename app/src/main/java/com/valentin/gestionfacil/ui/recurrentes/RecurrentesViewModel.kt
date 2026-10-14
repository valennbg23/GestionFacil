package com.valentin.gestionfacil.ui.recurrentes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valentin.gestionfacil.data.db.dao.RecurrenteConCategoria
import com.valentin.gestionfacil.data.entity.Recurrente
import com.valentin.gestionfacil.data.repository.FinanzasRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecurrentesViewModel(
    private val repository: FinanzasRepository
) : ViewModel() {

    val recurrentes: StateFlow<List<RecurrenteConCategoria>> = repository.observarRecurrentes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Pausa (false) o reanuda (true) un recurrente. */
    fun cambiarActivo(recurrente: Recurrente, activo: Boolean) {
        viewModelScope.launch {
            repository.cambiarActivoRecurrente(recurrente, activo)
        }
    }

    /** Borra la regla. Los movimientos que ya se crearon no se tocan. */
    fun eliminar(recurrente: Recurrente) {
        viewModelScope.launch {
            repository.eliminarRecurrente(recurrente)
        }
    }
}