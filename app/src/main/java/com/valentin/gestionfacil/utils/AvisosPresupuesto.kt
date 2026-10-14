package com.valentin.gestionfacil.utils

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.valentin.gestionfacil.MainActivity
import com.valentin.gestionfacil.R
import com.valentin.gestionfacil.data.db.dao.PresupuestoConCategoria
import com.valentin.gestionfacil.data.repository.FinanzasRepository
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Currency
import java.util.Locale

/**
 * Notificaciones cuando una categoría llega al 80% de su presupuesto del mes
 * o lo supera. Cada aviso sale una sola vez por categoría y mes.
 *
 * Mismos umbrales que la pantalla de Presupuestos (naranja al 80%, rojo al 100%).
 */
class AvisosPresupuesto(
    private val context: Context,
    private val repository: FinanzasRepository,
    private val preferencias: PreferenciasHelper
) {

    init {
        crearCanal()
    }

    /**
     * true si la app puede mostrar notificaciones.
     * En Android 13+ hace falta que el usuario dé el permiso; en versiones
     * anteriores basta con que no las haya desactivado en los ajustes del móvil.
     */
    fun permisoConcedido(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }

    /**
     * Revisa los presupuestos del mes actual y avisa de los que han
     * llegado al 80% o al 100% desde la última vez.
     */
    suspend fun comprobarMesActual(hoy: LocalDate = LocalDate.now()) {
        if (!preferencias.avisosPresupuesto || !permisoConcedido()) return

        val mes = hoy.monthValue
        val anio = hoy.year
        val presupuestos = repository.observarPresupuestosDelMes(mes, anio).first()

        presupuestos.forEach { p ->
            if (p.importeMaximo <= 0.0) return@forEach

            val gastado = repository.observarGastoCategoriaMes(
                p.categoriaId, anio.toString(), "%02d".format(mes)
            ).first()

            val porcentaje = gastado / p.importeMaximo * 100
            val nivel = when {
                porcentaje >= 100 -> 100
                porcentaje >= 80 -> 80
                else -> 0
            }

            if (nivel > preferencias.nivelAvisado(p.categoriaId, anio, mes)) {
                mostrar(p, gastado, porcentaje, nivel)
                preferencias.guardarNivelAvisado(p.categoriaId, anio, mes, nivel)
            }
        }
    }

    @SuppressLint("MissingPermission") // el permiso se comprueba en comprobarMesActual()
    private fun mostrar(p: PresupuestoConCategoria, gastado: Double, porcentaje: Double, nivel: Int) {
        val (titulo, texto) = if (nivel >= 100) {
            "Presupuesto superado: ${p.categoriaNombre}" to
                    "Llevas ${formatEur(gastado)} de ${formatEur(p.importeMaximo)} este mes"
        } else {
            "Llevas el ${porcentaje.toInt()}% de ${p.categoriaNombre}" to
                    "${formatEur(gastado)} de ${formatEur(p.importeMaximo)} este mes"
        }

        // Al pulsar la notificación se abre la app
        val abrirApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificacion = NotificationCompat.Builder(context, CANAL_ID)
            .setSmallIcon(R.drawable.ic_notificacion)
            .setContentTitle(titulo)
            .setContentText(texto)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        try {
            // Un id por categoría: el aviso del 100% sustituye al del 80%
            NotificationManagerCompat.from(context).notify(p.categoriaId.toInt(), notificacion)
        } catch (e: SecurityException) {
            // El usuario ha quitado el permiso desde los ajustes del móvil
        }
    }

    private fun crearCanal() {
        val canal = NotificationChannel(
            CANAL_ID,
            "Avisos de presupuesto",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisa al llegar al 80% o al superar el presupuesto de una categoría"
        }
        context.getSystemService(NotificationManager::class.java)
            ?.createNotificationChannel(canal)
    }

    private fun formatEur(cantidad: Double): String {
        val nf = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
        nf.currency = Currency.getInstance("EUR")
        return nf.format(cantidad)
    }

    private companion object {
        const val CANAL_ID = "avisos_presupuesto"
    }
}