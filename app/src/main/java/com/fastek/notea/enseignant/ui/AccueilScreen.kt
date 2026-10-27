package com.fastek.notea.enseignant.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fastek.notea.diagnostic.Journal
import com.fastek.notea.enseignant.data.entity.Classe
import com.fastek.notea.enseignant.data.entity.Professeur
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccueilScreen(vm: EnseignantViewModel, professeur: Professeur, onOuvrirClasse: (Long) -> Unit) {
    val classes by vm.classes.collectAsState()
    val contexte = LocalContext.current
    val scope = rememberCoroutineScope()
    var creation by remember { mutableStateOf(false) }
    var edition by remember { mutableStateOf<Classe?>(null) }
    var suppression by remember { mutableStateOf<Classe?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var exportEnCours by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes classes · ${professeur.matiere}") },
                actions = {
                    TextButton(
                        enabled = !exportEnCours && classes.isNotEmpty(),
                        onClick = {
                            scope.launch {
                                exportEnCours = true
                                message = null
                                try {
                                    val export = vm.preparerExport()
                                    if (export == null || export.classes.isEmpty()) {
                                        message = "Aucune classe à exporter."
                                    } else {
                                        partagerExcel(contexte, creerFichierExcel(contexte, export))
                                        Journal.ok("export.excel", "classeur enseignant")
                                    }
                                } catch (e: Exception) {
                                    message = "Erreur lors de l'export : ${e.message}"
                                    Journal.erreur("export.excel", e)
                                } finally {
                                    exportEnCours = false
                                }
                            }
                        }
                    ) { Text(if (exportEnCours) "Création…" else "Exporter Excel") }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = { creation = true }) { Text("Nouvelle classe") }
        }
    ) { marge ->
        Column(Modifier.padding(marge).fillMaxSize()) {
            message?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(16.dp))
            }
            if (classes.isEmpty()) {
                Text(
                    "Aucune classe pour le moment. Crée ta première classe avec le bouton en bas, " +
                        "puis ajoute tes élèves.",
                    modifier = Modifier.padding(24.dp),
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(classes, key = { it.id }) { classe ->
                    val effectif by remember(classe.id) { vm.effectif(classe.id) }.collectAsState(initial = 0)
                    Card(Modifier.fillMaxWidth().clickable { onOuvrirClasse(classe.id) }) {
                        Column(Modifier.padding(16.dp)) {
                            Text(classe.nom, style = MaterialTheme.typography.titleLarge)
                            Text(
                                (if (effectif > 1) "$effectif élèves" else "$effectif élève") +
                                    " · coefficient ${classe.coefficient}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row {
                                TextButton(onClick = { edition = classe }) { Text("Modifier") }
                                TextButton(onClick = { suppression = classe }) { Text("Supprimer") }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creation) {
        ClasseDialog(
            titre = "Nouvelle classe", nomInitial = "", coefficientInitial = 1,
            onValider = { nom, coefficient -> vm.creerClasse(nom, coefficient); creation = false },
            onFermer = { creation = false }
        )
    }
    edition?.let { classe ->
        ClasseDialog(
            titre = "Modifier la classe", nomInitial = classe.nom, coefficientInitial = classe.coefficient,
            onValider = { nom, coefficient -> vm.modifierClasse(classe, nom, coefficient); edition = null },
            onFermer = { edition = null }
        )
    }
    suppression?.let { classe ->
        AlertDialog(
            onDismissRequest = { suppression = null },
            title = { Text("Supprimer « ${classe.nom} » ?") },
            text = { Text("Les élèves, les évaluations et toutes les notes de cette classe seront effacés. Cette action est définitive.") },
            confirmButton = {
                TextButton(onClick = { vm.supprimerClasse(classe); suppression = null }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { suppression = null }) { Text("Annuler") } }
        )
    }
}

@Composable
fun ClasseDialog(
    titre: String,
    nomInitial: String,
    coefficientInitial: Int,
    onValider: (nom: String, coefficient: Int) -> Unit,
    onFermer: () -> Unit
) {
    var nom by remember { mutableStateOf(nomInitial) }
    var coefficient by remember { mutableStateOf(coefficientInitial.toString()) }
    val coef = coefficient.toIntOrNull()
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(titre) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nom, onValueChange = { nom = it }, label = { Text("Nom (ex. 3ème A)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = coefficient, onValueChange = { coefficient = it.filter(Char::isDigit) },
                    label = { Text("Coefficient de la matière") },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = nom.isNotBlank() && coef != null && coef > 0,
                onClick = { onValider(nom, coef ?: 1) }
            ) { Text("Valider") }
        },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } }
    )
}
