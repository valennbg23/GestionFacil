package com.valentin.gestionfacil.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Presupuesto mensual para una categoría concreta.
 * La combinación (categoriaId, mes, anio) es única — un solo presupuesto por categoría/mes.
 */
@Entity(
    tableName = "presupuestos",
    foreignKeys = [
        ForeignKey(
            entity = Categoria::class,
            parentColumns = ["id"],
            childColumns = ["categoriaId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["categoriaId", "mes", "anio"], unique = true)]
)
data class Presupuesto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val categoriaId: Long,
    val importeMaximo: Double,
    val mes: Int,   // 1..12
    val anio: Int
)