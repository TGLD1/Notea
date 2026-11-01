package com.fastek.notea.enseignant.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/** Page d'information : une carte par rubrique. */
@Composable
fun PageTexteScreen(identifiant: String, onRetour: () -> Unit) {
    val page = remember(identifiant) { TextesEnseignant.page(identifiant) }
    if (page == null) {
        EcranAvecEntete("Information", onRetour) { EtatVide("📄", "Page introuvable", "Cette page n'existe pas.") }
        return
    }
    EcranAvecEntete(page.titre, onRetour, sousTitre = page.sousTitre) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            page.sections.forEach { section ->
                CarteDouce(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        section.titre?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
                        Text(
                            section.texte, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.navigationBarsPadding().height(8.dp))
        }
    }
}

@Composable
fun ContactScreen(onJournal: () -> Unit, onRetour: () -> Unit) {
    val contexte = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }

    fun ouvrir(intent: Intent, indisponible: String) {
        try {
            contexte.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            message = indisponible
        }
    }

    EcranAvecEntete("Nous contacter", onRetour, sousTitre = "Question, idée ou problème") {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CarteDouce(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("✉️  Écris-nous", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Pour signaler un problème, décris ce que tu faisais et, si possible, joins le journal de diagnostic " +
                            "(Réglages → Journal de diagnostic) : il aide à comprendre ce qui s'est passé.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            BoutonPlein(
                "Écrire par e-mail",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    ouvrir(
                        Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${TextesEnseignant.EMAIL}"))
                            .putExtra(Intent.EXTRA_SUBJECT, "Notea Enseignant"),
                        "Aucune application d'e-mail trouvée. Écris à ${TextesEnseignant.EMAIL}."
                    )
                }
            )
            BoutonContour(
                "Écrire sur WhatsApp",
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    ouvrir(
                        Intent(Intent.ACTION_VIEW, Uri.parse(TextesEnseignant.WHATSAPP_LIEN)),
                        "WhatsApp n'est pas disponible. Numéro : ${TextesEnseignant.WHATSAPP_AFFICHE}."
                    )
                }
            )
            BoutonContour("Voir le journal de diagnostic", onClick = onJournal, modifier = Modifier.fillMaxWidth())
            message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(Modifier.navigationBarsPadding().height(8.dp))
        }
    }
}
