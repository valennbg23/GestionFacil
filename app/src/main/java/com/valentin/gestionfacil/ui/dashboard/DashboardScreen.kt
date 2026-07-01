package com.valentin.gestionfacil.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valentin.gestionfacil.data.db.dao.MovimientoConCategoria
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import com.valentin.gestionfacil.ui.common.AppViewModelProvider
import com.valentin.gestionfacil.ui.common.ModoTema
import com.valentin.gestionfacil.ui.common.TemaViewModel
import com.valentin.gestionfacil.ui.theme.ColorGasto
import com.valentin.gestionfacil.ui.theme.ColorIngreso
import com.valentin.gestionfacil.ui.theme.Pantalla
import com.valentin.gestionfacil.ui.theme.coloresDe
import com.valentin.gestionfacil.utils.EstadisticasRacha
import com.valentin.gestionfacil.utils.IconHelper
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

private val nombresMes = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onAddMovimiento: () -> Unit = {},
    viewModel: DashboardViewModel = viewModel(factory = AppViewModelProvider.Factory),
    temaViewModel: TemaViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val modoTema by temaViewModel.modo.collectAsStateWithLifecycle()
    val esOscuro = esModoOscuro(modoTema)
    val colores = coloresDe(Pantalla.DASHBOARD, forzarTemaOscuro = esOscuro)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("GestiónFácil", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Tu dinero, bajo control",
                            style = MaterialTheme.typography.bodySmall,
                            color = colores.onContenedor.copy(alpha = 0.75f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colores.contenedor,
                    titleContentColor = colores.onContenedor
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddMovimiento,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Añadir") },
                containerColor = colores.primario,
                contentColor = Color.White
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            item {
                SelectorMes(
                    mes = state.mes,
                    anio = state.anio,
                    esMesActual = state.esMesActual,
                    onAnterior = { viewModel.mesAnterior() },
                    onSiguiente = { viewModel.mesSiguiente() },
                    onActual = { viewModel.irAMesActual() }
                )
            }

            item { BalanceCard(state.ingresos, state.gastos, state.saldo, colores.contenedor, colores.onContenedor) }

            if (state.esMesActual) {
                item { TarjetaRacha(state.racha) }
            }

            if (state.gastosPorCategoria.isNotEmpty()) {
                item { GraficaTartaGastos(state.gastosPorCategoria) }
            }

            if (state.totalesPorMes.isNotEmpty()) {
                item { GraficaEvolucionMeses(state.totalesPorMes) }
            }

            item {
                Text(
                    "Últimos movimientos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colores.primario
                )
            }

            if (state.ultimosMovimientos.isEmpty()) {
                item {
                    Card(Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            Modifier.padding(24.dp).fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.AccountBalance,
                                null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Aún no has registrado ningún movimiento este mes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(state.ultimosMovimientos, key = { it.id }) { mov ->
                    MovimientoItem(mov)
                }
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun TarjetaRacha(racha: EstadisticasRacha) {
    val esRachaActiva = racha.rachaActual > 0
    val colorPrincipal = if (esRachaActiva)
        Color(0xFFFF6F00)
    else
        MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(colorPrincipal.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.LocalFireDepartment,
                        null,
                        tint = colorPrincipal,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (esRachaActiva)
                            "Llevas ${racha.rachaActual} ${if (racha.rachaActual == 1) "día" else "días"} sin gastar"
                        else
                            "Sin racha activa",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorPrincipal
                    )
                    Text(
                        racha.mensajeMotivador,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EstadisticaPequena(
                    modifier = Modifier.weight(1f),
                    icono = Icons.Default.EmojiEvents,
                    label = "Mejor racha",
                    valor = "${racha.mejorRacha} ${if (racha.mejorRacha == 1) "día" else "días"}",
                    color = Color(0xFFFFB300)
                )
                EstadisticaPequena(
                    modifier = Modifier.weight(1f),
                    icono = Icons.Default.LocalFireDepartment,
                    label = "Días sin gastar este mes",
                    valor = "${racha.diasSinGastosEsteMes}",
                    color = Color(0xFF43A047)
                )
            }
        }
    }
}

@Composable
private fun EstadisticaPequena(
    modifier: Modifier,
    icono: ImageVector,
    label: String,
    valor: String,
    color: Color
) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icono, null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            valor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun SelectorMes(
    mes: Int,
    anio: Int,
    esMesActual: Boolean,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onActual: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAnterior) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = "Mes anterior"
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${nombresMes[mes - 1]} $anio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (!esMesActual) {
                    TextButton(onClick = onActual) {
                        Text("Volver al mes actual", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            IconButton(
                onClick = onSiguiente,
                enabled = !esMesActual
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Mes siguiente"
                )
            }
        }
    }
}

@Composable
private fun BalanceCard(
    ingresos: Double,
    gastos: Double,
    saldo: Double,
    colorContenedor: Color,
    colorOnContenedor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = colorContenedor)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Saldo del mes",
                style = MaterialTheme.typography.labelMedium,
                color = colorOnContenedor
            )
            Text(
                formatEur(saldo),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = if (saldo >= 0) colorOnContenedor else ColorGasto
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                ChipValor(Modifier.weight(1f), "Ingresos", ingresos, ColorIngreso, Icons.Default.TrendingUp)
                ChipValor(Modifier.weight(1f), "Gastos", gastos, ColorGasto, Icons.Default.TrendingDown)
            }
        }
    }
}

@Composable
private fun ChipValor(
    modifier: Modifier,
    label: String,
    valor: Double,
    color: Color,
    icono: ImageVector
) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(
            Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icono, null, tint = color)
            Column {
                Text(label, style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatEur(valor), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun MovimientoItem(mov: MovimientoConCategoria) {
    val color = parseHex(mov.categoriaColor)
    val esGasto = mov.tipo == TipoMovimiento.GASTO
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(IconHelper.get(mov.categoriaIcono), null, tint = color, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(mov.categoriaNombre, fontWeight = FontWeight.Medium)
                Text(mov.descripcion.ifBlank { mov.fecha },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                (if (esGasto) "-" else "+") + formatEur(mov.importe),
                fontWeight = FontWeight.SemiBold,
                color = if (esGasto) ColorGasto else ColorIngreso
            )
        }
    }
}

private fun esModoOscuro(modo: ModoTema): Boolean? = when (modo) {
    ModoTema.SISTEMA -> null
    ModoTema.CLARO -> false
    ModoTema.OSCURO -> true
}

private fun formatEur(amount: Double): String {
    val nf = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
    nf.currency = Currency.getInstance("EUR")
    return nf.format(amount)
}

private fun parseHex(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) { Color.Gray }