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

/** Écran texte générique — utilisé pour À propos, Politique de confidentialité, CGU, Don. */
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
    const val A_PROPOS_APP = "Notea est une application de suivi scolaire conçue pour les élèves béninois. " +
        "Elle permet de saisir ses notes, de suivre ses moyennes et ses objectifs, de simuler ses résultats " +
        "et de produire un bulletin au format PDF. Elle fonctionne hors ligne, sans compte, et ne nécessite " +
        "internet que pour le don facultatif.\n\n" +
        "Développée par FASTEK, à Ouidah (Bénin)."

    const val A_PROPOS_DEV = "Notea est conçue et développée par Divin, fondateur de FASTEK.\n\n" +
        "Pour toute question, suggestion ou signalement :\nE-mail : tgldivin@gmail.com\nWhatsApp : +229 01 64 40 94 45"

    const val CONFIDENTIALITE = "Notea fonctionne hors ligne. Aucune donnée " +
        "personnelle (notes, nom, classe, etc.) n'est collectée, transmise ou partagée avec " +
        "FASTEK ou un tiers. Toutes les informations restent stockées uniquement sur ton " +
        "téléphone, dans la mémoire de l'application.\n\n" +
        "La photo de l'élève, facultative, est enregistrée dans la mémoire de l'application et sert " +
        "uniquement au bulletin PDF. Les archives des années précédentes restent aussi sur ton téléphone.\n\n" +
        "Si tu exportes une sauvegarde, une archive ou un fichier CSV, il est enregistré ou partagé là où " +
        "tu le choisis toi-même (téléphone, Drive, WhatsApp…) : FASTEK n'y a jamais accès.\n\n" +
        "Don en ligne (facultatif)\n" +
        "C'est la seule fonction qui utilise internet. Le paiement est traité par KkiaPay, notre " +
        "prestataire de paiement, dans sa propre fenêtre : c'est KkiaPay, et non Notea, qui reçoit " +
        "les informations nécessaires au paiement (nom, numéro de téléphone Mobile Money ou données " +
        "de carte bancaire). Notea ne voit, ne collecte et ne conserve aucune information de paiement ; " +
        "elle est seulement informée du résultat (réussi ou échoué). Le don ne demande aucun compte, " +
        "ni sur Notea ni chez KkiaPay. KkiaPay applique sa propre politique de confidentialité.\n\n" +
        "Notea ne contient ni publicité ni outil de suivi.\n\n" +
        "Contact : tgldivin@gmail.com"

    const val DON = "Notea est gratuite. Si elle t'est utile, ton don aide FASTEK à la maintenir et à la faire évoluer. " +
        "Le paiement s'effectue de manière sécurisée par Mobile Money ou par carte bancaire, via KkiaPay, " +
        "sans création de compte.\n\n" +
        "Merci pour ton soutien."

    const val CGU = "En utilisant Notea, tu reconnais et acceptes que :\n\n" +
        "• L'application est fournie « en l'état » et ne garantit aucun résultat scolaire.\n\n" +
        "• Les calculs de moyennes sont fournis à titre indicatif : en cas de différence, le bulletin " +
        "officiel de ton établissement fait foi.\n\n" +
        "• FASTEK ne peut être tenu responsable d'une perte de données due à une désinstallation, à un " +
        "changement de téléphone sans sauvegarde ou à un dysfonctionnement de l'appareil. Pense à " +
        "sauvegarder régulièrement tes données.\n\n" +
        "• Les dons sont facultatifs, volontaires et non remboursables.\n\n" +
        "• La revente ou la redistribution de l'application sans l'autorisation de FASTEK est interdite."
}
