package com.valentin.gestionfacil.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row
import java.time.LocalDate
import java.time.YearMonth
import java.util.Locale

private val nombresMes = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

/**
 * Navegador de mes con flechas izquierda/derecha.
 * Bloquea avanzar más allá del mes actual.
 */
@Composable
fun MesNavigator(
    anio: Int,
    mes: Int,
    onCambiarMes: (anio: Int, mes: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val actual = YearMonth.of(anio, mes)
    val hoy = YearMonth.now()
    val esMesActual = actual == hoy

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = {
            val anterior = actual.minusMonths(1)
            onCambiarMes(anterior.year, anterior.monthValue)
        }) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Mes anterior")
        }

        Text(
            "${nombresMes[mes - 1]} $anio",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        IconButton(
            onClick = {
                val siguiente = actual.plusMonths(1)
                onCambiarMes(siguiente.year, siguiente.monthValue)
            },
            enabled = !esMesActual
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowForward, "Mes siguiente")
        }
    }
}

/** Helper: obtener (anio, mes) del momento actual. */
fun mesYAnioActual(): Pair<Int, Int> {
    val hoy = LocalDate.now()
    return hoy.year to hoy.monthValue
}