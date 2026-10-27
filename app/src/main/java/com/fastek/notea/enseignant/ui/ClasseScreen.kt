package com.fastek.notea.enseignant.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fastek.notea.data.local.entity.TypeNote
import com.fastek.notea.enseignant.data.entity.Evaluation
import com.fastek.notea.enseignant.data.entity.Inscrit
import com.fastek.notea.enseignant.data.entity.NoteEleve
import com.fastek.notea.enseignant.data.entity.PeriodeClasse
import com.fastek.notea.enseignant.domain.ListeElevesParser
import java.time.format.DateTimeFormatter

private val FORMAT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClasseScreen(vm: ClasseViewModel, onRetour: () -> Unit) {
    val classe by vm.classe.collectAsState()
    val professeur by vm.professeur.collectAsState()
    val periodes by vm.periodes.collectAsState()
    val numero by vm.numero.collectAsState()
    val inscrits by vm.inscrits.collectAsState()
    val evaluations by vm.evaluations.collectAsState()
    val notes by vm.notes.collectAsState()
    val resultats by vm.resultats.collectAsState()
    var onglet by rememberSaveable { mutableIntStateOf(0) }
    var enSaisie by remember { mutableStateOf<Evaluation?>(null) }

    BackHandler(enabled = enSaisie != null) { enSaisie = null }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(classe?.nom.orEmpty()) },
                navigationIcon = {
                    TextButton(onClick = { if (enSaisie != null) enSaisie = null else onRetour() }) { Text("Retour") }
                }
            )
        }
    ) { marge ->
        Column(Modifier.padding(marge).fillMaxSize()) {
            val saisie = enSaisie
            if (saisie != null) {
                SaisieRapide(
                    evaluation = saisie,
                    libelle = libelleEvaluation(saisie, evaluations),
                    inscrits = inscrits,
                    notes = notes,
                    onSaisir = vm::saisirNote,
                    onTerminer = { enSaisie = null }
                )
            } else {
                TabRow(selectedTabIndex = onglet) {
                    listOf("Élèves", "Notes", "Statistiques").forEachIndexed { i, titre ->
                        Tab(selected = onglet == i, onClick = { onglet = i }, text = { Text(titre) })
                    }
                }
                if (onglet != 0) {
                    SelecteurPeriode(
                        periodes = periodes,
                        nomPeriode = professeur?.typeEtablissement?.nomPeriode ?: "Période",
                        numero = numero,
                        onChoisir = vm::choisirPeriode
                    )
                }
                when (onglet) {
                    0 -> ElevesTab(inscrits, vm)
                    1 -> NotesTab(evaluations, notes, inscrits.size, vm, onSaisir = { enSaisie = it })
                    else -> StatsTab(resultats)
                }
            }
        }
    }
}

@Composable
private fun SelecteurPeriode(
    periodes: List<PeriodeClasse>,
    nomPeriode: String,
    numero: Int,
    onChoisir: (Int) -> Unit
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periodes.forEach { p ->
            FilterChip(
                selected = p.numero == numero,
                onClick = { onChoisir(p.numero) },
                label = { Text("$nomPeriode ${p.numero}") }
            )
        }
    }
}

// --- Élèves -------------------------------------------------------------------------------------

@Composable
private fun ElevesTab(inscrits: List<Inscrit>, vm: ClasseViewModel) {
    var ajout by remember { mutableStateOf(false) }
    var collage by remember { mutableStateOf(false) }
    var edition by remember { mutableStateOf<Inscrit?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { ajout = true }) { Text("Ajouter") }
            OutlinedButton(onClick = { collage = true }) { Text("Coller une liste") }
        }
        if (inscrits.isEmpty()) {
            Text(
                "Aucun élève. Ajoute-les un par un, ou colle une liste (un élève par ligne).",
                modifier = Modifier.padding(16.dp)
            )
        }
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(inscrits, key = { _, i -> i.id }) { index, inscrit ->
                Row(
                    Modifier.fillMaxWidth().clickable { edition = inscrit }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Text("${index + 1}.", Modifier.width(36.dp))
                    Text(nomComplet(inscrit))
                }
                HorizontalDivider()
            }
        }
    }

    if (ajout) {
        EleveDialog(
            titre = "Nouvel élève", initial = null,
            onValider = { nom, prenom, matricule -> vm.ajouterInscrit(nom, prenom, matricule); ajout = false },
            onSupprimer = null,
            onFermer = { ajout = false }
        )
    }
    edition?.let { inscrit ->
        EleveDialog(
            titre = "Modifier l'élève", initial = inscrit,
            onValider = { nom, prenom, matricule -> vm.modifierInscrit(inscrit, nom, prenom, matricule); edition = null },
            onSupprimer = { vm.supprimerInscrit(inscrit); edition = null },
            onFermer = { edition = null }
        )
    }
    if (collage) {
        CollageDialog(
            onAjouter = { texte -> vm.ajouterListe(texte) { collage = false } },
            onFermer = { collage = false }
        )
    }
}

