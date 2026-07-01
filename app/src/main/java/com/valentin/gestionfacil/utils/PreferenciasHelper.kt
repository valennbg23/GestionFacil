package com.valentin.gestionfacil.utils

import android.content.Context
import android.content.SharedPreferences

/**
 * Preferencias persistentes de la app:
 * - Estado del onboarding
 * - Modo de tema (claro / oscuro / sistema)
 * - Bloqueo biométrico
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

    companion object {
        private const val KEY_ONBOARDING = "onboarding_completado"
        private const val KEY_TEMA = "modo_tema"
        private const val KEY_BLOQUEO = "bloqueo_activado"
    }
}