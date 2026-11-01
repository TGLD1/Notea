package com.fastek.notea.enseignant.ui

data class SectionTexte(val titre: String?, val texte: String)

data class PageTexte(val titre: String, val sousTitre: String, val sections: List<SectionTexte>)

/** Textes d'information de Notea Enseignant : à propos, calculs, confidentialité, conditions d'utilisation. */
object TextesEnseignant {

    const val EMAIL = "tgldivin@gmail.com"
    const val WHATSAPP_AFFICHE = "+229 01 64 40 94 45"
    const val WHATSAPP_LIEN = "https://wa.me/2290164409445"

    val IDENTIFIANTS = listOf("apropos", "calcul", "confidentialite", "conditions")

    fun page(identifiant: String): PageTexte? = when (identifiant) {
        "apropos" -> PageTexte(
            "À propos", "Notea Enseignant",
            listOf(
                SectionTexte(
                    null,
                    "Notea Enseignant est l'application des professeurs de la famille Notea. Elle sert à créer tes classes, " +
                        "inscrire tes élèves, saisir leurs notes, obtenir automatiquement les moyennes, les rangs et les " +
                        "statistiques, tenir ton emploi du temps avec des rappels pendant tes cours, et exporter un classeur Excel."
                ),
                SectionTexte(
                    "Hors ligne et sans compte",
                    "Tout fonctionne sans internet et sans compte. Internet n'est utilisé que pour le don facultatif."
                ),
                SectionTexte(
                    "Qui la développe",
                    "Notea est conçue et développée par Divin, fondateur de FASTEK, à Ouidah (Bénin).\n\n" +
                        "Pour toute question, suggestion ou signalement :\nE-mail : $EMAIL\nWhatsApp : $WHATSAPP_AFFICHE"
                )
            )
        )
        "calcul" -> PageTexte(
            "Comment sont calculées les moyennes", "Les règles de Notea",
            listOf(
                SectionTexte(
                    "Moyenne d'une période",
                    "La moyenne des interrogations compte pour une seule note. Elle s'ajoute aux devoirs, 2 au maximum par " +
                        "période : moyenne = (moyenne des interrogations + devoir 1 + devoir 2) ÷ (1 + nombre de devoirs).\n\n" +
                        "Sans devoir, c'est la moyenne des interrogations ; sans interrogation, la moyenne des devoirs. " +
                        "Les moyennes sont arrondies à 2 décimales."
                ),
                SectionTexte(
                    "Une case vide, c'est un absent",
                    "Une note non saisie n'est pas comptée et ne pénalise pas l'élève : il n'a pas de zéro. " +
                        "Pour mettre un zéro, saisis 0."
                ),
                SectionTexte(
                    "Moyenne annuelle",
                    "Établissement public : (2 × moyenne du 2ᵉ semestre + moyenne du 1ᵉʳ semestre) ÷ 3.\n" +
                        "Établissement privé : moyenne des 3 trimestres.\n\n" +
                        "Elle n'apparaît que lorsque toutes les périodes ont une moyenne."
                ),
                SectionTexte(
                    "Rang",
                    "Les élèves sont classés par moyenne décroissante. Les ex æquo partagent le même rang et le rang suivant " +
                        "est décalé : 1, 2, 2, 4."
                ),
                SectionTexte(
                    "Statistiques de la classe",
                    "La moyenne de la classe est la moyenne des moyennes des élèves qui en ont une. Le taux de réussite est " +
                        "la part des élèves à 10/20 ou plus. Les couleurs : rouge sous 10, ambre de 10 à 12, vert à partir de 12."
                ),
                SectionTexte(
                    "Le classeur Excel",
                    "Le classeur contient de vraies formules : si tu corriges une note dans Excel, les moyennes, les rangs " +
                        "et les statistiques se recalculent tout seuls."
                ),
                SectionTexte(
                    "À savoir",
                    "Ces calculs sont indicatifs : en cas de différence, le bulletin officiel de ton établissement fait foi."
                )
            )
        )
        "confidentialite" -> PageTexte(
            "Confidentialité", "Tes données et celles de tes élèves",
            listOf(
                SectionTexte(
                    "Tout reste sur ton téléphone",
                    "Notea Enseignant fonctionne hors ligne. Tes classes, les noms de tes élèves, leurs notes, ton emploi du " +
                        "temps et tes rappels sont stockés uniquement dans la mémoire de l'application, sur ton téléphone. " +
                        "FASTEK ne les collecte pas, ne les reçoit pas et ne les partage avec personne."
                ),
                SectionTexte(
                    "Les données de tes élèves",
                    "Les noms et les notes de tes élèves sont des informations personnelles dont tu as la garde. Utilise-les " +
                        "uniquement pour ton enseignement, protège ton téléphone par un verrouillage, et ne partage tes exports " +
                        "qu'avec les personnes autorisées."
                ),
                SectionTexte(
                    "Sauvegarde automatique d'Android",
                    "Si la sauvegarde Google est activée sur ton téléphone, Android enregistre lui-même une copie des données " +
                        "de l'application dans ton compte Google, pour les restaurer sur un nouvel appareil. Cette copie est " +
                        "gérée par Google et ton téléphone ; FASTEK n'y a jamais accès."
                ),
                SectionTexte(
                    "Exports, sauvegardes et archives",
                    "Le classeur Excel, les sauvegardes et les archives sont enregistrés ou partagés là où tu le choisis " +
                        "(téléphone, Drive, WhatsApp…) : FASTEK n'y a jamais accès. Les fichiers de liste d'élèves que tu " +
                        "importes sont lus sur ton téléphone et ne sont jamais envoyés."
                ),
                SectionTexte(
                    "Notifications et alarmes",
                    "Les rappels de cours sont programmés sur ton téléphone avec le système d'alarmes d'Android. Ils n'utilisent " +
                        "pas internet."
                ),
                SectionTexte(
                    "Don en ligne (facultatif)",
                    "C'est la seule fonction qui utilise internet. Le paiement est traité par FedaPay, notre prestataire de " +
                        "paiement, dans son propre formulaire : c'est FedaPay, et non Notea, qui reçoit les informations " +
                        "nécessaires au paiement (nom, numéro de téléphone Mobile Money ou données de carte bancaire). Notea ne " +
                        "voit, ne collecte et ne conserve aucune information de paiement ; elle est seulement informée du " +
                        "résultat (réussi ou échoué). Le don ne demande aucun compte. FedaPay applique sa propre politique de " +
                        "confidentialité."
                ),
                SectionTexte(
                    "Ni publicité ni suivi",
                    "Notea Enseignant ne contient ni publicité ni outil de suivi."
                ),
                SectionTexte("Contact", "E-mail : $EMAIL")
            )
        )
        "conditions" -> PageTexte(
            "Conditions d'utilisation", "En utilisant Notea Enseignant",
            listOf(
                SectionTexte(
                    null,
                    "En utilisant Notea Enseignant, tu reconnais et acceptes que :\n\n" +
                        "• L'application est fournie « en l'état », sans garantie de résultat.\n\n" +
                        "• Les calculs de moyennes, de rangs et de statistiques sont fournis à titre indicatif : en cas de " +
                        "différence, le bulletin officiel de ton établissement fait foi. Tu restes responsable de l'exactitude " +
                        "des notes que tu saisis.\n\n" +
                        "• Tu es responsable des données de tes élèves que tu enregistres, et de leur usage conforme aux règles " +
                        "de ton établissement.\n\n" +
                        "• FASTEK ne peut être tenu responsable d'une perte de données due à une désinstallation, à un " +
                        "changement de téléphone sans sauvegarde ou à un dysfonctionnement de l'appareil. Pense à sauvegarder " +
                        "régulièrement tes données et à archiver chaque année.\n\n" +
                        "• Les rappels de cours dépendent du système de ton téléphone : FASTEK ne garantit pas leur déclenchement " +
                        "si le téléphone est éteint, si les notifications sont coupées ou si le système limite l'application.\n\n" +
                        "• Les dons sont facultatifs, volontaires et non remboursables.\n\n" +
                        "• La revente ou la redistribution de l'application sans l'autorisation de FASTEK est interdite."
                )
            )
        )
        else -> null
    }
}
