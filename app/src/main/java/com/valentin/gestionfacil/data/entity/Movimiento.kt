package com.valentin.gestionfacil.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Tipo de movimiento — gasto o ingreso. */
enum class TipoMovimiento { GASTO, INGRESO }

/**
 * Entidad que representa un movimiento económico (gasto o ingreso).
 * Referencia a una [Categoria] mediante [categoriaId].
 */
@Entity(
    tableName = "movimientos",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.RESTRICT
        )
    ],
    indices = [Index("categoriaId"), Index("fecha")]
)
data class Movimiento(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val importe: Double,
    val tipo: TipoMovimiento,
    val categoriaId: Long,
    val fecha: LocalDate,
    @ColumnInfo(defaultValue = "") val descripcion: String = ""
)