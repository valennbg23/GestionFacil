package com.valentin.gestionfacil.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Preferencias persistentes de la app:
 * - Estado del onboarding
 * - Modo de tema (claro / oscuro / sistema)
 * - Bloqueo biométrico
 * - Avisos de presupuesto
 */
class PreferenciasHelper(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("gestionfacil_prefs", Context.MODE_PRIVATE)

    // ── Onboarding ──────────────────────────────────────────────
    var onboardingCompletado: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING, false)
        set(value) { prefs.edit().putBoolean(KEY_ONBOARDING, value).apply() }

    // ── Tema ────────────────────────────────────────────────────
    var modoTema: String
        get() = prefs.getString(KEY_TEMA, "system") ?: "system"
        set(value) { prefs.edit().putString(KEY_TEMA, value).apply() }

    // ── Bloqueo biométrico ──────────────────────────────────────
    var bloqueoActivado: Boolean
        get() = prefs.getBoolean(KEY_BLOQUEO, false)
        set(value) { prefs.edit().putBoolean(KEY_BLOQUEO, value).apply() }

    // ── Avisos de presupuesto ───────────────────────────────────
    var avisosPresupuesto: Boolean
        get() = prefs.getBoolean(KEY_AVISOS, false)
        set(value) { prefs.edit().putBoolean(KEY_AVISOS, value).apply() }

    /**
     * Último nivel avisado (0, 80 o 100) para una categoría en un mes.
     * Sirve para no repetir el mismo aviso cada vez que se abre la app.
     */
    fun nivelAvisado(categoriaId: Long, anio: Int, mes: Int): Int =
        prefs.getInt(claveAviso(categoriaId, anio, mes), 0)

    fun guardarNivelAvisado(categoriaId: Long, anio: Int, mes: Int, nivel: Int) {
        prefs.edit().putInt(claveAviso(categoriaId, anio, mes), nivel).apply()
    }

    private fun claveAviso(categoriaId: Long, anio: Int, mes: Int) =
        "aviso_presupuesto_${categoriaId}_${anio}_$mes"

    companion object {
        private const val KEY_ONBOARDING = "onboarding_completado"
        private const val KEY_TEMA = "modo_tema"
        private const val KEY_BLOQUEO = "bloqueo_activado"
        private const val KEY_AVISOS = "avisos_presupuesto"
    }
}