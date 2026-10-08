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
import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.diagnostic.Journal
import com.fastek.notea.domain.bulletin.BulletinData
import com.fastek.notea.domain.bulletin.BulletinPdfGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.Normalizer

/** Génère le bulletin PDF d'une période puis ouvre le menu de partage Android. */
@Composable
fun BulletinScreen(repository: NoteaRepository) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var periodes by remember { mutableStateOf<List<Int>>(emptyList()) }
    var type by remember { mutableStateOf(TypeEtablissement.PUBLIC) }
    var choix by remember { mutableStateOf<Int?>(null) }
    var enCours by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val profil = repository.getProfil() ?: return@LaunchedEffect
        type = profil.typeEtablissement
        val numeros = repository.observerPeriodes(profil.id).first().map { it.numero }
        periodes = numeros
        choix = numeros.maxOrNull()
    }

    val nomPeriode = if (type == TypeEtablissement.PUBLIC) "Semestre" else "Trimestre"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Bulletin PDF", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Choisis la période, puis partage le bulletin (WhatsApp, e-mail, Drive, impression…).",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))

        Row {
            periodes.forEach { numero ->
                if (numero == choix) {
                    Button(onClick = { choix = numero }) { Text("$nomPeriode $numero") }
                } else {
                    OutlinedButton(onClick = { choix = numero }) { Text("$nomPeriode $numero") }
                }
                Spacer(Modifier.width(8.dp))
            }
        }

        Spacer(Modifier.height(16.dp))
        Button(
            enabled = choix != null && !enCours,
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val numero = choix
                if (numero != null) {
                    scope.launch {
                        enCours = true
                        message = null
                        try {
                            val data = repository.construireBulletin(numero)
                            if (data == null) {
                                message = "Impossible de préparer le bulletin."
                                Journal.erreur("bulletin.pdf", null, "données introuvables, période $numero")
                            } else {
                                val fichier = withContext(Dispatchers.IO) {
                                    val dossier = File(context.cacheDir, "bulletins").apply { mkdirs() }
                                    val f = File(dossier, nomFichier(data))
                                    BulletinPdfGenerator.generer(data, f)
                                    f
                                }
                                partager(context, fichier)
                                Journal.ok("bulletin.pdf", "période $numero")
                            }
                        } catch (e: Exception) {
                            message = "Erreur lors de la création du PDF : ${e.message}"
                            Journal.erreur("bulletin.pdf", e, "période $numero")
                        } finally {
                            enCours = false
                        }
                    }
                }
            }
        ) {
            Text(if (enCours) "Création du PDF…" else "Générer et partager le bulletin")
        }

        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(Modifier.height(24.dp))
        Text(
            "Rang, moyenne de la classe, appréciations et décision du conseil ne peuvent pas être " +
                "calculés par l'application : ces cases restent vides sur le PDF pour que tu les " +
                "complètes (à la main ou après impression).",
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun nomFichier(data: BulletinData): String {
    val brut = "Bulletin_${data.nom}_${data.prenom}_${data.libellePeriode.take(1)}${data.numeroPeriode}"
    val sansAccents = Normalizer.normalize(brut, Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    return sansAccents.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_') + ".pdf"
}

private fun partager(context: Context, fichier: File) {
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", fichier)
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, "Partager le bulletin"))
}
