package com.fastek.notea.ui.parametres

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fastek.notea.data.local.entity.TypeEtablissement

@Composable
fun ProfilScreen(viewModel: ProfilViewModel) {
    val etat by viewModel.uiState.collectAsState()
    val eleveId = etat.eleveId

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Profil", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = etat.nom,
            onValueChange = { valeur -> eleveId?.let { viewModel.onNomChange(it, valeur) } },
            label = { Text("Nom") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = etat.prenom,
            onValueChange = { valeur -> eleveId?.let { viewModel.onPrenomChange(it, valeur) } },
            label = { Text("Prénom") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = etat.matricule,
            onValueChange = { valeur -> eleveId?.let { viewModel.onMatriculeChange(it, valeur) } },
            label = { Text("Numéro matricule") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))
        Text("En-tête du bulletin PDF (facultatif)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = etat.dateNaissance,
            onValueChange = { valeur -> eleveId?.let { viewModel.onDateNaissanceChange(it, valeur) } },
            label = { Text("Date de naissance (JJ/MM/AAAA)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = etat.lieuNaissance,
            onValueChange = { valeur -> eleveId?.let { viewModel.onLieuNaissanceChange(it, valeur) } },
            label = { Text("Lieu de naissance") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = etat.effectif,
            onValueChange = { valeur -> eleveId?.let { viewModel.onEffectifChange(it, valeur) } },
            label = { Text("Effectif de la classe") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = etat.aptitudeEps,
            onValueChange = { valeur -> eleveId?.let { viewModel.onAptitudeEpsChange(it, valeur) } },
            label = { Text("Aptitude EPS (Apte / Inapte)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = etat.redoublant,
            onValueChange = { valeur -> eleveId?.let { viewModel.onRedoublantChange(it, valeur) } },
            label = { Text("Redoublant (Oui / Non)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        LigneInfo("Classe", etat.classe)
        LigneInfo(
            "Établissement",
            if (etat.typeEtablissement == TypeEtablissement.PUBLIC) "Public" else "Privé"
        )
        LigneInfo("Année scolaire", etat.anneeScolaire)
        LigneInfo("Objectif annuel", "${etat.objectifAnnuel.toInt()}/20")

        Spacer(Modifier.height(16.dp))
        Text(
            "Classe, établissement et objectif annuel se modifient depuis l'onboarding ou l'écran Objectifs.",
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
