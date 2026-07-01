package com.valentin.gestionfacil.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity

/**
 * Utilidades para autenticación biométrica (huella, cara, PIN del dispositivo).
 * Usa BiometricPrompt de AndroidX, compatible desde API 23.
 */
object BiometricHelper {

    /**
     * Indica si el dispositivo está preparado para usar biometría o credencial.
     * Devuelve true si hay huella/cara registrada O si hay PIN/patrón configurado.
     */
    fun esBiometriaDisponible(context: Context): Boolean {
        val manager = BiometricManager.from(context)
        val tipos = BiometricManager.Authenticators.BIOMETRIC_WEAK or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        return manager.canAuthenticate(tipos) == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Lanza el diálogo de autenticación.
     *
     * @param activity actividad anfitriona (debe ser FragmentActivity)
     * @param onExito  callback cuando la autenticación es correcta
     * @param onError  callback cuando falla, se cancela o no se puede autenticar
     */
    fun autenticar(
        activity: FragmentActivity,
        onExito: () -> Unit,
        onError: (mensaje: String) -> Unit
    ) {
        val prompt = BiometricPrompt(
            activity,
            { runnable -> activity.runOnUiThread(runnable) },
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onExito()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString.toString())
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    // Failed = identificación incorrecta — el sistema deja reintentar.
                    // No llamamos a onError aquí, solo cuando el usuario cancela o se bloquea.
                }
            }
        )

        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle("GestiónFácil")
            .setSubtitle("Verifica tu identidad para acceder")
            .setDescription("Tus datos financieros están protegidos")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_WEAK or
                        BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        prompt.authenticate(info)
    }
}