package com.fastek.notea.ui.parametres

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastek.notea.diagnostic.Journal

/** Journal de diagnostic : état de chaque fonctionnalité + dernières lignes brutes. */
@Composable
fun JournalScreen() {
    val context = LocalContext.current
    var version by remember { mutableIntStateOf(0) }
    val resume = remember(version) { Journal.resume() }
    val texte = remember(version) { Journal.lire() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Journal de diagnostic", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Enregistre si chaque fonctionnalité a marché. Aucune note ni donnée personnelle " +
                "n'y est écrite. Si un problème survient, partage ce journal au développeur.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(12.dp))

        Row {
            Button(onClick = { partagerJournal(context, texte) }, enabled = texte.isNotBlank()) {
                Text("Partager")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { version++ }) { Text("Actualiser") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = { Journal.vider(); version++ }) { Text("Vider") }
        }

        Spacer(Modifier.height(16.dp))
        Text("État des fonctionnalités", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        if (resume.isEmpty()) {
            Text("Rien d'enregistré pour l'instant.", style = MaterialTheme.typography.bodyMedium)
        }
        resume.forEach { etat ->
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                Text(
                    text = if (etat.fonctionne) "OK" else "ÉCHEC",
                    color = if (etat.fonctionne) Color(0xFF2E7D32) else Color(0xFFC62828),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.width(56.dp)
                )
                Column {
                    Text(etat.fonction, style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "${etat.date} · ${etat.nbOk} réussite(s), ${etat.nbErreurs} échec(s)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Dernières lignes", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            text = texte.lines().takeLast(60).joinToString("\n").ifBlank { "—" },
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp
        )
    }
}

private fun partagerJournal(context: Context, texte: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, "Journal Notea")
        putExtra(Intent.EXTRA_TEXT, texte.takeLast(30_000))
    }
    context.startActivity(Intent.createChooser(intent, "Partager le journal"))
}
