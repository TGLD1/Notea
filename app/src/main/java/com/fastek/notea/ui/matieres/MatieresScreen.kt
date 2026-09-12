package com.fastek.notea.ui.matieres

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * [onMatiereClick] reçoit (matiereId, periodeId) pour naviguer vers la saisie des notes.
 */
@Composable
fun MatieresScreen(
    viewModel: MatieresViewModel,
    onMatiereClick: (matiereId: Long, periodeId: Long) -> Unit = { _, _ -> }
) {
    val etat by viewModel.uiState.collectAsState()

    if (etat.chargement) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (etat.periodesDisponibles.size > 1) {
            item {
                Row(modifier = Modifier.padding(16.dp)) {
                    etat.periodesDisponibles.forEach { numero ->
                        if (numero == etat.periodeNumero) {
                            Button(onClick = { viewModel.selectionnerPeriode(numero) }) {
                                Text("Semestre $numero")
                            }
                        } else {
                            OutlinedButton(onClick = { viewModel.selectionnerPeriode(numero) }) {
                                Text("Semestre $numero")
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
        }

        if (etat.matieres.isEmpty()) {
            item {
                Text(
                    "Aucune matière — ajoute-en depuis l'onboarding pour l'instant.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            items(etat.matieres) { item ->
                ListItem(
                    headlineContent = { Text(item.matiere.nom) },
                    supportingContent = { Text("Coefficient ${item.matiere.coefficient}") },
                    trailingContent = {
                        Text(
                            text = item.moyenne?.let { String.format("%.2f", it) } ?: "--",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    modifier = Modifier.clickable {
                        etat.periodeId?.let { periodeId -> onMatiereClick(item.matiere.id, periodeId) }
                    }
                )
            }
        }
    }
}
