package com.fastek.notea.domain.bulletin

import com.fastek.notea.data.local.entity.TypeEtablissement

/** Une ligne du tableau du bulletin (une matière pour la période choisie). */
data class LigneBulletin(
    val matiere: String,
    val coefficient: Int,
    /** Conduite : pas de colonnes interro/devoirs, seule la note du conseil compte. */
    val estConduite: Boolean,
    val moyenneInterro: Double?,
    val devoir1: Double?,
    val devoir2: Double?,
    /** Moyenne de la matière sur 20 — null tant qu'aucune note n'est saisie. */
    val moyenne: Double?
) {
    val moyenneCoef: Double? get() = moyenne?.let { it * coefficient }
}

/** Tout ce qu'il faut pour dessiner le bulletin PDF d'une période. */
data class BulletinData(
    val nom: String,
    val prenom: String,
    val matricule: String,
    val classe: String,
    val anneeScolaire: String,
    val typeEtablissement: TypeEtablissement,
    val numeroPeriode: Int,
    val lignes: List<LigneBulletin>,
    val moyenneGenerale: Double?,
    /** Somme des coefficients des matières déjà notées. */
    val totalCoefficients: Int,
    /** Somme des (moyenne × coefficient) des matières déjà notées. */
    val totalMoyCoef: Double,
    /** Renseignée uniquement sur le bulletin du semestre 2 (public). */
    val moyennePeriode1: Double?,
    /** Renseignée uniquement sur le bulletin du semestre 2 (public), si S1 et S2 existent. */
    val moyenneAnnuelle: Double?,
    /** Dernier bulletin de l'année : décision du conseil + appréciation générale. */
    val finAnnee: Boolean
) {
    val libellePeriode: String
        get() = if (typeEtablissement == TypeEtablissement.PUBLIC) "Semestre" else "Trimestre"
}
