package com.fastek.notea.ui.parametres

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.diagnostic.Journal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Exporte toutes les notes en fichier CSV (Excel, Google Sheets) puis ouvre le menu de partage. */
@Composable
fun ExportCsvScreen(repository: NoteaRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var enCours by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Exporter les notes", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Crée un fichier CSV avec toutes tes notes (période, matière, coefficient, type, note, date). " +
                "Il s'ouvre dans Excel ou Google Sheets.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))
        Button(
            enabled = !enCours,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                scope.launch {
                    enCours = true
                    message = null
                    try {
                        val contenu = repository.exporterNotesCsv()
                        if (contenu == null) {
                            message = "Aucun profil : rien à exporter."
                        } else {
                            val fichier = withContext(Dispatchers.IO) {
                                val dossier = File(context.cacheDir, "exports").apply { mkdirs() }
                                File(dossier, "Notea_notes.csv").apply { writeText(contenu, Charsets.UTF_8) }
                            }
                            partagerCsv(context, fichier)
                            Journal.ok("export.csv", "export des notes")
                        }
                    } catch (e: Exception) {
                        message = "Erreur lors de l'export : ${e.message}"
                        Journal.erreur("export.csv", e)
                    } finally {
                        enCours = false
                    }
                }
            }
        ) {
            Text(if (enCours) "Création du fichier…" else "Exporter et partager")
        }
        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
    }
}

private fun partagerCsv(context: Context, fichier: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fichier)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/csv"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Partager les notes"))
}
