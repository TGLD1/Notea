package com.fastek.notea.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fastek.notea.data.local.entity.TypeEtablissement

@Composable
fun OnboardingScreen(viewModel: OnboardingViewModel) {
    val etat by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Bienvenue sur Notea", style = MaterialTheme.typography.headlineSmall)
        }

        item {
            OutlinedTextField(
                value = etat.nom,
                onValueChange = viewModel::onNomChange,
                label = { Text("Nom") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = etat.prenom,
                onValueChange = viewModel::onPrenomChange,
                label = { Text("Prénom") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            OutlinedTextField(
                value = etat.classe,
                onValueChange = viewModel::onClasseChange,
                label = { Text("Classe") },
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Text("Type d'établissement", style = MaterialTheme.typography.titleMedium)
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = etat.typeEtablissement == TypeEtablissement.PUBLIC,
                    onClick = { viewModel.onTypeEtablissementChange(TypeEtablissement.PUBLIC) }
                )
                Text("Public (2 semestres)")
                Spacer(Modifier.width(16.dp))
                RadioButton(
                    selected = etat.typeEtablissement == TypeEtablissement.PRIVE,
                    onClick = { viewModel.onTypeEtablissementChange(TypeEtablissement.PRIVE) }
                )
                Text("Privé (3 trimestres)")
            }
        }

        item {
            Text("Tes matières", style = MaterialTheme.typography.titleMedium)
        }

        items(etat.matieres) { choix ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = choix.selectionnee,
                    onCheckedChange = { viewModel.onToggleMatiere(choix.nom) }
                )
                Text(choix.nom, modifier = Modifier.weight(1f))
                if (choix.selectionnee) {
                    OutlinedTextField(
                        value = choix.coefficient.toString(),
                        onValueChange = { valeur ->
                            valeur.toIntOrNull()?.let { viewModel.onCoefficientChange(choix.nom, it) }
                        },
                        label = { Text("Coef") },
                        modifier = Modifier.width(80.dp)
                    )
                }
            }
        }

        etat.erreur?.let { erreur ->
            item {
                Text(erreur, color = MaterialTheme.colorScheme.error)
            }
        }

        item {
            Button(
                onClick = viewModel::validerEtCreerProfil,
                enabled = etat.peutValider && !etat.enCours,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (etat.enCours) "Création..." else "Commencer")
            }
        }
    }
}
