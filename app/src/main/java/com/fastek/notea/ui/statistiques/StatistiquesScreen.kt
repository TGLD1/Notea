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
import com.fastek.notea.domain.calcul.MoyenneCalculator

@Composable
fun StatistiquesScreen(viewModel: StatistiquesViewModel) {
    val etat by viewModel.uiState.collectAsState()
    val passage by viewModel.passage.collectAsState()

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

        if (etat.impacts.isNotEmpty()) {
            Spacer(Modifier.height(32.dp))
            Text("Impact des matières sur ta moyenne", style = MaterialTheme.typography.titleMedium)
            Text(
                "De la matière qui tire le plus ta moyenne vers le bas à celle qui la tire le plus vers le haut " +
                    "(en points de moyenne générale, coefficients compris).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            etat.impacts.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${item.nom}  (${String.format("%.2f", item.moyenne)})")
                    Text(
                        (if (item.impact >= 0) "+" else "") + String.format("%.2f", item.impact) + " pt",
                        color = if (item.impact < 0) Color(0xFFF44336) else Color(0xFF4CAF50)
                    )
                }
            }
        }

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

        Spacer(Modifier.height(32.dp))
        Text("Quelle note me faut-il ?", style = MaterialTheme.typography.titleMedium)
        Text(
            "Avec la matière et le type choisis ci-dessus : la note minimale au prochain devoir/interro " +
                "pour atteindre la moyenne générale visée.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = etat.simulateur.cibleTexte,
            onValueChange = viewModel::onCibleChange,
            label = { Text("Moyenne générale visée (vide = objectif)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        ResultatRequis(etat.simulateur)

        Spacer(Modifier.height(32.dp))
        CalculateurPassage(passage)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ResultatRequis(simu: SimulateurState) {
    val cible = simu.cible?.let { String.format("%.2f", it) } ?: "--"
    when {
        simu.matiereId == null ->
            Text("Choisis une matière pour lancer le calcul.", style = MaterialTheme.typography.bodyMedium)
        simu.limiteDevoirs ->
            Text(
                "Cette matière a déjà 2 devoirs : choisis « Interrogation » pour ce calcul.",
                style = MaterialTheme.typography.bodyMedium
            )
        simu.requis == null -> Unit
        else -> {
            val requis = checkNotNull(simu.requis)
            val texte = when (requis.statut) {
                MoyenneCalculator.StatutRequis.ATTEIGNABLE ->
                    "Il te faut au moins ${String.format("%.2f", requis.valeur)}/20 pour atteindre $cible de moyenne générale."
                MoyenneCalculator.StatutRequis.DEJA_ATTEINT ->
                    "Objectif déjà atteint : même avec 0/20, ta moyenne générale resterait au-dessus de $cible."
                MoyenneCalculator.StatutRequis.IMPOSSIBLE ->
                    "Impossible avec une seule note : même 20/20 ne donnerait que " +
                        "${String.format("%.2f", requis.maximum ?: 0.0)} de moyenne générale."
            }
            Text(texte, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun CalculateurPassage(passage: PassageUi?) {
    Text("Calculateur de passage", style = MaterialTheme.typography.titleMedium)
    if (passage == null) {
        Text(
            "Indisponible pour le moment.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Text(
        "Moyenne à obtenir en ${passage.libelleDernierePeriode} pour atteindre une moyenne annuelle donnée.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    if (!passage.disponible) {
        Text(
            "Saisis d'abord des notes pour ${passage.libellePeriodePrecedente} : le calcul en dépend.",
            style = MaterialTheme.typography.bodyMedium
        )
        return
    }
    passage.resultats.forEach { resultat ->
        val cible = resultat.cible.let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format("%.2f", it) }
        val requis = resultat.requis
        val texte = when (requis.statut) {
            MoyenneCalculator.StatutRequis.ATTEIGNABLE ->
                "Pour $cible en annuelle : ${String.format("%.2f", requis.valeur)}/20 en ${passage.libelleDernierePeriode}."
            MoyenneCalculator.StatutRequis.DEJA_ATTEINT ->
                "Pour $cible en annuelle : déjà acquis, quelle que soit ta moyenne en ${passage.libelleDernierePeriode}."
            MoyenneCalculator.StatutRequis.IMPOSSIBLE ->
                "Pour $cible en annuelle : hors de portée (il faudrait ${String.format("%.2f", requis.valeur)}/20)."
        }
        Text(texte, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
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

    val couleurLigne = MaterialTheme.colorScheme.primary

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
