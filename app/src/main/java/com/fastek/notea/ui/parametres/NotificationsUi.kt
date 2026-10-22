package com.fastek.notea.ui.parametres

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fastek.notea.notification.NotificationsHelper

/** État des notifications, relu à chaque retour dans l'application (après un passage dans les réglages Android). */
@Composable
fun rememberNotificationsActivees(): MutableState<Boolean> {
    val context = LocalContext.current
    val etat = remember { mutableStateOf(NotificationsHelper.activees(context)) }
    val proprietaire = LocalLifecycleOwner.current
    DisposableEffect(proprietaire) {
        val observateur = LifecycleEventObserver { _, evenement ->
            if (evenement == Lifecycle.Event.ON_RESUME) etat.value = NotificationsHelper.activees(context)
        }
        proprietaire.lifecycle.addObserver(observateur)
        onDispose { proprietaire.lifecycle.removeObserver(observateur) }
    }
    return etat
}

/**
 * Bouton « Autoriser les notifications » : affiche la demande d'autorisation d'Android la première
 * fois, puis ouvre directement les réglages de l'application si elle a déjà été refusée.
 */
@Composable
fun rememberDemandeNotifications(etat: MutableState<Boolean>): () -> Unit {
    val context = LocalContext.current
    val lanceur = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        etat.value = NotificationsHelper.activees(context)
    }
    return {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !NotificationsHelper.dejaDemandee(context)) {
            NotificationsHelper.marquerDemandee(context)
            lanceur.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationsHelper.ouvrirReglages(context)
        }
    }
}

/** Bandeau affiché tant que les notifications sont désactivées : explique pourquoi et permet de les activer. */
@Composable
fun BandeauNotifications() {
    val etat = rememberNotificationsActivees()
    val demander = rememberDemandeNotifications(etat)
    if (etat.value) return

    val orange = Color(0xFFFF9800)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(orange.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text("Rappels désactivés", style = MaterialTheme.typography.titleSmall)
        Text(
            "Autorise les notifications pour être prévenu avant tes devoirs et tes événements.",
            style = MaterialTheme.typography.bodySmall
        )
        Spacer(Modifier.height(8.dp))
        Button(onClick = demander) { Text("Autoriser les notifications") }
    }
    Spacer(Modifier.height(12.dp))
}
