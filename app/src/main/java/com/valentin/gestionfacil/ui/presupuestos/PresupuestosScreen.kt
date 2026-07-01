package com.valentin.gestionfacil.ui.presupuestos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valentin.gestionfacil.data.entity.Categoria
import com.valentin.gestionfacil.ui.common.AppViewModelProvider
import com.valentin.gestionfacil.ui.common.ModoTema
import com.valentin.gestionfacil.ui.common.TemaViewModel
import com.valentin.gestionfacil.ui.theme.AlertaAlta
import com.valentin.gestionfacil.ui.theme.AlertaMedia
import com.valentin.gestionfacil.ui.theme.AlertaOk
import com.valentin.gestionfacil.ui.theme.Pantalla
import com.valentin.gestionfacil.ui.theme.coloresDe
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
fun PresupuestosScreen(
    viewModel: PresupuestosViewModel = viewModel(factory = AppViewModelProvider.Factory),
    temaViewModel: TemaViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val modoTema by temaViewModel.modo.collectAsStateWithLifecycle()
    val esOscuro = esModoOscuro(modoTema)
    val colores = coloresDe(Pantalla.PRESUPUESTOS, forzarTemaOscuro = esOscuro)

    var mostrarDialogo by remember { mutableStateOf(false) }
    var categoriaEditada by remember { mutableStateOf<Categoria?>(null) }
    var importeExistente by remember { mutableStateOf<Double?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Presupuestos", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Controla tu gasto por categoría",
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
                onClick = {
                    categoriaEditada = null
                    importeExistente = null
                    mostrarDialogo = true
                },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nuevo") },
                containerColor = colores.primario,
                contentColor = Color.White
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SelectorMes(
                    mes = state.mes,
                    anio = state.anio,
                    esMesActual = state.esMesActual,
                    onAnterior = { viewModel.mesAnterior() },
                    onSiguiente = { viewModel.mesSiguiente() },
                    onActual = { viewModel.irAMesActual() }
                )
            }

            if (state.items.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay presupuestos definidos para ${nombresMes[state.mes - 1]} ${state.anio}.\n\nPulsa el botón de abajo para crear el primero.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.items, key = { it.presupuesto.id }) { item ->
                        PresupuestoCard(
                            item = item,
                            onClick = {
                                categoriaEditada = state.categorias.find {
                                    it.id == item.presupuesto.categoriaId
                                }
                                importeExistente = item.presupuesto.importeMaximo
                                mostrarDialogo = true
                            }
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (mostrarDialogo) {
        DialogoPresupuesto(
            categorias = state.categorias,
            categoriaInicial = categoriaEditada,
            importeInicial = importeExistente,
            onDismiss = { mostrarDialogo = false },
            onGuardar = { catId, importe ->
                viewModel.guardarPresupuesto(catId, importe)
                mostrarDialogo = false
            },
            onEliminar = categoriaEditada?.let { cat ->
                {
                    viewModel.eliminarPresupuesto(cat.id)
                    mostrarDialogo = false
                }
            }
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
private fun PresupuestoCard(
    item: ItemPresupuesto,
    onClick: () -> Unit
) {
    val color = parseHex(item.presupuesto.categoriaColor)
    val colorProgreso = when (item.nivel) {
        NivelAlerta.OK       -> AlertaOk
        NivelAlerta.CERCA    -> AlertaMedia
        NivelAlerta.SUPERADO -> AlertaAlta
    }

    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(IconHelper.get(item.presupuesto.categoriaIcono), null, tint = color)
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        item.presupuesto.categoriaNombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${formatEur(item.gastado)} de ${formatEur(item.presupuesto.importeMaximo)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    "${(item.porcentaje * 100).toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    color = colorProgreso,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { item.porcentaje.coerceAtMost(1f) },
                color = colorProgreso,
                trackColor = colorProgreso.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
            )
            if (item.nivel != NivelAlerta.OK) {
                Spacer(Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Warning, null,
                        tint = colorProgreso,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        when (item.nivel) {
                            NivelAlerta.CERCA    -> "Cerca del límite (${(item.porcentaje * 100).toInt()}%)"
                            NivelAlerta.SUPERADO -> "¡Presupuesto superado!"
                            else                 -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colorProgreso,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoPresupuesto(
    categorias: List<Categoria>,
    categoriaInicial: Categoria?,
    importeInicial: Double?,
    onDismiss: () -> Unit,
    onGuardar: (Long, Double) -> Unit,
    onEliminar: (() -> Unit)? = null
) {
    var categoriaSeleccionada by remember { mutableStateOf(categoriaInicial) }
    var importeStr by remember {
        mutableStateOf(importeInicial?.let { "%.2f".format(Locale.US, it) } ?: "")
    }
    var desplegable by remember { mutableStateOf(false) }

    val soloGastos = categorias.filter { c ->
        !c.nombre.contains("Nómina", true) &&
                !c.nombre.contains("freelance", true) &&
                !c.nombre.contains("Otros ingresos", true)
    }

    val importe = importeStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val puedeGuardar = categoriaSeleccionada != null && importe > 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (categoriaInicial != null) "Editar presupuesto" else "Nuevo presupuesto")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                if (categoriaInicial == null) {
                    ExposedDropdownMenuBox(
                        expanded = desplegable,
                        onExpandedChange = { desplegable = !desplegable }
                    ) {
                        OutlinedTextField(
                            value = categoriaSeleccionada?.nombre ?: "Elegir categoría",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = desplegable)
                            },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = desplegable,
                            onDismissRequest = { desplegable = false }
                        ) {
                            soloGastos.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.nombre) },
                                    onClick = {
                                        categoriaSeleccionada = cat
                                        desplegable = false
                                    }
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "Categoría: ${categoriaInicial.nombre}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }

                OutlinedTextField(
                    value = importeStr,
                    onValueChange = {
                        importeStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                    },
                    label = { Text("Límite mensual") },
                    suffix = { Text("€") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { categoriaSeleccionada?.let { onGuardar(it.id, importe) } },
                enabled = puedeGuardar
            ) { Text("Guardar") }
        },
        dismissButton = {
            Row {
                if (onEliminar != null) {
                    TextButton(onClick = onEliminar) {
                        Text("Eliminar", color = AlertaAlta)
                    }
                }
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    )
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