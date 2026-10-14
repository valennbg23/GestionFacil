package com.valentin.gestionfacil.utils

import com.valentin.gestionfacil.data.db.dao.MovimientoConCategoria
import com.valentin.gestionfacil.data.db.dao.PresupuestoConCategoria
import com.valentin.gestionfacil.data.entity.Frecuencia
import com.valentin.gestionfacil.data.entity.Recurrente
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt

/** Tipo de consejo: decide el icono y el color en la pantalla. */
enum class TipoConsejo { AVISO, POSITIVO, INFO }

data class Consejo(
    val tipo: TipoConsejo,
    val titulo: String,
    val texto: String
)

/**
 * "Asistente" de consejos: analiza los movimientos del usuario y genera frases útiles.
 * Todo se calcula en el móvil, sin internet ni IA externa.
 *
 * Es un object sin dependencias de Android, así que es fácil de probar.
 */
object AsistenteConsejos {

    private const val MAXIMO_CONSEJOS = 3

    /** A partir de este día del mes la previsión ya es fiable. */
    private const val DIA_MINIMO_PREVISION = 7

    /** A partir de este día del mes se compara con el mes pasado. */
    private const val DIA_MINIMO_COMPARACION = 5

    /** Un movimiento con su fecha ya convertida a LocalDate. */
    private data class Mov(val datos: MovimientoConCategoria, val fecha: LocalDate) {
        val esGasto: Boolean get() = datos.tipo == TipoMovimiento.GASTO
    }

    /** Consejo con prioridad: cuanto más bajo el número, antes aparece. */
    private data class Candidato(val prioridad: Int, val consejo: Consejo)

    /** Gasto de una categoría ahora y en el mismo periodo del mes pasado. */
    private data class Cambio(val nombre: String, val ahora: Double, val antes: Double) {
        val diferencia: Double get() = ahora - antes
        val porcentaje: Int get() = ((ahora / antes - 1) * 100).roundToInt()
    }

    fun generar(
        movimientos: List<MovimientoConCategoria>,
        presupuestos: List<PresupuestoConCategoria>,
        recurrentes: List<Recurrente>,
        hoy: LocalDate = LocalDate.now()
    ): List<Consejo> {
        val mesActual = YearMonth.from(hoy)
        val movs = movimientos.mapNotNull { m ->
            runCatching { LocalDate.parse(m.fecha) }.getOrNull()?.let { Mov(m, it) }
        }
        val delMes = movs.filter { YearMonth.from(it.fecha) == mesActual }

        if (delMes.isEmpty()) {
            return listOf(
                Consejo(
                    TipoConsejo.INFO,
                    "Apunta tus gastos de este mes",
                    "Cuantos más movimientos registres, mejores consejos podré darte."
                )
            )
        }

        val activos = recurrentes.filter { it.activo }
        val candidatos = buildList {
            consejoPresupuesto(delMes, presupuestos, hoy)?.let { add(it) }
            consejoPrevision(delMes, activos, hoy)?.let { add(it) }
            addAll(consejosComparacion(movs, hoy))
            consejoGastosFijos(delMes, activos)?.let { add(it) }
            consejoMayorGasto(delMes, hoy)?.let { add(it) }
        }

        return candidatos
            .sortedBy { it.prioridad }
            .take(MAXIMO_CONSEJOS)
            .map { it.consejo }
    }

    // ── 1. Presupuestos ────────────────────────────────────────────────

    /** El presupuesto más apurado del mes (superado o por encima del 60%). */
    private fun consejoPresupuesto(
        delMes: List<Mov>,
        presupuestos: List<PresupuestoConCategoria>,
        hoy: LocalDate
    ): Candidato? {
        val masApurado = presupuestos
            .filter { it.importeMaximo > 0 }
            .map { p ->
                val gastado = delMes.filter { it.esGasto && it.datos.categoriaId == p.categoriaId }
                    .sumOf { it.datos.importe }
                p to gastado
            }
            .maxByOrNull { (p, gastado) -> gastado / p.importeMaximo }
            ?: return null

        val (p, gastado) = masApurado
        val porcentaje = gastado / p.importeMaximo
        val diasRestantes = hoy.lengthOfMonth() - hoy.dayOfMonth + 1  // incluye hoy

        return when {
            porcentaje >= 1.0 -> Candidato(
                0,
                Consejo(
                    TipoConsejo.AVISO,
                    "Te has pasado en ${p.categoriaNombre}",
                    "Llevas ${eur(gastado)} de ${eur(p.importeMaximo)}: " +
                            "${eur(gastado - p.importeMaximo)} por encima del presupuesto."
                )
            )
            porcentaje >= 0.6 -> {
                val queda = p.importeMaximo - gastado
                Candidato(
                    2,
                    Consejo(
                        TipoConsejo.AVISO,
                        "Te quedan ${eur(queda)} para ${p.categoriaNombre}",
                        "Para no pasarte, intenta no gastar más de " +
                                "${eur(queda / diasRestantes, decimales = 2)} al día hasta fin de mes."
                    )
                )
            }
            else -> null
        }
    }

