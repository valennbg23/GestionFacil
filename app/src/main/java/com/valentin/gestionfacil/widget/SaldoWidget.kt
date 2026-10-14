package com.valentin.gestionfacil.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.valentin.gestionfacil.GestionFacilApp
import com.valentin.gestionfacil.MainActivity
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Currency
import java.util.Locale

/** Datos que enseña el widget. */
internal data class DatosWidget(
    val nombreMes: String,
    val ingresos: Double,
    val gastos: Double,
    /** true si la app tiene bloqueo con huella: entonces no se enseñan las cantidades. */
    val oculto: Boolean
) {
    val saldo: Double get() = ingresos - gastos
}

// Colores de la app (claro / oscuro)
private val ColorFondo = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFF1E1E1E))
private val ColorTexto = ColorProvider(day = Color(0xFF1A1A1A), night = Color(0xFFE6E6E6))
private val ColorSecundario = ColorProvider(day = Color(0xFF5F6368), night = Color(0xFFBDBDBD))
private val ColorVerde = ColorProvider(day = Color(0xFF2E7D32), night = Color(0xFF81C784))
private val ColorRojo = ColorProvider(day = Color(0xFFD32F2F), night = Color(0xFFEF5350))
private val ColorBoton = ColorProvider(day = Color(0xFF2E7D32), night = Color(0xFF388E3C))
private val ColorTextoBoton = ColorProvider(day = Color(0xFFFFFFFF), night = Color(0xFFFFFFFF))

private val nombresMes = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

/**
 * Widget de la pantalla de inicio: saldo del mes, ingresos, gastos
 * y un botón para añadir un movimiento directamente.
 */
class SaldoWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as GestionFacilApp).container
        val hoy = LocalDate.now()
        val anio = hoy.year.toString()
        val mes = "%02d".format(hoy.monthValue)
        val nombreMes = nombresMes[hoy.monthValue - 1]
        val oculto = container.preferencias.bloqueoActivado

        // Escucha los totales del mes: si cambian mientras el widget está activo, se redibuja solo
        val flujoDatos = combine(
            container.repository.observarTotalMes(TipoMovimiento.INGRESO, anio, mes),
            container.repository.observarTotalMes(TipoMovimiento.GASTO, anio, mes)
        ) { ingresos, gastos ->
            DatosWidget(nombreMes, ingresos, gastos, oculto)
        }
        val datosIniciales = flujoDatos.first()

        provideContent {
            val datos by flujoDatos.collectAsState(initial = datosIniciales)
            ContenidoWidget(datos)
        }
    }
}

/** Recibe los avisos del sistema (añadir widget, actualizar...) y se los pasa a SaldoWidget. */
class SaldoWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = SaldoWidget()
}

// ── Diseño del widget ──────────────────────────────────────────────────

/** Clave del extra que le dice a la app a qué pantalla ir al abrirse. */
private val claveDestino = ActionParameters.Key<String>(MainActivity.EXTRA_DESTINO)

@Composable
private fun ContenidoWidget(datos: DatosWidget) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorFondo)
            .cornerRadius(20.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>())
    ) {
        // Cabecera: nombre de la app y mes
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "GestiónFácil",
                style = TextStyle(color = ColorVerde, fontSize = 13.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Text(
                datos.nombreMes,
                style = TextStyle(color = ColorSecundario, fontSize = 12.sp)
            )
        }

        Spacer(GlanceModifier.height(8.dp))

        // Saldo
        Text("Saldo del mes", style = TextStyle(color = ColorSecundario, fontSize = 12.sp))
        Text(
            if (datos.oculto) "•••• €" else formatEur(datos.saldo),
            style = TextStyle(
                color = when {
                    datos.oculto -> ColorTexto
                    datos.saldo >= 0 -> ColorTexto
                    else -> ColorRojo
                },
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        )

        Spacer(GlanceModifier.defaultWeight())

        // Ingresos, gastos y botón de añadir
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = GlanceModifier.defaultWeight()) {
                Text(
                    "▲ " + if (datos.oculto) "••••" else formatEur(datos.ingresos),
                    style = TextStyle(color = ColorVerde, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                )
                Text(
                    "▼ " + if (datos.oculto) "••••" else formatEur(datos.gastos),
                    style = TextStyle(color = ColorRojo, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                )
            }

            Box(
                modifier = GlanceModifier
                    .background(ColorBoton)
                    .cornerRadius(16.dp)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
                    .clickable(
                        actionStartActivity<MainActivity>(
                            actionParametersOf(claveDestino to MainActivity.DESTINO_ADD_MOVIMIENTO)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "+ Añadir",
                    style = TextStyle(color = ColorTextoBoton, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

private fun formatEur(cantidad: Double): String {
    val nf = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    nf.currency = Currency.getInstance("EUR")
    return nf.format(cantidad)
}