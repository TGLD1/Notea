package com.fastek.notea.enseignant.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.fastek.notea.data.local.entity.TypeEtablissement
import java.time.LocalDate

/** Année scolaire en cours : de septembre à décembre « 2026-2027 », de janvier à août « 2025-2026 ». */
internal fun anneeScolaireCourante(aujourdhui: LocalDate = LocalDate.now()): String {
    val debut = if (aujourdhui.monthValue >= 9) aujourdhui.year else aujourdhui.year - 1
    return "$debut-${debut + 1}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PremierDemarrageScreen(
    onValider: (nom: String, prenom: String, matiere: String, type: TypeEtablissement, annee: String, etablissement: String) -> Unit
) {
    var nom by rememberSaveable { mutableStateOf("") }
    var prenom by rememberSaveable { mutableStateOf("") }
    var matiere by rememberSaveable { mutableStateOf("") }
    var etablissement by rememberSaveable { mutableStateOf("") }
    var annee by rememberSaveable { mutableStateOf(anneeScolaireCourante()) }
    var type by rememberSaveable { mutableStateOf(TypeEtablissement.PUBLIC) }

    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Notea Enseignant", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Renseigne ton profil une seule fois. Les calculs sont ceux de Notea, classe par classe.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(4.dp))
        OutlinedTextField(
            value = nom, onValueChange = { nom = it }, label = { Text("Nom") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            value = prenom, onValueChange = { prenom = it }, label = { Text("Prénoms") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            value = matiere, onValueChange = { matiere = it }, label = { Text("Matière enseignée (ex. PCT)") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            value = etablissement, onValueChange = { etablissement = it },
            label = { Text("Établissement (facultatif)") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        )
        OutlinedTextField(
            value = annee, onValueChange = { annee = it }, label = { Text("Année scolaire") },
            singleLine = true, modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done)
        )
        Text("Type d'établissement", style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = type == TypeEtablissement.PUBLIC,
                onClick = { type = TypeEtablissement.PUBLIC },
                label = { Text("Public : 2 semestres") }
            )
            FilterChip(
                selected = type == TypeEtablissement.PRIVE,
                onClick = { type = TypeEtablissement.PRIVE },
                label = { Text("Privé : 3 trimestres") }
            )
        }
        Spacer(Modifier.height(8.dp))
        Button(
            onClick = { onValider(nom, prenom, matiere, type, annee, etablissement) },
            enabled = nom.isNotBlank() && matiere.isNotBlank() && annee.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text("Commencer") }
    }
}
