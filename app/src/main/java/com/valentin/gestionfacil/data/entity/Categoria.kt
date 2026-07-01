package com.valentin.gestionfacil.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa una categoría para clasificar movimientos.
 *
 * @property id              identificador único autogenerado por Room
 * @property nombre          nombre visible (ej. "Comida y restaurantes")
 * @property icono           nombre del icono de Material (ej. "Restaurant")
 * @property color           color en formato hex (ej. "#FF5733")
 * @property esPredefinida   true si viene precargada, false si la creó el usuario
 */
@Entity(tableName = "categorias")
data class Categoria(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nombre: String,
    val icono: String,
    val color: String,
    val esPredefinida: Boolean = false
)