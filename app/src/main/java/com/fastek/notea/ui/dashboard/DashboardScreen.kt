package com.fastek.notea.ui.dashboard

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.domain.calcul.MoyenneCalculator

@Composable
fun DashboardScreen(viewModel: DashboardViewModel) {
    val etat by viewModel.uiState.collectAsState()

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
        Text(
            text = etat.periodeNumero?.let { "Semestre $it" } ?: "Aucune période créée",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

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

        Spacer(Modifier.height(32.dp))

        Text("Tes matières", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))

        if (etat.matieres.isEmpty()) {
            Text(
                "Aucune matière pour l'instant.",
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
