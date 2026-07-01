package com.valentin.gestionfacil.utils

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Mapeo de nombres de iconos (string en BD) a iconos reales de Material Icons.
 * Si el nombre no existe, devuelve un icono genérico.
 *
 * Los nombres están alineados con los de CATEGORIAS_PREDEFINIDAS.
 */
object IconHelper {

    private val mapa: Map<String, ImageVector> = mapOf(
        // Gastos
        "Restaurant"       to Icons.Default.Restaurant,
        "ShoppingCart"     to Icons.Default.ShoppingCart,
        "DirectionsCar"    to Icons.Default.DirectionsCar,
        "SportsEsports"    to Icons.Default.SportsEsports,
        "Home"             to Icons.Default.Home,
        "LocalHospital"    to Icons.Default.LocalHospital,
        "Checkroom"        to Icons.Default.Checkroom,
        "School"           to Icons.Default.School,
        "Flight"           to Icons.Default.Flight,
        "Subscriptions"    to Icons.Default.Subscriptions,
        "MoreHoriz"        to Icons.Default.MoreHoriz,
        // Ingresos
        "Payments"         to Icons.Default.Payments,
        "Work"             to Icons.Default.Work,
        "AddCircle"        to Icons.Default.AddCircle,
        // Metas de ahorro (para la Fase 3)
        "Savings"          to Icons.Default.Savings,
        "BeachAccess"      to Icons.Default.BeachAccess,
        "CardGiftcard"     to Icons.Default.CardGiftcard,
        "Shield"           to Icons.Default.Shield,
        "DirectionsBike"   to Icons.Default.DirectionsBike,
        "Laptop"           to Icons.Default.Laptop,
        "House"            to Icons.Default.House,
        "Pets"             to Icons.Default.Pets,
        "Favorite"         to Icons.Default.Favorite,
        "Celebration"      to Icons.Default.Celebration,
        // Fallback
        "Category"         to Icons.Default.Category
    )

    /** Obtiene el icono a partir del nombre. Si no existe, devuelve Category. */
    fun get(nombre: String): ImageVector = mapa[nombre] ?: Icons.Default.Category

    /** Lista de nombres disponibles — útil para selectores en diálogos. */
    val nombresDisponibles: List<String> = mapa.keys.toList()
}