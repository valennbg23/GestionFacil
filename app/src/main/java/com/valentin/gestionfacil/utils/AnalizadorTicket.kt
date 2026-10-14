package com.valentin.gestionfacil.utils

import com.valentin.gestionfacil.data.entity.Categoria
import java.text.Normalizer
import java.time.LocalDate
import kotlin.math.abs

/** Lo que se ha podido sacar de un ticket. Lo que no se encuentre queda a null. */
data class DatosTicket(
    val importe: Double?,
    val fecha: LocalDate?,
    val comercio: String?,
    /** Todo el texto del ticket, fila a fila (sirve para adivinar la categoría). */
    val filas: List<String>
) {
    val vacio: Boolean get() = importe == null && fecha == null && comercio == null
}

/** Un trozo de texto leído del ticket, con su posición en la foto (ya enderezada). */
data class TrozoTicket(
    val texto: String,
    val x: Double,
    val y: Double,
    val alto: Double
)

/**
 * Saca el total, la fecha y el nombre de la tienda del texto de un ticket.
 * No depende de Android: solo trabaja con el texto ya leído.
 */
object AnalizadorTicket {

    // ── Filas ────────────────────────────────────────────────────────────

    /**
     * En los tickets el nombre del producto va a la izquierda y el precio a la derecha,
     * y a veces se leen como trozos separados. Aquí se juntan los que están a la misma
     * altura para recuperar cada fila completa ("TOTAL (€)   45,67").
     */
    fun agruparEnFilas(trozos: List<TrozoTicket>): List<String> {
        if (trozos.isEmpty()) return emptyList()

        val altos = trozos.map { it.alto }.sorted()
        val altoTipico = altos[altos.size / 2]

        val filas = mutableListOf<MutableList<TrozoTicket>>()
        var yFila = 0.0
        for (trozo in trozos.sortedBy { it.y }) {
            val fila = filas.lastOrNull()
            if (fila != null && abs(trozo.y - yFila) < altoTipico * 0.6) {
                fila.add(trozo)
                yFila = fila.sumOf { it.y } / fila.size
            } else {
                filas.add(mutableListOf(trozo))
                yFila = trozo.y
            }
        }
        return filas.map { fila -> fila.sortedBy { it.x }.joinToString(" ") { it.texto } }
    }

    fun analizar(filas: List<String>, hoy: LocalDate): DatosTicket = DatosTicket(
        importe = buscarTotal(filas),
        fecha = buscarFecha(filas, hoy),
        comercio = buscarComercio(filas),
        filas = filas
    )

    // ── Total ────────────────────────────────────────────────────────────

    /** Importes tipo 45,67 · 45.67 · 1.234,56 (pero no fechas como 15.10.26 ni porcentajes). */
    private val regexImporte =
        Regex("""(?<![\d.,/])(\d{1,3}(?:\.\d{3})+|\d+)\s?[,.]\s?(\d{2})(?!\d|\s?%|[.,/:-]\d)""")

    /** Filas que suelen llevar el total. */
    private val clavesTotal = listOf("TOTAL", "IMPORTE", "A PAGAR", "TARJETA", "PAGADO", "VISA", "MASTERCARD")

    /** Filas que tienen importes que NO son el total (dinero entregado, cambio, IVA...). */
    private val clavesExcluidas = listOf(
        "SUBTOTAL", "SUB TOTAL", "BASE", "CUOTA", "ENTREGADO", "EFECTIVO", "CAMBIO", "DEVOL",
        "AHORR", "DESCUENTO", "DTO", "PUNTOS", "SALDO", "PROPINA", "ACUMULAD", "HORA"
    )

    private fun buscarTotal(filas: List<String>): Double? {
        val normalizadas = filas.map { normalizar(it) }
        fun excluida(i: Int) = clavesExcluidas.any { normalizadas[i].contains(it) }

        val candidatos = mutableListOf<Double>()
        normalizadas.forEachIndexed { i, fila ->
            if (excluida(i) || clavesTotal.none { fila.contains(it) }) return@forEachIndexed
            val importes = importesDe(fila)
            if (importes.isNotEmpty()) {
                candidatos += importes
            } else if (i + 1 < normalizadas.size && !excluida(i + 1)) {
                // A veces el importe sale en la línea de debajo de "TOTAL"
                candidatos += importesDe(normalizadas[i + 1])
            }
        }
        // El total es el más alto de esas filas (en la tabla del IVA también pone
        // "TOTAL", pero ahí van la base y la cuota, que siempre son menores)
        if (candidatos.isNotEmpty()) return candidatos.maxOrNull()

        // No pone "TOTAL" en ningún sitio: el importe más alto del ticket
        return normalizadas.indices
            .filterNot { excluida(it) }
            .flatMap { importesDe(normalizadas[it]) }
            .maxOrNull()
    }

