package com.fastek.notea.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Point d'entrée temporaire — sert uniquement à valider que le pipeline de build
 * (Gradle + Kotlin + Compose) produit un APK installable. Sera remplacée par la
 * vraie navigation (Onboarding -> Dashboard) à l'étape suivante.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoteaPlaceholder()
        }
    }
}

@Composable
private fun NoteaPlaceholder() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Notea — build OK ✓")
                Text("Prochaine étape : les vrais écrans")
            }
        }
    }
}
