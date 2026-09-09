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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.domain.reference.NiveauxReference

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
            SelecteurDeroulant(
                label = "Classe",
                options = NiveauxReference.NIVEAUX,
                selection = etat.niveau,
                onSelection = viewModel::onNiveauChange
            )
        }

        if (NiveauxReference.necessiteSerie(etat.niveau)) {
            item {
                SelecteurDeroulant(
                    label = "Série",
                    options = NiveauxReference.SERIES,
                    selection = etat.serie ?: "",
                    onSelection = viewModel::onSerieChange
                )
            }
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
                        value = choix.coefficient,
                        onValueChange = { nouveau ->
                            if (nouveau.isEmpty() || (nouveau.length <= 2 && nouveau.all(Char::isDigit))) {
                                viewModel.onCoefficientChange(choix.nom, nouveau)
                            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelecteurDeroulant(
    label: String,
    options: List<String>,
    selection: String,
    onSelection: (String) -> Unit
) {
    var etendu by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = etendu,
        onExpandedChange = { etendu = it }
    ) {
        OutlinedTextField(
            value = selection,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = etendu) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = etendu,
            onDismissRequest = { etendu = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelection(option)
                        etendu = false
                    }
                )
            }
        }
    }
}
