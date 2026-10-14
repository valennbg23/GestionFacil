package com.valentin.gestionfacil.ui.historial

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.valentin.gestionfacil.data.db.dao.MovimientoConCategoria
import com.valentin.gestionfacil.data.entity.Categoria
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import com.valentin.gestionfacil.ui.common.AppViewModelProvider
import com.valentin.gestionfacil.ui.common.ModoTema
import com.valentin.gestionfacil.ui.common.TemaViewModel
import com.valentin.gestionfacil.ui.theme.AlertaAlta
import com.valentin.gestionfacil.ui.theme.AlertaMedia
import com.valentin.gestionfacil.ui.theme.AlertaOk
import com.valentin.gestionfacil.ui.theme.ColorGasto
import com.valentin.gestionfacil.ui.theme.ColorIngreso
import com.valentin.gestionfacil.ui.theme.Pantalla
import com.valentin.gestionfacil.ui.theme.coloresDe
import com.valentin.gestionfacil.utils.IconHelper
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

private val ES = Locale("es", "ES")

private val nombresMes = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

private val inicialesSemana = listOf("L", "M", "X", "J", "V", "S", "D")

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
    val focusManager = LocalFocusManager.current
    val filtros = state.filtros

    // El calendario se puede plegar tocando el nombre del mes
    var calendarioAbierto by rememberSaveable { mutableStateOf(true) }

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

            // ── Buscador (fijo arriba) ──────────────────────────────
            OutlinedTextField(
                value = filtros.busqueda,
                onValueChange = viewModel::setBusqueda,
                placeholder = { Text("Buscar (ej. café, Netflix, 12,50)", maxLines = 1) },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                trailingIcon = {
                    if (filtros.buscando) {
                        IconButton(onClick = {
                            viewModel.setBusqueda("")
                            focusManager.clearFocus()
                        }) {
                            Icon(Icons.Default.Close, "Borrar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colores.primario,
                    focusedLeadingIconColor = colores.primario,
                    cursorColor = colores.primario
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 4.dp)
            )

            // ── Todo lo demás se desplaza junto con la lista ────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Mes (o aviso de búsqueda)
                item {
                    if (filtros.buscando) {
                        Text(
                            "Buscando en todos los meses",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    } else {
                        CabeceraMes(
                            mes = filtros.mes,
                            calendarioAbierto = calendarioAbierto,
                            puedeAvanzar = !state.esMesActual,
                            colorPrimario = colores.primario,
                            onAnterior = viewModel::mesAnterior,
                            onSiguiente = viewModel::mesSiguiente,
                            onAlternarCalendario = { calendarioAbierto = !calendarioAbierto }
                        )
                    }
                }

                // Tipo: Todos / Gastos / Ingresos
                item {
                    SelectorTipo(
                        tipo = filtros.tipo,
                        colorPrimario = colores.primario,
                        onCambiar = viewModel::setTipo
                    )
                }

                // Calendario del mes (plegable)
                if (!filtros.buscando) {
                    item {
                        AnimatedVisibility(
                            visible = calendarioAbierto,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            CalendarioMes(
                                mes = filtros.mes,
                                diaSeleccionado = filtros.dia,
                                niveles = state.nivelesPorDia,
                                colorPrimario = colores.primario,
                                onDia = viewModel::seleccionarDia
                            )
                        }
                    }
                }

                // Categorías en círculos
                item {
                    SelectorCategorias(
                        categorias = state.categorias,
                        seleccionada = filtros.categoriaId,
                        colorPrimario = colores.primario,
                        onCambiar = viewModel::setCategoria
                    )
                }

                item { HorizontalDivider(Modifier.padding(top = 4.dp)) }

                // Resumen: resultados de la búsqueda o del día elegido
                val diaElegido = filtros.dia
                if (filtros.buscando) {
                    item {
                        ResumenFiltro(
                            titulo = plural(state.movimientos.size, "resultado", "resultados"),
                            movimientos = state.movimientos
                        )
                    }
                } else if (diaElegido != null) {
                    item {
                        ResumenFiltro(
                            titulo = formatearDia(diaElegido),
                            movimientos = state.movimientos
                        )
                    }
                }

                // Lista de movimientos
                if (state.movimientos.isEmpty()) {
                    item {
                        Text(
                            when {
                                filtros.buscando ->
                                    "No hay resultados para \"${filtros.busqueda.trim()}\""
                                filtros.dia != null -> "Ese día no hay movimientos"
                                else -> "No hay movimientos con estos filtros"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)
                        )
                    }
                } else {
                    items(state.movimientos, key = { it.id }) { mov ->
                        MovimientoRow(mov, onClick = { onEditarMovimiento(mov.id) })
                    }
                }
            }
        }
    }
}