    // ── 2. Previsión de fin de mes ─────────────────────────────────────

    /**
     * Calcula cómo acabará el mes. Los gastos fijos (recurrentes) se suman tal cual
     * y solo se "proyecta" el gasto del día a día, para que el alquiler del día 1
     * no dispare la previsión.
     */
    private fun consejoPrevision(
        delMes: List<Mov>,
        recurrentes: List<Recurrente>,
        hoy: LocalDate
    ): Candidato? {
        val dia = hoy.dayOfMonth
        val diasRestantes = hoy.lengthOfMonth() - dia
        if (dia < DIA_MINIMO_PREVISION || diasRestantes <= 0) return null

        val gastosHastaHoy = delMes.filter { it.esGasto && !it.fecha.isAfter(hoy) }
            .sumOf { it.datos.importe }
        if (gastosHastaHoy <= 0) return null

        // Lo ya apuntado en el mes (incluidos movimientos con fecha futura)
        val gastosMes = delMes.filter { it.esGasto }.sumOf { it.datos.importe }
        val ingresosMes = delMes.filter { !it.esGasto }.sumOf { it.datos.importe }

        // Recurrentes de este mes: lo que ya se ha cobrado/pagado y lo que falta
        var fijosYaPagados = 0.0
        var fijosPendientes = 0.0
        var ingresosPendientes = 0.0
        recurrentes.forEach { r ->
            val fechas = fechasEnMes(r, YearMonth.from(hoy))
            val pasadas = fechas.count { !it.isAfter(hoy) }
            val futuras = fechas.size - pasadas
            if (r.tipo == TipoMovimiento.GASTO) {
                fijosYaPagados += r.importe * pasadas
                fijosPendientes += r.importe * futuras
            } else {
                ingresosPendientes += r.importe * futuras
            }
        }

        val variableHastaHoy = (gastosHastaHoy - fijosYaPagados).coerceAtLeast(0.0)
        val ritmoDiario = variableHastaHoy / dia
        val gastoPrevisto = gastosMes + fijosPendientes + ritmoDiario * diasRestantes
        val ingresoPrevisto = ingresosMes + ingresosPendientes
        val saldoPrevisto = ingresoPrevisto - gastoPrevisto

        return when {
            ingresoPrevisto <= 0 -> Candidato(
                6,
                Consejo(
                    TipoConsejo.INFO,
                    "Previsión del mes",
                    "A este ritmo gastarás unos ${eur(gastoPrevisto)} este mes."
                )
            )
            saldoPrevisto < 0 -> Candidato(
                1,
                Consejo(
                    TipoConsejo.AVISO,
                    "A este ritmo acabarás el mes en negativo",
                    "Calculo que gastarás unos ${eur(gastoPrevisto)} e ingresarás " +
                            "${eur(ingresoPrevisto)}: te faltarían unos ${eur(-saldoPrevisto)}."
                )
            )
            else -> Candidato(
                4,
                Consejo(
                    TipoConsejo.POSITIVO,
                    "Vas bien este mes",
                    "A este ritmo acabarás con unos ${eur(saldoPrevisto)} de ahorro " +
                            "(gastarás unos ${eur(gastoPrevisto)})."
                )
            )
        }
    }

    // ── 3. Comparación con el mes pasado ───────────────────────────────

