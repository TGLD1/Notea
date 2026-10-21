package com.fastek.notea.ui.parametres

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.core.content.FileProvider
import com.fastek.notea.data.backup.ArchiveManager
import com.fastek.notea.data.backup.SauvegardeManager
import com.fastek.notea.diagnostic.Journal
import kotlinx.coroutines.launch
import java.io.File

private data class LigneArchive(val fichier: File, val resume: SauvegardeManager.ResumeArchive?)

/** Années précédentes archivées et bouton « Nouvelle année scolaire ». */
@Composable
fun ArchivesScreen(archives: ArchiveManager) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lignes by remember { mutableStateOf<List<LigneArchive>>(emptyList()) }
    var confirmer by remember { mutableStateOf(false) }
    var aSupprimer by remember { mutableStateOf<File?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var rechargement by remember { mutableStateOf(0) }

    LaunchedEffect(rechargement) {
        lignes = archives.lister().map { LigneArchive(it, archives.resumer(it)) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Années scolaires", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "« Nouvelle année » enregistre l'année en cours dans une archive, puis efface les notes et " +
                "les événements. Ton profil, tes matières, tes coefficients et tes objectifs sont conservés.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(12.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = { confirmer = true }) {
            Text("Démarrer une nouvelle année")
        }
        message?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(24.dp))
        Text("Années précédentes", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        if (lignes.isEmpty()) {
            Text("Aucune archive pour le moment.", style = MaterialTheme.typography.bodyMedium)
        }
        lignes.forEach { ligne ->
            val resume = ligne.resume
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(
                    if (resume != null) "${resume.anneeScolaire} — ${resume.classe}" else ligne.fichier.name,
                    style = MaterialTheme.typography.titleSmall
                )
                if (resume != null) {
                    resume.moyennesPeriodes.forEachIndexed { index, moyenne ->
                        Text(
                            "${resume.libellePeriode} ${index + 1} : ${moyenne?.let { String.format("%.2f", it) } ?: "--"}/20",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                    resume.moyenneAnnuelle?.let {
                        Text("Moyenne annuelle : ${String.format("%.2f", it)}/20", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { partagerArchive(context, ligne.fichier) }) { Text("Partager") }
                    TextButton(onClick = { aSupprimer = ligne.fichier }) { Text("Supprimer") }
                }
            }
            HorizontalDivider()
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Pour retrouver une année archivée dans Notea, partage le fichier d'archive puis utilise " +
                "Paramètres → Sauvegarde et restauration.",
            style = MaterialTheme.typography.bodySmall
        )
    }

    if (confirmer) {
        AlertDialog(
            onDismissRequest = { confirmer = false },
            title = { Text("Démarrer une nouvelle année ?") },
            text = {
                Text(
                    "Les notes et les événements de l'année en cours seront archivés puis effacés. " +
                        "Si tu changes de classe, modifie-la ensuite dans Profil."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmer = false
                    scope.launch {
                        try {
                            val nouvelle = archives.nouvelleAnnee()
                            Journal.ok("archive.nouvelle-annee")
                            message = "Année archivée. Nouvelle année : $nouvelle."
                            rechargement++
                        } catch (e: Exception) {
                            Journal.erreur("archive.nouvelle-annee", e)
                            message = "Impossible d'archiver : ${e.message}"
                        }
                    }
                }) { Text("Archiver et continuer") }
            },
            dismissButton = { TextButton(onClick = { confirmer = false }) { Text("Annuler") } }
        )
    }

    aSupprimer?.let { fichier ->
        AlertDialog(
            onDismissRequest = { aSupprimer = null },
            title = { Text("Supprimer cette archive ?") },
            text = { Text("Cette action est définitive.") },
            confirmButton = {
                TextButton(onClick = {
                    aSupprimer = null
                    scope.launch { archives.supprimer(fichier); rechargement++ }
                }) { Text("Supprimer") }
            },
            dismissButton = { TextButton(onClick = { aSupprimer = null }) { Text("Annuler") } }
        )
    }
}

private fun partagerArchive(context: Context, fichier: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fichier)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/json"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Partager l'archive"))
}
