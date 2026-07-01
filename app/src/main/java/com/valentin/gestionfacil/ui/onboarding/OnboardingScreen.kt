package com.valentin.gestionfacil.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class PaginaOnboarding(
    val icono: ImageVector,
    val titulo: String,
    val descripcion: String
)

private val paginas = listOf(
    PaginaOnboarding(
        icono = Icons.Default.AccountBalanceWallet,
        titulo = "Controla tus gastos",
        descripcion = "Registra cada gasto e ingreso de forma rápida y organízalos por categorías personalizables."
    ),
    PaginaOnboarding(
        icono = Icons.AutoMirrored.Filled.TrendingUp,
        titulo = "Visualiza tu dinero",
        descripcion = "Consulta tu balance mensual, define presupuestos y recibe alertas cuando te acerques al límite."
    ),
    PaginaOnboarding(
        icono = Icons.Default.Lock,
        titulo = "Privacidad total",
        descripcion = "Tus datos se almacenan únicamente en tu dispositivo. Sin cuenta, sin internet, sin compartir nada."
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingScreen(onFinalizar: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { paginas.size })
    val scope = rememberCoroutineScope()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Pager principal
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pagina ->
                PaginaContenido(paginas[pagina])
            }

            // Indicadores de página
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(paginas.size) { index ->
                    val seleccionado = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(
                                width = if (seleccionado) 24.dp else 8.dp,
                                height = 8.dp
                            )
                            .clip(CircleShape)
                            .background(
                                if (seleccionado) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            // Botonera inferior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botón saltar (solo si no estamos en la última página)
                if (pagerState.currentPage < paginas.size - 1) {
                    TextButton(onClick = onFinalizar) {
                        Text("Saltar")
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }

                // Botón principal: Siguiente / Empezar
                Button(
                    onClick = {
                        if (pagerState.currentPage < paginas.size - 1) {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        } else {
                            onFinalizar()
                        }
                    },
                    modifier = Modifier.height(48.dp)
                ) {
                    Text(
                        if (pagerState.currentPage < paginas.size - 1) "Siguiente"
                        else "Empezar",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun PaginaContenido(pagina: PaginaOnboarding) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(160.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                pagina.icono,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(90.dp)
            )
        }

        Spacer(Modifier.height(48.dp))

        Text(
            pagina.titulo,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(16.dp))

        Text(
            pagina.descripcion,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}