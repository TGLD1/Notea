package com.fastek.notea.ui.planning

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.clickable
import com.fastek.notea.notification.ReglagesRappel
import com.fastek.notea.ui.commun.BoutonDate
import com.fastek.notea.ui.commun.ChoixDateEcran
import com.fastek.notea.ui.commun.FORMAT_DATE_ECRAN
import com.fastek.notea.ui.parametres.BandeauNotifications
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter


@Composable
fun PlanningScreen(viewModel: PlanningViewModel) {
    val etat by viewModel.uiState.collectAsState()
    val eleveId = etat.eleveId
    var choixDate by remember { mutableStateOf(false) }

    if (choixDate) {
        val annee = java.time.LocalDate.now().year
        ChoixDateEcran(
            titre = "Date de l'événement",
            valeurInitiale = etat.dateTexte,
            anneeMin = annee,
            anneeMax = annee + 2,
            onValider = { viewModel.onDateTexteChange(it); choixDate = false },
            onAnnuler = { choixDate = false }
        )
        return
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Planning", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        BandeauNotifications()
        Text(
            "Rappels envoyés à ${ReglagesRappel.heure(androidx.compose.ui.platform.LocalContext.current)}h00 : " +
                "deux jours avant, la veille et le jour même. Réglages dans Paramètres → Rappels et notifications.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = etat.titre,
            onValueChange = viewModel::onTitreChange,
            label = { Text("Intitulé (ex. : Devoir de mathématiques)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        BoutonDate(libelle = "Date", valeur = etat.dateTexte, onClick = { choixDate = true })
        Spacer(Modifier.height(8.dp))

        SelecteurMatiereOptionnel(
            matieres = etat.matieresDisponibles,
            selectionId = etat.matiereId,
            onSelection = viewModel::onMatiereChange
        )
        Spacer(Modifier.height(8.dp))

        etat.erreur?.let { erreur ->
            Text(erreur, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(8.dp))
        }

        Button(
            onClick = { eleveId?.let { viewModel.ajouterEvenement(it) } },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (etat.editionId == null) "Ajouter au planning" else "Enregistrer les modifications")
        }
        if (etat.editionId != null) {
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = viewModel::annulerEdition,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Annuler la modification")
            }
        }

        Spacer(Modifier.height(24.dp))
        Text("À venir", style = MaterialTheme.typography.titleMedium)

        if (etat.evenements.isEmpty()) {
            Text(
                "Aucun événement à venir. Ajoute un devoir, une interrogation ou un examen pour recevoir un rappel.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(etat.evenements, key = { it.id }) { evenement ->
                    ListItem(
                        modifier = Modifier.clickable { viewModel.commencerEdition(evenement) },
                        headlineContent = { Text(evenement.titre) },
                        supportingContent = {
                            Text(
                                evenement.matiereNom?.let { "${evenement.dateAffichee} — $it" }
                                    ?: evenement.dateAffichee
                            )
                        },
                        trailingContent = {
                            TextButton(onClick = { viewModel.supprimerEvenement(evenement) }) {
                                Text("Supprimer")
                            }
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelecteurMatiereOptionnel(
    matieres: List<com.fastek.notea.data.local.entity.Matiere>,
    selectionId: Long?,
    onSelection: (Long?) -> Unit
) {
    var etendu by remember { mutableStateOf(false) }
    val libelleSelection = matieres.find { it.id == selectionId }?.nom ?: "Aucune matière"

    ExposedDropdownMenuBox(expanded = etendu, onExpandedChange = { etendu = it }) {
        OutlinedTextField(
            value = libelleSelection,
            onValueChange = {},
            readOnly = true,
            label = { Text("Matière (facultatif)") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = etendu) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = etendu, onDismissRequest = { etendu = false }) {
            DropdownMenuItem(
                text = { Text("Aucune matière") },
                onClick = { onSelection(null); etendu = false }
            )
            matieres.forEach { matiere ->
                DropdownMenuItem(
                    text = { Text(matiere.nom) },
                    onClick = { onSelection(matiere.id); etendu = false }
                )
            }
        }
    }
}
