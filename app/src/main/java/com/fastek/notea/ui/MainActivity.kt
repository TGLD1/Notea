package com.fastek.notea.ui

import android.Manifest
import android.content.Intent
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.fastek.notea.ui.intro.PresentationScreen
import com.fastek.notea.ui.intro.PresentationVue
import com.fastek.notea.ui.theme.NoteaTheme
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import android.content.Intent
import androidx.activity.compose.setContent
import co.opensi.kkiapay.uikit.Kkiapay
import com.fastek.notea.BuildConfig
import com.fastek.notea.diagnostic.Journal
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import kotlinx.coroutines.launch
import com.fastek.notea.NoteaApplication
import com.fastek.notea.data.backup.SauvegardeManager
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
import com.fastek.notea.ui.parametres.BulletinScreen
import com.fastek.notea.ui.parametres.JournalScreen
import com.fastek.notea.ui.parametres.ParametresScreen
import com.fastek.notea.ui.parametres.ProfilScreen
import com.fastek.notea.ui.parametres.ProfilViewModel
import com.fastek.notea.ui.parametres.DonScreen
import com.fastek.notea.ui.parametres.EtatDon
import com.fastek.notea.ui.parametres.RappelScreen
import com.fastek.notea.ui.parametres.SauvegardeScreen
import com.fastek.notea.ui.parametres.restaurerDepuisFichier
import com.fastek.notea.ui.parametres.TextScreen
import com.fastek.notea.ui.parametres.TextesStatiques
import com.fastek.notea.ui.planning.PlanningScreen
import com.fastek.notea.ui.planning.PlanningViewModel
import com.fastek.notea.ui.statistiques.StatistiquesScreen
import com.fastek.notea.ui.statistiques.StatistiquesViewModel

class MainActivity : ComponentActivity() {

    /** Ouvre la fenêtre de paiement KkiaPay pour le don. */
    private fun lancerDon(montant: Int) {
        EtatDon.message = null
        try {
            Kkiapay.get().requestPayment(
                this,
                montant,
                reason = "Don à FASTEK pour Notea",
                api_key = BuildConfig.KKIAPAY_PUBLIC_KEY,
                sandbox = BuildConfig.KKIAPAY_SANDBOX,
                name = "Donateur Notea",
                partnerId = "notea-don",
                phone = "",
                email = "",
                paymentMethods = listOf("momo", "card")
            )
        } catch (e: Exception) {
            Journal.erreur("don.lancement", e)
            EtatDon.reussi = false
            EtatDon.message = "Impossible d'ouvrir le paiement. Vérifie ta connexion internet."
        }
    }

    @Deprecated("Requis par le SDK KkiaPay pour récupérer le résultat du paiement")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (BuildConfig.KKIAPAY_PUBLIC_KEY.isNotBlank()) {
            try {
                Kkiapay.get().handleActivityResult(requestCode, resultCode, data)
            } catch (e: Exception) {
                Journal.erreur("don.resultat", e)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        if (BuildConfig.KKIAPAY_PUBLIC_KEY.isNotBlank()) {
            Kkiapay.get().setListener { status, transactionId ->
                val succes = status.name == "SUCCESS"
                Journal.ok("don.statut.${status.name}")
                EtatDon.reussi = succes
                EtatDon.message = if (succes) {
                    "Merci pour ton don ! Référence : $transactionId"
                } else {
                    "Le paiement n'a pas abouti. Aucun montant n'a été débité."
                }
            }
        }
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
    val contexte = LocalContext.current
    var introVue by remember { mutableStateOf(PresentationVue.dejaVue(contexte)) }

    val lanceurPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* accordée ou non, les notifications restent un simple plus, pas bloquant */ }

    LaunchedEffect(Unit) {
        profilExiste = app.repository.getProfil() != null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            lanceurPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    NoteaTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (profilExiste) {
                null -> Text("Chargement...")
                false -> if (!introVue) {
                    PresentationScreen(onTerminer = {
                        PresentationVue.marquerVue(contexte)
                        introVue = true
                    })
                } else {
                    val onboardingViewModel: OnboardingViewModel = viewModel(factory = factory)
                    val etat by onboardingViewModel.uiState.collectAsState()
                    if (etat.profilCreeAvecId != null) {
                        profilExiste = true
                    } else {
                        val scope = rememberCoroutineScope()
                        val context = LocalContext.current
                        var erreurRestauration by remember { mutableStateOf<String?>(null) }
                        OnboardingScreen(
                            viewModel = onboardingViewModel,
                            erreurRestauration = erreurRestauration,
                            onRestaurer = { uri ->
                                scope.launch {
                                    val resultat = restaurerDepuisFichier(context, app.sauvegarde, uri)
                                    if (resultat == null) {
                                        profilExiste = true
                                    } else {
                                        erreurRestauration = resultat
                                    }
                                }
                            }
                        )
                    }
                }
                true -> NoteaNavHost(factory, app.repository, app.sauvegarde, onDon = { montant -> lancerDon(montant) })
            }
        }
    }
}