@Composable
private fun EleveDialog(
    titre: String,
    initial: Inscrit?,
    onValider: (nom: String, prenom: String, matricule: String) -> Unit,
    onSupprimer: (() -> Unit)?,
    onFermer: () -> Unit
) {
    var nom by remember { mutableStateOf(initial?.nom.orEmpty()) }
    var prenom by remember { mutableStateOf(initial?.prenom.orEmpty()) }
    var matricule by remember { mutableStateOf(initial?.matricule.orEmpty()) }
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text(titre) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nom, onValueChange = { nom = it }, label = { Text("Nom") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = prenom, onValueChange = { prenom = it }, label = { Text("Prénoms") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = matricule, onValueChange = { matricule = it }, label = { Text("Matricule (facultatif)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(enabled = nom.isNotBlank(), onClick = { onValider(nom, prenom, matricule) }) { Text("Valider") }
        },
        dismissButton = {
            Row {
                if (onSupprimer != null) TextButton(onClick = onSupprimer) { Text("Supprimer") }
                TextButton(onClick = onFermer) { Text("Annuler") }
            }
        }
    )
}

@Composable
private fun CollageDialog(onAjouter: (String) -> Unit, onFermer: () -> Unit) {
    var texte by remember { mutableStateOf("") }
    val nombre = remember(texte) { ListeElevesParser.analyser(texte).size }
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Coller une liste d'élèves") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Un élève par ligne : « NOM Prénoms », ou séparés par « ; » ou une tabulation.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = texte, onValueChange = { texte = it },
                    modifier = Modifier.fillMaxWidth(), minLines = 6, maxLines = 10
                )
                Text(
                    if (nombre > 1) "$nombre élèves détectés" else "$nombre élève détecté",
                    style = MaterialTheme.typography.labelMedium
                )
            }
        },
        confirmButton = {
            TextButton(enabled = nombre > 0, onClick = { onAjouter(texte) }) { Text("Ajouter") }
        },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } }
    )
}

// --- Notes et évaluations -------------------------------------------------------------------------

@Composable
private fun NotesTab(
    evaluations: List<Evaluation>,
    notes: List<NoteEleve>,
    effectif: Int,
    vm: ClasseViewModel,
    onSaisir: (Evaluation) -> Unit
) {
    var ajout by remember { mutableStateOf(false) }
    var suppression by remember { mutableStateOf<Evaluation?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(12.dp)) {
            Button(onClick = { message = null; ajout = true }) { Text("Nouvelle évaluation") }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
        if (evaluations.isEmpty()) {
            Text(
                "Aucune évaluation dans cette période. Crée une interrogation ou un devoir, puis saisis les notes.",
                modifier = Modifier.padding(16.dp)
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(evaluations, key = { it.id }) { ev ->
                val saisies = notes.count { it.evaluationId == ev.id }
                Card(Modifier.fillMaxWidth().clickable { onSaisir(ev) }) {
                    Column(Modifier.padding(16.dp)) {
                        Text(libelleEvaluation(ev, evaluations), style = MaterialTheme.typography.titleMedium)
                        Text(
                            (if (ev.type == TypeNote.DEVOIR) "Devoir" else "Interrogation") +
                                " · ${ev.date.format(FORMAT_DATE)} · $saisies/$effectif notes",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Row {
                            TextButton(onClick = { onSaisir(ev) }) { Text("Saisir les notes") }
                            TextButton(onClick = { suppression = ev }) { Text("Supprimer") }
                        }
                    }
                }
            }
        }
    }

    if (ajout) {
        EvaluationDialog(
            onValider = { type, intitule ->
                vm.ajouterEvaluation(type, intitule) { reussi ->
                    if (!reussi) message = "Cette période compte déjà 2 devoirs."
                }
                ajout = false
            },
            onFermer = { ajout = false }
        )
    }
    suppression?.let { ev ->
        AlertDialog(
            onDismissRequest = { suppression = null },
            title = { Text("Supprimer cette évaluation ?") },
            text = { Text("Toutes les notes saisies pour « ${libelleEvaluation(ev, evaluations)} » seront effacées.") },
            confirmButton = {
                TextButton(onClick = { vm.supprimerEvaluation(ev); suppression = null }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { suppression = null }) { Text("Annuler") } }
        )
    }
}

@Composable
private fun EvaluationDialog(onValider: (TypeNote, String) -> Unit, onFermer: () -> Unit) {
    var type by remember { mutableStateOf(TypeNote.INTERROGATION) }
    var intitule by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onFermer,
        title = { Text("Nouvelle évaluation") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = type == TypeNote.INTERROGATION,
                        onClick = { type = TypeNote.INTERROGATION },
                        label = { Text("Interrogation") }
                    )
                    FilterChip(
                        selected = type == TypeNote.DEVOIR,
                        onClick = { type = TypeNote.DEVOIR },
                        label = { Text("Devoir") }
                    )
                }
                OutlinedTextField(
                    value = intitule, onValueChange = { intitule = it },
                    label = { Text("Intitulé (facultatif)") },
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { TextButton(onClick = { onValider(type, intitule) }) { Text("Créer") } },
        dismissButton = { TextButton(onClick = onFermer) { Text("Annuler") } }
    )
}

/** Saisie élève par élève : le clavier passe à l'élève suivant. Une case vide = élève absent. */
@Composable
private fun SaisieRapide(
    evaluation: Evaluation,
    libelle: String,
    inscrits: List<Inscrit>,
    notes: List<NoteEleve>,
    onSaisir: (evaluationId: Long, inscritId: Long, valeur: Double?) -> Unit,
    onTerminer: () -> Unit
) {
    val focus = LocalFocusManager.current
    val textes = remember(evaluation.id) {
        mutableStateMapOf<Long, String>().also { carte ->
            notes.filter { it.evaluationId == evaluation.id }.forEach { carte[it.inscritId] = formaterNote(it.valeur) }
        }
    }
    val saisies = textes.values.count { it.isNotBlank() }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(libelle, style = MaterialTheme.typography.titleMedium)
                Text("$saisies/${inscrits.size} notes · sur 20, case vide si absent", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onTerminer) { Text("Terminer") }
        }
        HorizontalDivider()
        LazyColumn(Modifier.fillMaxSize()) {
            itemsIndexed(inscrits, key = { _, i -> i.id }) { index, inscrit ->
                val texte = textes[inscrit.id].orEmpty()
                val valeur = texte.replace(',', '.').toDoubleOrNull()
                val erreur = texte.isNotEmpty() && (valeur == null || valeur > 20.0)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1}.", Modifier.width(32.dp))
                    Text(nomComplet(inscrit), Modifier.weight(1f))
                    OutlinedTextField(
                        value = texte,
                        onValueChange = { saisi ->
                            val propre = saisi.filter { it.isDigit() || it == ',' || it == '.' }
                            textes[inscrit.id] = propre
                            val v = propre.replace(',', '.').toDoubleOrNull()
                            when {
                                propre.isEmpty() -> onSaisir(evaluation.id, inscrit.id, null)
                                v != null && v <= 20.0 -> onSaisir(evaluation.id, inscrit.id, v)
                                else -> Unit
                            }
                        },
                        isError = erreur,
                        singleLine = true,
                        modifier = Modifier.width(96.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
                    )
                }
            }
        }
    }
}