// ── Cabecera del mes ───────────────────────────────────────────────────

@Composable
private fun CabeceraMes(
    mes: YearMonth,
    calendarioAbierto: Boolean,
    puedeAvanzar: Boolean,
    colorPrimario: Color,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit,
    onAlternarCalendario: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onAnterior) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior")
        }

        // Al tocar el nombre del mes se pliega o despliega el calendario
        Row(
            Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(onClick = onAlternarCalendario)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "${nombresMes[mes.monthValue - 1]} ${mes.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                if (calendarioAbierto) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (calendarioAbierto) "Plegar calendario" else "Desplegar calendario",
                tint = colorPrimario
            )
        }

        IconButton(onClick = onSiguiente, enabled = puedeAvanzar) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente")
        }
    }
}

// ── Selector de tipo ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelectorTipo(
    tipo: FiltroTipo,
    colorPrimario: Color,
    onCambiar: (FiltroTipo) -> Unit
) {
    val opciones = listOf(
        FiltroTipo.TODOS to "Todos",
        FiltroTipo.GASTOS to "Gastos",
        FiltroTipo.INGRESOS to "Ingresos"
    )
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        opciones.forEachIndexed { indice, (valor, etiqueta) ->
            SegmentedButton(
                selected = tipo == valor,
                onClick = { onCambiar(valor) },
                shape = SegmentedButtonDefaults.itemShape(indice, opciones.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = colorPrimario.copy(alpha = 0.15f),
                    activeContentColor = colorPrimario,
                    activeBorderColor = colorPrimario
                )
            ) { Text(etiqueta) }
        }
    }
}

// ── Calendario ─────────────────────────────────────────────────────────

