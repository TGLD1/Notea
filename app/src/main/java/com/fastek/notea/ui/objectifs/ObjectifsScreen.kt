package com.fastek.notea.ui.objectifs

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ObjectifsScreen(viewModel: ObjectifsViewModel) {
    val etat by viewModel.uiState.collectAsState()
    val eleveId = etat.eleveId

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Objectifs", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))

        Text("Objectif annuel", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = etat.objectifAnnuel,
            onValueChange = { valeur -> eleveId?.let { viewModel.onObjectifAnnuelChange(it, valeur) } },
            label = { Text("Sur 20") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        etat.periodes.forEach { periode ->
            Text("Semestre ${periode.numero}", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = periode.objectif,
                    onValueChange = { viewModel.onObjectifPeriodeChange(periode.id, it) },
                    label = { Text("Objectif /20") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(
                    value = periode.conduite,
                    onValueChange = { viewModel.onConduiteChange(periode.id, it) },
                    label = { Text("Conduite /20") },
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                "Conduite : laisse vide tant que le conseil des professeurs ne l'a pas attribuée.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}
