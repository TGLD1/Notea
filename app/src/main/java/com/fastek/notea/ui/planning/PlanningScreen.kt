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

private val FORMAT_DATE_ECRAN = DateTimeFormatter.ofPattern("dd/MM/yyyy")

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

        ChampDate(dateTexte = etat.dateTexte, onDateChange = viewModel::onDateTexteChange)
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

/** Champ date en lecture seule : le bouton « Choisir » ouvre un calendrier (plus de saisie à la main). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChampDate(dateTexte: String, onDateChange: (String) -> Unit) {
    var ouvert by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = dateTexte,
        onValueChange = {},
        readOnly = true,
        label = { Text("Date") },
        trailingIcon = { TextButton(onClick = { ouvert = true }) { Text("Choisir") } },
        modifier = Modifier.fillMaxWidth()
    )

    if (ouvert) {
        val departMillis = try {
            LocalDate.parse(dateTexte, FORMAT_DATE_ECRAN).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        } catch (e: Exception) {
            null
        }
        val etatCalendrier = rememberDatePickerState(initialSelectedDateMillis = departMillis)
        DatePickerDialog(
            onDismissRequest = { ouvert = false },
            confirmButton = {
                TextButton(onClick = {
                    etatCalendrier.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onDateChange(date.format(FORMAT_DATE_ECRAN))
                    }
                    ouvert = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { ouvert = false }) { Text("Annuler") } }
        ) {
            DatePicker(state = etatCalendrier)
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
