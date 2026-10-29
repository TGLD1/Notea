package com.fastek.notea.enseignant.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastek.notea.diagnostic.Journal
import com.fastek.notea.enseignant.data.entity.Classe
import com.fastek.notea.enseignant.data.entity.Professeur
import kotlinx.coroutines.launch

@Composable
fun AccueilScreen(
    vm: EnseignantViewModel,
    professeur: Professeur,
    onOuvrirClasse: (Long) -> Unit,
    onParametres: () -> Unit
) {
    val classes by vm.classes.collectAsState()
    val totalEleves by vm.totalEleves.collectAsState()
    val erreurVm by vm.erreur.collectAsState()
    val contexte = LocalContext.current
    val scope = rememberCoroutineScope()
    var creation by remember { mutableStateOf(false) }
    var edition by remember { mutableStateOf<Classe?>(null) }
    var suppression by remember { mutableStateOf<Classe?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var exportEnCours by remember { mutableStateOf(false) }

    fun ouvrirCreation() {
        Journal.ok("ui.nouvelleClasse")
        creation = true
    }

    fun exporter() {
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

    FondEcran {
        Column(Modifier.fillMaxSize()) {
            EnTete(
                titre = "Bonjour, ${professeur.prenom.trim().substringBefore(' ').ifBlank { professeur.nom }}",
                sousTitre = "${professeur.matiere} · ${professeur.anneeScolaire}",
                droite = { BoutonEntete("Réglages", onParametres) }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PuceInfo(pluriel(classes.size, "classe"))
                    PuceInfo(pluriel(totalEleves, "élève"))
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 112.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                erreurVm?.let { texte -> item { BandeauErreur(texte, onFermer = vm::effacerErreur) } }
                message?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
                if (classes.isEmpty()) {
                    item {
                        EtatVide(
                            "📚", "Aucune classe pour le moment",
                            "Crée ta première classe, puis ajoute tes élèves."
                        )
                    }
                    item {
                        BoutonPlein("＋  Créer ma première classe", onClick = ::ouvrirCreation, modifier = Modifier.fillMaxWidth())
                    }
                } else {
                    item {
                        BoutonPlein("＋  Nouvelle classe", onClick = ::ouvrirCreation, modifier = Modifier.fillMaxWidth())
                    }
                    item { TitreSection("MES CLASSES") }
                    items(classes, key = { it.id }) { classe ->
                        val effectif by remember(classe.id) { vm.effectif(classe.id) }.collectAsState(initial = 0)
                        CarteClasse(
                            classe = classe, effectif = effectif,
                            onOuvrir = { onOuvrirClasse(classe.id) },
                            onModifier = { edition = classe },
                            onSupprimer = { suppression = classe }
                        )
                    }
                    item { Spacer(Modifier.height(4.dp)) }
                    item { CarteExport(enCours = exportEnCours, onExporter = ::exporter) }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = ::ouvrirCreation,
            modifier = Modifier.align(Alignment.BottomEnd).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shape = CircleShape
        ) { Text("＋  Nouvelle classe", style = MaterialTheme.typography.labelLarge) }
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
        Dialogue(
            titre = "Supprimer « ${classe.nom} » ?",
            onFermer = { suppression = null },
            confirmer = { TextButton(onClick = { vm.supprimerClasse(classe); suppression = null }) { Text("Supprimer") } },
            annuler = { TextButton(onClick = { suppression = null }) { Text("Annuler") } }
        ) {
            Text("Les élèves, les évaluations et toutes les notes de cette classe seront effacés. Cette action est définitive.")
        }
    }
}

@Composable
private fun CarteClasse(
    classe: Classe,
    effectif: Int,
    onOuvrir: () -> Unit,
    onModifier: () -> Unit,
    onSupprimer: () -> Unit
) {
    val couleur = Couleurs.pourTexte(classe.nom)
    var menu by remember { mutableStateOf(false) }
    CarteDouce(Modifier.fillMaxWidth(), onClick = onOuvrir) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(initialesClasse(classe.nom), couleur, 52.dp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(classe.nom, style = MaterialTheme.typography.titleMedium)
                Text(
                    pluriel(effectif, "élève") + " · coef. ${classe.coefficient}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Box {
                Text(
                    "⋮",
                    modifier = Modifier.clip(CircleShape).clickable { menu = true }.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(text = { Text("Modifier") }, onClick = { menu = false; onModifier() })
                    DropdownMenuItem(text = { Text("Supprimer") }, onClick = { menu = false; onSupprimer() })
                }
            }
        }
    }
}

@Composable
private fun BandeauErreur(texte: String, onFermer: () -> Unit) {
    CarteDouce(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp, end = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                texte, modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = onFermer) { Text("OK") }
        }
    }
}

@Composable
private fun CarteExport(enCours: Boolean, onExporter: () -> Unit) {
    CarteDouce(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).padding(12.dp)
                ) { Text("📊", fontSize = 22.sp) }
                Column(Modifier.padding(start = 14.dp)) {
                    Text("Exporter vers Excel", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Une feuille par classe, avec moyennes, rangs et statistiques.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            BoutonPlein(
                if (enCours) "Création du classeur…" else "Créer et partager",
                onClick = onExporter, enabled = !enCours, modifier = Modifier.fillMaxWidth()
            )
        }
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
    Dialogue(
        titre = titre,
        onFermer = onFermer,
        confirmer = {
            TextButton(enabled = nom.isNotBlank() && coef != null && coef > 0, onClick = { onValider(nom, coef ?: 1) }) {
                Text("Valider")
            }
        },
        annuler = { TextButton(onClick = onFermer) { Text("Annuler") } }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Champ(nom, { nom = it }, "Nom (ex. 3ème A)")
            Champ(
                coefficient, { coefficient = it.filter(Char::isDigit) }, "Coefficient de la matière",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}
