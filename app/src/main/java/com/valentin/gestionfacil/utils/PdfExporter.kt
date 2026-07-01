package com.valentin.gestionfacil.utils

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.valentin.gestionfacil.data.db.dao.TotalPorCategoria
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale

data class DatosResumenMes(
    val anio: Int,
    val mes: Int,
    val ingresos: Double,
    val gastos: Double,
    val totalesPorCategoria: List<TotalPorCategoria>
)

/**
 * Genera un PDF con el resumen mensual usando PdfDocument nativo (sin librerías externas).
 * El archivo se guarda en filesDir/pdfs/ y se comparte con Intent.ACTION_SEND.
 */
object PdfExporter {

    private val nombresMes = listOf(
        "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
        "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
    )

    fun generarYCompartir(context: Context, datos: DatosResumenMes) {
        val file = generar(context, datos)
        compartir(context, file)
    }

    private fun generar(context: Context, datos: DatosResumenMes): File {
        val document = PdfDocument()
        // A4 a 72 dpi = 595 x 842 puntos
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = document.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = android.graphics.Color.rgb(46, 125, 50)
            textSize = 26f
            isFakeBoldText = true
        }
        val subtitlePaint = Paint().apply {
            color = android.graphics.Color.rgb(21, 101, 192)
            textSize = 16f
            isFakeBoldText = true
        }
        val textPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 12f
        }
        val textBold = Paint(textPaint).apply { isFakeBoldText = true }

        var y = 60f
        canvas.drawText("GestiónFácil", 40f, y, titlePaint)
        y += 28f
        canvas.drawText(
            "Resumen mensual — ${nombresMes[datos.mes - 1]} ${datos.anio}",
            40f, y, subtitlePaint
        )
        y += 40f

        // Balance
        canvas.drawText("Balance del mes", 40f, y, subtitlePaint)
        y += 22f
        canvas.drawText("Ingresos:", 40f, y, textBold)
        canvas.drawText(formatEur(datos.ingresos), 220f, y, textPaint)
        y += 18f
        canvas.drawText("Gastos:", 40f, y, textBold)
        canvas.drawText(formatEur(datos.gastos), 220f, y, textPaint)
        y += 18f
        canvas.drawText("Saldo:", 40f, y, textBold)
        val saldo = datos.ingresos - datos.gastos
        val saldoPaint = Paint(textPaint).apply {
            color = if (saldo >= 0) android.graphics.Color.rgb(46, 125, 50)
            else android.graphics.Color.rgb(211, 47, 47)
            isFakeBoldText = true
        }
        canvas.drawText(formatEur(saldo), 220f, y, saldoPaint)
        y += 36f

        // Detalle por categoría
        canvas.drawText("Gastos por categoría", 40f, y, subtitlePaint)
        y += 22f

        if (datos.totalesPorCategoria.isEmpty()) {
            canvas.drawText("No se han registrado gastos este mes.", 40f, y, textPaint)
        } else {
            // Cabecera de tabla
            canvas.drawText("Categoría", 40f, y, textBold)
            canvas.drawText("Importe", 320f, y, textBold)
            canvas.drawText("% del total", 460f, y, textBold)
            y += 18f

            val totalGastos = datos.gastos.takeIf { it > 0 } ?: 1.0
            datos.totalesPorCategoria.forEach { t ->
                if (y > 780f) return@forEach  // límite de página
                val pct = (t.total / totalGastos * 100).toInt()
                canvas.drawText(t.categoriaNombre, 40f, y, textPaint)
                canvas.drawText(formatEur(t.total), 320f, y, textPaint)
                canvas.drawText("$pct%", 460f, y, textPaint)
                y += 18f
            }
        }

        // Pie de página
        val pie = Paint(textPaint).apply {
            color = android.graphics.Color.GRAY
            textSize = 10f
        }
        val hoy = LocalDate.now()
            .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
        canvas.drawText(
            "Generado el $hoy  ·  GestiónFácil",
            40f, 820f, pie
        )

        document.finishPage(page)

        val dir = File(context.filesDir, "pdfs").apply { mkdirs() }
        val fileName = "GestionFacil_${datos.anio}_${"%02d".format(datos.mes)}.pdf"
        val file = File(dir, fileName)
        FileOutputStream(file).use { document.writeTo(it) }
        document.close()
        return file
    }

    private fun compartir(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Compartir resumen PDF").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun formatEur(amount: Double): String {
        val nf = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
        nf.currency = Currency.getInstance("EUR")
        return nf.format(amount)
    }
}