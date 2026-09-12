package com.fastek.notea.ui.notes

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
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fastek.notea.data.local.entity.Note
import com.fastek.notea.data.local.entity.TypeNote

@Composable
fun NotesScreen(viewModel: NotesViewModel) {
    val etat by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(etat.matiereNom, style = MaterialTheme.typography.headlineSmall)
        Text(
            text = "Moyenne actuelle : ${etat.moyenne?.let { String.format("%.2f", it) } ?: "--"} / 20",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(16.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            RadioButton(
                selected = etat.nouveauType == TypeNote.DEVOIR,
                onClick = { viewModel.onTypeChange(TypeNote.DEVOIR) },
                enabled = !etat.devoirsAtteints
            )
            Text("Devoir")
            Spacer(Modifier.width(16.dp))
            RadioButton(
                selected = etat.nouveauType == TypeNote.INTERROGATION,
                onClick = { viewModel.onTypeChange(TypeNote.INTERROGATION) }
            )
            Text("Interrogation")
        }
        if (etat.devoirsAtteints) {
            Text(
                "Limite de 2 devoirs atteinte pour ce semestre.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 8.dp)
        ) {
            OutlinedTextField(
                value = etat.nouvelleValeur,
                onValueChange = viewModel::onValeurChange,
                label = { Text("Note /20") },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(8.dp))
            Button(onClick = viewModel::ajouterNote) {
                Text("Ajouter")
            }
        }

        etat.erreur?.let { erreur ->
            Text(erreur, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        Text("Notes saisies", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (etat.notes.isEmpty()) {
            Text(
                "Aucune note pour l'instant.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(etat.notes, key = { it.id }) { note ->
                    LigneNote(note = note, onSupprimer = { viewModel.supprimerNote(note) })
                }
            }
        }
    }
}

@Composable
private fun LigneNote(note: Note, onSupprimer: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(if (note.type == TypeNote.DEVOIR) "Devoir" else "Interrogation")
        },
        supportingContent = { Text(note.date.toString()) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(String.format("%.1f", note.valeur), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onSupprimer) {
                    Text("Suppr.")
                }
            }
        }
    )
}
