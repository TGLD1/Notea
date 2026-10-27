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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

class EnseignantActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val repo = (application as NoteaApplication).enseignantRepository
        setContent { EnseignantApp(repo) }
    }
}

@Composable
private fun EnseignantApp(repo: EnseignantRepository) {
    val vm: EnseignantViewModel = viewModel(factory = EnseignantViewModelFactory(repo))
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
                        onValider = { nom, prenom, matiere, type, annee, etablissement ->
                            vm.creerProfesseur(nom, prenom, matiere, type, annee, etablissement)
                        }
                    )
                    is EtatProfesseur.Present -> Navigation(vm, repo, e)
                }
            }
        }
    }
}

@Composable
private fun Navigation(vm: EnseignantViewModel, repo: EnseignantRepository, etat: EtatProfesseur.Present) {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "accueil") {
        composable("accueil") {
            AccueilScreen(vm = vm, professeur = etat.professeur, onOuvrirClasse = { id -> nav.navigate("classe/$id") })
        }
        composable("classe/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) { entree ->
            val id = entree.arguments?.getLong("id") ?: 0L
            val classeVm: ClasseViewModel = viewModel(key = "classe$id", factory = EnseignantViewModelFactory(repo, id))
            ClasseScreen(vm = classeVm, onRetour = { nav.popBackStack() })
        }
    }
}