    /**
     * Compara cada categoría con el mismo periodo del mes pasado
     * (del día 1 al mismo día), para que la comparación sea justa.
     * Devuelve como mucho una subida y una bajada.
     */
    private fun consejosComparacion(movs: List<Mov>, hoy: LocalDate): List<Candidato> {
        if (hoy.dayOfMonth < DIA_MINIMO_COMPARACION) return emptyList()

        val inicioActual = hoy.withDayOfMonth(1)
        val mesPasado = YearMonth.from(hoy).minusMonths(1)
        val inicioPasado = mesPasado.atDay(1)
        val finPasado = mesPasado.atDay(minOf(hoy.dayOfMonth, mesPasado.lengthOfMonth()))

        fun gastosPorCategoria(desde: LocalDate, hasta: LocalDate) = movs
            .filter { it.esGasto && !it.fecha.isBefore(desde) && !it.fecha.isAfter(hasta) }
            .groupBy { it.datos.categoriaId }
            .mapValues { (_, lista) -> lista.sumOf { it.datos.importe } }

        val actual = gastosPorCategoria(inicioActual, hoy)
        val pasado = gastosPorCategoria(inicioPasado, finPasado)
        val nombres = movs.associate { it.datos.categoriaId to it.datos.categoriaNombre }

        val cambios = pasado
            .filter { (_, antes) -> antes >= 20.0 }  // ignora categorías casi sin gasto
            .map { (id, antes) -> Cambio(nombres[id] ?: "Otros", actual[id] ?: 0.0, antes) }

        val subida = cambios
            .filter { it.diferencia >= 10.0 && it.ahora >= it.antes * 1.25 }
            .maxByOrNull { it.diferencia }
        val bajada = cambios
            .filter { -it.diferencia >= 10.0 && it.ahora <= it.antes * 0.75 }
            .minByOrNull { it.diferencia }

        return buildList {
            subida?.let {
                add(
                    Candidato(
                        3,
                        Consejo(
                            TipoConsejo.AVISO,
                            "Gastas más en ${it.nombre}",
                            "Llevas ${eur(it.ahora)}, un ${it.porcentaje}% más que a estas " +
                                    "alturas del mes pasado (${eur(it.antes)})."
                        )
                    )
                )
            }
            bajada?.let {
                add(
                    Candidato(
                        5,
                        Consejo(
                            TipoConsejo.POSITIVO,
                            "Gastas menos en ${it.nombre}",
                            "Llevas ${eur(it.ahora)}, un ${-it.porcentaje}% menos que a estas " +
                                    "alturas del mes pasado (${eur(it.antes)}). ¡Bien!"
                        )
                    )
                )
            }
        }
    }

    // ── 4. Gastos fijos ────────────────────────────────────────────────

    /** Cuánto suman al mes los gastos recurrentes activos. */
    private fun consejoGastosFijos(delMes: List<Mov>, recurrentes: List<Recurrente>): Candidato? {
        val gastosFijos = recurrentes.filter { it.tipo == TipoMovimiento.GASTO }
        if (gastosFijos.isEmpty()) return null

        val totalMensual = gastosFijos.sumOf { r ->
            when (r.frecuencia) {
                Frecuencia.SEMANAL -> r.importe * 52 / 12
                Frecuencia.MENSUAL -> r.importe
                Frecuencia.ANUAL -> r.importe / 12
            }
        }
        val ingresosMes = delMes.filter { !it.esGasto }.sumOf { it.datos.importe }
        val n = gastosFijos.size
        val cuantos = if (n == 1) "1 gasto recurrente" else "$n gastos recurrentes"

        val texto = if (ingresosMes > 0) {
            val pct = (totalMensual / ingresosMes * 100).roundToInt()
            "Son $cuantos y se llevan el $pct% de tus ingresos de este mes."
        } else {
            "Son $cuantos. Revisa de vez en cuando si sigues usándolos todos."
        }

        return Candidato(
            7,
            Consejo(TipoConsejo.INFO, "Tus gastos fijos: ${eur(totalMensual)} al mes", texto)
        )
    }

    // ── 5. Categoría con más gasto ─────────────────────────────────────

    private fun consejoMayorGasto(delMes: List<Mov>, hoy: LocalDate): Candidato? {
        val porCategoria = delMes
            .filter { it.esGasto && !it.fecha.isAfter(hoy) }
            .groupBy { it.datos.categoriaNombre }
            .mapValues { (_, lista) -> lista.sumOf { it.datos.importe } }
        if (porCategoria.size < 2) return null

        val total = porCategoria.values.sum()
        val (nombre, importe) = porCategoria.maxBy { it.value }
        val pct = (importe / total * 100).roundToInt()

        return Candidato(
            8,
            Consejo(
                TipoConsejo.INFO,
                "Donde más gastas: $nombre",
                "${eur(importe)} este mes, el $pct% de todo lo que llevas gastado."
            )
        )
    }

    // ── Utilidades ─────────────────────────────────────────────────────

    /** Fechas en las que un recurrente cae dentro de [mes]. */
    private fun fechasEnMes(r: Recurrente, mes: YearMonth): List<LocalDate> {
        val inicioMes = mes.atDay(1)
        val finMes = mes.atEndOfMonth()
        val fechas = mutableListOf<LocalDate>()
        var n = 0L
        while (true) {
            val fecha = CalendarioRecurrente.fechaRepeticion(r.fechaInicio, r.frecuencia, n)
            if (fecha.isAfter(finMes)) break
            if (!fecha.isBefore(inicioMes)) fechas.add(fecha)
            n++
        }
        return fechas
    }

    private fun eur(cantidad: Double, decimales: Int = 0): String {
        val nf = NumberFormat.getCurrencyInstance(Locale("es", "ES"))
        nf.currency = Currency.getInstance("EUR")
        nf.minimumFractionDigits = decimales
        nf.maximumFractionDigits = decimales
        return nf.format(cantidad)
    }
}