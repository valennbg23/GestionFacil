package com.valentin.gestionfacil.ui.addmovimiento

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valentin.gestionfacil.data.entity.Categoria
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import com.valentin.gestionfacil.ui.common.AppViewModelProvider
import com.valentin.gestionfacil.ui.theme.ColorGasto
import com.valentin.gestionfacil.utils.IconHelper
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMovimientoScreen(
    movimientoId: Long? = null,
    onNavigateBack: () -> Unit,
    viewModel: AddMovimientoViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val state by viewModel.ui.collectAsStateWithLifecycle()

    LaunchedEffect(movimientoId) {
        if (movimientoId != null && movimientoId > 0) viewModel.cargarParaEditar(movimientoId)
    }

    LaunchedEffect(state.guardado) {
        if (state.guardado) onNavigateBack()
    }

    var mostrarDatePicker by remember { mutableStateOf(false) }
    var mostrarConfirmEliminar by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.esEdicion) "Editar movimiento" else "Nuevo movimiento") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    if (state.esEdicion) {
                        IconButton(onClick = { mostrarConfirmEliminar = true }) {
                            Icon(Icons.Default.Delete, "Eliminar", tint = ColorGasto)
                        }
                    }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick = viewModel::guardar,
                    enabled = state.puedeGuardar,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp)
                ) {
                    Text(
                        if (state.guardando) "Guardando..." else "Guardar",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
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

            // Selector de tipo
            item {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.tipo == TipoMovimiento.GASTO,
                        onClick = { viewModel.setTipo(TipoMovimiento.GASTO) },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Gasto") }
                    SegmentedButton(
                        selected = state.tipo == TipoMovimiento.INGRESO,
                        onClick = { viewModel.setTipo(TipoMovimiento.INGRESO) },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Ingreso") }
                }
            }

            // Importe
            item {
                OutlinedTextField(
                    value = state.importeStr,
                    onValueChange = viewModel::setImporte,
                    label = { Text("Importe") },
                    suffix = { Text("€") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Fecha
            item {
                OutlinedCard(
                    onClick = { mostrarDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Fecha",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            formatearFecha(state.fecha),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }

            // Descripción
            item {
                OutlinedTextField(
                    value = state.descripcion,
                    onValueChange = viewModel::setDescripcion,
                    label = { Text("Descripción (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Título categoría
            item {
                Text(
                    "Categoría",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Filtramos categorías según el tipo (simple heurística por nombre)
            val categoriasFiltradas = when (state.tipo) {
                TipoMovimiento.INGRESO -> state.categorias.filter { c ->
                    c.nombre.contains("Nómina", true) ||
                            c.nombre.contains("freelance", true) ||
                            c.nombre.contains("Otros ingresos", true)
                }
                TipoMovimiento.GASTO -> state.categorias.filter { c ->
                    !c.nombre.contains("Nómina", true) &&
                            !c.nombre.contains("freelance", true) &&
                            !c.nombre.contains("Otros ingresos", true)
                }
            }.ifEmpty { state.categorias }

            items(categoriasFiltradas, key = { it.id }) { cat ->
                CategoriaItem(
                    categoria = cat,
                    seleccionada = state.categoriaId == cat.id,
                    onClick = { viewModel.setCategoria(cat.id) }
                )
            }

            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // Date picker
    if (mostrarDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.fecha
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val nuevaFecha = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.setFecha(nuevaFecha)
                    }
                    mostrarDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = datePickerState) }
    }

    // Confirmación eliminar
    if (mostrarConfirmEliminar) {
        AlertDialog(
            onDismissRequest = { mostrarConfirmEliminar = false },
            title = { Text("Eliminar movimiento") },
            text = { Text("¿Seguro que quieres eliminar este movimiento?") },
            confirmButton = {
                TextButton(onClick = {
                    mostrarConfirmEliminar = false
                    viewModel.eliminar()
                }) { Text("Eliminar", color = ColorGasto) }
            },
            dismissButton = {
                TextButton(onClick = { mostrarConfirmEliminar = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun CategoriaItem(
    categoria: Categoria,
    seleccionada: Boolean,
    onClick: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(categoria.color))
    } catch (e: Exception) { Color.Gray }

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (seleccionada) color.copy(alpha = 0.15f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (seleccionada)
            androidx.compose.foundation.BorderStroke(2.dp, color) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    IconHelper.get(categoria.icono),
                    null,
                    tint = color,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                categoria.nombre,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = if (seleccionada) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

private fun formatearFecha(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", Locale("es", "ES"))
    return date.format(formatter)
}