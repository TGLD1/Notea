package com.fastek.notea.enseignant.ui

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fastek.notea.NoteaApplication
import com.fastek.notea.data.backup.ArchiveManager
import com.fastek.notea.diagnostic.Journal
import com.fastek.notea.enseignant.data.backup.ArchivesEnseignant
import com.fastek.notea.enseignant.data.entity.Professeur
import com.fastek.notea.enseignant.data.repository.EnseignantRepository
import com.fastek.notea.enseignant.rappel.RappelScheduler
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val FORMAT_JOUR: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

/**
 * Archive d'abord l'année en cours (sauvegarde JSON et classeur Excel), puis efface ses données.
 * Si l'archive échoue, rien n'est effacé. Retourne un message d'erreur, ou null si réussi.
 */
suspend fun passerALaNouvelleAnnee(
    context: Context,
    repo: EnseignantRepository,
    archives: ArchivesEnseignant,
    garderClasses: Boolean
): String? = try {
    val professeur = repo.obtenirProfesseur() ?: throw IllegalStateException("Profil introuvable.")
    archives.archiver(professeur)
    val suivante = ArchiveManager.anneeSuivante(professeur.anneeScolaire)
    val rappelsSupprimes = repo.nouvelleAnnee(professeur, suivante, garderClasses)
    rappelsSupprimes.forEach { RappelScheduler.annuler(context, it) }
    Journal.ok("annee.nouvelle", suivante)
    null
} catch (e: Exception) {
    Journal.erreur("annee.nouvelle", e)
    "Impossible de passer à la nouvelle année : ${e.message ?: e.javaClass.simpleName}"
}

@Composable
fun NouvelleAnneeScreen(professeur: Professeur, app: NoteaApplication, onRetour: () -> Unit) {
    val contexte = LocalContext.current
    val scope = rememberCoroutineScope()
    val suivante = remember(professeur.anneeScolaire) { ArchiveManager.anneeSuivante(professeur.anneeScolaire) }
    var garderClasses by remember { mutableStateOf(true) }
    var confirmation by remember { mutableStateOf(false) }
    var enCours by remember { mutableStateOf(false) }
    var erreur by remember { mutableStateOf<String?>(null) }

    EcranAvecEntete("Nouvelle année scolaire", onRetour, sousTitre = "${professeur.anneeScolaire}  →  $suivante") {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CarteDouce(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("📦  D'abord, une archive", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Avant d'effacer quoi que ce soit, Notea enregistre l'année ${professeur.anneeScolaire} : " +
                            "une sauvegarde complète (restaurable) et le classeur Excel avec toutes les moyennes. " +
                            "Tu les retrouveras dans « Archives ».",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            CarteDouce(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TitreSection("ET ENSUITE ?")
                    ChoixType(
                        titre = "Garder mes classes et mon emploi du temps",
                        detail = "Les élèves, les évaluations et les notes sont effacés. Les classes, les cours et les rappels restent.",
                        choisi = garderClasses, onClick = { garderClasses = true }
                    )
                    ChoixType(
                        titre = "Tout recommencer",
                        detail = "Les classes, les élèves, les notes, l'emploi du temps et les rappels sont effacés. Seul ton profil reste.",
                        choisi = !garderClasses, onClick = { garderClasses = false }
                    )
                }
            }
            erreur?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            BoutonPlein(
                if (enCours) "Archivage en cours…" else "Archiver et passer à $suivante",
                onClick = { confirmation = true }, enabled = !enCours, modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.navigationBarsPadding().height(8.dp))
        }
    }

    if (confirmation) {
        Dialogue(
            titre = "Passer à $suivante ?",
            onFermer = { confirmation = false },
            confirmer = {
                TextButton(onClick = {
                    confirmation = false
                    scope.launch {
                        enCours = true
                        erreur = passerALaNouvelleAnnee(contexte, app.enseignantRepository, app.archivesEnseignant, garderClasses)
                        enCours = false
                        if (erreur == null) onRetour()
                    }
                }) { Text("Archiver et continuer") }
            },
            annuler = { TextButton(onClick = { confirmation = false }) { Text("Annuler") } }
        ) {
            Text(
                "L'année ${professeur.anneeScolaire} sera archivée, puis " +
                    (if (garderClasses) "les élèves et les notes seront effacés." else "toutes les classes et leurs données seront effacées.")
            )
        }
    }
}

