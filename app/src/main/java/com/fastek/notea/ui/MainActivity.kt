package com.fastek.notea.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
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
import com.fastek.notea.NoteaApplication
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
                true -> Text("Profil créé — Dashboard à venir")
            }
        }
    }
}
