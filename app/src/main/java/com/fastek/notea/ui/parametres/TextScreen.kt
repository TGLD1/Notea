package com.fastek.notea.ui.parametres

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Écran texte générique — utilisé pour À propos, Politique de confidentialité, CGU. */
@Composable
fun TextScreen(titre: String, contenu: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(titre, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(16.dp))
        Text(contenu, style = MaterialTheme.typography.bodyMedium)
    }
}

object TextesStatiques {
    const val A_PROPOS_APP = "Notea est une application de suivi scolaire conçue pour les " +
        "lycéens béninois. Elle permet de gérer ses notes, ses matières et ses objectifs, " +
        "avec un tableau de bord visuel — 100% hors ligne, sans connexion internet requise.\n\n" +
        "Développée par FASTEK.\nVersion 0.1."

    const val A_PROPOS_DEV = "Notea est développée par Divin, fondateur de FASTEK.\n\n" +
        "Contact :\nEmail : tgldivin@gmail.com\nWhatsApp : +229 01 64 40 94 45"

    const val CONFIDENTIALITE = "Notea fonctionne entièrement hors ligne. Aucune donnée " +
        "personnelle (notes, nom, classe, etc.) n'est collectée, transmise ou partagée avec " +
        "FASTEK ou un tiers. Toutes les informations restent stockées uniquement sur ton " +
        "téléphone, dans la mémoire de l'application.\n\n" +
        "Si tu utilises la sauvegarde Google Drive (fonctionnalité à venir), tes données " +
        "seront alors envoyées vers ton propre compte Google Drive personnel — jamais vers " +
        "un serveur FASTEK."

    const val CGU = "En utilisant Notea, tu acceptes que :\n\n" +
        "• L'application est fournie « en l'état », sans garantie de résultat scolaire.\n\n" +
        "• Les calculs de moyennes sont fournis à titre indicatif ; se référer au bulletin " +
        "officiel de ton établissement en cas de doute.\n\n" +
        "• FASTEK ne pourra être tenu responsable de la perte de données en cas de " +
        "désinstallation, de changement de téléphone sans sauvegarde, ou de dysfonctionnement " +
        "de l'appareil.\n\n" +
        "• Il est interdit de revendre ou redistribuer l'application sans autorisation de FASTEK."
}