@Composable
fun ArchivesScreen(app: NoteaApplication, onRetour: () -> Unit) {
    val contexte = LocalContext.current
    val scope = rememberCoroutineScope()
    var archives by remember { mutableStateOf<List<ArchivesEnseignant.Archive>?>(null) }
    var actualisation by remember { mutableStateOf(0) }
    var aRestaurer by remember { mutableStateOf<ArchivesEnseignant.Archive?>(null) }
    var aSupprimer by remember { mutableStateOf<ArchivesEnseignant.Archive?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var erreur by remember { mutableStateOf(false) }

    LaunchedEffect(actualisation) { archives = app.archivesEnseignant.lister() }

    EcranAvecEntete("Archives", onRetour, sousTitre = "Les années scolaires passées") {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            message?.let {
                Text(it, color = if (erreur) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            val liste = archives
            if (liste != null && liste.isEmpty()) {
                EtatVide("📦", "Aucune archive", "L'archive d'une année est créée quand tu passes à la nouvelle année scolaire.")
            }
            liste.orEmpty().forEach { archive ->
                val resume = archive.resume
                CarteDouce(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            if (resume != null) "${resume.anneeScolaire} · ${resume.matiere}" else archive.json.nameWithoutExtension,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            (if (resume != null) {
                                pluriel(resume.classes, "classe") + " · " + pluriel(resume.eleves, "élève") + " · " +
                                    pluriel(resume.notes, "note") + " · "
                            } else {
                                "Fichier illisible · "
                            }) + "archivée le " +
                                Instant.ofEpochMilli(archive.json.lastModified()).atZone(ZoneId.systemDefault()).toLocalDate().format(FORMAT_JOUR),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (archive.excel != null) {
                                TextButton(onClick = { partagerExcel(contexte, archive.excel) }) { Text("Excel") }
                            }
                            TextButton(onClick = { partagerSauvegarde(contexte, archive.json) }) { Text("Sauvegarde") }
                            if (resume != null) TextButton(onClick = { aRestaurer = archive }) { Text("Restaurer") }
                            TextButton(onClick = { aSupprimer = archive }) {
                                Text("Supprimer", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(8.dp))
        }
    }

    aRestaurer?.let { archive ->
        Dialogue(
            titre = "Restaurer cette archive ?",
            onFermer = { aRestaurer = null },
            confirmer = {
                TextButton(onClick = {
                    aRestaurer = null
                    scope.launch {
                        val resultat = restaurerEnseignantDepuisTexte(
                            contexte, app.sauvegardeEnseignant, app.archivesEnseignant.lire(archive)
                        )
                        erreur = resultat != null
                        message = resultat ?: "Archive restaurée."
                    }
                }) { Text("Restaurer") }
            },
            annuler = { TextButton(onClick = { aRestaurer = null }) { Text("Annuler") } }
        ) {
            Text("Les classes, élèves, notes, cours et rappels actuels seront remplacés par ceux de cette archive. Pense à archiver l'année en cours avant.")
        }
    }
    aSupprimer?.let { archive ->
        Dialogue(
            titre = "Supprimer cette archive ?",
            onFermer = { aSupprimer = null },
            confirmer = {
                TextButton(onClick = {
                    aSupprimer = null
                    scope.launch {
                        app.archivesEnseignant.supprimer(archive)
                        actualisation++
                    }
                }) { Text("Supprimer", color = MaterialTheme.colorScheme.error) }
            },
            annuler = { TextButton(onClick = { aSupprimer = null }) { Text("Annuler") } }
        ) {
            Text("La sauvegarde et le classeur Excel de cette année seront effacés définitivement de ce téléphone.")
        }
    }
}
