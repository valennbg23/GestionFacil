package com.valentin.gestionfacil.ui.ajustes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valentin.gestionfacil.GestionFacilApp
import com.valentin.gestionfacil.data.entity.Categoria
import com.valentin.gestionfacil.ui.common.AppViewModelProvider
import com.valentin.gestionfacil.ui.common.ModoTema
import com.valentin.gestionfacil.ui.common.TemaViewModel
import com.valentin.gestionfacil.ui.theme.Pantalla
import com.valentin.gestionfacil.ui.theme.coloresDe
import com.valentin.gestionfacil.utils.BiometricHelper
import com.valentin.gestionfacil.utils.IconHelper
import com.valentin.gestionfacil.utils.PdfExporter
import kotlinx.coroutines.launch

private val paletaColores = listOf(
    "#FF6B6B", "#4ECDC4", "#45B7D1", "#F7B801", "#A4C639",
    "#E84855", "#9B5DE5", "#00BBF9", "#FF8C42", "#6A4C93",
    "#8D99AE", "#06A77D", "#118AB2", "#4D908E", "#F15BB5"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    viewModel: AjustesViewModel = viewModel(factory = AppViewModelProvider.Factory),
    temaViewModel: TemaViewModel = viewModel(factory = AppViewModelProvider.Factory)
) {
    val categorias by viewModel.categorias.collectAsStateWithLifecycle()
    val uiState by viewModel.ui.collectAsStateWithLifecycle()
    val modoTema by temaViewModel.modo.collectAsStateWithLifecycle()
    val esOscuro = esModoOscuro(modoTema)
    val colores = coloresDe(Pantalla.AJUSTES, forzarTemaOscuro = esOscuro)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val preferencias = (context.applicationContext as GestionFacilApp).container.preferencias

    var bloqueoActivado by remember { mutableStateOf(preferencias.bloqueoActivado) }
    var mostrarDialogoNueva by remember { mutableStateOf(false) }
    var categoriaEditando by remember { mutableStateOf<Categoria?>(null) }
    var categoriaEliminar by remember { mutableStateOf<Categoria?>(null) }

    LaunchedEffect(uiState.mensaje, uiState.error) {
        uiState.mensaje?.let { snackbarHostState.showSnackbar(it) }
        uiState.error?.let { snackbarHostState.showSnackbar(it) }
        if (uiState.mensaje != null || uiState.error != null) viewModel.limpiarMensaje()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colores.contenedor,
                    titleContentColor = colores.onContenedor
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── APARIENCIA ─────────────────────────────────────────
            item {
                Text(
                    "Apariencia",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colores.primario
                )
            }

            item {
                TarjetaApariencia(
                    modoActual = modoTema,
                    colorPrimario = colores.primario,
                    onCambiarModo = { temaViewModel.cambiarModo(it) }
                )
            }

            // ── SEGURIDAD ─────────────────────────────────────────
            item {
                Text(
                    "Seguridad",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colores.primario,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                TarjetaSeguridad(
                    activado = bloqueoActivado,
                    colorPrimario = colores.primario,
                    onCambio = { nuevoValor ->
                        if (nuevoValor) {
                            if (BiometricHelper.esBiometriaDisponible(context)) {
                                bloqueoActivado = true
                                preferencias.bloqueoActivado = true
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Bloqueo activado. Se pedirá al abrir la app."
                                    )
                                }
                            } else {
                                scope.launch {
                                    snackbarHostState.showSnackbar(
                                        "Configura primero huella o PIN en tu móvil"
                                    )
                                }
                            }
                        } else {
                            bloqueoActivado = false
                            preferencias.bloqueoActivado = false
                            scope.launch {
                                snackbarHostState.showSnackbar("Bloqueo desactivado")
                            }
                        }
                    }
                )
            }

            // ── EXPORTAR PDF ──────────────────────────────────────
            item {
                Text(
                    "Exportar datos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colores.primario,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            item {
                Card(
                    onClick = {
                        scope.launch {
                            val datos = viewModel.obtenerDatosResumenActual()
                            PdfExporter.generarYCompartir(context, datos)
                        }
                    }
                ) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Default.PictureAsPdf, null,
                            tint = colores.primario,
                            modifier = Modifier.size(32.dp)
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Exportar PDF",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "Resumen del mes actual",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── CATEGORÍAS ────────────────────────────────────────
            item {
                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Gestión de categorías",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colores.primario,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = { mostrarDialogoNueva = true },
                        colors = IconButtonDefaults.filledTonalIconButtonColors(
                            containerColor = colores.primario.copy(alpha = 0.18f),
                            contentColor = colores.primario
                        )
                    ) {
                        Icon(Icons.Default.Add, "Añadir categoría")
                    }
                }
            }

            items(categorias, key = { it.id }) { cat ->
                CategoriaCard(
                    cat = cat,
                    onClick = { if (!cat.esPredefinida) categoriaEditando = cat },
                    onEliminar = { if (!cat.esPredefinida) categoriaEliminar = cat }
                )
            }

            item {
                Spacer(Modifier.height(16.dp))
                Text(
                    "GestiónFácil v1.0 · Valentín Borreguero González · TFC DAM 2025/2026",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    if (mostrarDialogoNueva) {
        DialogoCategoria(
            categoria = null,
            onDismiss = { mostrarDialogoNueva = false },
            onGuardar = { nombre, color ->
                viewModel.insertarCategoria(nombre, color)
                mostrarDialogoNueva = false
            }
        )
    }

    categoriaEditando?.let { cat ->
        DialogoCategoria(
            categoria = cat,
            onDismiss = { categoriaEditando = null },
            onGuardar = { nombre, color ->
                viewModel.actualizarCategoria(cat.copy(nombre = nombre, color = color))
                categoriaEditando = null
            }
        )
    }

    categoriaEliminar?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoriaEliminar = null },
            title = { Text("Eliminar categoría") },
            text = {
                Text("¿Seguro que quieres eliminar \"${cat.nombre}\"? Si tiene movimientos asociados no se podrá eliminar.")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.eliminarCategoria(cat)
                    categoriaEliminar = null
                }) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { categoriaEliminar = null }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun TarjetaSeguridad(
    activado: Boolean,
    colorPrimario: Color,
    onCambio: (Boolean) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.Fingerprint,
                null,
                tint = colorPrimario,
                modifier = Modifier.size(32.dp)
            )
            Column(Modifier.weight(1f)) {
                Text("Bloqueo con huella o PIN", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Pide tu identidad al abrir la app",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = activado,
                onCheckedChange = onCambio,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = colorPrimario
                )
            )
        }
    }
}

