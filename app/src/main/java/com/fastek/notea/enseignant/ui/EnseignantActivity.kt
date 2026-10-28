package com.fastek.notea.enseignant.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fastek.notea.NoteaApplication
import com.fastek.notea.enseignant.data.repository.EnseignantRepository
import com.fastek.notea.ui.intro.SplashFastek
import com.fastek.notea.ui.theme.NoteaTheme
import kotlinx.coroutines.launch

class EnseignantActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val app = application as NoteaApplication
        setContent { EnseignantApp(app) }
    }
}

@Composable
private fun EnseignantApp(app: NoteaApplication) {
    val repo = app.enseignantRepository
    val vm: EnseignantViewModel = viewModel(factory = EnseignantViewModelFactory(repo))
    val contexte = LocalContext.current
    val scope = rememberCoroutineScope()
    var erreurRestauration by remember { mutableStateOf<String?>(null) }
    // Animation FASTEK à chaque lancement à froid (conservée à la rotation de l'écran).
    var splashActif by rememberSaveable { mutableStateOf(true) }

    NoteaTheme {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            if (splashActif) {
                SplashFastek(onTermine = { splashActif = false })
            } else {
                val etat by vm.etat.collectAsState()
                when (val e = etat) {
                    is EtatProfesseur.Chargement -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is EtatProfesseur.Absent -> PremierDemarrageScreen(
                        erreurRestauration = erreurRestauration,
                        onRestaurer = { uri ->
                            scope.launch {
                                erreurRestauration = restaurerEnseignantDepuisFichier(contexte, app.sauvegardeEnseignant, uri)
                            }
                        },
                        onValider = { nom, prenom, matiere, type, annee, etablissement ->
                            vm.creerProfesseur(nom, prenom, matiere, type, annee, etablissement)
                        }
                    )
                    is EtatProfesseur.Present -> Navigation(vm, repo, app, e)
                }
            }
        }
    }
}

@Composable
private fun Navigation(
    vm: EnseignantViewModel,
    repo: EnseignantRepository,
    app: NoteaApplication,
    etat: EtatProfesseur.Present
) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "accueil") {
        composable("accueil") {
            AccueilScreen(
                vm = vm,
                professeur = etat.professeur,
                onOuvrirClasse = { id -> nav.navigate("classe/$id") },
                onParametres = { nav.navigate("parametres") }
            )
        }
        composable("parametres") {
            ParametresScreen(
                onProfil = { nav.navigate("parametres/profil") },
                onSauvegarde = { nav.navigate("parametres/sauvegarde") },
                onJournal = { nav.navigate("parametres/journal") },
                onRetour = { nav.popBackStack() }
            )
        }
        composable("parametres/profil") {
            ProfilScreen(
                professeur = etat.professeur,
                onEnregistrer = vm::mettreAJourProfesseur,
                onRetour = { nav.popBackStack() }
            )
        }
        composable("parametres/sauvegarde") {
            SauvegardeEcran(manager = app.sauvegardeEnseignant, onRetour = { nav.popBackStack() })
        }
        composable("parametres/journal") {
            JournalEcran(onRetour = { nav.popBackStack() })
        }
        composable("classe/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entree ->
            val id = entree.arguments?.getLong("id") ?: 0L
            val classeVm: ClasseViewModel = viewModel(key = "classe$id", factory = EnseignantViewModelFactory(repo, id))
            ClasseScreen(vm = classeVm, onRetour = { nav.popBackStack() })
        }
    }
}
