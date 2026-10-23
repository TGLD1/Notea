package com.fastek.notea.ui.statistiques

import androidx.compose.foundation.Canvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Button
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
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

        if (etat.periodesDisponibles.size > 1) {
            Row(modifier = Modifier.padding(top = 8.dp)) {
                etat.periodesDisponibles.forEach { numero ->
                    if (numero == etat.periodeNumero) {
                        Button(onClick = { viewModel.selectionnerPeriode(numero) }) { Text("${etat.nomPeriode} $numero") }
                    } else {
                        OutlinedButton(onClick = { viewModel.selectionnerPeriode(numero) }) { Text("${etat.nomPeriode} $numero") }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        Text("Évolution de la moyenne générale", style = MaterialTheme.typography.titleMedium)
        Text(
            "Moyenne générale après chacune de tes dernières notes (30 maximum), avec ton objectif en pointillés verts " +
                "et la moyenne de 10/20 en pointillés rouges.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(8.dp))
        GraphiqueEvolution(points = etat.evolution, objectif = etat.objectif)

        if (etat.moyennesMatieres.isNotEmpty()) {
            Spacer(Modifier.height(32.dp))
            Text("Moyennes par matière", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            BarresMatieres(etat.moyennesMatieres)
        }

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
            "Saisis une note hypothétique pour voir son effet sur la moyenne de la matière et sur ta moyenne générale.",
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
            label = { Text("Note hypothétique sur 20") },
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
            "Pour la matière et le type de note choisis ci-dessus : la note minimale à obtenir au prochain " +
                "devoir ou à la prochaine interrogation pour atteindre la moyenne générale visée.",
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
                "Cette matière compte déjà 2 devoirs : choisis « Interrogation » pour ce calcul.",
                style = MaterialTheme.typography.bodyMedium
            )
        simu.requis == null -> Unit
        else -> {
            val requis = checkNotNull(simu.requis)
            val texte = when (requis.statut) {
                MoyenneCalculator.StatutRequis.ATTEIGNABLE ->
                    "Note minimale à obtenir : ${String.format("%.2f", requis.valeur)}/20 pour atteindre une moyenne générale de $cible."
                MoyenneCalculator.StatutRequis.DEJA_ATTEINT ->
                    "Objectif déjà atteint : même avec 0/20, ta moyenne générale resterait supérieure ou égale à $cible."
                MoyenneCalculator.StatutRequis.IMPOSSIBLE ->
                    "Objectif hors de portée avec une seule note : même 20/20 ne porterait la moyenne générale qu'à " +
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
                "Pour une moyenne annuelle de $cible : ${String.format("%.2f", requis.valeur)}/20 en ${passage.libelleDernierePeriode}."
            MoyenneCalculator.StatutRequis.DEJA_ATTEINT ->
                "Pour une moyenne annuelle de $cible : déjà acquis, quelle que soit ta moyenne en ${passage.libelleDernierePeriode}."
            MoyenneCalculator.StatutRequis.IMPOSSIBLE ->
                "Pour une moyenne annuelle de $cible : hors de portée (il faudrait ${String.format("%.2f", requis.valeur)}/20)."
        }
        Text(texte, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun GraphiqueEvolution(points: List<PointGraphique>, objectif: Double) {
    val valeurs = points.mapNotNull { it.moyenne }
    if (valeurs.isEmpty()) {
        Text(
            "Ajoute une première note pour voir apparaître l'évolution de ta moyenne.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val couleurLigne = MaterialTheme.colorScheme.primary
    val couleurGrille = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
    val couleurTexte = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
    val vert = Color(0xFF4CAF50)
    val rouge = Color(0xFFF44336)
    val pointille = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val margeGauche = 56f
        val margeBas = 8f
        val margeHaut = 8f
        val largeur = size.width - margeGauche - 8f
        val hauteur = size.height - margeBas - margeHaut

        fun yDe(valeur: Double) = margeHaut + hauteur - (valeur / 20.0 * hauteur).toFloat()

        val peinture = android.graphics.Paint().apply {
            color = couleurTexte
            textSize = 28f
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.RIGHT
        }
        listOf(0, 5, 10, 15, 20).forEach { graduation ->
            val y = yDe(graduation.toDouble())
            drawLine(couleurGrille, Offset(margeGauche, y), Offset(margeGauche + largeur, y), strokeWidth = 2f)
            drawContext.canvas.nativeCanvas.drawText("$graduation", margeGauche - 10f, y + 10f, peinture)
        }
        drawLine(rouge, Offset(margeGauche, yDe(10.0)), Offset(margeGauche + largeur, yDe(10.0)), strokeWidth = 3f, pathEffect = pointille)
        if (objectif in 0.0..20.0) {
            drawLine(vert, Offset(margeGauche, yDe(objectif)), Offset(margeGauche + largeur, yDe(objectif)), strokeWidth = 3f, pathEffect = pointille)
        }

        val pas = if (points.size > 1) largeur / (points.size - 1) else 0f
        var precedent: Offset? = null
        points.forEachIndexed { index, point ->
            val valeur = point.moyenne ?: return@forEachIndexed
            val x = if (points.size > 1) margeGauche + index * pas else margeGauche + largeur / 2f
            val actuel = Offset(x, yDe(valeur))
            precedent?.let { drawLine(couleurLigne, it, actuel, strokeWidth = 5f) }
            drawCircle(couleurLigne, radius = 7f, center = actuel)
            precedent = actuel
        }
    }
    Text(
        "Dernière valeur : ${String.format("%.2f", valeurs.last())}/20",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun BarresMatieres(matieres: List<ImpactMatiere>) {
    matieres.sortedByDescending { it.moyenne }.forEach { item ->
        val couleur = when {
            item.moyenne >= 12 -> Color(0xFF4CAF50)
            item.moyenne >= 10 -> Color(0xFFFFB300)
            else -> Color(0xFFF44336)
        }
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(item.nom, style = MaterialTheme.typography.bodyMedium)
                Text(String.format("%.2f", item.moyenne), style = MaterialTheme.typography.bodyMedium)
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((item.moyenne / 20.0).toFloat().coerceIn(0.02f, 1f))
                        .height(8.dp)
                        .background(couleur, RoundedCornerShape(4.dp))
                )
            }
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
