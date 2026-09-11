package com.fastek.notea.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fastek.notea.NoteaApplication
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.ui.dashboard.DashboardScreen
import com.fastek.notea.ui.dashboard.DashboardViewModel
import com.fastek.notea.ui.matieres.MatieresScreen
import com.fastek.notea.ui.matieres.MatieresViewModel
import com.fastek.notea.ui.notes.NotesScreen
import com.fastek.notea.ui.notes.NotesViewModel
import com.fastek.notea.ui.notes.NotesViewModelFactory
import com.fastek.notea.ui.objectifs.ObjectifsScreen
import com.fastek.notea.ui.objectifs.ObjectifsViewModel
import com.fastek.notea.ui.onboarding.OnboardingScreen
import com.fastek.notea.ui.onboarding.OnboardingViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as NoteaApplication
        setContent {
            NoteaApp(app)
        }
    }
}

@Composable
private fun NoteaApp(app: NoteaApplication) {
    val factory = remember { NoteaViewModelFactory(app.repository) }
    var profilExiste by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(Unit) {
        profilExiste = app.repository.getProfil() != null
    }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            when (profilExiste) {
                null -> Text("Chargement...")
                false -> {
                    val onboardingViewModel: OnboardingViewModel = viewModel(factory = factory)
                    val etat by onboardingViewModel.uiState.collectAsState()
                    if (etat.profilCreeAvecId != null) {
                        profilExiste = true
                    } else {
                        OnboardingScreen(viewModel = onboardingViewModel)
                    }
                }
                true -> NoteaNavHost(factory, app.repository)
            }
        }
    }
}

private data class OngletBas(val route: String, val label: String)

private val ONGLETS = listOf(
    OngletBas("dashboard", "Dashboard"),
    OngletBas("matieres", "Matières"),
    OngletBas("objectifs", "Objectifs")
)

@Composable
private fun NoteaNavHost(factory: NoteaViewModelFactory, repository: NoteaRepository) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = backStackEntry?.destination

                ONGLETS.forEach { onglet ->
                    NavigationBarItem(
                        selected = currentRoute?.hierarchy?.any { it.route == onglet.route } == true,
                        onClick = {
                            navController.navigate(onglet.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {},
                        label = { Text(onglet.label) }
                    )
                }
            }
        }
    ) { paddingInterieur ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(paddingInterieur)
        ) {
            composable("dashboard") {
                val vm: DashboardViewModel = viewModel(factory = factory)
                DashboardScreen(viewModel = vm)
            }
            composable("matieres") {
                val vm: MatieresViewModel = viewModel(factory = factory)
                MatieresScreen(viewModel = vm) { matiereId, periodeId ->
                    navController.navigate("notes/$matiereId/$periodeId")
                }
            }
            composable("objectifs") {
                val vm: ObjectifsViewModel = viewModel(factory = factory)
                ObjectifsScreen(viewModel = vm)
            }
            composable(
                route = "notes/{matiereId}/{periodeId}",
                arguments = listOf(
                    navArgument("matiereId") { type = NavType.LongType },
                    navArgument("periodeId") { type = NavType.LongType }
                )
            ) { backStackEntry ->
                val matiereId = backStackEntry.arguments?.getLong("matiereId") ?: 0L
                val periodeId = backStackEntry.arguments?.getLong("periodeId") ?: 0L
                val notesFactory = remember(matiereId, periodeId) {
                    NotesViewModelFactory(repository, matiereId, periodeId)
                }
                val vm: NotesViewModel = viewModel(factory = notesFactory)
                NotesScreen(viewModel = vm)
            }
        }
    }
}
