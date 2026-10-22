package com.fastek.notea.ui.parametres

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fastek.notea.BuildConfig

/** Résultat du dernier paiement, alimenté par l'écoute KkiaPay de l'activité. */
object EtatDon {
    var message by mutableStateOf<String?>(null)
    var reussi by mutableStateOf(false)
}

private const val MONTANT_MINIMUM = 100
private val MONTANTS_PROPOSES = listOf(500, 1000, 2000, 5000)

/** Don libre via KkiaPay (Mobile Money et carte). Le paiement est géré par le SDK KkiaPay. */
@Composable
fun DonScreen(onDon: (montant: Int) -> Unit) {
    val disponible = BuildConfig.KKIAPAY_PUBLIC_KEY.isNotBlank()
    var montant by remember { mutableStateOf(1000) }
    var autre by remember { mutableStateOf("") }
    val montantEffectif = autre.toIntOrNull() ?: montant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("Faire un don à FASTEK", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(TextesStatiques.DON, style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(16.dp))

        if (!disponible) {
            Text(
                "Le don en ligne est momentanément indisponible dans cette version de l'application.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
            return@Column
        }

        Text(
            if (BuildConfig.KKIAPAY_SANDBOX) "Mode test : aucun montant ne sera débité." else "Paiement sécurisé par KkiaPay.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MONTANTS_PROPOSES.forEach { valeur ->
                val selectionne = autre.isEmpty() && montant == valeur
                val action = { montant = valeur; autre = "" }
                if (selectionne) {
                    Button(onClick = action, modifier = Modifier.weight(1f)) { Text("$valeur") }
                } else {
                    OutlinedButton(onClick = action, modifier = Modifier.weight(1f)) { Text("$valeur") }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = autre,
            onValueChange = { saisie -> autre = saisie.filter { it.isDigit() }.take(7) },
            label = { Text("Autre montant (en FCFA)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                EtatDon.message = null
                onDon(montantEffectif)
            },
            enabled = montantEffectif >= MONTANT_MINIMUM,
            modifier = Modifier.fillMaxWidth()
        ) { Text("Donner $montantEffectif FCFA") }

        if (montantEffectif < MONTANT_MINIMUM) {
            Spacer(Modifier.height(4.dp))
            Text("Montant minimum : $MONTANT_MINIMUM FCFA", style = MaterialTheme.typography.bodySmall)
        }

        EtatDon.message?.let {
            Spacer(Modifier.height(12.dp))
            Text(
                it,
                color = if (EtatDon.reussi) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}
