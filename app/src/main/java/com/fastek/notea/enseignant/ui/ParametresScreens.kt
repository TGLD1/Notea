package com.fastek.notea.enseignant.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fastek.notea.data.backup.SauvegardeInvalideException
import com.fastek.notea.diagnostic.Journal
import com.fastek.notea.domain.reference.MatieresReference
import com.fastek.notea.enseignant.data.backup.SauvegardeEnseignant
import com.fastek.notea.enseignant.data.entity.Professeur
import com.fastek.notea.enseignant.rappel.RappelScheduler
import com.fastek.notea.ui.parametres.JournalScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@Composable
fun ParametresScreen(
    onDon: () -> Unit,
    onProfil: () -> Unit,
    onAnnee: () -> Unit,
    onArchives: () -> Unit,
    onSauvegarde: () -> Unit,
    onJournal: () -> Unit,
    onRetour: () -> Unit
) {
    EcranAvecEntete("Réglages", onRetour, sousTitre = "Ton profil et tes données") {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            LigneReglage("💜", "Soutenir Notea", "Un don volontaire par Mobile Money ou carte", onDon)
            LigneReglage("👤", "Mon profil", "Nom, matière, établissement, année scolaire", onProfil)
            LigneReglage("🎓", "Nouvelle année scolaire", "Archiver l'année écoulée et repartir à zéro", onAnnee)
            LigneReglage("📦", "Archives", "Retrouver, partager ou restaurer les années passées", onArchives)
            LigneReglage("💾", "Sauvegarde et restauration", "Garder tes classes et tes notes en sécurité", onSauvegarde)
            LigneReglage("🩺", "Journal de diagnostic", "À partager si un problème survient", onJournal)
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun LigneReglage(emoji: String, titre: String, detail: String, onClick: () -> Unit) {
    CarteDouce(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer).padding(12.dp)
            ) { Text(emoji, fontSize = 22.sp) }
            Column(Modifier.padding(start = 14.dp).weight(1f)) {
                Text(titre, style = MaterialTheme.typography.titleMedium)
                Text(
                    detail, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.outline)
        }
    }
}

@Composable
fun JournalEcran(onRetour: () -> Unit) {
    EcranAvecEntete("Journal de diagnostic", onRetour) { JournalScreen() }
}

@Composable
fun ProfilScreen(professeur: Professeur, onEnregistrer: (Professeur) -> Unit, onRetour: () -> Unit) {
    var nom by remember { mutableStateOf(professeur.nom) }
    var prenom by remember { mutableStateOf(professeur.prenom) }
    var matiere by remember { mutableStateOf(professeur.matiere) }
    var etablissement by remember { mutableStateOf(professeur.etablissement) }
    var annee by remember { mutableStateOf(professeur.anneeScolaire) }
    var choixMatiere by remember { mutableStateOf(false) }

    EcranAvecEntete("Mon profil", onRetour, sousTitre = professeur.matiere) {
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CarteDouce(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TitreSection("IDENTITÉ")
                    Champ(nom, { nom = it }, "Nom")
                    Champ(prenom, { prenom = it }, "Prénoms")
                }
            }
            CarteDouce(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    TitreSection("ENSEIGNEMENT")
                    ChampChoix("Matière enseignée", matiere, onClick = { choixMatiere = true })
                    Champ(etablissement, { etablissement = it }, "Établissement")
                    Champ(annee, { annee = it }, "Année scolaire")
                    Text(
                        "Type d'établissement : " +
                            (if (professeur.typeEtablissement.nombrePeriodes == 2) "public (2 semestres)" else "privé (3 trimestres)") +
                            ". Il ne peut pas être changé une fois les classes créées.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            BoutonPlein(
                "Enregistrer",
                enabled = nom.isNotBlank() && matiere.isNotBlank() && annee.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onEnregistrer(
                        professeur.copy(
                            nom = nom.trim(), prenom = prenom.trim(), matiere = matiere.trim(),
                            etablissement = etablissement.trim(), anneeScolaire = annee.trim()
                        )
                    )
                    onRetour()
                }
            )
            Spacer(Modifier.navigationBarsPadding().height(8.dp))
        }
    }

    if (choixMatiere) {
        ListeChoix(
            titre = "Matière enseignée",
            options = (MatieresReference.LISTE + matiere).filter { it.isNotBlank() }.distinct(),
            selection = matiere,
            onChoisir = { matiere = it; choixMatiere = false },
            onFermer = { choixMatiere = false }
        )
    }
}

