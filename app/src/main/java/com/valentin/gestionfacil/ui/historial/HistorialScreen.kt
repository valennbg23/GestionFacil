package com.valentin.gestionfacil.ui.historial

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.valentin.gestionfacil.utils.IconHelper
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistorialScreen(
    onEditarMovimiento: (Long) -> Unit,
    viewModel: HistorialViewModel = viewModel(factory = AppViewModelProvider.Factory),
    temaViewModel: TemaViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val modoTema by temaViewModel.modo.collectAsStateWithLifecycle()
    val esOscuro = esModoOscuro(modoTema)
    val colores = coloresDe(Pantalla.HISTORIAL, forzarTemaOscuro = esOscuro)

    val nombresMes = listOf(
        "Ene", "Feb", "Mar", "Abr", "May", "Jun",
        "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colores.contenedor,
                    titleContentColor = colores.onContenedor
                )
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {

            // Filtro de tipo
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.filtros.tipo == FiltroTipo.TODOS,
                    onClick = { viewModel.setTipo(FiltroTipo.TODOS) },
                    label = { Text("Todos") },
                    colors = chipColores(colores.primario)
                )
                FilterChip(
                    selected = state.filtros.tipo == FiltroTipo.GASTOS,
                    onClick = { viewModel.setTipo(FiltroTipo.GASTOS) },
                    label = { Text("Gastos") },
                    colors = chipColores(colores.primario)
                )
                FilterChip(
                    selected = state.filtros.tipo == FiltroTipo.INGRESOS,
                    onClick = { viewModel.setTipo(FiltroTipo.INGRESOS) },
                    label = { Text("Ingresos") },
                    colors = chipColores(colores.primario)
                )
            }

            // Filtro de mes
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.filtros.mes == null,
                    onClick = { viewModel.setMes(null) },
                    label = { Text("Todo el año") },
                    colors = chipColores(colores.primario)
                )
                (1..12).forEach { mes ->
                    FilterChip(
                        selected = state.filtros.mes == mes,
                        onClick = { viewModel.setMes(mes) },
                        label = { Text(nombresMes[mes - 1]) },
                        colors = chipColores(colores.primario)
                    )
                }
            }

            // Filtro de categoría
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.filtros.categoriaId == null,
                    onClick = { viewModel.setCategoria(null) },
                    label = { Text("Todas") },
                    leadingIcon = { Icon(Icons.Default.FilterList, null) },
                    colors = chipColores(colores.primario)
                )
                state.categorias.forEach { cat ->
                    FilterChip(
                        selected = state.filtros.categoriaId == cat.id,
                        onClick = { viewModel.setCategoria(cat.id) },
                        label = { Text(cat.nombre) },
                        colors = chipColores(colores.primario)
                    )
                }
            }

            HorizontalDivider()

            // Lista
            if (state.movimientos.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "No hay movimientos con los filtros seleccionados",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.movimientos, key = { it.id }) { mov ->
                        MovimientoRow(mov, onClick = { onEditarMovimiento(mov.id) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun chipColores(colorPrimario: Color) = FilterChipDefaults.filterChipColors(
    selectedContainerColor = colorPrimario.copy(alpha = 0.18f),
    selectedLabelColor = colorPrimario,
    selectedLeadingIconColor = colorPrimario
)

@Composable
private fun MovimientoRow(
    mov: MovimientoConCategoria,
    onClick: () -> Unit
) {
    val color = parseHex(mov.categoriaColor)
    val esGasto = mov.tipo == TipoMovimiento.GASTO
    val fechaParsed = runCatching { LocalDate.parse(mov.fecha) }.getOrNull()
    val fechaFmt = fechaParsed?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: mov.fecha

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(IconHelper.get(mov.categoriaIcono), null, tint = color, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(
                    mov.categoriaNombre,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    buildString {
                        if (mov.descripcion.isNotBlank()) {
                            append(mov.descripcion)
                            append(" · ")
                        }
                        append(fechaFmt)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                (if (esGasto) "-" else "+") + formatEur(mov.importe),
                style = MaterialTheme.typography.titleMedium,
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