package com.fastek.notea.ui.parametres

import android.content.Context
import android.provider.Settings
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.fastek.notea.data.backup.SauvegardeManager
import com.fastek.notea.diagnostic.Journal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

/** Lit le fichier choisi et remplace les données. Renvoie un message d'erreur, ou null si réussi. */
suspend fun restaurerDepuisFichier(context: Context, manager: SauvegardeManager, uri: Uri): String? =
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

/** Export d'un fichier JSON et restauration depuis un fichier, sans compte ni connexion. */
@Composable
fun SauvegardeScreen(manager: SauvegardeManager) {
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Sauvegarde et restauration", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(12.dp))
        Text("Sauvegarde automatique", style = MaterialTheme.typography.titleMedium)
        Text(
            "Notea n'a rien à régler : Android sauvegarde automatiquement tes données dans ton compte Google " +
                "(environ une fois par jour, quand le téléphone est branché, en veille et connecté à internet). " +
                "Si tu changes de téléphone ou réinstalles Notea depuis le Play Store avec le même compte Google, " +
                "tes notes sont restaurées toutes seules. Cette sauvegarde doit être activée dans les réglages Android " +
                "(Google → Sauvegarde).",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = {
                try {
                    context.startActivity(Intent(Settings.ACTION_PRIVACY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                } catch (e: Exception) {
                    message = "Ouvre les réglages Android, puis Google → Sauvegarde."
                    erreur = false
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Ouvrir les réglages de sauvegarde") }

        Spacer(Modifier.height(20.dp))
        Text("Sauvegarde manuelle", style = MaterialTheme.typography.titleMedium)
        Text(
            "Crée un fichier que tu gardes toi-même (téléphone, Drive, WhatsApp) : utile si tu installes Notea " +
                "sans passer par le Play Store.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Enregistre tes données dans un fichier que tu gardes où tu veux (téléphone, Drive, " +
                "WhatsApp…). En cas de changement de téléphone, tu les retrouves en restaurant ce fichier.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { lanceurExport.launch("notea-sauvegarde-${LocalDate.now()}.json") },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Créer une sauvegarde") }

        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            onClick = { lanceurImport.launch(arrayOf("application/json", "application/octet-stream", "text/plain", "*/*")) },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Restaurer une sauvegarde") }

        message?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = if (erreur) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
        }
    }

    fichierARestaurer?.let { uri ->
        AlertDialog(
            onDismissRequest = { fichierARestaurer = null },
            title = { Text("Remplacer les données ?") },
            text = { Text("Toutes les données actuelles seront remplacées par celles de la sauvegarde.") },
            confirmButton = {
                TextButton(onClick = {
                    fichierARestaurer = null
                    scope.launch {
                        val resultat = restaurerDepuisFichier(context, manager, uri)
                        erreur = resultat != null
                        message = resultat ?: "Données restaurées."
                    }
                }) { Text("Remplacer") }
            },
            dismissButton = { TextButton(onClick = { fichierARestaurer = null }) { Text("Annuler") } }
        )
    }
}