    private fun importesDe(fila: String): List<Double> =
        regexImporte.findAll(fila)
            .mapNotNull { m ->
                val entero = m.groupValues[1].replace(".", "")
                "$entero.${m.groupValues[2]}".toDoubleOrNull()
            }
            .filter { it in 0.01..99_999.0 }
            .toList()

    // ── Fecha ────────────────────────────────────────────────────────────

    /** Fechas tipo 15/10/2026 · 15-10-26 · 15.10.2026 */
    private val regexFecha = Regex("""(?<!\d)(\d{1,2})[/.-](\d{1,2})[/.-](\d{4}|\d{2})(?!\d)""")

    private fun buscarFecha(filas: List<String>, hoy: LocalDate): LocalDate? {
        for (fila in filas) {
            for (m in regexFecha.findAll(fila)) {
                val (dia, mes, anioTexto) = m.destructured
                val anio = anioTexto.toInt().let { if (it < 100) 2000 + it else it }
                val fecha = runCatching { LocalDate.of(anio, mes.toInt(), dia.toInt()) }.getOrNull()
                    ?: continue
                // Solo vale si no es del futuro ni demasiado antigua
                if (!fecha.isAfter(hoy) && fecha.isAfter(hoy.minusYears(2))) return fecha
            }
        }
        return null
    }

    // ── Tienda ───────────────────────────────────────────────────────────

    /** Palabras de las filas de cabecera que no son el nombre de la tienda. */
    private val palabrasNoComercio = listOf(
        "TICKET", "FACTURA", "SIMPLIFICADA", "NIF", "CIF", "N.I.F", "C.I.F", "TEL", "TELF", "TFNO",
        "TLF", "TELEFONO", "CALLE", "AVDA", "AVENIDA", "CTRA", "CARRETERA", "BIENVENIDO",
        "BIENVENIDOS", "GRACIAS", "FECHA", "HORA", "COPIA", "CLIENTE", "CAJA", "OPERADOR",
        "ATENDIO", "DOCUMENTO"
    ).map { palabraCompleta(it) }

    private val trozosNoComercio = listOf("C/", "@", "WWW", "HTTP", ".COM", ".ES")

    /** El nombre de la tienda suele ser la primera fila "con letras" del ticket. */
    private fun buscarComercio(filas: List<String>): String? {
        for (fila in filas.take(8)) {
            val normal = normalizar(fila)
            val letras = fila.count { it.isLetter() }
            val digitos = fila.count { it.isDigit() }
            if (letras < 3 || letras < (letras + digitos) * 0.6) continue
            if (Regex("""^\s*\d{5}\b""").containsMatchIn(normal)) continue // código postal + pueblo
            if (palabrasNoComercio.any { it.containsMatchIn(normal) }) continue
            if (trozosNoComercio.any { normal.contains(it) }) continue
            limpiarComercio(fila)?.let { return it }
        }
        return null
    }

    private fun limpiarComercio(fila: String): String? {
        var nombre = fila
            .replace(Regex("""\b[A-Z]-?\d{7,8}[A-Z]?\b""", RegexOption.IGNORE_CASE), " ") // CIF suelto
            .replace(Regex("""\s+"""), " ")
            .trim(' ', ',', '.', '-', '*', ':')
        // Quita "S.A.", "S.L.", "S.A.U."... del final
        nombre = nombre
            .replace(Regex("""[,\s]+S\.?\s?[AL]\.?(\s?U\.?)?$""", RegexOption.IGNORE_CASE), "")
            .trim(' ', ',', '.', '-')

        if (nombre.count { it.isLetter() } < 3) return null
        if (nombre == nombre.uppercase()) nombre = enMayusculaInicial(nombre)
        return nombre.take(40)
    }

    private val palabrasEnMinuscula = setOf("de", "del", "y")

    /** "BAR EL RINCON DE PEPE" → "Bar El Rincon de Pepe" */
    private fun enMayusculaInicial(texto: String): String =
        texto.lowercase().split(" ").mapIndexed { i, palabra ->
            if (i > 0 && palabra in palabrasEnMinuscula) palabra
            else palabra.replaceFirstChar { it.titlecase() }
        }.joinToString(" ")

    // ── Categoría ────────────────────────────────────────────────────────

