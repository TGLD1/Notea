package com.fastek.notea.enseignant.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fastek.notea.data.backup.SauvegardeInvalideException
import com.fastek.notea.diagnostic.Journal
import com.fastek.notea.enseignant.data.backup.SauvegardeEnseignant
import com.fastek.notea.enseignant.data.entity.Professeur
import com.fastek.notea.ui.parametres.JournalScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Cadre commun des écrans secondaires : barre de titre et bouton « Retour ». */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranAvecRetour(titre: String, onRetour: () -> Unit, contenu: @Composable () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(titre) },
                navigationIcon = { TextButton(onClick = onRetour) { Text("Retour") } }
            )
        }
    ) { marge ->
        Box(Modifier.padding(marge).fillMaxSize()) { contenu() }
    }
}

@Composable
fun ParametresScreen(
    onProfil: () -> Unit,
    onSauvegarde: () -> Unit,
    onJournal: () -> Unit,
    onRetour: () -> Unit
) {
    EcranAvecRetour("Réglages", onRetour) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            LigneReglage("Mon profil", "Nom, matière, établissement, année scolaire", onProfil)
            LigneReglage("Sauvegarde et restauration", "Garder tes classes et tes notes en sécurité", onSauvegarde)
            LigneReglage("Journal de diagnostic", "À partager si un problème survient", onJournal)
        }
    }
}

@Composable
private fun LigneReglage(titre: String, detail: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 14.dp)) {
        Text(titre, style = MaterialTheme.typography.titleMedium)
        Text(detail, style = MaterialTheme.typography.bodyMedium)
    }
    HorizontalDivider()
}

@Composable
fun JournalEcran(onRetour: () -> Unit) {
    EcranAvecRetour("Journal de diagnostic", onRetour) { JournalScreen() }
}

@Composable
fun ProfilScreen(professeur: Professeur, onEnregistrer: (Professeur) -> Unit, onRetour: () -> Unit) {
    var nom by remember { mutableStateOf(professeur.nom) }
    var prenom by remember { mutableStateOf(professeur.prenom) }
    var matiere by remember { mutableStateOf(professeur.matiere) }
    var etablissement by remember { mutableStateOf(professeur.etablissement) }
    var annee by remember { mutableStateOf(professeur.anneeScolaire) }

    EcranAvecRetour("Mon profil", onRetour) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = nom, onValueChange = { nom = it }, label = { Text("Nom") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = prenom, onValueChange = { prenom = it }, label = { Text("Prénoms") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = matiere, onValueChange = { matiere = it }, label = { Text("Matière enseignée") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = etablissement, onValueChange = { etablissement = it }, label = { Text("Établissement") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = annee, onValueChange = { annee = it }, label = { Text("Année scolaire") },
                singleLine = true, modifier = Modifier.fillMaxWidth()
            )
            Text(
                "Type d'établissement : ${if (professeur.typeEtablissement.nombrePeriodes == 2) "public (2 semestres)" else "privé (3 trimestres)"}. " +
                    "Il ne peut pas être changé une fois les classes créées.",
                style = MaterialTheme.typography.bodySmall
            )
            Button(
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
            ) { Text("Enregistrer") }
        }
    }
}

/** Lit le fichier choisi et remplace les données. Renvoie un message d'erreur, ou null si réussi. */
suspend fun restaurerEnseignantDepuisFichier(context: Context, manager: SauvegardeEnseignant, uri: Uri): String? =
    try {
        val contenu = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
        } ?: throw SauvegardeInvalideException("Impossible d'ouvrir le fichier.")
        manager.restaurer(contenu)
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

    EcranAvecRetour("Sauvegarde et restauration", onRetour) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Sauvegarde automatique", style = MaterialTheme.typography.titleMedium)
            Text(
                "Android sauvegarde automatiquement les données de l'application dans ton compte Google " +
                    "(environ une fois par jour, téléphone branché, en veille et connecté à internet). " +
                    "Cette option doit être activée dans les réglages Android (Google → Sauvegarde).",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    try {
                        context.startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    } catch (e: Exception) {
                        erreur = false
                        message = "Ouvre les réglages Android, puis Google → Sauvegarde."
                    }
                }
            ) { Text("Ouvrir les réglages de sauvegarde") }

            Spacer(Modifier.height(20.dp))
            Text("Sauvegarde manuelle", style = MaterialTheme.typography.titleMedium)
            Text(
                "Enregistre toutes tes classes, tes élèves et leurs notes dans un fichier que tu gardes où tu veux " +
                    "(téléphone, Drive, WhatsApp…). En cas de changement de téléphone, tu les retrouves en restaurant ce fichier.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(8.dp))
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = { lanceurExport.launch("notea-enseignant-sauvegarde-${LocalDate.now()}.json") }
            ) { Text("Enregistrer une sauvegarde") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = { lanceurImport.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")) }
            ) { Text("Restaurer depuis un fichier") }

            message?.let {
                Spacer(Modifier.height(12.dp))
                Text(it, color = if (erreur) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        }
    }

    fichierARestaurer?.let { uri ->
        AlertDialog(
            onDismissRequest = { fichierARestaurer = null },
            title = { Text("Restaurer cette sauvegarde ?") },
            text = { Text("Toutes les classes, tous les élèves et toutes les notes actuels seront remplacés par ceux du fichier.") },
            confirmButton = {
                TextButton(onClick = {
                    fichierARestaurer = null
                    scope.launch {
                        val resultat = restaurerEnseignantDepuisFichier(context, manager, uri)
                        erreur = resultat != null
                        message = resultat ?: "Sauvegarde restaurée."
                    }
                }) { Text("Restaurer") }
            },
            dismissButton = { TextButton(onClick = { fichierARestaurer = null }) { Text("Annuler") } }
        )
    }
}
