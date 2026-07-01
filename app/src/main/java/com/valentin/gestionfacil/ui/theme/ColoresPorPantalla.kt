package com.valentin.gestionfacil.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Conjunto de colores para personalizar una pantalla concreta.
 * En modo oscuro siempre devuelve los colores del tema (mejor legibilidad).
 */
data class ColoresDePantalla(
    val primario: Color,
    val contenedor: Color,
    val onContenedor: Color
)

enum class Pantalla {
    DASHBOARD, HISTORIAL, PRESUPUESTOS, METAS, AJUSTES
}

/**
 * Devuelve los colores de la pantalla solicitada.
 * En modo oscuro devuelve siempre los del tema general para mantener legibilidad.
 */
@Composable
fun coloresDe(pantalla: Pantalla, forzarTemaOscuro: Boolean? = null): ColoresDePantalla {
    val esOscuro = forzarTemaOscuro ?: isSystemInDarkTheme()

    if (esOscuro) {
        return ColoresDePantalla(
            primario = MaterialTheme.colorScheme.primary,
            contenedor = MaterialTheme.colorScheme.surfaceVariant,
            onContenedor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    return when (pantalla) {
        Pantalla.DASHBOARD -> ColoresDePantalla(
            primario = ColoresPantalla.DashboardPrimario,
            contenedor = ColoresPantalla.DashboardContenedor,
            onContenedor = ColoresPantalla.DashboardOnContenedor
        )
        Pantalla.HISTORIAL -> ColoresDePantalla(
            primario = ColoresPantalla.HistorialPrimario,
            contenedor = ColoresPantalla.HistorialContenedor,
            onContenedor = ColoresPantalla.HistorialOnContenedor
        )
        Pantalla.PRESUPUESTOS -> ColoresDePantalla(
            primario = ColoresPantalla.PresupuestosPrimario,
            contenedor = ColoresPantalla.PresupuestosContenedor,
            onContenedor = ColoresPantalla.PresupuestosOnContenedor
        )
        Pantalla.METAS -> ColoresDePantalla(
            primario = ColoresPantalla.MetasPrimario,
            contenedor = ColoresPantalla.MetasContenedor,
            onContenedor = ColoresPantalla.MetasOnContenedor
        )
        Pantalla.AJUSTES -> ColoresDePantalla(
            primario = ColoresPantalla.AjustesPrimario,
            contenedor = ColoresPantalla.AjustesContenedor,
            onContenedor = ColoresPantalla.AjustesOnContenedor
        )
    }
}