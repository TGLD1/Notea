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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.mutableStateMapOf
import com.fastek.notea.domain.bulletin.LigneBulletin
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
    var lignes by remember { mutableStateOf<List<LigneBulletin>>(emptyList()) }
    val appreciations = remember { mutableStateMapOf<String, String>() }

    LaunchedEffect(choix) {
        val numero = choix ?: return@LaunchedEffect
        lignes = repository.construireBulletin(numero)?.lignes?.filter { it.moyenne != null } ?: emptyList()
        appreciations.clear()
    }

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
            "Choisis la période, puis génère le bulletin au format PDF pour le partager (WhatsApp, e-mail, Drive) ou l'imprimer.",
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

        if (lignes.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("Appréciations par matière (facultatif)", style = MaterialTheme.typography.titleMedium)
            Text(
                "Choisis dans le menu ou écris librement. Les matières laissées vides restent à remplir à la main.",
                style = MaterialTheme.typography.bodySmall
            )
            TextButton(onClick = {
                lignes.forEach { appreciations[it.matiere] = appreciationAuto(it.moyenne ?: 0.0) }
            }) { Text("Proposer selon les moyennes") }
            lignes.forEach { ligne ->
                LigneAppreciation(
                    matiere = ligne.matiere,
                    moyenne = ligne.moyenne,
                    valeur = appreciations[ligne.matiere].orEmpty(),
                    onChange = { appreciations[ligne.matiere] = it }
                )
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
                            val data = repository.construireBulletin(numero)?.let { brut ->
                                brut.copy(
                                    lignes = brut.lignes.map {
                                        it.copy(appreciation = appreciations[it.matiere].orEmpty().trim())
                                    }
                                )
                            }
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
            "Le rang, la moyenne de la classe et la décision du conseil ne peuvent pas être calculés par " +
                "l'application : ces cases restent vides sur le PDF, à compléter à la main après impression.",
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

private val APPRECIATIONS_RAPIDES = listOf(
    "Excellent", "Très bien", "Bien", "Assez bien", "Passable", "Insuffisant", "Médiocre", "Faible"
)

/** Appréciation proposée d'après la moyenne sur 20 de la matière. */
private fun appreciationAuto(moyenne: Double): String = when {
    moyenne >= 18 -> "Excellent"
    moyenne >= 16 -> "Très bien"
    moyenne >= 14 -> "Bien"
    moyenne >= 12 -> "Assez bien"
    moyenne >= 10 -> "Passable"
    moyenne >= 8 -> "Insuffisant"
    moyenne >= 5 -> "Médiocre"
    else -> "Faible"
}

@Composable
private fun LigneAppreciation(matiere: String, moyenne: Double?, valeur: String, onChange: (String) -> Unit) {
    var menu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = valeur,
            onValueChange = { onChange(it.take(24)) },
            label = { Text("$matiere (${moyenne?.let { String.format("%.2f", it) } ?: "--"})") },
            singleLine = true,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(4.dp))
        androidx.compose.foundation.layout.Box {
            TextButton(onClick = { menu = true }) { Text("Menu") }
            DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                APPRECIATIONS_RAPIDES.forEach { terme ->
                    DropdownMenuItem(text = { Text(terme) }, onClick = { onChange(terme); menu = false })
                }
                DropdownMenuItem(text = { Text("Effacer") }, onClick = { onChange(""); menu = false })
            }
        }
    }
}