@Composable
private fun CalendarioMes(
    mes: YearMonth,
    diaSeleccionado: LocalDate?,
    niveles: Map<Int, NivelDia>,
    colorPrimario: Color,
    onDia: (LocalDate) -> Unit
) {
    val hoy = LocalDate.now()
    // Huecos antes del día 1 (la semana empieza en lunes)
    val huecos = mes.atDay(1).dayOfWeek.value - 1
    val totalCeldas = huecos + mes.lengthOfMonth()
    val semanas = (totalCeldas + 6) / 7

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(horizontal = 8.dp, vertical = 12.dp)) {
            // Iniciales de los días de la semana
            Row(Modifier.fillMaxWidth()) {
                inicialesSemana.forEach { inicial ->
                    Text(
                        inicial,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(6.dp))

            // Semanas
            for (semana in 0 until semanas) {
                Row(Modifier.fillMaxWidth()) {
                    for (columna in 0 until 7) {
                        val numero = semana * 7 + columna - huecos + 1
                        Box(
                            Modifier.weight(1f).height(44.dp).padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (numero in 1..mes.lengthOfMonth()) {
                                val fecha = mes.atDay(numero)
                                CeldaDia(
                                    numero = numero,
                                    nivel = niveles[numero],
                                    seleccionado = fecha == diaSeleccionado,
                                    esHoy = fecha == hoy,
                                    esFuturo = fecha.isAfter(hoy),
                                    colorPrimario = colorPrimario,
                                    onClick = { onDia(fecha) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            LeyendaCalendario(mostrarIngresos = niveles.values.any { it == NivelDia.INGRESO })
        }
    }
}

@Composable
private fun CeldaDia(
    numero: Int,
    nivel: NivelDia?,
    seleccionado: Boolean,
    esHoy: Boolean,
    esFuturo: Boolean,
    colorPrimario: Color,
    onClick: () -> Unit
) {
    val forma = RoundedCornerShape(10.dp)
    Column(
        Modifier
            .fillMaxSize()
            .clip(forma)
            .background(if (seleccionado) colorPrimario else Color.Transparent)
            .then(
                if (esHoy && !seleccionado) Modifier.border(1.dp, colorPrimario, forma)
                else Modifier
            )
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "$numero",
            fontSize = 13.sp,
            fontWeight = if (seleccionado || esHoy) FontWeight.SemiBold else FontWeight.Normal,
            color = when {
                seleccionado -> Color.White
                esFuturo -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
        Spacer(Modifier.height(3.dp))
        // Puntito de gasto (o hueco del mismo tamaño para que no baile el número)
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(
                    when {
                        nivel == null -> Color.Transparent
                        seleccionado -> Color.White
                        else -> colorNivel(nivel)
                    }
                )
        )
    }
}

@Composable
private fun LeyendaCalendario(mostrarIngresos: Boolean) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (mostrarIngresos) {
            ElementoLeyenda(ColorIngreso, "Días con ingresos")
        } else {
            ElementoLeyenda(AlertaOk, "Poco")
            Spacer(Modifier.width(16.dp))
            ElementoLeyenda(AlertaMedia, "Normal")
            Spacer(Modifier.width(16.dp))
            ElementoLeyenda(AlertaAlta, "Mucho")
        }
    }
}

@Composable
private fun ElementoLeyenda(color: Color, texto: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(Modifier.width(4.dp))
        Text(
            texto,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun colorNivel(nivel: NivelDia): Color = when (nivel) {
    NivelDia.BAJO -> AlertaOk
    NivelDia.MEDIO -> AlertaMedia
    NivelDia.ALTO -> AlertaAlta
    NivelDia.INGRESO -> ColorIngreso
}

// ── Categorías ─────────────────────────────────────────────────────────

@Composable
private fun SelectorCategorias(
    categorias: List<Categoria>,
    seleccionada: Long?,
    colorPrimario: Color,
    onCambiar: (Long?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 4.dp)
    ) {
        item {
            CirculoCategoria(
                nombre = "Todas",
                icono = { Icon(Icons.Default.Apps, null, tint = colorPrimario) },
                color = colorPrimario,
                seleccionada = seleccionada == null,
                onClick = { onCambiar(null) }
            )
        }
        items(categorias, key = { it.id }) { cat ->
            val color = parseHex(cat.color)
            CirculoCategoria(
                nombre = cat.nombre,
                icono = { Icon(IconHelper.get(cat.icono), null, tint = color) },
                color = color,
                seleccionada = seleccionada == cat.id,
                // Tocar la categoría ya elegida la quita
                onClick = { onCambiar(if (seleccionada == cat.id) null else cat.id) }
            )
        }
    }
}

@Composable
private fun CirculoCategoria(
    nombre: String,
    icono: @Composable () -> Unit,
    color: Color,
    seleccionada: Boolean,
    onClick: () -> Unit
) {
    Column(
        Modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (seleccionada) 0.30f else 0.12f))
                .then(
                    if (seleccionada) Modifier.border(BorderStroke(2.dp, color), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) { icono() }
        Spacer(Modifier.height(4.dp))
        // Nombre en 2 líneas como mucho ("Comida y / restaurantes")
        Text(
            nombre,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (seleccionada) FontWeight.SemiBold else FontWeight.Normal,
            color = if (seleccionada) color else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

// ── Resumen y lista ────────────────────────────────────────────────────

/** Título + cuánto suman los gastos e ingresos de lo que se está viendo. */
@Composable
private fun ResumenFiltro(titulo: String, movimientos: List<MovimientoConCategoria>) {
    val totalGastos = movimientos.filter { it.tipo == TipoMovimiento.GASTO }.sumOf { it.importe }
    val totalIngresos = movimientos.filter { it.tipo == TipoMovimiento.INGRESO }.sumOf { it.importe }

    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            titulo,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f)
        )
        if (totalGastos > 0) {
            Text(
                "-" + formatEur(totalGastos),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = ColorGasto
            )
        }
        if (totalIngresos > 0) {
            Text(
                "+" + formatEur(totalIngresos),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = ColorIngreso
            )
        }
    }
}

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

// ── Utilidades ─────────────────────────────────────────────────────────

/** "Viernes 10 de octubre" */
private fun formatearDia(fecha: LocalDate): String =
    fecha.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES))
        .replaceFirstChar { it.titlecase(ES) }

private fun plural(n: Int, singular: String, plural: String): String =
    "$n ${if (n == 1) singular else plural}"

private fun esModoOscuro(modo: ModoTema): Boolean? = when (modo) {
    ModoTema.SISTEMA -> null
    ModoTema.CLARO -> false
    ModoTema.OSCURO -> true
}

private fun formatEur(amount: Double): String {
    val nf = NumberFormat.getCurrencyInstance(ES)
    nf.currency = Currency.getInstance("EUR")
    return nf.format(amount)
}

private fun parseHex(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) { Color.Gray }