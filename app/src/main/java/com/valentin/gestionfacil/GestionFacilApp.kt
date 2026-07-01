package com.valentin.gestionfacil

import android.app.Application
import com.valentin.gestionfacil.di.AppContainer

/**
 * Punto de entrada de la aplicación.
 * Crea el contenedor de dependencias una única vez, disponible en toda la app.
 */
class GestionFacilApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}