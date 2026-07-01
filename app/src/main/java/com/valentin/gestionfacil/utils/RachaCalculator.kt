package com.valentin.gestionfacil.utils

import com.valentin.gestionfacil.data.db.dao.MovimientoConCategoria
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * Cálculos de gamificación: rachas de días sin gastos.
 * Trabaja únicamente con la lista de movimientos en memoria,
 * para no añadir consultas SQL adicionales.
 */
data class EstadisticasRacha(
    val rachaActual: Int,
    val mejorRacha: Int,
    val diasSinGastosEsteMes: Int,
    val mensajeMotivador: String
)

object RachaCalculator {

    /**
     * Calcula todas las métricas a partir de la lista de movimientos.
     * Solo cuenta GASTOS (los ingresos no rompen la racha).
     */
    fun calcular(movimientos: List<MovimientoConCategoria>): EstadisticasRacha {
        val hoy = LocalDate.now()

        // Fechas únicas en las que hubo al menos un GASTO
        val fechasConGasto: Set<LocalDate> = movimientos
            .filter { it.tipo == TipoMovimiento.GASTO }
            .mapNotNull { runCatching { LocalDate.parse(it.fecha) }.getOrNull() }
            .toSet()

        // Racha actual: cuántos días seguidos hasta hoy SIN gastos
        var rachaActual = 0
        var fecha = hoy
        while (fecha !in fechasConGasto) {
            rachaActual++
            fecha = fecha.minusDays(1)
            // Tope de seguridad: si lleva más de 1 año sin gastos, paramos
            if (rachaActual > 365) break
        }

        // Mejor racha histórica: buscamos la racha más larga sin gastos
        // entre la primera fecha registrada y hoy
        val mejorRacha = calcularMejorRacha(fechasConGasto, hoy)

        // Días sin gastos del mes actual
        val mesActual = YearMonth.from(hoy)
        val diasMesHastaHoy = if (mesActual == YearMonth.from(hoy)) hoy.dayOfMonth else mesActual.lengthOfMonth()
        val diasConGastoEsteMes = fechasConGasto.count {
            YearMonth.from(it) == mesActual && !it.isAfter(hoy)
        }
        val diasSinGastosEsteMes = (diasMesHastaHoy - diasConGastoEsteMes).coerceAtLeast(0)

        // Mensaje motivador según racha actual
        val mensaje = mensajePara(rachaActual)

        return EstadisticasRacha(
            rachaActual = rachaActual,
            mejorRacha = maxOf(mejorRacha, rachaActual),
            diasSinGastosEsteMes = diasSinGastosEsteMes,
            mensajeMotivador = mensaje
        )
    }

    /**
     * Encuentra la mejor racha de días consecutivos sin gastos
     * desde la primera fecha registrada hasta hoy.
     */
    private fun calcularMejorRacha(fechasConGasto: Set<LocalDate>, hoy: LocalDate): Int {
        if (fechasConGasto.isEmpty()) {
            // Si nunca ha habido gastos, su mejor racha es desde el primer día de uso
            return 0
        }

        val primeraFecha = fechasConGasto.min()
        val totalDias = ChronoUnit.DAYS.between(primeraFecha, hoy).toInt() + 1
        if (totalDias <= 0) return 0

        var mejorRacha = 0
        var rachaActual = 0
        var fecha = primeraFecha

        repeat(totalDias) {
            if (fecha in fechasConGasto) {
                rachaActual = 0
            } else {
                rachaActual++
                if (rachaActual > mejorRacha) mejorRacha = rachaActual
            }
            fecha = fecha.plusDays(1)
        }

        return mejorRacha
    }

    private fun mensajePara(dias: Int): String = when {
        dias == 0  -> "Hoy registraste un gasto. ¡Mañana es otra oportunidad!"
        dias == 1  -> "¡Empieza una nueva racha! Sigue así 💪"
        dias in 2..3   -> "Vas bien. La constancia es clave 🎯"
        dias in 4..6   -> "¡Genial! Estás cogiendo el ritmo 🔥"
        dias in 7..13  -> "¡Una semana completa! Eres un crack 🏆"
        dias in 14..29 -> "¡Más de 2 semanas! Imparable 🚀"
        dias in 30..59 -> "¡Un mes entero! Estás en otra dimensión 💎"
        else           -> "¡Leyenda viva! Inspiras a cualquiera 👑"
    }
}