    /**
     * Un tipo de gasto: cómo se suelen llamar sus categorías y qué palabras
     * del ticket lo delatan (nombres de tiendas, productos...).
     */
    private class Grupo(val raicesCategoria: List<String>, pistas: List<String>) {
        val regexPistas = pistas.map { palabraCompleta(it.uppercase()) }
    }

    // El orden importa en caso de empate (por ejemplo, Carrefour supermercado vs gasolinera)
    private val grupos = listOf(
        Grupo(
            listOf("SUPERMERC", "ALIMENT", "COMPRA", "COMIDA"),
            listOf(
                "mercadona", "carrefour", "lidl", "aldi", "eroski", "alcampo", "hipercor", "consum",
                "ahorramas", "supermercado", "supermercados", "hipermercado", "dia retail", "coviran",
                "gadis", "el arbol", "spar", "froiz", "masymas", "condis", "caprabo", "bonpreu",
                "alimerka", "fruteria", "carniceria", "panaderia", "pescaderia"
            )
        ),
        Grupo(
            listOf("TRANSPORTE", "GASOLINA", "COMBUSTIBLE", "COCHE"),
            listOf(
                "repsol", "cepsa", "moeve", "galp", "shell", "bp", "petronor", "ballenoil", "plenoil",
                "petroprix", "carrefour", "gasolinera", "estacion de servicio", "gasoleo", "gasolina",
                "diesel", "sin plomo", "carburante", "renfe", "alsa", "avanza", "taxi", "cabify",
                "uber", "parking", "aparcamiento"
            )
        ),
        Grupo(
            listOf("SALUD", "FARMACIA"),
            listOf("farmacia", "parafarmacia", "optica", "clinica", "dental")
        ),
        Grupo(
            listOf("RESTAUR", "COMIDA"),
            listOf(
                "restaurante", "bar", "cafeteria", "cerveceria", "taberna", "meson", "pizzeria",
                "mcdonald", "mcdonalds", "burger king", "telepizza", "kfc", "foster", "goiko",
                "montaditos", "vips", "starbucks", "camarero", "comensales"
            )
        ),
        Grupo(
            listOf("ROPA", "CALZADO"),
            listOf(
                "zara", "primark", "h&m", "pull&bear", "bershka", "stradivarius", "mango", "lefties",
                "decathlon", "springfield", "cortefiel", "massimo dutti", "oysho", "kiabi",
                "zapateria", "calzados", "deichmann", "jd sports", "foot locker"
            )
        ),
        Grupo(
            listOf("HOGAR", "CASA"),
            listOf(
                "ikea", "leroy merlin", "bricomart", "brico depot", "bricor", "aki", "conforama",
                "jysk", "ferreteria", "drogueria"
            )
        ),
        Grupo(
            listOf("OCIO", "ENTRETEN"),
            listOf("cine", "cines", "yelmo", "cinesa", "teatro", "bolera", "museo", "fnac")
        )
    )

    /** Devuelve el id de la categoría que mejor encaja con el ticket, o null si no lo tiene claro. */
    fun sugerirCategoria(datos: DatosTicket, categorias: List<Categoria>): Long? {
        val comercio = normalizar(datos.comercio ?: "")
        val todo = normalizar(datos.filas.joinToString("\n"))

        // El nombre de la tienda cuenta mucho más que una palabra suelta del ticket
        fun puntos(grupo: Grupo) = grupo.regexPistas.sumOf { r ->
            (if (r.containsMatchIn(comercio)) 5 else 0) + (if (r.containsMatchIn(todo)) 1 else 0)
        }

        val mejor = grupos.maxByOrNull { puntos(it) } ?: return null
        if (puntos(mejor) == 0) return null

        for (raiz in mejor.raicesCategoria) {
            categorias.firstOrNull { normalizar(it.nombre).contains(raiz) }?.let { return it.id }
        }
        return null
    }

    // ── Utilidades ───────────────────────────────────────────────────────

    /** Mayúsculas y sin tildes, para comparar sin problemas ("Cafetería" → "CAFETERIA"). */
    private fun normalizar(texto: String): String =
        Normalizer.normalize(texto, Normalizer.Form.NFD)
            .replace(Regex("""\p{Mn}+"""), "")
            .uppercase()

    /** Busca la palabra entera: "BAR" no debe encontrarse dentro de "BARRA". */
    private fun palabraCompleta(palabra: String) =
        Regex("(?<![A-Z0-9])" + Regex.escape(palabra) + "(?![A-Z0-9])")
}