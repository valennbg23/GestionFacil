package com.valentin.gestionfacil.data.db.converter

import androidx.room.TypeConverter
import com.valentin.gestionfacil.data.entity.TipoMovimiento
import com.valentin.gestionfacil.data.entity.TipoMovimientoMeta
import java.time.LocalDate

/**
 * Conversores de tipos para Room.
 * Room solo sabe guardar tipos básicos en SQLite, así que necesita estos
 * métodos para convertir LocalDate ↔ String y los enums ↔ String.
 */
class Converters {

    // ── LocalDate ────────────────────────────────────────────────
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    // ── TipoMovimiento ───────────────────────────────────────────
    @TypeConverter
    fun fromTipoMovimiento(tipo: TipoMovimiento): String = tipo.name

    @TypeConverter
    fun toTipoMovimiento(value: String): TipoMovimiento = TipoMovimiento.valueOf(value)

    // ── TipoMovimientoMeta ───────────────────────────────────────
    @TypeConverter
    fun fromTipoMovimientoMeta(tipo: TipoMovimientoMeta): String = tipo.name

    @TypeConverter
    fun toTipoMovimientoMeta(value: String): TipoMovimientoMeta = TipoMovimientoMeta.valueOf(value)
}