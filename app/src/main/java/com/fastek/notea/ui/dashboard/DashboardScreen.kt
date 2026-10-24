package com.fastek.notea.ui.dashboard

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.domain.calcul.MoyenneCalculator

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val etat by viewModel.uiState.collectAsState()
    val nomPeriode = etat.nomPeriode

    if (etat.chargement) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Bonjour ${etat.prenom}", style = MaterialTheme.typography.headlineSmall)

        if (etat.periodesDisponibles.size > 1) {
            Row(modifier = Modifier.padding(top = 8.dp)) {
                etat.periodesDisponibles.forEach { numero ->
                    val selectionne = numero == etat.periodeNumero
                    if (selectionne) {
                        Button(onClick = { viewModel.selectionnerPeriode(numero) }) {
                            Text("$nomPeriode $numero")
                        }
                    } else {
                        OutlinedButton(onClick = { viewModel.selectionnerPeriode(numero) }) {
                            Text("$nomPeriode $numero")
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
        } else {
            Text(
                text = etat.periodeNumero?.let { "$nomPeriode $it" } ?: "Aucune période créée",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        etat.periodeId?.let { periodeId ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("$nomPeriode terminé", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Active cette option à la fin de la période : l'application affichera alors la suivante.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = etat.periodeTerminee,
                    onCheckedChange = { viewModel.definirPeriodeTerminee(periodeId, it) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        val couleurGenerale = couleurStatut(etat.statutGeneral)
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .size(160.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(couleurGenerale.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                progress = ((etat.moyenneGenerale ?: 0.0) / 20.0).toFloat().coerceIn(0f, 1f),
                modifier = Modifier.size(120.dp),
                color = couleurGenerale,
                strokeWidth = 8.dp,
                trackColor = couleurGenerale.copy(alpha = 0.15f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = etat.moyenneGenerale?.let { String.format("%.2f", it) } ?: "--",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text("/ 20", style = MaterialTheme.typography.bodySmall)
            }
        }

        Text(
            text = "Objectif : ${etat.objectifPeriode.toInt()}/20",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp)
        )

        val contexte = LocalContext.current
        if (etat.moyenneGenerale != null) {
            OutlinedButton(
                onClick = { partagerResume(contexte, etat) },
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
            ) { Text("Partager mon résumé") }
        }

        Spacer(Modifier.height(24.dp))

        AlerteMatieres(etat.matieres, etat.moyenneGenerale)

        Text("Tes matières", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (etat.matieres.isEmpty()) {
            Text(
                "Aucune matière enregistrée.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            etat.matieres.forEach { item ->
                LigneMatiere(item)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Matières « en danger » : moyenne sous le seuil de réussite (10/20), de la plus faible à la
 * moins faible, avec leur poids sur la moyenne générale de la période.
 */
@Composable
private fun AlerteMatieres(matieres: List<NoteaRepository.MatiereAvecMoyenne>, moyenneGenerale: Double?) {
    val notees = matieres.filter { it.moyenne != null }
    val enDanger = notees
        .filter { it.moyenne!! < MoyenneCalculator.SEUIL_REUSSITE }
        .sortedBy { it.moyenne }
    if (enDanger.isEmpty()) return

    val totalCoefficients = notees.sumOf { it.matiere.coefficient }
    val rouge = Color(0xFFF44336)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(rouge.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text("Matières à surveiller", style = MaterialTheme.typography.titleMedium, color = rouge)
        Text(
            "Matières dont la moyenne est inférieure à ${MoyenneCalculator.SEUIL_REUSSITE.toInt()}/20 :",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(6.dp))
        enDanger.forEach { item ->
            val moyenne = item.moyenne!!
            val poids = if (moyenneGenerale != null && totalCoefficients > 0) {
                (moyenne - moyenneGenerale) * item.matiere.coefficient / totalCoefficients
            } else {
                null
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.matiere.nom, style = MaterialTheme.typography.bodyMedium)
                Text(
                    buildString {
                        append(String.format("%.2f", moyenne))
                        if (poids != null && poids < 0) append("  ·  ${String.format("%.2f", poids)} pt")
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = rouge
                )
            }
        }
        Text(
            "Les points indiquent l'effet de la matière sur ta moyenne générale. " +
                "Pour savoir quelle note viser : menu ⋮ → Statistiques → « Quelle note me faut-il ? ».",
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
    Spacer(Modifier.height(24.dp))
}

/** Résumé en texte (WhatsApp, SMS…) : moyenne générale et moyenne de chaque matière notée. */
private fun partagerResume(contexte: Context, etat: DashboardUiState) {
    val texte = buildString {
        append("Notea — ${etat.nomPeriode} ${etat.periodeNumero ?: ""}\n")
        append("Moyenne générale : ${String.format("%.2f", etat.moyenneGenerale ?: 0.0)}/20")
        append(" (objectif ${etat.objectifPeriode.toInt()}/20)\n\n")
        etat.matieres.filter { it.moyenne != null }.forEach {
            append("${it.matiere.nom} : ${String.format("%.2f", it.moyenne)}\n")
        }
    }.trim()
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, texte)
    }
    contexte.startActivity(Intent.createChooser(intent, "Partager mon résumé"))
}

@Composable
private fun LigneMatiere(item: NoteaRepository.MatiereAvecMoyenne) {
    val couleur = couleurStatut(item.statut)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(couleur.copy(alpha = 0.25f), Color.Transparent)
                ),
                shape = RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(item.matiere.nom, style = MaterialTheme.typography.bodyLarge)
            Text("Coef ${item.matiere.coefficient}", style = MaterialTheme.typography.bodySmall)
        }
        Text(
            text = item.moyenne?.let { String.format("%.2f", it) } ?: "--",
            style = MaterialTheme.typography.titleMedium,
            color = couleur
        )
    }
}

/** Vert = objectif atteint, jaune = proche, rouge = loin, gris = pas encore de note. */
private fun couleurStatut(statut: MoyenneCalculator.StatutObjectif?): Color = when (statut) {
    MoyenneCalculator.StatutObjectif.ATTEINT -> Color(0xFF4CAF50)
    MoyenneCalculator.StatutObjectif.PROCHE -> Color(0xFFFFC107)
    MoyenneCalculator.StatutObjectif.LOIN -> Color(0xFFF44336)
    null -> Color(0xFF9E9E9E)
}
