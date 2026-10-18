package com.fastek.notea.ui.parametres

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
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
import com.fastek.notea.notification.ReglagesRappel

/** Choix de l'heure du rappel quotidien (événements des 48 prochaines heures). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RappelScreen() {
    val context = LocalContext.current
    var heure by remember { mutableIntStateOf(ReglagesRappel.heure(context)) }
    var etendu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Heure du rappel", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "Chaque jour à cette heure, Notea te prévient des événements du planning prévus " +
                "dans les 48 prochaines heures.",
            style = MaterialTheme.typography.bodyMedium
        )
        Spacer(Modifier.height(16.dp))

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
    }
}
