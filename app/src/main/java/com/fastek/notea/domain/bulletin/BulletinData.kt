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
    val moyenne: Double?,
    /** Appréciation de la matière (menu rapide ou saisie libre) ; vide = case à remplir à la main. */
    val appreciation: String = ""
) {
    /** Comme sur le bulletin officiel : la moyenne arrondie à 2 décimales × le coefficient. */
    val moyenneCoef: Double? get() = moyenne?.let { Math.round(it * 100.0) / 100.0 * coefficient }
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
    /**
     * Moyennes des périodes précédentes (période 1, 2…), renseignées uniquement sur le
     * dernier bulletin de l'année ; une valeur null = période sans note.
     */
    val moyennesPrecedentes: List<Double?>,
    /** Renseignée uniquement sur le dernier bulletin de l'année, si toutes les périodes ont une moyenne. */
    val moyenneAnnuelle: Double?,
    /** Dernier bulletin de l'année : décision du conseil + appréciation générale. */
    val finAnnee: Boolean,
    // En-tête facultatif, vide tant que l'élève ne l'a pas renseigné dans son profil.
    val dateNaissance: String = "",
    val lieuNaissance: String = "",
    val effectif: String = "",
    val aptitudeEps: String = "",
    val redoublant: String = ""
) {
    val libellePeriode: String
        get() = typeEtablissement.nomPeriode

    /** Formule affichée à côté de la moyenne annuelle. */
    val formuleAnnuelle: String
        get() = if (typeEtablissement == TypeEtablissement.PUBLIC) "(2 × S2 + S1) ÷ 3" else "(T1 + T2 + T3) ÷ 3"
}