@Composable
private fun TarjetaApariencia(
    modoActual: ModoTema,
    colorPrimario: Color,
    onCambiarModo: (ModoTema) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Brightness6,
                    null,
                    tint = colorPrimario,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("Tema de la app", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Elige cómo quieres ver la app",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BotonTema(
                    modifier = Modifier.weight(1f),
                    icono = Icons.Default.LightMode,
                    label = "Claro",
                    seleccionado = modoActual == ModoTema.CLARO,
                    colorPrimario = colorPrimario,
                    onClick = { onCambiarModo(ModoTema.CLARO) }
                )
                BotonTema(
                    modifier = Modifier.weight(1f),
                    icono = Icons.Default.DarkMode,
                    label = "Oscuro",
                    seleccionado = modoActual == ModoTema.OSCURO,
                    colorPrimario = colorPrimario,
                    onClick = { onCambiarModo(ModoTema.OSCURO) }
                )
                BotonTema(
                    modifier = Modifier.weight(1f),
                    icono = Icons.Default.PhoneAndroid,
                    label = "Sistema",
                    seleccionado = modoActual == ModoTema.SISTEMA,
                    colorPrimario = colorPrimario,
                    onClick = { onCambiarModo(ModoTema.SISTEMA) }
                )
            }
        }
    }
}

@Composable
private fun BotonTema(
    modifier: Modifier,
    icono: ImageVector,
    label: String,
    seleccionado: Boolean,
    colorPrimario: Color,
    onClick: () -> Unit
) {
    val colorFondo = if (seleccionado)
        colorPrimario.copy(alpha = 0.18f)
    else
        MaterialTheme.colorScheme.surfaceVariant
    val colorTexto = if (seleccionado)
        colorPrimario
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        color = colorFondo,
        shape = MaterialTheme.shapes.medium,
        border = if (seleccionado)
            androidx.compose.foundation.BorderStroke(2.dp, colorPrimario)
        else null
    ) {
        Column(
            Modifier.padding(8.dp).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icono, null, tint = colorTexto, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = colorTexto,
                fontWeight = if (seleccionado) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

@Composable
private fun CategoriaCard(
    cat: Categoria,
    onClick: () -> Unit,
    onEliminar: () -> Unit
) {
    val color = parseHex(cat.color)
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) { Icon(IconHelper.get(cat.icono), null, tint = color) }
            Column(Modifier.weight(1f)) {
                Text(cat.nombre, style = MaterialTheme.typography.titleSmall)
                Text(
                    if (cat.esPredefinida) "Predefinida" else "Personalizada",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (!cat.esPredefinida) {
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

@Composable
private fun DialogoCategoria(
    categoria: Categoria?,
    onDismiss: () -> Unit,
    onGuardar: (String, String) -> Unit
) {
    var nombre by remember { mutableStateOf(categoria?.nombre ?: "") }
    var color by remember { mutableStateOf(categoria?.color ?: paletaColores.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (categoria != null) "Editar categoría" else "Nueva categoría") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Color", style = MaterialTheme.typography.labelLarge)
                val ringColor = MaterialTheme.colorScheme.primary
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(paletaColores) { hex ->
                        val seleccionado = color == hex
                        Box(
                            modifier = Modifier
                                .size(40.dp)
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
                onClick = { onGuardar(nombre.trim(), color) },
                enabled = nombre.isNotBlank()
            ) { Text("Guardar") }
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

private fun parseHex(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) { Color.Gray }