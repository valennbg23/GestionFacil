package com.valentin.gestionfacil.utils

import android.content.Context
import android.graphics.BitmapFactory
import android.media.ExifInterface
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.IOException
import java.time.LocalDate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Lee el texto de la foto de un ticket con ML Kit de Google.
 * Funciona sin internet: el modelo va dentro de la app.
 */
object LectorTickets {

    /** Las fotos se reducen a este tamaño como máximo: sobra para leer y no llena la memoria. */
    private const val LADO_MAXIMO = 3000

    suspend fun leer(context: Context, uri: Uri): DatosTicket {
        val imagen = withContext(Dispatchers.IO) { cargarImagen(context, uri) }
        val reconocedor = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            val texto = reconocedor.process(imagen).esperar()
            val filas = AnalizadorTicket.agruparEnFilas(trozosDe(texto))
            return AnalizadorTicket.analizar(filas, LocalDate.now())
        } finally {
            reconocedor.close()
        }
    }

    private fun cargarImagen(context: Context, uri: Uri): InputImage {
        val resolver = context.contentResolver

        // 1. Mide la foto sin cargarla entera
        val medidas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, medidas) }
        var reduccion = 1
        while (maxOf(medidas.outWidth, medidas.outHeight) / reduccion > LADO_MAXIMO) reduccion *= 2

        // 2. La carga ya reducida
        val opciones = BitmapFactory.Options().apply { inSampleSize = reduccion }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opciones) }
            ?: throw IOException("No se ha podido abrir la imagen")

        // 3. Si la cámara guardó la foto girada, se le dice a ML Kit para que la enderece
        val giro = runCatching {
            resolver.openInputStream(uri)?.use { entrada ->
                when (ExifInterface(entrada).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                )) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            }
        }.getOrNull() ?: 0

        return InputImage.fromBitmap(bitmap, giro)
    }

    /**
     * Convierte lo que ha leído ML Kit en trozos con su posición.
     * Si la foto está algo torcida, se "endereza" para que cada fila quede a la misma altura.
     */
    private fun trozosDe(texto: Text): List<TrozoTicket> {
        val lineas = texto.textBlocks.flatMap { it.lines }

        // Inclinación de la foto: la mediana del ángulo de todas las líneas
        val angulos = lineas.mapNotNull { linea ->
            val p = linea.cornerPoints
            if (p == null || p.size < 4) null
            else atan2((p[1].y - p[0].y).toDouble(), (p[1].x - p[0].x).toDouble())
        }.sorted()
        val angulo = if (angulos.isEmpty()) 0.0 else angulos[angulos.size / 2]
        val seno = sin(angulo)
        val coseno = cos(angulo)

        return lineas.mapNotNull { linea ->
            val caja = linea.boundingBox ?: return@mapNotNull null
            val p = linea.cornerPoints
            val alto = if (p != null && p.size >= 4)
                hypot((p[3].x - p[0].x).toDouble(), (p[3].y - p[0].y).toDouble())
            else
                caja.height().toDouble()

            val cx = caja.exactCenterX().toDouble()
            val cy = caja.exactCenterY().toDouble()
            TrozoTicket(
                texto = linea.text,
                x = cx * coseno + cy * seno,
                y = -cx * seno + cy * coseno,
                alto = alto
            )
        }
    }

    /** Espera al resultado de ML Kit sin bloquear la app. */
    private suspend fun <T> Task<T>.esperar(): T = suspendCancellableCoroutine { cont ->
        addOnSuccessListener { cont.resume(it) }
        addOnFailureListener { cont.resumeWithException(it) }
    }
}