// --- Statistiques ----------------------------------------------------------------------------------

@Composable
private fun StatsTab(resultats: ResultatsUi) {
    val s = resultats.periode.stats
    val classement = remember(resultats) {
        resultats.lignes.sortedWith(
            compareBy<LigneUi>({ it.ligne.rang ?: Int.MAX_VALUE }, { nomComplet(it.inscrit).lowercase() })
        )
    }
    val tranches = listOf("0 à 5", "5 à 10", "10 à 15", "15 à 20")
    val maximum = (s.repartition.maxOrNull() ?: 1).coerceAtLeast(1)

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    LigneStat("Élèves ayant une moyenne", "${s.effectifNote} / ${resultats.lignes.size}")
                    LigneStat("Moyenne de la classe", formaterMoyenne(s.moyenneClasse))
                    LigneStat("Plus forte moyenne", formaterMoyenne(s.plusForte))
                    LigneStat("Plus faible moyenne", formaterMoyenne(s.plusFaible))
                    LigneStat("Taux de réussite (≥ 10)", formaterTaux(s.tauxReussite))
                }
            }
        }
        item { Text("Répartition des moyennes", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 8.dp)) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                tranches.forEachIndexed { i, titre ->
                    val effectif = s.repartition.getOrElse(i) { 0 }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(titre, Modifier.width(64.dp), style = MaterialTheme.typography.bodySmall)
                        Box(Modifier.weight(1f).height(16.dp)) {
                            Box(
                                Modifier.fillMaxHeight()
                                    .fillMaxWidth(effectif.toFloat() / maximum)
                                    .background(MaterialTheme.colorScheme.primary)
                            )
                        }
                        Text("$effectif", Modifier.width(32.dp).padding(start = 8.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        item { Text("Classement", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp)) }
        if (classement.isEmpty()) {
            item { Text("Aucun élève dans cette classe.") }
        }
        items(classement, key = { it.inscrit.id }) { ligne ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Text(ligne.ligne.rang?.toString() ?: "—", Modifier.width(36.dp))
                Text(nomComplet(ligne.inscrit), Modifier.weight(1f))
                Text(formaterMoyenne(ligne.ligne.moyenne))
            }
            HorizontalDivider()
        }
    }
}

@Composable
private fun LigneStat(libelle: String, valeur: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(libelle, Modifier.weight(1f))
        Text(valeur, style = MaterialTheme.typography.titleSmall)
    }
}
