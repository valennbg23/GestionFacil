package com.valentin.gestionfacil.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Cada cuánto se repite un movimiento recurrente. */
enum class Frecuencia { SEMANAL, MENSUAL, ANUAL }

/**
 * Plantilla de un gasto o ingreso que se repite solo (alquiler, nómina, Netflix...).
 *
 * No es un movimiento en sí: cada vez que llega la fecha, la app crea un
 * [Movimiento] normal con estos datos.
 *
 * @property fechaInicio  fecha del primer movimiento. Marca el día en que se repite
 *                        (ej. día 1 de cada mes, o cada martes).
 * @property ultimaFecha  última fecha para la que ya se ha creado el movimiento.
 * @property activo       false si el usuario lo ha pausado.
 */
@Entity(
    tableName = "recurrentes",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoriaId")]
)
data class Recurrente(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val importe: Double,
    val tipo: TipoMovimiento,
    val categoriaId: Long,
    val descripcion: String,
    val frecuencia: Frecuencia,
    val fechaInicio: LocalDate,
    val ultimaFecha: LocalDate,
    val activo: Boolean = true
)