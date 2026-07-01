package com.valentin.gestionfacil.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Meta de ahorro personal (hucha virtual).
 * El usuario puede transferir dinero manualmente a/desde esta hucha.
 *
 * @property id             identificador único autogenerado
 * @property nombre         nombre visible (ej. "Viaje Japón")
 * @property icono          nombre del icono (ver IconHelper)
 * @property color          color hex (ej. "#F7B801")
 * @property objetivo       cantidad total a ahorrar
 * @property ahorrado       cantidad acumulada hasta el momento
 * @property fechaLimite    fecha objetivo opcional
 * @property fechaCreacion  fecha de creación de la meta
 */
@Entity(tableName = "metas")
data class Meta(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val nombre: String,
    val icono: String,
    val color: String,
    val objetivo: Double,
    val ahorrado: Double = 0.0,
    val fechaLimite: LocalDate? = null,
    val fechaCreacion: LocalDate = LocalDate.now()
)