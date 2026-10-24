package com.fastek.notea.ui.commun

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** Format de date affiché et enregistré partout dans l'application : JJ/MM/AAAA. */
val FORMAT_DATE_ECRAN: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

private fun enMillis(date: LocalDate): Long = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

/**
 * Bouton de date commun à tous les écrans : affiche la date choisie ; au toucher, l'écran appelant
 * affiche [ChoixDateEcran] (le calendrier prend la place du formulaire, sans fenêtre flottante).
 */
@Composable
fun BoutonDate(libelle: String, valeur: String, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(if (valeur.isBlank()) "$libelle : choisir une date" else "$libelle : $valeur  (modifier)")
    }
}

/**
 * Calendrier plein écran. [valeurInitiale] au format JJ/MM/AAAA (peut être vide) ;
 * [anneeMin]..[anneeMax] bornent les années proposées ; [moisInitial] est utilisé quand aucune date
 * n'est encore choisie (par exemple pour ouvrir une date de naissance sur une année plausible).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChoixDateEcran(
    titre: String,
    valeurInitiale: String,
    anneeMin: Int,
    anneeMax: Int,
    moisInitial: LocalDate? = null,
    onValider: (String) -> Unit,
    onAnnuler: () -> Unit
) {
    val dateInitiale = try {
        LocalDate.parse(valeurInitiale, FORMAT_DATE_ECRAN)
    } catch (e: Exception) {
        null
    }
    val etatCalendrier = rememberDatePickerState(
        initialSelectedDateMillis = dateInitiale?.let { enMillis(it) },
        initialDisplayedMonthMillis = (dateInitiale ?: moisInitial)?.let { enMillis(it) },
        yearRange = anneeMin..anneeMax
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(titre, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        DatePicker(state = etatCalendrier, showModeToggle = false)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(onClick = onAnnuler, modifier = Modifier.weight(1f)) { Text("Annuler") }
            Button(
                onClick = {
                    etatCalendrier.selectedDateMillis?.let { millis ->
                        val date = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        onValider(date.format(FORMAT_DATE_ECRAN))
                    } ?: onAnnuler()
                },
                modifier = Modifier.weight(1f)
            ) { Text("Valider") }
        }
    }
}
