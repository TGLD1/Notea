package com.fastek.notea.ui.parametres

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val OPTIONS = listOf(
    "profil" to "Profil",
    "apropos-app" to "À propos de l'application",
    "apropos-dev" to "À propos du développeur",
    "confidentialite" to "Politique de confidentialité",
    "cgu" to "Conditions d'utilisation"
)

@Composable
fun ParametresScreen(onNavigate: (route: String) -> Unit) {
    LazyColumn {
        items(OPTIONS) { (route, label) ->
            ListItem(
                headlineContent = { Text(label) },
                modifier = Modifier.clickable { onNavigate(route) }
            )
            HorizontalDivider()
        }
    }
}
