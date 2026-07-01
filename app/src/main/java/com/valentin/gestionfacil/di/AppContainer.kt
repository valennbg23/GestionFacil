package com.valentin.gestionfacil.di

import android.content.Context
import com.valentin.gestionfacil.data.db.AppDatabase
import com.valentin.gestionfacil.data.repository.FinanzasRepository
import com.valentin.gestionfacil.utils.PreferenciasHelper

/**
 * Contenedor manual de dependencias.
 * Se instancia una única vez en GestionFacilApp.
 */
class AppContainer(context: Context) {

    private val database: AppDatabase = AppDatabase.getInstance(context)

    val repository: FinanzasRepository = FinanzasRepository(
        categoriaDao = database.categoriaDao(),
        movimientoDao = database.movimientoDao(),
        presupuestoDao = database.presupuestoDao(),
        metaDao = database.metaDao()
    )

    val preferencias: PreferenciasHelper = PreferenciasHelper(context)
}