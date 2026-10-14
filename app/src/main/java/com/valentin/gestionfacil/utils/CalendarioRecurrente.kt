package com.valentin.gestionfacil.utils

import com.valentin.gestionfacil.data.entity.Frecuencia
import java.time.LocalDate

/**
 * Cálculo de fechas de los movimientos recurrentes.
 *
 * Cada repetición se calcula SIEMPRE desde la fecha de inicio, no desde la anterior.
 * Así, algo que empieza el 31 de enero cae el 28/29 de febrero (último día del mes)
 * y vuelve al 31 en marzo, en vez de quedarse en el 28 para siempre.
 */
object CalendarioRecurrente {

    /** Límite de seguridad para no crear miles de movimientos de golpe. */
    private const val MAXIMO_POR_VEZ = 500

    /** Fecha de la repetición número [n] (n = 0 es la propia fecha de inicio). */
    fun fechaRepeticion(inicio: LocalDate, frecuencia: Frecuencia, n: Long): LocalDate =
        when (frecuencia) {
            Frecuencia.SEMANAL -> inicio.plusWeeks(n)
            Frecuencia.MENSUAL -> inicio.plusMonths(n)
            Frecuencia.ANUAL -> inicio.plusYears(n)
        }

    /**
     * Fechas en las que toca crear movimiento: posteriores a [despuesDe]
     * y como mucho hasta [hasta] (incluida).
     */
    fun fechasPendientes(
        inicio: LocalDate,
        frecuencia: Frecuencia,
        despuesDe: LocalDate,
        hasta: LocalDate
    ): List<LocalDate> {
        val fechas = mutableListOf<LocalDate>()
        var n = 1L
        while (fechas.size < MAXIMO_POR_VEZ) {
            val fecha = fechaRepeticion(inicio, frecuencia, n)
            if (fecha.isAfter(hasta)) break
            if (fecha.isAfter(despuesDe)) fechas.add(fecha)
            n++
        }
        return fechas
    }

    /** Próxima fecha en la que se creará el movimiento, después de [despuesDe]. */
    fun proximaFecha(inicio: LocalDate, frecuencia: Frecuencia, despuesDe: LocalDate): LocalDate {
        var n = 1L
        while (true) {
            val fecha = fechaRepeticion(inicio, frecuencia, n)
            if (fecha.isAfter(despuesDe)) return fecha
            n++
        }
    }
}