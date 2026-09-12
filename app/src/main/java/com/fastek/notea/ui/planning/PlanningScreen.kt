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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PlanningScreen(viewModel: PlanningViewModel) {
    val etat by viewModel.uiState.collectAsState()
    val eleveId = etat.eleveId

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Planning", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        OutlinedTextField(
            value = etat.titre,
            onValueChange = viewModel::onTitreChange,
            label = { Text("Titre (ex : Devoir de Maths)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = etat.dateTexte,
                onValueChange = viewModel::onDateTexteChange,
                label = { Text("Date (JJ/MM/AAAA)") },
                modifier = Modifier.weight(1f)
            )
        }
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
            Text("Ajouter au planning")
        }

        Spacer(Modifier.height(24.dp))
        Text("À venir", style = MaterialTheme.typography.titleMedium)

        if (etat.evenements.isEmpty()) {
            Text(
                "Aucun événement pour l'instant.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(etat.evenements, key = { it.id }) { evenement ->
                    ListItem(
                        headlineContent = { Text(evenement.titre) },
                        supportingContent = {
                            Text(
                                evenement.matiereNom?.let { "${evenement.dateAffichee} — $it" }
                                    ?: evenement.dateAffichee
                            )
                        },
                        trailingContent = {
                            TextButton(onClick = { viewModel.supprimerEvenement(evenement) }) {
                                Text("Suppr.")
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
            label = { Text("Matière (optionnel)") },
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