/** Lit le fichier choisi et remplace les données. Renvoie un message d'erreur, ou null si réussi. */
suspend fun restaurerEnseignantDepuisFichier(context: Context, manager: SauvegardeEnseignant, uri: Uri): String? =
    try {
        val contenu = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        } ?: throw SauvegardeInvalideException("Impossible d'ouvrir le fichier.")
        restaurerEnseignantDepuisTexte(context, manager, contenu)
    } catch (e: SauvegardeInvalideException) {
        Journal.erreur("sauvegarde.restauration", e)
        e.message
    } catch (e: Exception) {
        Journal.erreur("sauvegarde.restauration", e)
        "Erreur lors de la restauration : ${e.message}"
    }

/** Remplace les données par celles de la sauvegarde [contenu] et reprogramme les rappels. Null si réussi. */
suspend fun restaurerEnseignantDepuisTexte(context: Context, manager: SauvegardeEnseignant, contenu: String): String? =
    try {
        val anciensRappels = manager.idsRappels()
        manager.restaurer(contenu)
        anciensRappels.forEach { RappelScheduler.annuler(context, it) }
        RappelScheduler.replanifierTout(context)
        Journal.ok("sauvegarde.restauration")
        null
    } catch (e: SauvegardeInvalideException) {
        Journal.erreur("sauvegarde.restauration", e)
        e.message
    } catch (e: Exception) {
        Journal.erreur("sauvegarde.restauration", e)
        "Erreur lors de la restauration : ${e.message}"
    }

@Composable
fun SauvegardeEcran(manager: SauvegardeEnseignant, onRetour: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by remember { mutableStateOf<String?>(null) }
    var erreur by remember { mutableStateOf(false) }
    var fichierARestaurer by remember { mutableStateOf<Uri?>(null) }

    val lanceurExport = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val contenu = manager.exporter()
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(contenu.toByteArray(Charsets.UTF_8)) }
                            ?: throw IllegalStateException("Fichier inaccessible")
                    }
                    Journal.ok("sauvegarde.export")
                    erreur = false
                    message = "Sauvegarde enregistrée."
                } catch (e: Exception) {
                    Journal.erreur("sauvegarde.export", e)
                    erreur = true
                    message = "Erreur lors de la sauvegarde : ${e.message}"
                }
            }
        }
    }
    val lanceurImport = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) fichierARestaurer = uri }

    EcranAvecEntete("Sauvegarde", onRetour, sousTitre = "Tes classes et tes notes en sécurité") {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CarteDouce(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("☁️  Sauvegarde automatique", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Android sauvegarde les données de l'application dans ton compte Google (environ une fois par jour, " +
                            "téléphone branché, en veille et connecté à internet). Cette option doit être activée dans les " +
                            "réglages Android (Google → Sauvegarde).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BoutonContour(
                        "Ouvrir les réglages de sauvegarde",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_PRIVACY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                )
                            } catch (e: Exception) {
                                erreur = false
                                message = "Ouvre les réglages Android, puis Google → Sauvegarde."
                            }
                        }
                    )
                }
            }
            CarteDouce(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("📁  Sauvegarde manuelle", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Enregistre toutes tes classes, tes élèves et leurs notes dans un fichier que tu gardes où tu veux " +
                            "(téléphone, Drive, WhatsApp…). En cas de changement de téléphone, tu les retrouves en restaurant ce fichier.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    BoutonPlein(
                        "Enregistrer une sauvegarde",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { lanceurExport.launch("notea-enseignant-sauvegarde-${LocalDate.now()}.json") }
                    )
                    BoutonContour(
                        "Restaurer depuis un fichier",
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            lanceurImport.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*"))
                        }
                    )
                }
            }
            message?.let {
                Text(it, color = if (erreur) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.navigationBarsPadding().height(8.dp))
        }
    }

    fichierARestaurer?.let { uri ->
        Dialogue(
            titre = "Restaurer cette sauvegarde ?",
            onFermer = { fichierARestaurer = null },
            confirmer = {
                TextButton(onClick = {
                    fichierARestaurer = null
                    scope.launch {
                        val resultat = restaurerEnseignantDepuisFichier(context, manager, uri)
                        erreur = resultat != null
                        message = resultat ?: "Sauvegarde restaurée."
                    }
                }) { Text("Restaurer") }
            },
            annuler = { TextButton(onClick = { fichierARestaurer = null }) { Text("Annuler") } }
        ) {
            Text("Toutes les classes, tous les élèves et toutes les notes actuels seront remplacés par ceux du fichier.")
        }
    }
}
