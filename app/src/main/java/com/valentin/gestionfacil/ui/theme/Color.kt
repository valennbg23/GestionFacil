package com.valentin.gestionfacil.ui.theme

import androidx.compose.ui.graphics.Color

// ============================================================
// TEMA CLARO
// ============================================================
val LightPrimary = Color(0xFF2E7D32)         // Verde principal
val LightOnPrimary = Color(0xFFFFFFFF)
val LightSecondary = Color(0xFF1565C0)       // Azul
val LightOnSecondary = Color(0xFFFFFFFF)
val LightTertiary = Color(0xFF7B1FA2)        // Morado
val LightBackground = Color(0xFFF7F7F7)
val LightOnBackground = Color(0xFF1A1A1A)
val LightSurface = Color(0xFFFFFFFF)
val LightOnSurface = Color(0xFF1A1A1A)
val LightError = Color(0xFFD32F2F)

// ============================================================
// TEMA OSCURO
// ============================================================
val DarkPrimary = Color(0xFF81C784)
val DarkOnPrimary = Color(0xFF003D08)
val DarkSecondary = Color(0xFF64B5F6)
val DarkOnSecondary = Color(0xFF002E5C)
val DarkTertiary = Color(0xFFCE93D8)
val DarkBackground = Color(0xFF121212)
val DarkOnBackground = Color(0xFFE6E6E6)
val DarkSurface = Color(0xFF1E1E1E)
val DarkOnSurface = Color(0xFFE6E6E6)
val DarkError = Color(0xFFEF5350)

// ============================================================
// COLORES SEMÁNTICOS
// ============================================================
val ColorIngreso = Color(0xFF2E7D32)
val ColorGasto = Color(0xFFD32F2F)
val AlertaOk = Color(0xFF43A047)
val AlertaMedia = Color(0xFFFB8C00)
val AlertaAlta = Color(0xFFD32F2F)

// ============================================================
// COLORES POR PESTAÑA (solo modo claro)
// ============================================================
/** Color identificativo de cada pantalla principal. */
object ColoresPantalla {
    // Inicio - Verde (financiero, calma)
    val DashboardPrimario = Color(0xFF2E7D32)
    val DashboardContenedor = Color(0xFFC8E6C9)
    val DashboardOnContenedor = Color(0xFF1B5E20)

    // Historial - Azul (información, datos)
    val HistorialPrimario = Color(0xFF1565C0)
    val HistorialContenedor = Color(0xFFBBDEFB)
    val HistorialOnContenedor = Color(0xFF0D47A1)

    // Presupuestos - Naranja (atención, límites)
    val PresupuestosPrimario = Color(0xFFE65100)
    val PresupuestosContenedor = Color(0xFFFFE0B2)
    val PresupuestosOnContenedor = Color(0xFFBF360C)

    // Metas - Morado (sueños, ambición)
    val MetasPrimario = Color(0xFF7B1FA2)
    val MetasContenedor = Color(0xFFE1BEE7)
    val MetasOnContenedor = Color(0xFF4A148C)

    // Ajustes - Gris azulado (sistema, neutro)
    val AjustesPrimario = Color(0xFF455A64)
    val AjustesContenedor = Color(0xFFCFD8DC)
    val AjustesOnContenedor = Color(0xFF263238)
}