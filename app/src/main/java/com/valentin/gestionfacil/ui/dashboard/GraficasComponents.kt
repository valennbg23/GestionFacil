package com.valentin.gestionfacil.ui.dashboard


import android.graphics.Color as AndroidColor
import android.graphics.Typeface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.ValueFormatter
import com.valentin.gestionfacil.data.db.dao.TotalPorCategoria
import com.valentin.gestionfacil.data.db.dao.TotalPorMes
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tarjeta con gráfica de tarta: distribución de gastos por categoría del mes.
 */
@Composable
fun GraficaTartaGastos(
    totales: List<TotalPorCategoria>,
    modifier: Modifier = Modifier
) {
    if (totales.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Gastos por categoría",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp),
                factory = { ctx ->
                    PieChart(ctx).apply {
                        description.isEnabled = false
                        isDrawHoleEnabled = true
                        holeRadius = 55f
                        transparentCircleRadius = 58f
                        setHoleColor(AndroidColor.TRANSPARENT)
                        setEntryLabelColor(AndroidColor.BLACK)
                        setEntryLabelTextSize(11f)
                        setEntryLabelTypeface(Typeface.DEFAULT_BOLD)
                        legend.apply {
                            verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                            horizontalAlignment = Legend.LegendHorizontalAlignment.CENTER
                            orientation = Legend.LegendOrientation.HORIZONTAL
                            isWordWrapEnabled = true
                            textSize = 10f
                        }
                        setUsePercentValues(true)
                    }
                },
                update = { chart ->
                    val entries = totales.map { PieEntry(it.total.toFloat(), it.categoriaNombre) }
                    val colores = totales.map { parseColorInt(it.categoriaColor) }
                    val dataSet = PieDataSet(entries, "").apply {
                        colors = colores
                        valueTextSize = 11f
                        valueTextColor = AndroidColor.BLACK
                        valueTypeface = Typeface.DEFAULT_BOLD
                        sliceSpace = 2f
                        valueFormatter = object : ValueFormatter() {
                            override fun getFormattedValue(value: Float): String =
                                "${value.toInt()}%"
                        }
                    }
                    chart.data = PieData(dataSet)
                    chart.invalidate()
                }
            )
        }
    }
}

/**
 * Tarjeta con gráfica de línea: evolución de ingresos vs gastos últimos 6 meses.
 */
@Composable
fun GraficaEvolucionMeses(
    totales: List<TotalPorMes>,
    modifier: Modifier = Modifier
) {
    if (totales.isEmpty()) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Evolución 6 meses",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                factory = { ctx ->
                    LineChart(ctx).apply {
                        description.isEnabled = false
                        setTouchEnabled(true)
                        setPinchZoom(false)
                        setDrawGridBackground(false)
                        axisRight.isEnabled = false
                        legend.textSize = 11f
                        xAxis.apply {
                            position = XAxis.XAxisPosition.BOTTOM
                            setDrawGridLines(false)
                            granularity = 1f
                            textSize = 10f
                        }
                        axisLeft.apply {
                            setDrawGridLines(true)
                            textSize = 10f
                        }
                    }
                },
                update = { chart ->
                    // Agrupar por periodo (yyyy-MM) y construir series
                    val periodos = totales.map { it.periodo }.distinct().sorted()
                    val etiquetas = periodos.map { periodo ->
                        try {
                            YearMonth.parse(periodo)
                                .format(DateTimeFormatter.ofPattern("MMM yy", Locale("es", "ES")))
                                .replaceFirstChar { it.uppercase() }
                        } catch (e: Exception) { periodo }
                    }

                    val ingresos = periodos.mapIndexed { idx, per ->
                        val total = totales.find { it.periodo == per && it.tipo == TipoMovimiento.INGRESO }?.total ?: 0.0
                        Entry(idx.toFloat(), total.toFloat())
                    }
                    val gastos = periodos.mapIndexed { idx, per ->
                        val total = totales.find { it.periodo == per && it.tipo == TipoMovimiento.GASTO }?.total ?: 0.0
                        Entry(idx.toFloat(), total.toFloat())
                    }

                    val dsIngresos = LineDataSet(ingresos, "Ingresos").apply {
                        color = AndroidColor.rgb(67, 160, 71)
                        setCircleColor(AndroidColor.rgb(67, 160, 71))
                        lineWidth = 2.5f
                        circleRadius = 4f
                        valueTextSize = 9f
                        setDrawValues(false)
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                    }
                    val dsGastos = LineDataSet(gastos, "Gastos").apply {
                        color = AndroidColor.rgb(229, 57, 53)
                        setCircleColor(AndroidColor.rgb(229, 57, 53))
                        lineWidth = 2.5f
                        circleRadius = 4f
                        valueTextSize = 9f
                        setDrawValues(false)
                        mode = LineDataSet.Mode.CUBIC_BEZIER
                    }

                    chart.data = LineData(dsIngresos, dsGastos)
                    chart.xAxis.valueFormatter = object : ValueFormatter() {
                        override fun getFormattedValue(value: Float): String =
                            etiquetas.getOrNull(value.toInt()) ?: ""
                    }
                    chart.xAxis.labelCount = etiquetas.size
                    chart.animateX(800)
                    chart.invalidate()
                }
            )
        }
    }
}

private fun parseColorInt(hex: String): Int = try {
    AndroidColor.parseColor(hex)
} catch (e: Exception) { AndroidColor.GRAY }