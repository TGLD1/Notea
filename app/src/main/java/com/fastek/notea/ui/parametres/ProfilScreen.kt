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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.ui.commun.BoutonDate
import com.fastek.notea.ui.commun.ChoixDateEcran
import java.time.LocalDate

/**
 * Écran Profil. Chaque champ garde son propre texte local, enregistré en base à chaque frappe :
 * le texte affiché ne dépend pas du retour de la base, ce qui évitait que le curseur revienne au
 * début (« 10 » devenait « 01 »).
 */
@Composable
fun ProfilScreen(viewModel: ProfilViewModel) {
    val etat by viewModel.uiState.collectAsState()
    val eleveId = etat.eleveId

    // Les états locaux sont recréés quand le profil est chargé (eleveId passe de null à son identifiant).
    var nom by remember(eleveId) { mutableStateOf(etat.nom) }
    var prenom by remember(eleveId) { mutableStateOf(etat.prenom) }
    var matricule by remember(eleveId) { mutableStateOf(etat.matricule) }
    var dateNaissance by remember(eleveId) { mutableStateOf(etat.dateNaissance) }
    var lieuNaissance by remember(eleveId) { mutableStateOf(etat.lieuNaissance) }
    var effectif by remember(eleveId) { mutableStateOf(etat.effectif) }
    var aptitudeEps by remember(eleveId) { mutableStateOf(etat.aptitudeEps) }
    var redoublant by remember(eleveId) { mutableStateOf(etat.redoublant) }
    var choixDateNaissance by remember { mutableStateOf(false) }

    if (choixDateNaissance) {
        ChoixDateEcran(
            titre = "Date de naissance",
            valeurInitiale = dateNaissance,
            anneeMin = 1990,
            anneeMax = LocalDate.now().year,
            moisInitial = LocalDate.now().minusYears(16),
            onValider = { valeur ->
                dateNaissance = valeur
                eleveId?.let { viewModel.onDateNaissanceChange(it, valeur) }
                choixDateNaissance = false
            },
            onAnnuler = { choixDateNaissance = false }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Profil", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = nom,
            onValueChange = { valeur -> nom = valeur; eleveId?.let { viewModel.onNomChange(it, valeur) } },
            label = { Text("Nom") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = prenom,
            onValueChange = { valeur -> prenom = valeur; eleveId?.let { viewModel.onPrenomChange(it, valeur) } },
            label = { Text("Prénom") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = matricule,
            onValueChange = { valeur -> matricule = valeur; eleveId?.let { viewModel.onMatriculeChange(it, valeur) } },
            label = { Text("Numéro matricule") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))
        Text("En-tête du bulletin PDF (facultatif)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        BoutonDate(libelle = "Date de naissance", valeur = dateNaissance, onClick = { choixDateNaissance = true })
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = lieuNaissance,
            onValueChange = { valeur -> lieuNaissance = valeur; eleveId?.let { viewModel.onLieuNaissanceChange(it, valeur) } },
            label = { Text("Lieu de naissance") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = effectif,
            onValueChange = { valeur ->
                val chiffres = valeur.filter { it.isDigit() }.take(3)
                effectif = chiffres
                eleveId?.let { viewModel.onEffectifChange(it, chiffres) }
            },
            label = { Text("Effectif de la classe") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = aptitudeEps,
            onValueChange = { valeur -> aptitudeEps = valeur; eleveId?.let { viewModel.onAptitudeEpsChange(it, valeur) } },
            label = { Text("Aptitude en EPS (Apte / Inapte)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = redoublant,
            onValueChange = { valeur -> redoublant = valeur; eleveId?.let { viewModel.onRedoublantChange(it, valeur) } },
            label = { Text("Redoublant (Oui / Non)") },
            singleLine = true,
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
            "La classe et le type d'établissement sont définis à la création du profil. L'objectif annuel se modifie dans l'écran Objectifs.",
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