private data class OngletBas(val route: String, val label: String)

/** Onglets principaux seulement — Objectifs/Statistiques/Paramètres passent par le menu ⋮ du haut. */
private val ONGLETS = listOf(
    OngletBas("dashboard", "Accueil"),
    OngletBas("matieres", "Matières"),
    OngletBas("planning", "Planning")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteaNavHost(
    factory: NoteaViewModelFactory,
    repository: NoteaRepository,
    sauvegarde: SauvegardeManager,
    onDon: (Int) -> Unit
) {
    val navController = rememberNavController()
    var menuOuvert by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notea") },
                actions = {
                    TextButton(onClick = { menuOuvert = true }) {
                        Text("⋮", style = MaterialTheme.typography.titleLarge)
                    }
                    DropdownMenu(expanded = menuOuvert, onDismissRequest = { menuOuvert = false }) {
                        DropdownMenuItem(
                            text = { Text("Objectifs") },
                            onClick = {
                                menuOuvert = false
                                navController.navigate("objectifs")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Statistiques") },
                            onClick = {
                                menuOuvert = false
                                navController.navigate("statistiques")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Paramètres") },
                            onClick = {
                                menuOuvert = false
                                navController.navigate("parametres")
                            }
                        )
                    }
                }
            )
        },
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
            composable("planning") {
                val vm: PlanningViewModel = viewModel(factory = factory)
                PlanningScreen(viewModel = vm)
            }
            composable("objectifs") {
                val vm: ObjectifsViewModel = viewModel(factory = factory)
                ObjectifsScreen(viewModel = vm)
            }
            composable("statistiques") {
                val vm: StatistiquesViewModel = viewModel(factory = factory)
                StatistiquesScreen(viewModel = vm)
            }
            composable("parametres") {
                val context = LocalContext.current
                ParametresScreen { route ->
                    if (route == "signaler-probleme") {
                        val intent = Intent(Intent.ACTION_SENDTO).apply {
                            data = Uri.parse("mailto:tgldivin@gmail.com")
                            putExtra(Intent.EXTRA_SUBJECT, "Problème avec Notea")
                        }
                        context.startActivity(intent)
                    } else {
                        navController.navigate(route)
                    }
                }
            }
            composable("journal") {
                JournalScreen()
            }
            composable("bulletin") {
                BulletinScreen(repository)
            }
            composable("presentation") {
                PresentationScreen(onTerminer = { navController.popBackStack() })
            }
            composable("rappel") {
                RappelScreen()
            }
            composable("sauvegarde") {
                SauvegardeScreen(sauvegarde)
            }
            composable("don") {
                DonScreen(onDon = onDon)
            }
            composable("profil") {
                val vm: ProfilViewModel = viewModel(factory = factory)
                ProfilScreen(viewModel = vm)
            }
            composable("apropos-app") {
                TextScreen("À propos de l'application", TextesStatiques.A_PROPOS_APP)
            }
            composable("apropos-dev") {
                TextScreen("À propos du développeur", TextesStatiques.A_PROPOS_DEV)
            }
            composable("confidentialite") {
                TextScreen("Politique de confidentialité", TextesStatiques.CONFIDENTIALITE)
            }
            composable("cgu") {
                TextScreen("Conditions d'utilisation", TextesStatiques.CGU)
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
