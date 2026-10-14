package com.valentin.gestionfacil.data.backup

import android.content.ContentResolver
import android.net.Uri
import com.valentin.gestionfacil.data.db.dao.BackupDao
import com.valentin.gestionfacil.data.db.dao.ContenidoBd
import com.valentin.gestionfacil.data.entity.Categoria
import com.valentin.gestionfacil.data.entity.Frecuencia
import com.valentin.gestionfacil.data.entity.Meta
import com.valentin.gestionfacil.data.entity.Movimiento
import com.valentin.gestionfacil.data.entity.MovimientoMeta
import com.valentin.gestionfacil.data.entity.Presupuesto
import com.valentin.gestionfacil.data.entity.Recurrente
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import com.valentin.gestionfacil.data.entity.TipoMovimientoMeta
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime

/** Cuántos datos se han guardado o restaurado (para el mensaje al usuario). */
data class ResumenBackup(
    val categorias: Int,
    val movimientos: Int,
    val presupuestos: Int,
    val metas: Int
)

/** Error con un mensaje pensado para mostrárselo directamente al usuario. */
class BackupException(mensaje: String, causa: Throwable? = null) : Exception(mensaje, causa)

/**
 * Crea y restaura copias de seguridad en un archivo JSON.
 *
 * El archivo lo elige el usuario con el selector del sistema (Descargas,
 * Google Drive...), así que no hace falta ningún permiso de almacenamiento.
 * Se usa org.json, que ya viene con Android: no añade librerías nuevas.
 */
class BackupManager(
    private val backupDao: BackupDao,
    private val contentResolver: ContentResolver
) {

    /** Guarda todos los datos de la app en el archivo [uri]. */
    suspend fun exportar(uri: Uri): ResumenBackup = withContext(Dispatchers.IO) {
        val contenido = backupDao.leerTodo()
        val texto = contenidoAJson(contenido).toString(2)

        val salida = contentResolver.openOutputStream(uri)
            ?: throw BackupException("No se pudo crear el archivo de la copia")
        salida.use { it.write(texto.toByteArray(Charsets.UTF_8)) }

        contenido.resumen()
    }

    /**
     * Sustituye todos los datos de la app por los del archivo [uri].
     * El archivo se lee y se valida entero ANTES de tocar la base de datos.
     */
    suspend fun restaurar(uri: Uri): ResumenBackup = withContext(Dispatchers.IO) {
        val texto = contentResolver.openInputStream(uri)
            ?.use { it.readBytes().toString(Charsets.UTF_8) }
            ?: throw BackupException("No se pudo abrir el archivo")

        val contenido = jsonAContenido(texto)

        try {
            backupDao.reemplazarTodo(contenido)
        } catch (e: Exception) {
            throw BackupException(
                "No se pudo restaurar la copia. Tus datos actuales no se han modificado",
                e
            )
        }

        contenido.resumen()
    }

    // ── Conversión a JSON ──────────────────────────────────────────────

    private fun contenidoAJson(c: ContenidoBd): JSONObject = JSONObject()
        .put(CLAVE_APP, NOMBRE_APP)
        .put(CLAVE_FORMATO, VERSION_FORMATO)
        .put("creadaEl", LocalDateTime.now().withNano(0).toString())
        .put("categorias", JSONArray(c.categorias.map { it.aJson() }))
        .put("movimientos", JSONArray(c.movimientos.map { it.aJson() }))
        .put("presupuestos", JSONArray(c.presupuestos.map { it.aJson() }))
        .put("metas", JSONArray(c.metas.map { it.aJson() }))
        .put("movimientosMeta", JSONArray(c.movimientosMeta.map { it.aJson() }))
        .put("recurrentes", JSONArray(c.recurrentes.map { it.aJson() }))

    // ── Lectura y validación ───────────────────────────────────────────

    private fun jsonAContenido(texto: String): ContenidoBd {
        val raiz = try {
            JSONObject(texto)
        } catch (e: Exception) {
            throw BackupException("Este archivo no es una copia de GestiónFácil", e)
        }

        if (raiz.optString(CLAVE_APP) != NOMBRE_APP) {
            throw BackupException("Este archivo no es una copia de GestiónFácil")
        }

        val formato = raiz.optInt(CLAVE_FORMATO, -1)
        if (formato > VERSION_FORMATO) {
            throw BackupException(
                "La copia se hizo con una versión más nueva de la app. Actualiza GestiónFácil e inténtalo de nuevo"
            )
        }
        if (formato < 1) {
            throw BackupException("El archivo de copia está dañado")
        }

        val contenido = try {
            ContenidoBd(
                categorias = raiz.getJSONArray("categorias").mapObjetos { it.aCategoria() },
                movimientos = raiz.getJSONArray("movimientos").mapObjetos { it.aMovimiento() },
                presupuestos = raiz.getJSONArray("presupuestos").mapObjetos { it.aPresupuesto() },
                metas = raiz.getJSONArray("metas").mapObjetos { it.aMeta() },
                movimientosMeta = raiz.getJSONArray("movimientosMeta").mapObjetos { it.aMovimientoMeta() },
                // Las copias del formato 1 (antes de los recurrentes) no tienen esta lista
                recurrentes = raiz.optJSONArray("recurrentes")?.mapObjetos { it.aRecurrente() }
                    ?: emptyList()
            )
        } catch (e: Exception) {
            throw BackupException("El archivo de copia está dañado o incompleto", e)
        }

        validar(contenido)
        return contenido
    }

    /** Comprueba que todas las referencias entre tablas apuntan a algo que existe. */
    private fun validar(c: ContenidoBd) {
        if (c.categorias.isEmpty()) {
            throw BackupException("La copia no contiene categorías")
        }

        val idsCategorias = c.categorias.map { it.id }.toSet()
        val idsMetas = c.metas.map { it.id }.toSet()

        val incoherente =
            c.movimientos.any { it.categoriaId !in idsCategorias } ||
                    c.presupuestos.any { it.categoriaId !in idsCategorias } ||
                    c.recurrentes.any { it.categoriaId !in idsCategorias } ||
                    c.movimientosMeta.any { it.metaId !in idsMetas }

        if (incoherente) {
            throw BackupException("La copia tiene datos incoherentes y no se ha restaurado")
        }
    }

    private fun ContenidoBd.resumen() = ResumenBackup(
        categorias = categorias.size,
        movimientos = movimientos.size,
        presupuestos = presupuestos.size,
        metas = metas.size
    )

    private companion object {
        const val CLAVE_APP = "app"
        const val NOMBRE_APP = "GestionFacil"
        const val CLAVE_FORMATO = "formato"

        /**
         * Súbelo si algún día cambia la estructura del archivo.
         * 1 = versión inicial · 2 = añade los movimientos recurrentes.
         */
        const val VERSION_FORMATO = 2
    }
}

