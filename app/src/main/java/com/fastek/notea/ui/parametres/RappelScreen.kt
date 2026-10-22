package com.fastek.notea.ui.parametres

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fastek.notea.diagnostic.Journal
import com.fastek.notea.notification.NotificationsHelper
import com.fastek.notea.notification.ReglagesRappel

/** Rappels du planning : autorisation, heure d'envoi, notification d'essai et conseils de fiabilité. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RappelScreen() {
    val context = LocalContext.current
    var heure by remember { mutableIntStateOf(ReglagesRappel.heure(context)) }
    var etendu by remember { mutableStateOf(false) }
    val activees = rememberNotificationsActivees()
    val demander = rememberDemandeNotifications(activees)
    var resultatTest by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Rappels", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Chaque jour à l'heure choisie, Notea te prévient des événements du planning : deux jours avant " +
                "(une seule fois), la veille et le jour même.",
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(20.dp))
        Text("Autorisation", style = MaterialTheme.typography.titleMedium)
        Text(
            if (activees.value) "Les notifications sont autorisées." else "Les notifications sont désactivées : aucun rappel ne pourra s'afficher.",
            style = MaterialTheme.typography.bodyMedium
        )
        if (!activees.value) {
            Spacer(Modifier.height(8.dp))
            Button(onClick = demander, modifier = Modifier.fillMaxWidth()) { Text("Autoriser les notifications") }
        }

        Spacer(Modifier.height(20.dp))
        Text("Heure d'envoi", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        ExposedDropdownMenuBox(expanded = etendu, onExpandedChange = { etendu = it }) {
            OutlinedTextField(
                value = "${heure}h00",
                onValueChange = {},
                readOnly = true,
                label = { Text("Heure") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = etendu) },
                modifier = Modifier.menuAnchor().fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = etendu, onDismissRequest = { etendu = false }) {
                ReglagesRappel.HEURES_PROPOSEES.forEach { option ->
                    DropdownMenuItem(
                        text = { Text("${option}h00") },
                        onClick = {
                            heure = option
                            etendu = false
                            Journal.suivre("notification.reglage_heure") {
                                ReglagesRappel.definirHeure(context, option)
                            }
                        }
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))
        Text("Vérifier que ça marche", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                val envoyee = NotificationsHelper.afficherTest(context)
                resultatTest = if (envoyee) {
                    "Notification envoyée. Si tu ne la vois pas, vérifie le mode « Ne pas déranger »."
                } else {
                    "Impossible : les notifications sont désactivées pour Notea."
                }
            }
        ) { Text("Envoyer une notification de test") }
        resultatTest?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(20.dp))
        Text("Si les rappels n'arrivent pas", style = MaterialTheme.typography.titleMedium)
        Text(
            "Certains téléphones (Tecno, Infinix, Xiaomi, Huawei…) arrêtent les applications en arrière-plan pour " +
                "économiser la batterie. Dans les réglages de batterie, retire les restrictions pour Notea et " +
                "autorise son démarrage automatique.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(8.dp))
        OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = { NotificationsHelper.ouvrirReglagesBatterie(context) }
        ) { Text("Ouvrir les réglages de batterie") }
    }
}
