package com.fastek.notea.enseignant.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Champ en lecture seule qui ressemble à un champ de saisie ; un appui ouvre la liste de choix. */
@Composable
fun ChampChoix(libelle: String, valeur: String, onClick: () -> Unit, modifier: Modifier = Modifier.fillMaxWidth()) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color.Transparent,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(libelle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (valeur.isBlank()) "Choisir dans la liste" else valeur,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (valeur.isBlank()) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.onSurface
                )
            }
            Text("▾", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Liste de choix à une seule sélection, affichée dans la fenêtre de l'application. */
@Composable
fun ListeChoix(
    titre: String,
    options: List<String>,
    selection: String,
    onChoisir: (String) -> Unit,
    onFermer: () -> Unit
) {
    Dialogue(
        titre = titre,
        onFermer = onFermer,
        confirmer = { TextButton(onClick = onFermer) { Text("Fermer") } }
    ) {
        Column {
            options.forEach { option ->
                val choisi = option == selection
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onChoisir(option) }
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier.size(20.dp).clip(CircleShape)
                            .background(if (choisi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (choisi) Text("✓", color = MaterialTheme.colorScheme.onPrimary, fontSize = 12.sp)
                    }
                    Text(
                        option,
                        modifier = Modifier.padding(start = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = if (choisi) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

/** Saisie d'une heure en deux champs (heures, minutes) ; [onChange] reçoit les minutes depuis minuit. */
@Composable
fun SaisieHeure(libelle: String, minutes: Int, onChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    var heures by remember { mutableStateOf((minutes / 60).toString()) }
    var mins by remember { mutableStateOf((minutes % 60).toString().padStart(2, '0')) }
    fun total(h: String, m: String): Int = (h.toIntOrNull() ?: 0) * 60 + (m.toIntOrNull() ?: 0)
    Column(modifier) {
        Text(libelle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Champ(
                heures,
                { saisi ->
                    heures = filtrerNombreBorne(saisi, heures, 23)
                    onChange(total(heures, mins))
                },
                "h", Modifier.width(76.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Text(":", style = MaterialTheme.typography.titleMedium)
            Champ(
                mins,
                { saisi ->
                    mins = filtrerNombreBorne(saisi, mins, 59)
                    onChange(total(heures, mins))
                },
                "min", Modifier.width(76.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
        }
    }
}