// ── Utilidades de conversión ───────────────────────────────────────────

private inline fun <T> JSONArray.mapObjetos(convertir: (JSONObject) -> T): List<T> =
    List(length()) { i -> convertir(getJSONObject(i)) }

// Categoría
private fun Categoria.aJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("nombre", nombre)
    .put("icono", icono)
    .put("color", color)
    .put("esPredefinida", esPredefinida)

private fun JSONObject.aCategoria() = Categoria(
    id = getLong("id"),
    nombre = getString("nombre"),
    icono = getString("icono"),
    color = getString("color"),
    esPredefinida = getBoolean("esPredefinida")
)

// Movimiento
private fun Movimiento.aJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("importe", importe)
    .put("tipo", tipo.name)
    .put("categoriaId", categoriaId)
    .put("fecha", fecha.toString())
    .put("descripcion", descripcion)

private fun JSONObject.aMovimiento() = Movimiento(
    id = getLong("id"),
    importe = getDouble("importe"),
    tipo = TipoMovimiento.valueOf(getString("tipo")),
    categoriaId = getLong("categoriaId"),
    fecha = LocalDate.parse(getString("fecha")),
    descripcion = optString("descripcion", "")
)

// Presupuesto
private fun Presupuesto.aJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("categoriaId", categoriaId)
    .put("importeMaximo", importeMaximo)
    .put("mes", mes)
    .put("anio", anio)

private fun JSONObject.aPresupuesto() = Presupuesto(
    id = getLong("id"),
    categoriaId = getLong("categoriaId"),
    importeMaximo = getDouble("importeMaximo"),
    mes = getInt("mes"),
    anio = getInt("anio")
)

// Meta de ahorro
private fun Meta.aJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("nombre", nombre)
    .put("icono", icono)
    .put("color", color)
    .put("objetivo", objetivo)
    .put("ahorrado", ahorrado)
    .put("fechaLimite", fechaLimite?.toString() ?: JSONObject.NULL)
    .put("fechaCreacion", fechaCreacion.toString())

private fun JSONObject.aMeta() = Meta(
    id = getLong("id"),
    nombre = getString("nombre"),
    icono = getString("icono"),
    color = getString("color"),
    objetivo = getDouble("objetivo"),
    ahorrado = getDouble("ahorrado"),
    fechaLimite = if (isNull("fechaLimite")) null else LocalDate.parse(getString("fechaLimite")),
    fechaCreacion = LocalDate.parse(getString("fechaCreacion"))
)

// Movimiento de una meta (aporte o retiro)
private fun MovimientoMeta.aJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("metaId", metaId)
    .put("importe", importe)
    .put("tipo", tipo.name)
    .put("fecha", fecha.toString())
    .put("nota", nota)

private fun JSONObject.aMovimientoMeta() = MovimientoMeta(
    id = getLong("id"),
    metaId = getLong("metaId"),
    importe = getDouble("importe"),
    tipo = TipoMovimientoMeta.valueOf(getString("tipo")),
    fecha = LocalDate.parse(getString("fecha")),
    nota = optString("nota", "")
)

// Movimiento recurrente
private fun Recurrente.aJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("importe", importe)
    .put("tipo", tipo.name)
    .put("categoriaId", categoriaId)
    .put("descripcion", descripcion)
    .put("frecuencia", frecuencia.name)
    .put("fechaInicio", fechaInicio.toString())
    .put("ultimaFecha", ultimaFecha.toString())
    .put("activo", activo)

private fun JSONObject.aRecurrente() = Recurrente(
    id = getLong("id"),
    importe = getDouble("importe"),
    tipo = TipoMovimiento.valueOf(getString("tipo")),
    categoriaId = getLong("categoriaId"),
    descripcion = optString("descripcion", ""),
    frecuencia = Frecuencia.valueOf(getString("frecuencia")),
    fechaInicio = LocalDate.parse(getString("fechaInicio")),
    ultimaFecha = LocalDate.parse(getString("ultimaFecha")),
    activo = getBoolean("activo")
)