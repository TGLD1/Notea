package com.fastek.notea.enseignant.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastek.notea.data.local.entity.TypeNote
import com.fastek.notea.enseignant.data.entity.Evaluation
import com.fastek.notea.enseignant.data.entity.Inscrit
import com.fastek.notea.enseignant.data.entity.NoteEleve
import com.fastek.notea.enseignant.data.entity.PeriodeClasse
import com.fastek.notea.enseignant.domain.ListeElevesParser
import java.time.format.DateTimeFormatter

private val FORMAT_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

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

    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
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
            EnTete(
                titre = classe?.nom.orEmpty(),
                sousTitre = professeur?.matiere,
                gauche = { BoutonEntete("‹ Retour", onRetour) }
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PuceInfo("coef. ${classe?.coefficient ?: 1}")
                    PuceInfo(pluriel(inscrits.size, "élève"))
                }
            }
            OngletsPilules(
                titres = listOf("Élèves", "Notes", "Stats"),
                selection = onglet,
                onChoisir = { onglet = it },
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp)
            )
            if (onglet != 0) {
                SelecteurPeriode(
                    periodes = periodes,
                    nomPeriode = professeur?.typeEtablissement?.nomPeriode ?: "Période",
                    numero = numero,
                    onChoisir = vm::choisirPeriode
                )
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
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
        Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        periodes.forEach { p ->
            PuceChoix(texte = "$nomPeriode ${p.numero}", choisie = p.numero == numero, onClick = { onChoisir(p.numero) })
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
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            BoutonPlein("＋  Ajouter", { ajout = true }, Modifier.weight(1f))
            BoutonContour("Coller une liste", { collage = true }, Modifier.weight(1f))
        }
        if (inscrits.isEmpty()) {
            EtatVide(
                "🧑‍🎓", "Aucun élève",
                "Ajoute-les un par un, ou colle une liste entière : un élève par ligne."
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(inscrits, key = { _, i -> i.id }) { index, inscrit ->
                CarteDouce(Modifier.fillMaxWidth(), onClick = { edition = inscrit }) {
                    Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Avatar(initiales(inscrit.nom, inscrit.prenom), Couleurs.pourTexte(inscrit.nom + inscrit.prenom), 40.dp)
                        Column(Modifier.padding(start = 12.dp).weight(1f)) {
                            Text(inscrit.nom, style = MaterialTheme.typography.titleSmall)
                            if (inscrit.prenom.isNotBlank()) {
                                Text(
                                    inscrit.prenom, style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Text(
                            "${index + 1}", style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
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
    Dialogue(
        titre = titre,
        onFermer = onFermer,
        confirmer = {
            TextButton(enabled = nom.isNotBlank(), onClick = { onValider(nom, prenom, matricule) }) { Text("Valider") }
        },
        annuler = {
            Row {
                if (onSupprimer != null) {
                    TextButton(onClick = onSupprimer) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
                }
                TextButton(onClick = onFermer) { Text("Annuler") }
            }
        }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Champ(nom, { nom = it }, "Nom")
            Champ(prenom, { prenom = it }, "Prénoms")
            Champ(matricule, { matricule = it }, "Matricule (facultatif)")
        }
    }
}

@Composable
private fun CollageDialog(onAjouter: (String) -> Unit, onFermer: () -> Unit) {
    var texte by remember { mutableStateOf("") }
    val nombre = remember(texte) { ListeElevesParser.analyser(texte).size }
    Dialogue(
        titre = "Coller une liste",
        onFermer = onFermer,
        confirmer = { TextButton(enabled = nombre > 0, onClick = { onAjouter(texte) }) { Text("Ajouter") } },
        annuler = { TextButton(onClick = onFermer) { Text("Annuler") } }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                "Un élève par ligne : « NOM Prénoms », ou séparés par « ; » ou une tabulation.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Champ(texte, { texte = it }, "Liste des élèves", singleLine = false, minLines = 6, maxLines = 10)
            Text(
                if (nombre > 1) "$nombre élèves détectés" else "$nombre élève détecté",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
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
        BoutonPlein(
            "＋  Nouvelle évaluation", { message = null; ajout = true },
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
        )
        message?.let {
            Text(
                it, color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
            )
        }
        if (evaluations.isEmpty()) {
            EtatVide(
                "✏️", "Aucune évaluation",
                "Crée une interrogation ou un devoir pour cette période, puis saisis les notes."
            )
        }
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(evaluations, key = { it.id }) { ev ->
                val saisies = notes.count { it.evaluationId == ev.id }
                CarteEvaluation(
                    libelle = libelleEvaluation(ev, evaluations),
                    evaluation = ev, saisies = saisies, effectif = effectif,
                    onSaisir = { onSaisir(ev) },
                    onSupprimer = { suppression = ev }
                )
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
        Dialogue(
            titre = "Supprimer cette évaluation ?",
            onFermer = { suppression = null },
            confirmer = { TextButton(onClick = { vm.supprimerEvaluation(ev); suppression = null }) { Text("Supprimer") } },
            annuler = { TextButton(onClick = { suppression = null }) { Text("Annuler") } }
        ) {
            Text("Toutes les notes saisies pour « ${libelleEvaluation(ev, evaluations)} » seront effacées.")
        }
    }
}

@Composable
private fun CarteEvaluation(
    libelle: String,
    evaluation: Evaluation,
    saisies: Int,
    effectif: Int,
    onSaisir: () -> Unit,
    onSupprimer: () -> Unit
) {
    val estDevoir = evaluation.type == TypeNote.DEVOIR
    val couleur = if (estDevoir) Couleurs.VioletVif else Couleurs.Cyan
    var menu by remember { mutableStateOf(false) }
    val fraction = if (effectif == 0) 0f else saisies.toFloat() / effectif
    CarteDouce(Modifier.fillMaxWidth(), onClick = onSaisir) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(6.dp).fillMaxHeight().background(couleur))
            Column(Modifier.padding(16.dp).weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(libelle, style = MaterialTheme.typography.titleMedium)
                        Text(
                            (if (estDevoir) "Devoir" else "Interrogation") + " · ${evaluation.date.format(FORMAT_DATE)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = couleur
                        )
                    }
                    Box {
                        Text(
                            "⋮",
                            modifier = Modifier.clip(CircleShape).clickable { menu = true }.padding(horizontal = 12.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Supprimer") }, onClick = { menu = false; onSupprimer() })
                        }
                    }
                }
                ProgressionFine(fraction, if (saisies == effectif && effectif > 0) Couleurs.Succes else couleur)
                Text(
                    "$saisies / $effectif notes saisies",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EvaluationDialog(onValider: (TypeNote, String) -> Unit, onFermer: () -> Unit) {
    var type by remember { mutableStateOf(TypeNote.INTERROGATION) }
    var intitule by remember { mutableStateOf("") }
    Dialogue(
        titre = "Nouvelle évaluation",
        onFermer = onFermer,
        confirmer = { TextButton(onClick = { onValider(type, intitule) }) { Text("Créer") } },
        annuler = { TextButton(onClick = onFermer) { Text("Annuler") } }
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PuceChoix("Interrogation", type == TypeNote.INTERROGATION) { type = TypeNote.INTERROGATION }
                PuceChoix("Devoir", type == TypeNote.DEVOIR) { type = TypeNote.DEVOIR }
            }
            Champ(intitule, { intitule = it }, "Intitulé (facultatif)")
        }
    }
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
    val fraction = if (inscrits.isEmpty()) 0f else saisies.toFloat() / inscrits.size

    EnTete(
        titre = libelle,
        sousTitre = "$saisies / ${inscrits.size} notes · sur 20 · case vide si absent",
        droite = { BoutonEntete("Terminer", onTerminer) }
    ) {
        ProgressionFine(fraction, Color.White, fond = Color.White.copy(alpha = 0.25f))
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(inscrits, key = { _, i -> i.id }) { _, inscrit ->
            val texte = textes[inscrit.id].orEmpty()
            val valeur = texte.replace(',', '.').toDoubleOrNull()
            val erreur = texte.isNotEmpty() && (valeur == null || valeur > 20.0)
            val teinte = when {
                erreur -> MaterialTheme.colorScheme.error
                valeur != null -> Couleurs.pourMoyenne(valeur)
                else -> MaterialTheme.colorScheme.outline
            }
            CarteDouce(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Avatar(initiales(inscrit.nom, inscrit.prenom), Couleurs.pourTexte(inscrit.nom + inscrit.prenom), 40.dp)
                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                        Text(inscrit.nom, style = MaterialTheme.typography.titleSmall)
                        if (inscrit.prenom.isNotBlank()) {
                            Text(
                                inscrit.prenom, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OutlinedTextField(
                        value = texte,
                        onValueChange = { saisi ->
                            // La frappe qui ferait dépasser 20 est ignorée : impossible de saisir plus de 20.
                            val propre = filtrerSaisieNote(saisi, texte)
                            textes[inscrit.id] = propre
                            val v = propre.replace(',', '.').toDoubleOrNull()
                            when {
                                propre.isEmpty() -> onSaisir(evaluation.id, inscrit.id, null)
                                v != null -> onSaisir(evaluation.id, inscrit.id, v)
                                else -> Unit
                            }
                        },
                        isError = erreur,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        textStyle = MaterialTheme.typography.titleMedium.copy(textAlign = TextAlign.Center, color = teinte),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = teinte,
                            unfocusedBorderColor = teinte.copy(alpha = 0.55f)
                        ),
                        modifier = Modifier.width(92.dp),
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
    val couleursTranches = listOf(Couleurs.Danger, Couleurs.Alerte, Couleurs.CyanClair, Couleurs.Succes)
    val maximum = (s.repartition.maxOrNull() ?: 1).coerceAtLeast(1)

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            CarteDouce(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    AnneauMoyenne(s.moyenneClasse, 116.dp)
                    Column(Modifier.padding(start = 20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Moyenne de la classe", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "${s.effectifNote} élève(s) sur ${resultats.lignes.size} ont une moyenne",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Indicateur("Plus forte", formaterMoyenne(s.plusForte), Couleurs.Succes, Modifier.weight(1f))
                Indicateur("Plus faible", formaterMoyenne(s.plusFaible), Couleurs.Danger, Modifier.weight(1f))
                Indicateur("Réussite", formaterTaux(s.tauxReussite), Couleurs.VioletVif, Modifier.weight(1f))
            }
        }
        item {
            CarteDouce(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Répartition des moyennes", style = MaterialTheme.typography.titleMedium)
                    tranches.forEachIndexed { i, titre ->
                        val effectif = s.repartition.getOrElse(i) { 0 }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(titre, Modifier.width(60.dp), style = MaterialTheme.typography.bodySmall)
                            ProgressionFine(effectif.toFloat() / maximum, couleursTranches[i], Modifier.weight(1f))
                            Text(
                                "$effectif", Modifier.width(32.dp).padding(start = 8.dp),
                                style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
        }
        item { TitreSection("CLASSEMENT") }
        if (classement.isEmpty()) {
            item { EtatVide("🏆", "Pas encore de classement", "Ajoute des élèves et saisis des notes pour voir les rangs.") }
        }
        items(classement, key = { it.inscrit.id }) { ligne ->
            val rang = ligne.ligne.rang
            CarteDouce(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.width(40.dp), contentAlignment = Alignment.Center) {
                        when (rang) {
                            1 -> Text("🥇", fontSize = 24.sp)
                            2 -> Text("🥈", fontSize = 24.sp)
                            3 -> Text("🥉", fontSize = 24.sp)
                            null -> Text("—", color = MaterialTheme.colorScheme.outline)
                            else -> Text(
                                "$rang", style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(Modifier.padding(start = 8.dp).weight(1f)) {
                        Text(ligne.inscrit.nom, style = MaterialTheme.typography.titleSmall)
                        if (ligne.inscrit.prenom.isNotBlank()) {
                            Text(
                                ligne.inscrit.prenom, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    val couleur = Couleurs.pourMoyenne(ligne.ligne.moyenne)
                    Box(
                        Modifier.clip(CircleShape).background(couleur.copy(alpha = 0.14f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            formaterMoyenne(ligne.ligne.moyenne), color = couleur,
                            fontWeight = FontWeight.Bold, fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Indicateur(libelle: String, valeur: String, couleur: Color, modifier: Modifier) {
    CarteDouce(modifier) {
        Column(Modifier.padding(12.dp).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(valeur, color = couleur, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                libelle, style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
