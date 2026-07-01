package com.valentin.gestionfacil.ui.common

import androidx.lifecycle.ViewModel
import com.valentin.gestionfacil.utils.PreferenciasHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ModoTema(val clave: String) {
    SISTEMA("system"),
    CLARO("light"),
    OSCURO("dark");

    companion object {
        fun desde(clave: String): ModoTema = entries.find { it.clave == clave } ?: SISTEMA
    }
}

class TemaViewModel(
    private val preferencias: PreferenciasHelper
) : ViewModel() {

    private val _modo = MutableStateFlow(ModoTema.desde(preferencias.modoTema))
    val modo: StateFlow<ModoTema> = _modo.asStateFlow()

    fun cambiarModo(nuevo: ModoTema) {
        preferencias.modoTema = nuevo.clave
        _modo.value = nuevo
    }
}