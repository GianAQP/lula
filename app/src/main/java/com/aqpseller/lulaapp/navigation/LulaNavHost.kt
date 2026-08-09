package com.aqpseller.lulaapp.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.aqpseller.lulaapp.core.utils.DateTimeUtils
import com.aqpseller.lulaapp.domain.model.TipoActividad
import com.aqpseller.lulaapp.features.calendar.CalendarScreen
import com.aqpseller.lulaapp.features.care_circle.CareCircleScreen
import com.aqpseller.lulaapp.features.common.ProximamenteScreen
import com.aqpseller.lulaapp.features.daily_review.CerrarDiaScreen
import com.aqpseller.lulaapp.features.diary.DiaryCalendarScreen
import com.aqpseller.lulaapp.features.diary.DiaryEntryEditorScreen
import com.aqpseller.lulaapp.features.diary.DiaryListScreen
import com.aqpseller.lulaapp.features.finances.CrearMovimientoScreen
import com.aqpseller.lulaapp.features.finances.FinancesHistoryScreen
import com.aqpseller.lulaapp.features.finances.FinancesScreen
import com.aqpseller.lulaapp.features.goals.CrearMetaScreen
import com.aqpseller.lulaapp.features.goals.GoalsListScreen
import com.aqpseller.lulaapp.features.goals.MetaDetailScreen
import com.aqpseller.lulaapp.features.habits.CrearHabitoScreen
import com.aqpseller.lulaapp.features.habits.HabitDetailScreen
import com.aqpseller.lulaapp.features.habits.HabitsListScreen
import com.aqpseller.lulaapp.features.health.AccionTomaScreen
import com.aqpseller.lulaapp.features.health.CrearCitaScreen
import com.aqpseller.lulaapp.features.health.CrearMedicamentoScreen
import com.aqpseller.lulaapp.features.health.CitaDetailScreen
import com.aqpseller.lulaapp.features.health.HealthScreen
import com.aqpseller.lulaapp.features.health.MedicamentoDetailScreen
import com.aqpseller.lulaapp.features.history.HistoryScreen
import com.aqpseller.lulaapp.features.home.HomeScreen
import com.aqpseller.lulaapp.features.important_dates.CrearFechaImportanteScreen
import com.aqpseller.lulaapp.features.important_dates.ImportantDatesListScreen
import com.aqpseller.lulaapp.features.legal.LegalTextScreen
import com.aqpseller.lulaapp.features.lists.CrearListaScreen
import com.aqpseller.lulaapp.features.lists.ListDetailScreen
import com.aqpseller.lulaapp.features.lists.ListHistoryScreen
import com.aqpseller.lulaapp.features.lists.ListsScreen
import com.aqpseller.lulaapp.features.notes.NoteEditorScreen
import com.aqpseller.lulaapp.features.notes.NotesListScreen
import com.aqpseller.lulaapp.features.privacy.PrivacyGateScreen
import com.aqpseller.lulaapp.features.progress.ProgresoScreen
import com.aqpseller.lulaapp.features.reminders.RecordatorioAccionScreen
import com.aqpseller.lulaapp.features.routines.CrearRutinaScreen
import com.aqpseller.lulaapp.features.routines.RoutineDetailScreen
import com.aqpseller.lulaapp.features.routines.RoutinesListScreen
import com.aqpseller.lulaapp.features.family.CrearRetoFamiliarScreen
import com.aqpseller.lulaapp.features.family.FamiliaScreen
import com.aqpseller.lulaapp.features.family.RetosFamiliaresScreen
import com.aqpseller.lulaapp.features.profile.ProfileScreen
import com.aqpseller.lulaapp.features.proposito.EditarRespuestaPropositoScreen
import com.aqpseller.lulaapp.features.proposito.PropositoPersonalScreen
import com.aqpseller.lulaapp.features.settings.SettingsScreen
import com.aqpseller.lulaapp.features.tasks.CrearTareaScreen
import com.aqpseller.lulaapp.features.tasks.TaskDetailScreen
import com.aqpseller.lulaapp.features.tasks.TasksListScreen
import com.aqpseller.lulaapp.features.weekly_review.WeeklyReviewHistoryScreen
import com.aqpseller.lulaapp.features.weekly_review.WeeklyReviewScreen

