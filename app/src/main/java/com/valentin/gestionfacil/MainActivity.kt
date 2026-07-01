package com.valentin.gestionfacil

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.valentin.gestionfacil.ui.addmovimiento.AddMovimientoScreen
import com.valentin.gestionfacil.ui.ajustes.AjustesScreen
import com.valentin.gestionfacil.ui.bloqueo.BloqueoScreen
import com.valentin.gestionfacil.ui.common.AppViewModelProvider
import com.valentin.gestionfacil.ui.common.ModoTema
import com.valentin.gestionfacil.ui.common.TemaViewModel
import com.valentin.gestionfacil.ui.dashboard.DashboardScreen
import com.valentin.gestionfacil.ui.historial.HistorialScreen
import com.valentin.gestionfacil.ui.metas.MetasScreen
import com.valentin.gestionfacil.ui.onboarding.OnboardingScreen
import com.valentin.gestionfacil.ui.presupuestos.PresupuestosScreen
import com.valentin.gestionfacil.ui.splash.SplashScreen
import com.valentin.gestionfacil.ui.theme.GestionFacilTheme
import com.valentin.gestionfacil.utils.BiometricHelper

sealed class TabDestino(
    val ruta: String,
    val label: String,
    val icono: ImageVector
) {
    data object Dashboard    : TabDestino("dashboard",    "Inicio",       Icons.Default.Home)
    data object Historial    : TabDestino("historial",    "Historial",    Icons.Default.List)
    data object Presupuestos : TabDestino("presupuestos", "Presupuestos", Icons.Default.AccountBalanceWallet)
    data object Metas        : TabDestino("metas",        "Metas",        Icons.Default.Savings)
    data object Ajustes      : TabDestino("ajustes",      "Ajustes",      Icons.Default.Settings)
}

private val tabs = listOf(
    TabDestino.Dashboard, TabDestino.Historial,
    TabDestino.Presupuestos, TabDestino.Metas, TabDestino.Ajustes
)

object Rutas {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val BLOQUEO = "bloqueo"
}

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val temaViewModel: TemaViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val modo by temaViewModel.modo.collectAsStateWithLifecycle()

            val forzarOscuro: Boolean? = when (modo) {
                ModoTema.SISTEMA -> null
                ModoTema.CLARO -> false
                ModoTema.OSCURO -> true
            }

            GestionFacilTheme(forzarOscuro = forzarOscuro) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController: NavHostController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val context = LocalContext.current
    val app = context.applicationContext as GestionFacilApp
    val preferencias = app.container.preferencias

    val esTabPrincipal = tabs.any { it.ruta == currentRoute }

    Scaffold(
        bottomBar = {
            if (esTabPrincipal) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.ruta,
                            onClick = {
                                if (currentRoute != tab.ruta) {
                                    navController.navigate(tab.ruta) {
                                        popUpTo(TabDestino.Dashboard.ruta) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = { Icon(tab.icono, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Rutas.SPLASH,
            modifier = Modifier.padding(padding)
        ) {
            composable(Rutas.SPLASH) {
                SplashScreen(
                    onTimeout = {
                        val siguiente = when {
                            !preferencias.onboardingCompletado -> Rutas.ONBOARDING
                            preferencias.bloqueoActivado &&
                                    BiometricHelper.esBiometriaDisponible(context) -> Rutas.BLOQUEO
                            else -> TabDestino.Dashboard.ruta
                        }
                        navController.navigate(siguiente) {
                            popUpTo(Rutas.SPLASH) { inclusive = true }
                        }
                    }
                )
            }
            composable(Rutas.ONBOARDING) {
                OnboardingScreen(
                    onFinalizar = {
                        preferencias.onboardingCompletado = true
                        navController.navigate(TabDestino.Dashboard.ruta) {
                            popUpTo(Rutas.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }
            composable(Rutas.BLOQUEO) {
                BloqueoScreen(
                    onAutenticado = {
                        navController.navigate(TabDestino.Dashboard.ruta) {
                            popUpTo(Rutas.BLOQUEO) { inclusive = true }
                        }
                    }
                )
            }
            composable(TabDestino.Dashboard.ruta) {
                DashboardScreen(
                    onAddMovimiento = { navController.navigate("add_movimiento") }
                )
            }
            composable(TabDestino.Historial.ruta) {
                HistorialScreen(
                    onEditarMovimiento = { id ->
                        navController.navigate("add_movimiento?id=$id")
                    }
                )
            }
            composable(TabDestino.Presupuestos.ruta) {
                PresupuestosScreen()
            }
            composable(TabDestino.Metas.ruta) {
                MetasScreen()
            }
            composable(TabDestino.Ajustes.ruta) {
                AjustesScreen()
            }
            composable(
                route = "add_movimiento?id={id}",
                arguments = listOf(navArgument("id") {
                    type = NavType.LongType
                    defaultValue = -1L
                })
            ) { entry ->
                val id = entry.arguments?.getLong("id") ?: -1L
                AddMovimientoScreen(
                    movimientoId = if (id > 0) id else null,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}