package com.fastek.notea.ui.parametres

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fastek.notea.data.local.entity.TypeEtablissement

@Composable
fun ProfilScreen(viewModel: ProfilViewModel) {
    val profil by viewModel.profil.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Profil", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        profil?.let { eleve ->
            LigneInfo("Nom", eleve.nom)
            LigneInfo("Prénom", eleve.prenom)
            LigneInfo("Classe", eleve.classe)
            LigneInfo(
                "Établissement",
                if (eleve.typeEtablissement == TypeEtablissement.PUBLIC) "Public" else "Privé"
            )
            LigneInfo("Année scolaire", eleve.anneeScolaire)
            LigneInfo("Objectif annuel", "${eleve.objectifAnnuel.toInt()}/20")
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "La modification du profil sera bientôt disponible.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun LigneInfo(label: String, valeur: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(valeur, style = MaterialTheme.typography.bodyLarge)
    }
}
