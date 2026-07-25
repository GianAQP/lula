package com.aqpseller.lulaapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aqpseller.lulaapp.features.common.ProximamenteScreen
import com.aqpseller.lulaapp.features.daily_review.CerrarDiaScreen
import com.aqpseller.lulaapp.features.finances.CrearMovimientoScreen
import com.aqpseller.lulaapp.features.habits.CrearHabitoScreen
import com.aqpseller.lulaapp.features.home.HomeScreen
import com.aqpseller.lulaapp.features.tasks.CrearTareaScreen
import androidx.navigation.compose.composable

@Composable
fun LulaNavHost() {
    val navController = rememberNavController()
    var mostrarMenuAgregar by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            LulaBottomBar(
                currentRoute = currentRoute,
                onNavigate = { ruta -> navController.navigate(ruta) { launchSingleTop = true } },
                onAddClick = { mostrarMenuAgregar = true },
            )
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = LulaDestinations.HOY,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(LulaDestinations.HOY) {
                HomeScreen(
                    onCerrarDia = { navController.navigate(LulaDestinations.CERRAR_DIA) },
                    onAgregarAlgo = { mostrarMenuAgregar = true },
                )
            }
            composable(
                route = LulaDestinations.PROXIMAMENTE,
                arguments = listOf(navArgument("titulo") { type = NavType.StringType }),
            ) { entry ->
                ProximamenteScreen(titulo = entry.arguments?.getString("titulo").orEmpty())
            }
            composable(LulaDestinations.CREAR_HABITO) {
                CrearHabitoScreen(onGuardado = { navController.popBackStack() })
            }
            composable(LulaDestinations.CREAR_TAREA) {
                CrearTareaScreen(onGuardado = { navController.popBackStack() })
            }
            composable(
                route = LulaDestinations.CREAR_MOVIMIENTO,
                arguments = listOf(navArgument("tipo") { type = NavType.StringType }),
            ) {
                CrearMovimientoScreen(onGuardado = { navController.popBackStack() })
            }
            composable(LulaDestinations.CERRAR_DIA) {
                CerrarDiaScreen(
                    onVolverAHoy = {
                        navController.navigate(LulaDestinations.HOY) {
                            popUpTo(LulaDestinations.HOY) { inclusive = true }
                        }
                    },
                )
            }
        }
    }

    if (mostrarMenuAgregar) {
        AddMenuSheet(
            onDismiss = { mostrarMenuAgregar = false },
            onOpcionSeleccionada = { opcion ->
                mostrarMenuAgregar = false
                when (opcion) {
                    "Hábito" -> navController.navigate(LulaDestinations.CREAR_HABITO)
                    "Tarea" -> navController.navigate(LulaDestinations.CREAR_TAREA)
                    "Gasto" -> navController.navigate(LulaDestinations.crearMovimiento("EGRESO"))
                    "Ingreso" -> navController.navigate(LulaDestinations.crearMovimiento("INGRESO"))
                    else -> navController.navigate(LulaDestinations.proximamente(opcion))
                }
            },
        )
    }
}
