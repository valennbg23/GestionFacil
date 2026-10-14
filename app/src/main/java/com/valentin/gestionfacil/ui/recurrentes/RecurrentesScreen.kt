package com.valentin.gestionfacil.ui.recurrentes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valentin.gestionfacil.data.db.dao.RecurrenteConCategoria
import com.valentin.gestionfacil.data.entity.Frecuencia
import com.valentin.gestionfacil.data.entity.Recurrente
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import com.valentin.gestionfacil.ui.common.AppViewModelProvider
import com.valentin.gestionfacil.ui.theme.ColorGasto
import com.valentin.gestionfacil.ui.theme.ColorIngreso
import com.valentin.gestionfacil.utils.CalendarioRecurrente
import com.valentin.gestionfacil.utils.IconHelper
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecurrentesScreen(
    onNavigateBack: () -> Unit,
    viewModel: RecurrentesViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val recurrentes by viewModel.recurrentes.collectAsStateWithLifecycle()
    var recurrenteEliminar by remember { mutableStateOf<Recurrente?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Movimientos recurrentes", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (recurrentes.isEmpty()) {
            EstadoVacio(Modifier.padding(padding))
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Se añaden solos al abrir la app cuando llega su fecha. " +
                                "Pausa uno con el interruptor o bórralo con la papelera.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                items(recurrentes, key = { it.recurrente.id }) { fila ->
                    RecurrenteCard(
                        fila = fila,
                        onCambiarActivo = { activo -> viewModel.cambiarActivo(fila.recurrente, activo) },
                        onEliminar = { recurrenteEliminar = fila.recurrente }
                    )
                }
            }
        }
    }

    recurrenteEliminar?.let { rec ->
        AlertDialog(
            onDismissRequest = { recurrenteEliminar = null },
            title = { Text("Eliminar recurrente") },
            text = {
                Text(
                    "Dejará de añadirse automáticamente. Los movimientos que ya se " +
                            "crearon seguirán en tu historial."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminar(rec)
                    recurrenteEliminar = null
                }) { Text("Eliminar", color = ColorGasto) }
            },
            dismissButton = {
                TextButton(onClick = { recurrenteEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun RecurrenteCard(
    fila: RecurrenteConCategoria,
    onCambiarActivo: (Boolean) -> Unit,
    onEliminar: () -> Unit
) {
    val rec = fila.recurrente
    val colorCategoria = parseHex(fila.categoriaColor)
    val esGasto = rec.tipo == TipoMovimiento.GASTO
    val titulo = rec.descripcion.ifBlank { fila.categoriaNombre }

    val detalle = if (rec.activo) {
        val proxima = CalendarioRecurrente.proximaFecha(rec.fechaInicio, rec.frecuencia, rec.ultimaFecha)
        "${nombreFrecuencia(rec.frecuencia)} · próximo: " +
                proxima.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } else {
        "${nombreFrecuencia(rec.frecuencia)} · en pausa"
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier
                .padding(12.dp)
                .fillMaxWidth()
                .alpha(if (rec.activo) 1f else 0.6f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(colorCategoria.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(IconHelper.get(fila.categoriaIcono), null, tint = colorCategoria)
            }

            Column(Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleSmall, maxLines = 1)
                Text(
                    (if (esGasto) "-" else "+") + formatEur(rec.importe),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (esGasto) ColorGasto else ColorIngreso
                )
                Text(
                    detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Switch(checked = rec.activo, onCheckedChange = onCambiarActivo)

            IconButton(onClick = onEliminar) {
                Icon(Icons.Default.Delete, "Eliminar", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun EstadoVacio(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Repeat,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Aún no tienes movimientos recurrentes",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Al añadir un gasto o un ingreso, activa \"Repetir automáticamente\" " +
                    "y aparecerá aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

private fun nombreFrecuencia(frecuencia: Frecuencia): String = when (frecuencia) {
    Frecuencia.SEMANAL -> "Semanal"
    Frecuencia.MENSUAL -> "Mensual"
    Frecuencia.ANUAL -> "Anual"
}

private fun formatEur(amount: Double): String {
    val nf = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    nf.currency = Currency.getInstance("EUR")
    return nf.format(amount)
}

private fun parseHex(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) { Color.Gray }