/** Pantallas que solo se llega a ver detrás de Zona Privada — si se re-bloquea sola, sacar de acá. */
private val RUTAS_ZONA_PRIVADA = setOf(
    LulaDestinations.FINANZAS,
    LulaDestinations.FINANZAS_HISTORIAL,
    LulaDestinations.DIARIO,
    LulaDestinations.DIARIO_ENTRADA,
    LulaDestinations.DIARIO_CALENDARIO,
    LulaDestinations.NOTAS,
    LulaDestinations.NOTA_EDITOR,
)

@Composable
fun LulaNavHost(
    destinoInicial: String? = null,
    onDestinoConsumido: () -> Unit = {},
) {
    val navController = rememberNavController()
    var mostrarMenuAgregar by remember { mutableStateOf(false) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    /** Al tocar una notificación (con la app ya abierta o recién abriéndose), ir directo al
     * hábito/tarea en vez de solo abrir la app en Hoy. Ver `MainActivity`/`RecordatorioReceiver`. */
    LaunchedEffect(destinoInicial) {
        if (destinoInicial != null) {
            navController.navigate(destinoInicial)
            onDestinoConsumido()
        }
    }

    // Zona Privada puede re-bloquearse sola por inactividad (`SesionPrivadaState`) mientras el
    // usuario sigue parado en Finanzas/Diario/Notas — sin esto, la pantalla se queda abierta
    // mostrando datos privados aunque el candado ya se haya cerrado.
    val appViewModel: AppViewModel = hiltViewModel()
    val zonaPrivadaDesbloqueada by appViewModel.zonaPrivadaDesbloqueada.collectAsState()
    val bottomBarPosicion2 by appViewModel.bottomBarPosicion2.collectAsState()
    val bottomBarPosicion3 by appViewModel.bottomBarPosicion3.collectAsState()
    val bottomBarPosicion4 by appViewModel.bottomBarPosicion4.collectAsState()
    LaunchedEffect(zonaPrivadaDesbloqueada, currentRoute) {
        if (!zonaPrivadaDesbloqueada && currentRoute in RUTAS_ZONA_PRIVADA) {
            navController.navigate(LulaDestinations.HOY) {
                popUpTo(LulaDestinations.HOY) { inclusive = true }
            }
        }
    }

    Scaffold(
        topBar = {
            LulaTopBar(
                currentRoute = currentRoute,
                onAbrirAjustes = { navController.navigate(LulaDestinations.AJUSTES) },
                onAbrirPerfil = { navController.navigate(LulaDestinations.PERFIL) },
                onAbrirFamilia = { navController.navigate(LulaDestinations.FAMILIA) },
                onAbrirCirculoDeCuidado = { navController.navigate(LulaDestinations.CIRCULO_CUIDADO) },
                onVerHistorial = { navController.navigate(LulaDestinations.HISTORIAL) },
                onVerFinanzas = { navController.navigate(LulaDestinations.ZONA_PRIVADA_GATE) },
                onAbrirDiario = { navController.navigate(LulaDestinations.ZONA_PRIVADA_GATE_DIARIO) },
            )
        },
        bottomBar = {
            LulaBottomBar(
                currentRoute = currentRoute,
                posicion2Id = bottomBarPosicion2,
                posicion3Id = bottomBarPosicion3,
                posicion4Id = bottomBarPosicion4,
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
                    onVerTareas = { navController.navigate(LulaDestinations.TAREAS) },
                    onVerMetas = { navController.navigate(LulaDestinations.METAS) },
                    onVerRutinas = { navController.navigate(LulaDestinations.RUTINAS) },
                    onVerProgreso = { navController.navigate(LulaDestinations.PROGRESO) },
                    onVerListas = { navController.navigate(LulaDestinations.LISTAS) },
                    onVerSalud = { navController.navigate(LulaDestinations.SALUD) },
                    onVerFechasImportantes = { navController.navigate(LulaDestinations.FECHAS_IMPORTANTES) },
                    onVerNotas = { navController.navigate(LulaDestinations.ZONA_PRIVADA_GATE_NOTAS) },
                    onVerCalendario = { navController.navigate(LulaDestinations.CALENDARIO) },
                    onVerFamilia = { navController.navigate(LulaDestinations.FAMILIA) },
                    onVerCita = { id -> navController.navigate(LulaDestinations.citaDetalle(id)) },
                    onVerFechaImportante = { id -> navController.navigate(LulaDestinations.crearFechaImportante(id)) },
                    onVerMetaDetail = { id -> navController.navigate(LulaDestinations.metaDetalle(id)) },
                )
            }
            composable(
                route = LulaDestinations.PROXIMAMENTE,
                arguments = listOf(navArgument("titulo") { type = NavType.StringType }),
            ) { entry ->
                ProximamenteScreen(titulo = entry.arguments?.getString("titulo").orEmpty())
            }
            composable(
                route = LulaDestinations.CREAR_HABITO,
                arguments = listOf(navArgument("actividadId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                CrearHabitoScreen(
                    onGuardado = { navController.popBackStack() },
                    onVerHabitos = { navController.navigate(LulaDestinations.HABITOS) },
                )
            }
            composable(
                route = LulaDestinations.CREAR_TAREA,
                arguments = listOf(navArgument("actividadId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                CrearTareaScreen(
                    onGuardado = { navController.popBackStack() },
                    onVerTareas = { navController.navigate(LulaDestinations.TAREAS) },
                )
            }
            composable(
                route = LulaDestinations.CREAR_MOVIMIENTO,
                arguments = listOf(
                    navArgument("tipo") { type = NavType.StringType },
                    navArgument("movimientoId") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                CrearMovimientoScreen(
                    onGuardado = { navController.popBackStack() },
                    onVerFinanzas = { navController.navigate(LulaDestinations.FINANZAS) },
                )
            }
            composable(LulaDestinations.CERRAR_DIA) {
                CerrarDiaScreen(
                    onVolverAHoy = {
                        navController.navigate(LulaDestinations.HOY) {
                            popUpTo(LulaDestinations.HOY) { inclusive = true }
                        }
                    },
                    onVerHistorial = { navController.navigate(LulaDestinations.HISTORIAL) },
                )
            }
            composable(LulaDestinations.HISTORIAL) {
                HistoryScreen()
            }
            composable(LulaDestinations.HABITOS) {
                HabitsListScreen(
                    onHabitoClick = { id -> navController.navigate(LulaDestinations.habitoDetalle(id)) },
                    onNuevoHabito = { navController.navigate(LulaDestinations.crearHabito()) },
                )
            }
            composable(
                route = LulaDestinations.HABITO_DETALLE,
                arguments = listOf(navArgument("actividadId") { type = NavType.StringType }),
            ) {
                HabitDetailScreen(
                    onEditar = { id -> navController.navigate(LulaDestinations.crearHabito(id)) },
                    onEliminado = { navController.popBackStack() },
                )
            }
            composable(LulaDestinations.TAREAS) {
                TasksListScreen(
                    onTareaClick = { id -> navController.navigate(LulaDestinations.tareaDetalle(id)) },
                    onNuevaTarea = { navController.navigate(LulaDestinations.crearTarea()) },
                )
            }
            composable(
                route = LulaDestinations.TAREA_DETALLE,
                arguments = listOf(navArgument("actividadId") { type = NavType.StringType }),
            ) {
                TaskDetailScreen(
                    onEditar = { id -> navController.navigate(LulaDestinations.crearTarea(id)) },
                    onEliminada = { navController.popBackStack() },
                )
            }
            composable(LulaDestinations.ZONA_PRIVADA_GATE) {
                PrivacyGateScreen(
                    onDesbloqueado = {
                        navController.navigate(LulaDestinations.FINANZAS) {
                            popUpTo(LulaDestinations.ZONA_PRIVADA_GATE) { inclusive = true }
                        }
                    },
                )
            }
            composable(LulaDestinations.ZONA_PRIVADA_GATE_DIARIO) {
                PrivacyGateScreen(
                    onDesbloqueado = {
                        navController.navigate(LulaDestinations.DIARIO) {
                            popUpTo(LulaDestinations.ZONA_PRIVADA_GATE_DIARIO) { inclusive = true }
                        }
                    },
                )
            }
            composable(LulaDestinations.ZONA_PRIVADA_GATE_NOTAS) {
                PrivacyGateScreen(
                    onDesbloqueado = {
                        navController.navigate(LulaDestinations.NOTAS) {
                            popUpTo(LulaDestinations.ZONA_PRIVADA_GATE_NOTAS) { inclusive = true }
                        }
                    },
                )
            }
            composable(LulaDestinations.FINANZAS) {
                FinancesScreen(
                    onAgregarGasto = { navController.navigate(LulaDestinations.crearMovimiento("EGRESO")) },
                    onAgregarIngreso = { navController.navigate(LulaDestinations.crearMovimiento("INGRESO")) },
                    onVerHistorial = { navController.navigate(LulaDestinations.FINANZAS_HISTORIAL) },
                )
            }
            composable(LulaDestinations.FINANZAS_HISTORIAL) {
                FinancesHistoryScreen(
                    onMovimientoClick = { movimiento ->
                        navController.navigate(LulaDestinations.crearMovimiento(movimiento.tipo.name, movimiento.id))
                    },
                )
            }
            composable(LulaDestinations.METAS) {
                GoalsListScreen(
                    onMetaClick = { id -> navController.navigate(LulaDestinations.metaDetalle(id)) },
                    onNuevaMeta = { navController.navigate(LulaDestinations.crearMeta()) },
                )
            }
            composable(
                route = LulaDestinations.CREAR_META,
                arguments = listOf(navArgument("metaId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                CrearMetaScreen(
                    onGuardado = { navController.popBackStack() },
                    onVerMetas = { navController.navigate(LulaDestinations.METAS) },
                )
            }
            composable(
                route = LulaDestinations.META_DETALLE,
                arguments = listOf(navArgument("metaId") { type = NavType.StringType }),
            ) {
                MetaDetailScreen(
                    onEditar = { id -> navController.navigate(LulaDestinations.crearMeta(id)) },
                    onEliminada = { navController.popBackStack() },
                )
            }
            composable(LulaDestinations.RUTINAS) {
                RoutinesListScreen(
                    onRutinaClick = { id -> navController.navigate(LulaDestinations.rutinaDetalle(id)) },
                    onNuevaRutina = { navController.navigate(LulaDestinations.crearRutina()) },
                )
            }
            composable(
                route = LulaDestinations.CREAR_RUTINA,
                arguments = listOf(navArgument("actividadId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                CrearRutinaScreen(
                    onGuardado = { navController.popBackStack() },
                    onVerRutinas = { navController.navigate(LulaDestinations.RUTINAS) },
                )
            }
            composable(
                route = LulaDestinations.RUTINA_DETALLE,
                arguments = listOf(navArgument("actividadId") { type = NavType.StringType }),
            ) {
                RoutineDetailScreen(
                    onEditar = { id -> navController.navigate(LulaDestinations.crearRutina(id)) },
                    onEliminada = { navController.popBackStack() },
                )
            }
            composable(LulaDestinations.PROGRESO) {
                ProgresoScreen(
                    onVerRevisionSemanal = { navController.navigate(LulaDestinations.REVISION_SEMANAL) },
                )
            }
            composable(LulaDestinations.REVISION_SEMANAL) {
                WeeklyReviewScreen(
                    onVerHistorial = { navController.navigate(LulaDestinations.REVISION_SEMANAL_HISTORIAL) },
                    onGuardada = { navController.popBackStack() },
                )
            }
            composable(LulaDestinations.REVISION_SEMANAL_HISTORIAL) {
                WeeklyReviewHistoryScreen()
            }
            composable(
                route = LulaDestinations.RECORDATORIO_ACCION,
                arguments = listOf(navArgument("actividadId") { type = NavType.StringType }),
            ) {
                RecordatorioAccionScreen(onListo = { navController.popBackStack() })
            }
            composable(LulaDestinations.AJUSTES) {
                SettingsScreen()
            }
            composable(LulaDestinations.PERFIL) {
                ProfileScreen(
                    onVerProposito = { navController.navigate(LulaDestinations.PROPOSITO_PERSONAL) },
                    onVerTextoLegal = { tipo -> navController.navigate(LulaDestinations.textoLegal(tipo)) },
                )
            }
            composable(
                LulaDestinations.TEXTO_LEGAL,
                arguments = listOf(navArgument("tipo") { type = NavType.StringType }),
            ) {
                LegalTextScreen()
            }
            composable(LulaDestinations.PROPOSITO_PERSONAL) {
                PropositoPersonalScreen(
                    onPreguntaClick = { preguntaId ->
                        navController.navigate(LulaDestinations.editarRespuestaProposito(preguntaId))
                    },
                )
            }
            composable(
                LulaDestinations.EDITAR_RESPUESTA_PROPOSITO,
                arguments = listOf(navArgument("preguntaId") { type = NavType.StringType }),
            ) {
                EditarRespuestaPropositoScreen(
                    onGuardado = { navController.popBackStack() },
                    onEliminada = { navController.popBackStack() },
                )
            }
            composable(LulaDestinations.FAMILIA) {
                FamiliaScreen(
                    onEspacioCambiado = {
                        navController.navigate(LulaDestinations.HOY) {
                            popUpTo(LulaDestinations.HOY) { inclusive = true }
                        }
                    },
                    onVerRetosFamiliares = { navController.navigate(LulaDestinations.RETOS_FAMILIARES) },
                )
            }
            composable(LulaDestinations.RETOS_FAMILIARES) {
                RetosFamiliaresScreen(
                    onCrearReto = { navController.navigate(LulaDestinations.CREAR_RETO_FAMILIAR) },
                )
            }
            composable(LulaDestinations.CREAR_RETO_FAMILIAR) {
                CrearRetoFamiliarScreen(onGuardado = { navController.popBackStack() })
            }
            composable(LulaDestinations.LISTAS) {
                ListsScreen(
                    onListaClick = { id -> navController.navigate(LulaDestinations.listaDetalle(id)) },
                    onNuevaLista = { navController.navigate(LulaDestinations.CREAR_LISTA) },
                )
            }
            composable(LulaDestinations.CREAR_LISTA) {
                CrearListaScreen(
                    onGuardado = { navController.popBackStack() },
                    onVerListas = { navController.navigate(LulaDestinations.LISTAS) },
                )
            }
            composable(
                route = LulaDestinations.LISTA_DETALLE,
                arguments = listOf(navArgument("listaId") { type = NavType.StringType }),
            ) {
                ListDetailScreen(
                    onEliminada = { navController.popBackStack() },
                    onVerHistorial = { listaId -> navController.navigate(LulaDestinations.listaHistorial(listaId)) },
                )
            }
            composable(
                route = LulaDestinations.LISTA_HISTORIAL,
                arguments = listOf(navArgument("listaId") { type = NavType.StringType }),
            ) {
                ListHistoryScreen()
            }
            composable(LulaDestinations.SALUD) {
                HealthScreen(
                    onNuevoMedicamento = { navController.navigate(LulaDestinations.crearMedicamento()) },
                    onNuevaCita = { navController.navigate(LulaDestinations.crearCita()) },
                    onVerMedicamento = { id -> navController.navigate(LulaDestinations.medicamentoDetalle(id)) },
                    onVerCita = { id -> navController.navigate(LulaDestinations.citaDetalle(id)) },
                )
            }
            composable(
                route = LulaDestinations.CREAR_MEDICAMENTO,
                arguments = listOf(navArgument("actividadId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                CrearMedicamentoScreen(
                    onGuardado = { navController.popBackStack() },
                    onVerSalud = { navController.navigate(LulaDestinations.SALUD) },
                )
            }
            composable(
                route = LulaDestinations.MEDICAMENTO_DETALLE,
                arguments = listOf(navArgument("actividadId") { type = NavType.StringType }),
            ) {
                MedicamentoDetailScreen(
                    onEditar = { id -> navController.navigate(LulaDestinations.crearMedicamento(id)) },
                    onEliminado = { navController.popBackStack() },
                )
            }
            composable(
                route = LulaDestinations.CREAR_CITA,
                arguments = listOf(navArgument("actividadId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                CrearCitaScreen(
                    onGuardado = { navController.popBackStack() },
                    onVerSalud = { navController.navigate(LulaDestinations.SALUD) },
                )
            }
            composable(
                route = LulaDestinations.CITA_DETALLE,
                arguments = listOf(navArgument("actividadId") { type = NavType.StringType }),
            ) {
                CitaDetailScreen(
                    onEditar = { id -> navController.navigate(LulaDestinations.crearCita(id)) },
                    onEliminado = { navController.popBackStack() },
                )
            }
            composable(
                route = LulaDestinations.ACCION_TOMA,
                arguments = listOf(
                    navArgument("actividadId") { type = NavType.StringType },
                    navArgument("horario") { type = NavType.StringType },
                ),
            ) {
                AccionTomaScreen(onListo = { navController.popBackStack() })
            }
            composable(LulaDestinations.CIRCULO_CUIDADO) {
                CareCircleScreen()
            }
            composable(LulaDestinations.FECHAS_IMPORTANTES) {
                ImportantDatesListScreen(
                    onFechaClick = { id -> navController.navigate(LulaDestinations.crearFechaImportante(id)) },
                    onNuevaFecha = { navController.navigate(LulaDestinations.crearFechaImportante()) },
                )
            }
            composable(
                route = LulaDestinations.CREAR_FECHA_IMPORTANTE,
                arguments = listOf(navArgument("actividadId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                CrearFechaImportanteScreen(
                    onGuardado = { navController.popBackStack() },
                    onVerFechasImportantes = { navController.navigate(LulaDestinations.FECHAS_IMPORTANTES) },
                )
            }
            composable(LulaDestinations.NOTAS) {
                NotesListScreen(
                    onNotaClick = { id -> navController.navigate(LulaDestinations.notaEditor(id)) },
                    onNuevaNota = { navController.navigate(LulaDestinations.notaEditor()) },
                )
            }
            composable(
                route = LulaDestinations.NOTA_EDITOR,
                arguments = listOf(navArgument("notaId") { type = NavType.StringType; nullable = true; defaultValue = null }),
            ) {
                NoteEditorScreen(
                    onGuardado = { navController.popBackStack() },
                    onEliminada = { navController.popBackStack() },
                )
            }
            composable(LulaDestinations.DIARIO) {
                DiaryListScreen(
                    onEntradaClick = { id -> navController.navigate(LulaDestinations.diarioEntrada(id)) },
                    onNuevaEntrada = { navController.navigate(LulaDestinations.diarioEntrada()) },
                    onVerCalendario = { navController.navigate(LulaDestinations.DIARIO_CALENDARIO) },
                )
            }
            composable(
                route = LulaDestinations.DIARIO_ENTRADA,
                arguments = listOf(
                    navArgument("entradaId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("fecha") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) {
                DiaryEntryEditorScreen(
                    onGuardado = { navController.popBackStack() },
                    onEliminada = { navController.popBackStack() },
                )
            }
            composable(LulaDestinations.DIARIO_CALENDARIO) {
                DiaryCalendarScreen(
                    onDiaClick = { fecha, entradaId ->
                        navController.navigate(
                            LulaDestinations.diarioEntrada(
                                entradaId = entradaId,
                                fecha = if (entradaId == null) DateTimeUtils.localDateAEpochMillis(fecha) else null,
                            ),
                        )
                    },
                )
            }
            composable(LulaDestinations.CALENDARIO) {
                CalendarScreen(
                    onItemClick = { item ->
                        when (item.tipo) {
                            TipoActividad.HABITO -> navController.navigate(LulaDestinations.habitoDetalle(item.actividadId))
                            TipoActividad.TAREA -> navController.navigate(LulaDestinations.tareaDetalle(item.actividadId))
                            TipoActividad.MEDICAMENTO -> {
                                val horario = item.tomaHorario
                                if (horario != null) {
                                    navController.navigate(LulaDestinations.accionToma(item.actividadId, horario))
                                } else {
                                    navController.navigate(LulaDestinations.SALUD)
                                }
                            }
                            TipoActividad.CITA -> navController.navigate(LulaDestinations.citaDetalle(item.actividadId))
                            TipoActividad.FECHA_IMPORTANTE ->
                                navController.navigate(LulaDestinations.crearFechaImportante(item.actividadId))
                            TipoActividad.RUTINA -> navController.navigate(LulaDestinations.rutinaDetalle(item.actividadId))
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
                    "Hábito" -> navController.navigate(LulaDestinations.crearHabito())
                    "Tarea" -> navController.navigate(LulaDestinations.crearTarea())
                    "Rutina" -> navController.navigate(LulaDestinations.crearRutina())
                    "Meta" -> navController.navigate(LulaDestinations.crearMeta())
                    "Lista" -> navController.navigate(LulaDestinations.CREAR_LISTA)
                    "Gasto" -> navController.navigate(LulaDestinations.crearMovimiento("EGRESO"))
                    "Ingreso" -> navController.navigate(LulaDestinations.crearMovimiento("INGRESO"))
                    "Medicamento" -> navController.navigate(LulaDestinations.crearMedicamento())
                    "Cita" -> navController.navigate(LulaDestinations.crearCita())
                    "Fecha importante" -> navController.navigate(LulaDestinations.crearFechaImportante())
                    // Notas y Diario viven detrás de Zona Privada — la captura rápida desde
                    // "+" pasa primero por el gate, no directo a la lista/editor.
                    "Nota" -> navController.navigate(LulaDestinations.ZONA_PRIVADA_GATE_NOTAS)
                    "Diario" -> navController.navigate(LulaDestinations.ZONA_PRIVADA_GATE_DIARIO)
                    else -> navController.navigate(LulaDestinations.proximamente(opcion))
                }
            },
        )
    }
}
