package com.fastek.notea.ui.statistiques

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fastek.notea.data.local.entity.Matiere
import com.fastek.notea.data.local.entity.TypeNote

@Composable
fun StatistiquesScreen(viewModel: StatistiquesViewModel) {
    val etat by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Statistiques", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Text("Évolution de la moyenne générale (30 jours)", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        GraphiqueEvolution(points = etat.evolution)

        Spacer(Modifier.height(32.dp))
        Text("Simulateur", style = MaterialTheme.typography.titleMedium)
        Text(
            "Si j'ai cette note au prochain devoir/interro, ma moyenne devient...",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))

        SelecteurMatiere(
            matieres = etat.matieresDisponibles,
            selectionId = etat.simulateur.matiereId,
            onSelection = viewModel::onMatiereChange
        )
        Spacer(Modifier.height(8.dp))

        Row {
            RadioButton(
                selected = etat.simulateur.type == TypeNote.DEVOIR,
                onClick = { viewModel.onTypeChange(TypeNote.DEVOIR) }
            )
            Text("Devoir", modifier = Modifier.padding(top = 12.dp))
            Spacer(Modifier.width(16.dp))
            RadioButton(
                selected = etat.simulateur.type == TypeNote.INTERROGATION,
                onClick = { viewModel.onTypeChange(TypeNote.INTERROGATION) }
            )
            Text("Interrogation", modifier = Modifier.padding(top = 12.dp))
        }

        OutlinedTextField(
            value = etat.simulateur.valeurTexte,
            onValueChange = viewModel::onValeurChange,
            label = { Text("Note hypothétique /20") },
            modifier = Modifier.fillMaxWidth()
        )

        etat.simulateur.erreur?.let { erreur ->
            Text(erreur, color = MaterialTheme.colorScheme.error)
        }

        etat.simulateur.nouvelleMoyenneMatiere?.let { moyenneMatiere ->
            Spacer(Modifier.height(8.dp))
            Text("Nouvelle moyenne de la matière : ${String.format("%.2f", moyenneMatiere)}/20")
        }
        etat.simulateur.nouvelleMoyenneGenerale?.let { moyenneGenerale ->
            Text("Nouvelle moyenne générale : ${String.format("%.2f", moyenneGenerale)}/20")
        }
    }
}

@Composable
private fun GraphiqueEvolution(points: List<PointGraphique>) {
    val valeurs = points.mapNotNull { it.moyenne }
    if (valeurs.isEmpty()) {
        Text(
            "Pas encore assez de notes pour afficher un graphique.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val couleurLigne = Color(0xFF3B82F6)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
    ) {
        val largeur = size.width
        val hauteur = size.height
        val nbSegments = (points.size - 1).coerceAtLeast(1)
        val espacement = largeur / nbSegments
        val minEchelle = 0.0
        val maxEchelle = 20.0

        var pointPrecedent: Offset? = null
        points.forEachIndexed { index, point ->
            val valeur = point.moyenne ?: return@forEachIndexed
            val x = index * espacement
            val y = hauteur - ((valeur - minEchelle) / (maxEchelle - minEchelle) * hauteur).toFloat()
            val actuel = Offset(x, y)

            pointPrecedent?.let { precedent ->
                drawLine(color = couleurLigne, start = precedent, end = actuel, strokeWidth = 4f)
            }
            drawCircle(color = couleurLigne, radius = 4f, center = actuel)
            pointPrecedent = actuel
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SelecteurMatiere(
    matieres: List<Matiere>,
    selectionId: Long?,
    onSelection: (Long?) -> Unit
) {
    var etendu by remember { mutableStateOf(false) }
    val libelle = matieres.find { it.id == selectionId }?.nom ?: "Choisir une matière"

    ExposedDropdownMenuBox(expanded = etendu, onExpandedChange = { etendu = it }) {
        OutlinedTextField(
            value = libelle,
            onValueChange = {},
            readOnly = true,
            label = { Text("Matière") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = etendu) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = etendu, onDismissRequest = { etendu = false }) {
            matieres.forEach { matiere ->
                DropdownMenuItem(
                    text = { Text(matiere.nom) },
                    onClick = { onSelection(matiere.id); etendu = false }
                )
            }
        }
    }
}
