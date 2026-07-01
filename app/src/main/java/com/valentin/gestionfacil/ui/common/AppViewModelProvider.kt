package com.valentin.gestionfacil.ui.common

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.valentin.gestionfacil.GestionFacilApp
import com.valentin.gestionfacil.data.repository.FinanzasRepository
import com.valentin.gestionfacil.ui.addmovimiento.AddMovimientoViewModel
import com.valentin.gestionfacil.ui.ajustes.AjustesViewModel
import com.valentin.gestionfacil.ui.dashboard.DashboardViewModel
import com.valentin.gestionfacil.ui.historial.HistorialViewModel
import com.valentin.gestionfacil.ui.metas.MetasViewModel
import com.valentin.gestionfacil.ui.presupuestos.PresupuestosViewModel
import com.valentin.gestionfacil.utils.PreferenciasHelper

object AppViewModelProvider {

    val Factory: ViewModelProvider.Factory = viewModelFactory {
        initializer { DashboardViewModel(repo()) }
        initializer { AddMovimientoViewModel(repo()) }
        initializer { HistorialViewModel(repo()) }
        initializer { PresupuestosViewModel(repo()) }
        initializer { AjustesViewModel(repo()) }
        initializer { MetasViewModel(repo()) }
        initializer { TemaViewModel(prefs()) }
    }
}

private fun CreationExtras.repo(): FinanzasRepository =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GestionFacilApp)
        .container.repository

private fun CreationExtras.prefs(): PreferenciasHelper =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as GestionFacilApp)
        .container.preferencias