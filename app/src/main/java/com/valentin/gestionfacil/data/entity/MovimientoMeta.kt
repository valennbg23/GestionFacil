package com.valentin.gestionfacil.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Tipo de movimiento de la hucha: aportar o retirar. */
enum class TipoMovimientoMeta { APORTE, RETIRO }

/**
 * Movimiento hacia una meta (aporte) o desde una meta (retiro).
 * Permite llevar el historial de cada hucha.
 */
@Entity(
    tableName = "movimientos_meta",
    foreignKeys = [
        ForeignKey(
            entity = Meta::class,
            parentColumns = ["id"],
            childColumns = ["metaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("metaId"), Index("fecha")]
)
data class MovimientoMeta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val metaId: Long,
    val importe: Double,
    val tipo: TipoMovimientoMeta,
    val fecha: LocalDate,
    val nota: String = ""
)