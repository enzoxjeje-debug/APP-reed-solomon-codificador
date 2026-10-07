package com.example.reedsolomonlab

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.reedsolomonlab.ui.components.NeonBackground
import com.example.reedsolomonlab.ui.decoding.DecodingScreen
import com.example.reedsolomonlab.ui.encoding.EncodingScreen
import com.example.reedsolomonlab.ui.info.InfoScreen
import com.example.reedsolomonlab.ui.theme.RsColors
import com.example.reedsolomonlab.viewmodel.AppViewModel

private data class Tab(val route: String, val label: String, val icon: @Composable () -> Unit)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReedSolomonApp(appViewModel: AppViewModel = viewModel()) {
    val navController = rememberNavController()
    val soundEnabled by appViewModel.soundEnabled.collectAsState()

    val tabs = listOf(
        Tab("codificar", "CODIFICAR") { Icon(Icons.Filled.Memory, contentDescription = null) },
        Tab("decodificar", "DECODIFICAR") { Icon(Icons.Filled.Shield, contentDescription = null) },
        Tab("info", "MÁS INFO") { Icon(Icons.Filled.Info, contentDescription = null) }
    )

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    // Navegación única para las pestañas y el traspaso codificador →
    // decodificador: siempre con la misma política (sin entradas duplicadas
    // en la pila, que dejaban pestañas sin responder al pulsarlas).
    fun goToTab(route: String) {
        if (currentRoute == route) return
        appViewModel.play(com.example.reedsolomonlab.audio.SoundManager.Sound.NAVIGATE)
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NeonBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "⬢ RS-COM2026",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = RsColors.Cyan
                        )
                    },
                    actions = {
                        IconButton(onClick = { appViewModel.setSoundEnabled(!soundEnabled) }) {
                            Icon(
                                if (soundEnabled) Icons.Filled.VolumeUp else Icons.Filled.VolumeOff,
                                contentDescription = if (soundEnabled) "Silenciar" else "Activar sonido",
                                tint = if (soundEnabled) RsColors.Cyan else RsColors.TextDim
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            bottomBar = {
                NavigationBar(containerColor = RsColors.Surface.copy(alpha = 0.92f)) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = { goToTab(tab.route) },
                            icon = tab.icon,
                            label = {
                                Text(tab.label, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = RsColors.Background,
                                selectedTextColor = RsColors.Cyan,
                                unselectedIconColor = RsColors.TextDim,
                                unselectedTextColor = RsColors.TextDim,
                                indicatorColor = RsColors.Cyan
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                NavHost(
                    navController = navController,
                    startDestination = "codificar",
                    enterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { it / 3 },
                            animationSpec = tween(300)
                        ) + fadeIn(tween(300))
                    },
                    exitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { -it / 3 },
                            animationSpec = tween(300)
                        ) + fadeOut(tween(300))
                    },
                    popEnterTransition = {
                        slideInHorizontally(
                            initialOffsetX = { -it / 3 },
                            animationSpec = tween(300)
                        ) + fadeIn(tween(300))
                    },
                    popExitTransition = {
                        slideOutHorizontally(
                            targetOffsetX = { it / 3 },
                            animationSpec = tween(300)
                        ) + fadeOut(tween(300))
                    }
                ) {
                    composable("codificar") {
                        EncodingScreen(
                            appViewModel = appViewModel,
                            onNavigateToDecoder = { goToTab("decodificar") }
                        )
                    }
                    composable("decodificar") {
                        DecodingScreen(appViewModel = appViewModel)
                    }
                    composable("info") {
                        InfoScreen(appViewModel = appViewModel)
                    }
                }
            }
        }
    }
}
