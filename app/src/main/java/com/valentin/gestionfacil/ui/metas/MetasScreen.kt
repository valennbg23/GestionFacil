package com.valentin.gestionfacil.ui.metas

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Savings
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
import com.valentin.gestionfacil.data.entity.Meta
import com.valentin.gestionfacil.ui.common.AppViewModelProvider
import com.valentin.gestionfacil.ui.common.ModoTema
import com.valentin.gestionfacil.ui.common.TemaViewModel
import com.valentin.gestionfacil.ui.theme.AlertaAlta
import com.valentin.gestionfacil.ui.theme.ColorIngreso
import com.valentin.gestionfacil.ui.theme.Pantalla
import com.valentin.gestionfacil.ui.theme.coloresDe
import com.valentin.gestionfacil.utils.IconHelper
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

private val paletaColoresMetas = listOf(
    "#F7B801", "#4ECDC4", "#45B7D1", "#E84855", "#A4C639",
    "#9B5DE5", "#00BBF9", "#FF8C42", "#06A77D", "#F15BB5"
)

private val iconosDisponibles = listOf(
    "Savings", "BeachAccess", "Flight", "CardGiftcard", "Shield",
    "DirectionsBike", "Laptop", "House", "Pets", "Favorite",
    "Celebration", "SportsEsports", "School"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetasScreen(
    viewModel: MetasViewModel = viewModel(factory = AppViewModelProvider.Factory),
    temaViewModel: TemaViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val metas by viewModel.metas.collectAsStateWithLifecycle()
    val uiState by viewModel.ui.collectAsStateWithLifecycle()
    val modoTema by temaViewModel.modo.collectAsStateWithLifecycle()
    val esOscuro = esModoOscuro(modoTema)
    val colores = coloresDe(Pantalla.METAS, forzarTemaOscuro = esOscuro)
    val snackbarHostState = remember { SnackbarHostState() }

    var mostrarDialogoNueva by remember { mutableStateOf(false) }
    var metaEditando by remember { mutableStateOf<Meta?>(null) }
    var metaAccion by remember { mutableStateOf<Meta?>(null) }
    var tipoAccion by remember { mutableStateOf(TipoAccion.APORTAR) }
    var metaEliminar by remember { mutableStateOf<Meta?>(null) }

    LaunchedEffect(uiState.mensaje, uiState.error) {
        uiState.mensaje?.let { snackbarHostState.showSnackbar(it) }
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
        if (uiState.mensaje != null || uiState.error != null) viewModel.limpiarMensaje()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Metas de ahorro", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Huchas virtuales para tus sueños",
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
                onClick = { mostrarDialogoNueva = true },
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Nueva") },
                containerColor = colores.primario,
                contentColor = Color.White
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (metas.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Default.Savings,
                    null,
                    modifier = Modifier.size(80.dp),
                    tint = colores.primario.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Crea tu primera meta de ahorro",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Un viaje, un regalo especial, un fondo de emergencia... Define una cantidad objetivo y ve aportando poco a poco.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(metas, key = { it.id }) { meta ->
                    MetaCard(
                        meta = meta,
                        onEditar = { metaEditando = meta },
                        onEliminar = { metaEliminar = meta },
                        onAportar = {
                            metaAccion = meta
                            tipoAccion = TipoAccion.APORTAR
                        },
                        onRetirar = {
                            metaAccion = meta
                            tipoAccion = TipoAccion.RETIRAR
                        }
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (mostrarDialogoNueva) {
        DialogoMeta(
            meta = null,
            onDismiss = { mostrarDialogoNueva = false },
            onGuardar = { nombre, objetivo, icono, color, fechaLimite ->
                viewModel.crearMeta(nombre, objetivo, icono, color, fechaLimite)
                mostrarDialogoNueva = false
            }
        )
    }

    metaEditando?.let { meta ->
        DialogoMeta(
            meta = meta,
            onDismiss = { metaEditando = null },
            onGuardar = { nombre, objetivo, icono, color, fechaLimite ->
                viewModel.actualizarMeta(
                    meta.copy(
                        nombre = nombre,
                        objetivo = objetivo,
                        icono = icono,
                        color = color,
                        fechaLimite = fechaLimite
                    )
                )
                metaEditando = null
            }
        )
    }

    metaAccion?.let { meta ->
        DialogoMovimiento(
            meta = meta,
            tipoAccion = tipoAccion,
            onDismiss = { metaAccion = null },
            onConfirmar = { importe, nota ->
                if (tipoAccion == TipoAccion.APORTAR) viewModel.aportar(meta, importe, nota)
                else viewModel.retirar(meta, importe, nota)
                metaAccion = null
            }
        )
    }

    metaEliminar?.let { meta ->
        AlertDialog(
            onDismissRequest = { metaEliminar = null },
            title = { Text("Eliminar meta") },
            text = {
                Text("¿Seguro que quieres eliminar \"${meta.nombre}\"? Esta acción eliminará también todo su historial de aportes.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarMeta(meta)
                    metaEliminar = null
                }) { Text("Eliminar", color = AlertaAlta) }
            },
            dismissButton = {
                TextButton(onClick = { metaEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

private enum class TipoAccion { APORTAR, RETIRAR }

@Composable
private fun MetaCard(
    meta: Meta,
    onEditar: () -> Unit,
    onEliminar: () -> Unit,
    onAportar: () -> Unit,
    onRetirar: () -> Unit
) {
    val color = parseHex(meta.color)
    val porcentaje = if (meta.objetivo > 0) (meta.ahorrado / meta.objetivo).toFloat().coerceIn(0f, 1f) else 0f
    val completada = meta.ahorrado >= meta.objetivo

    Card(onClick = onEditar, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(IconHelper.get(meta.icono), null, tint = color, modifier = Modifier.size(28.dp))
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        meta.nombre,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${formatEur(meta.ahorrado)} de ${formatEur(meta.objetivo)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    meta.fechaLimite?.let { fecha ->
                        Text(
                            "🎯 Para el ${fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "${(porcentaje * 100).toInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (completada) ColorIngreso else color
                )
            }

            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { porcentaje },
                color = if (completada) ColorIngreso else color,
                trackColor = color.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
            )

            if (completada) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "🎉 ¡Meta alcanzada!",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorIngreso
                )
            }

            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onAportar,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = ColorIngreso.copy(alpha = 0.15f),
                        contentColor = ColorIngreso
                    )
                ) {
                    Icon(Icons.Default.ArrowUpward, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Aportar")
                }
                FilledTonalButton(
                    onClick = onRetirar,
                    enabled = meta.ahorrado > 0,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.ArrowDownward, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Retirar")
                }
                IconButton(onClick = onEliminar) {
                    Icon(
                        Icons.Default.Delete, "Eliminar",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DialogoMeta(
    meta: Meta?,
    onDismiss: () -> Unit,
    onGuardar: (String, Double, String, String, LocalDate?) -> Unit
) {
    var nombre by remember { mutableStateOf(meta?.nombre ?: "") }
    var objetivoStr by remember {
        mutableStateOf(meta?.objetivo?.let { "%.2f".format(Locale.US, it) } ?: "")
    }
    var icono by remember { mutableStateOf(meta?.icono ?: iconosDisponibles.first()) }
    var color by remember { mutableStateOf(meta?.color ?: paletaColoresMetas.first()) }
    var fechaLimite by remember { mutableStateOf(meta?.fechaLimite) }
    var mostrarDatePicker by remember { mutableStateOf(false) }

    val objetivo = objetivoStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val puedeGuardar = nombre.isNotBlank() && objetivo > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (meta != null) "Editar meta" else "Nueva meta") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre (ej: Viaje Japón)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = objetivoStr,
                    onValueChange = {
                        objetivoStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                    },
                    label = { Text("Objetivo a ahorrar") },
                    suffix = { Text("€") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedCard(
                    onClick = { mostrarDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(12.dp)) {
                        Text(
                            "Fecha límite (opcional)",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            fechaLimite?.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) ?: "Sin fecha",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                if (fechaLimite != null) {
                    TextButton(onClick = { fechaLimite = null }) {
                        Text("Quitar fecha")
                    }
                }

                Text("Icono", style = MaterialTheme.typography.labelLarge)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(iconosDisponibles) { iconoNombre ->
                        val seleccionado = icono == iconoNombre
                        val colorCirculo = parseHex(color)
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(
                                    if (seleccionado) colorCirculo.copy(alpha = 0.25f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                                .then(
                                    if (seleccionado)
                                        Modifier.border(2.dp, colorCirculo, CircleShape)
                                    else Modifier
                                )
                                .clickable { icono = iconoNombre },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                IconHelper.get(iconoNombre), null,
                                tint = if (seleccionado) colorCirculo else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Text("Color", style = MaterialTheme.typography.labelLarge)
                val ringColor = MaterialTheme.colorScheme.primary
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(paletaColoresMetas) { hex ->
                        val seleccionado = color == hex
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(parseHex(hex))
                                .then(
                                    if (seleccionado)
                                        Modifier.border(3.dp, ringColor, CircleShape)
                                    else Modifier
                                )
                                .clickable { color = hex }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onGuardar(nombre.trim(), objetivo, icono, color, fechaLimite) },
                enabled = puedeGuardar
            ) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )

    if (mostrarDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = fechaLimite
                ?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                ?: LocalDate.now().plusMonths(6)
                    .atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { mostrarDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        fechaLimite = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault()).toLocalDate()
                    }
                    mostrarDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDatePicker = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun DialogoMovimiento(
    meta: Meta,
    tipoAccion: TipoAccion,
    onDismiss: () -> Unit,
    onConfirmar: (Double, String) -> Unit
) {
    var importeStr by remember { mutableStateOf("") }
    var nota by remember { mutableStateOf("") }
    val importe = importeStr.replace(",", ".").toDoubleOrNull() ?: 0.0
    val puedeConfirmar = importe > 0 &&
            (tipoAccion == TipoAccion.APORTAR || importe <= meta.ahorrado)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (tipoAccion == TipoAccion.APORTAR)
                    "Aportar a \"${meta.nombre}\""
                else
                    "Retirar de \"${meta.nombre}\""
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Disponible: ${formatEur(meta.ahorrado)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = importeStr,
                    onValueChange = {
                        importeStr = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                    },
                    label = { Text("Importe") },
                    suffix = { Text("€") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = nota,
                    onValueChange = { nota = it },
                    label = { Text("Nota (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirmar(importe, nota.trim()) },
                enabled = puedeConfirmar
            ) {
                Text(if (tipoAccion == TipoAccion.APORTAR) "Aportar" else "Retirar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
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