package com.fastek.notea.enseignant.export

import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.data.local.entity.TypeNote
import com.fastek.notea.enseignant.domain.ClasseCalculator
import com.fastek.notea.enseignant.domain.EleveNotes
import com.fastek.notea.enseignant.domain.NoteSaisie
import com.fastek.notea.enseignant.domain.StatistiquesClasse
import java.io.OutputStream

data class EvaluationExport(val id: Long, val type: TypeNote, val intitule: String)

data class PeriodeExport(val numero: Int, val evaluations: List<EvaluationExport>)

data class EleveExport(val id: Long, val nom: String, val prenom: String, val matricule: String = "")

/** [notes] : identifiant d'évaluation -> (identifiant d'élève -> note sur 20). */
data class ClasseExport(
    val nom: String,
    val coefficient: Int,
    val eleves: List<EleveExport>,
    val periodes: List<PeriodeExport>,
    val notes: Map<Long, Map<Long, Double>>
)

data class ClasseurExport(
    val professeur: String,
    val matiere: String,
    val type: TypeEtablissement,
    val anneeScolaire: String,
    val etablissement: String,
    val classes: List<ClasseExport>
)

/**
 * Construit le classeur Excel du professeur : une feuille « Synthèse » puis une feuille par classe.
 *
 * Chaque feuille de classe contient de vraies formules qui reproduisent le moteur de Notea :
 *   moyenne de période = somme / nombre de valeurs parmi (moyenne des interros, devoir 1, devoir 2)
 *   — ce qui équivaut à (M_intero + devoirs) / (1 + nombre de devoirs) quand il y a des interros.
 * Les moyennes sont arrondies à 2 décimales ; les cases vides ne comptent pas (élève absent).
 * Deux colonnes d'interrogation vierges sont ajoutées par période pour saisir directement dans Excel.
 */
object ClasseurEnseignant {

    private const val INTERROS_SUPPLEMENTAIRES = 2
    private const val LIGNE_PREMIERE_ELEVE = 5
    private const val COLONNE_PREMIERE_PERIODE = 3

    /** Adresses des statistiques d'une feuille de classe, pour la feuille « Synthèse ». */
    private class AdressesClasse(
        val nomFeuille: String,
        val effectif: Int,
        val colonnesMoyenne: List<Int>,
        val colonneAnnuelle: Int,
        val ligneEffectif: Int,
        val statsPeriodes: List<StatistiquesClasse>,
        val statsAnnuelle: StatistiquesClasse
    ) {
        val ligneMoyenne get() = ligneEffectif + 1
        val lignePlusForte get() = ligneEffectif + 2
        val lignePlusFaible get() = ligneEffectif + 3
        val ligneTaux get() = ligneEffectif + 4
    }

    private class BlocPeriode(
        val periode: PeriodeExport,
        val interros: List<EvaluationExport>,
        val devoirs: List<EvaluationExport>,
        val colDebut: Int
    ) {
        val nbColsInterro = interros.size + INTERROS_SUPPLEMENTAIRES
        val colMoyInterro = colDebut + nbColsInterro
        val colDevoir1 = colMoyInterro + 1
        val colDevoir2 = colMoyInterro + 2
        val colMoyenne = colMoyInterro + 3
        val colMoyCoef = colMoyInterro + 4
        val colRang = colMoyInterro + 5
        val largeur = nbColsInterro + 6
    }

    fun ecrire(export: ClasseurExport, sortie: OutputStream) =
        XlsxWriter.ecrire(construire(export), sortie)

    fun construire(export: ClasseurExport): List<FeuilleXlsx> {
        val utilises = mutableListOf("Synthèse")
        val feuilles = mutableListOf<FeuilleXlsx>()
        val adresses = mutableListOf<AdressesClasse>()
        export.classes.forEach { classe ->
            val nom = XlsxWriter.nomFeuilleValide(classe.nom, utilises)
            utilises.add(nom)
            val (feuille, adresse) = construireClasse(export, classe, nom)
            feuilles.add(feuille)
            adresses.add(adresse)
        }
        return listOf(construireSynthese(export, adresses)) + feuilles
    }

    private fun nomPeriode(type: TypeEtablissement, numero: Int) =
        if (type == TypeEtablissement.PUBLIC) "Semestre $numero" else "Trimestre $numero"

    private fun ref(col: Int, ligne: Int, colFixe: Boolean = false, ligneFixe: Boolean = false) =
        XlsxWriter.reference(col, ligne, colFixe, ligneFixe)

    private fun formule(texte: String, resultat: Any?, style: Int) =
        Cellule.Formule(texte, resultat ?: "", style)

    private fun construireClasse(
        export: ClasseurExport,
        c: ClasseExport,
        nomFeuille: String
    ): Pair<FeuilleXlsx, AdressesClasse> {
        val f = FeuilleXlsx(nomFeuille)
        val type = export.type

        val periodes = (1..type.nombrePeriodes).map { n ->
            c.periodes.firstOrNull { it.numero == n } ?: PeriodeExport(n, emptyList())
        }
        var col = COLONNE_PREMIERE_PERIODE
        val blocs = periodes.map { p ->
            val bloc = BlocPeriode(
                periode = p,
                interros = p.evaluations.filter { it.type == TypeNote.INTERROGATION },
                devoirs = p.evaluations.filter { it.type == TypeNote.DEVOIR }.take(2),
                colDebut = col
            )
            col += bloc.largeur
            bloc
        }
        val colAnnuelle = col
        val colRangAnnuel = col + 1

        val premiere = LIGNE_PREMIERE_ELEVE
        val derniere = premiere + maxOf(c.eleves.size, 1) - 1

        // Résultats calculés par Notea : sert aux valeurs en cache et aux statistiques.
        val resultats = blocs.map { b ->
            val evaluations = b.interros + b.devoirs
            val eleves = c.eleves.map { e ->
                EleveNotes(
                    e.id,
                    evaluations.mapNotNull { ev ->
                        c.notes[ev.id]?.get(e.id)?.let { NoteSaisie(ev.type, it) }
                    }
                )
            }
            ClasseCalculator.calculerPeriode(eleves)
        }
        val annuelles = c.eleves.indices.map { i ->
            ClasseCalculator.moyenneAnnuelle(type, resultats.map { it.lignes[i].moyenne })
        }
        val rangsAnnuels = ClasseCalculator.rangs(annuelles)
        val statsAnnuelles = ClasseCalculator.statistiques(annuelles)

        // En-tête de la feuille
        f.poser(0, 0, Cellule.Texte("${c.nom} — ${export.matiere}", StylesXlsx.TITRE))
        val sousTitre = buildString {
            append("Professeur : ${export.professeur} — Année scolaire : ${export.anneeScolaire}")
            if (export.etablissement.isNotBlank()) append(" — ${export.etablissement}")
        }
        f.poser(1, 0, Cellule.Texte(sousTitre, StylesXlsx.DEFAUT))
        f.poser(2, 1, Cellule.Texte("Coefficient", StylesXlsx.LABEL))
        f.poser(2, 2, Cellule.Nombre(c.coefficient.toDouble(), StylesXlsx.ENTIER))

        // Lignes d'en-tête du tableau (groupes de période puis colonnes)
        for (cc in 0..2) f.poser(3, cc, Cellule.Vide(StylesXlsx.ENTETE))
        f.poser(4, 0, Cellule.Texte("N°", StylesXlsx.ENTETE))
        f.poser(4, 1, Cellule.Texte("Nom", StylesXlsx.ENTETE))
        f.poser(4, 2, Cellule.Texte("Prénoms", StylesXlsx.ENTETE))
        blocs.forEach { b ->
            f.poser(3, b.colDebut, Cellule.Texte(nomPeriode(type, b.periode.numero), StylesXlsx.ENTETE))
            for (k in 1 until b.largeur) f.poser(3, b.colDebut + k, Cellule.Vide(StylesXlsx.ENTETE))
            f.fusionner(3, b.colDebut, 3, b.colDebut + b.largeur - 1)
            for (j in 0 until b.nbColsInterro) {
                val intitule = b.interros.getOrNull(j)?.intitule.orEmpty().ifBlank { "Interro ${j + 1}" }
                f.poser(4, b.colDebut + j, Cellule.Texte(intitule, StylesXlsx.ENTETE))
                f.largeur(b.colDebut + j, 9.0)
            }
            f.poser(4, b.colMoyInterro, Cellule.Texte("Moy. interros", StylesXlsx.ENTETE))
            for (d in 0..1) {
                val intitule = b.devoirs.getOrNull(d)?.intitule.orEmpty().ifBlank { "Devoir ${d + 1}" }
                f.poser(4, b.colDevoir1 + d, Cellule.Texte(intitule, StylesXlsx.ENTETE))
                f.largeur(b.colDevoir1 + d, 9.0)
            }
            f.poser(4, b.colMoyenne, Cellule.Texte("Moy. /20", StylesXlsx.ENTETE))
            f.poser(4, b.colMoyCoef, Cellule.Texte("Moy. × coef.", StylesXlsx.ENTETE))
            f.poser(4, b.colRang, Cellule.Texte("Rang", StylesXlsx.ENTETE))
            f.largeur(b.colMoyInterro, 11.0)
            f.largeur(b.colMoyenne, 10.0)
            f.largeur(b.colMoyCoef, 11.0)
            f.largeur(b.colRang, 7.0)
        }
        f.poser(3, colAnnuelle, Cellule.Texte("Année", StylesXlsx.ENTETE))
        f.poser(3, colRangAnnuel, Cellule.Vide(StylesXlsx.ENTETE))
        f.fusionner(3, colAnnuelle, 3, colRangAnnuel)
        f.poser(4, colAnnuelle, Cellule.Texte("Moy. annuelle", StylesXlsx.ENTETE))
        f.poser(4, colRangAnnuel, Cellule.Texte("Rang", StylesXlsx.ENTETE))
        f.largeur(0, 5.0)
        f.largeur(1, 22.0)
        f.largeur(2, 24.0)
        f.largeur(colAnnuelle, 13.0)
        f.largeur(colRangAnnuel, 7.0)

        // Élèves
        c.eleves.forEachIndexed { i, e ->
            val r = premiere + i
            f.poser(r, 0, Cellule.Nombre((i + 1).toDouble(), StylesXlsx.ENTIER))
            f.poser(r, 1, Cellule.Texte(e.nom))
            f.poser(r, 2, Cellule.Texte(e.prenom))

            blocs.forEachIndexed { bi, b ->
                val ligne = resultats[bi].lignes[i]
                for (j in 0 until b.nbColsInterro) {
                    val v = b.interros.getOrNull(j)?.let { c.notes[it.id]?.get(e.id) }
                    f.poser(r, b.colDebut + j, if (v != null) Cellule.Nombre(v) else Cellule.Vide(StylesXlsx.NOTE))
                }
                val plageInterros = "${ref(b.colDebut, r)}:${ref(b.colMoyInterro - 1, r)}"
                f.poser(
                    r, b.colMoyInterro,
                    formule("IF(COUNT($plageInterros)=0,\"\",AVERAGE($plageInterros))", ligne.moyenneInterro, StylesXlsx.NOTE)
                )
                for (d in 0..1) {
                    val v = b.devoirs.getOrNull(d)?.let { c.notes[it.id]?.get(e.id) }
                    f.poser(r, b.colDevoir1 + d, if (v != null) Cellule.Nombre(v) else Cellule.Vide(StylesXlsx.NOTE))
                }
                val plageMoyenne = "${ref(b.colMoyInterro, r)}:${ref(b.colDevoir2, r)}"
                val celluleMoyenne = ref(b.colMoyenne, r)
                f.poser(
                    r, b.colMoyenne,
                    formule(
                        "IF(COUNT($plageMoyenne)=0,\"\",ROUND(SUM($plageMoyenne)/COUNT($plageMoyenne),2))",
                        ligne.moyenne, StylesXlsx.NOTE
                    )
                )
                f.poser(
                    r, b.colMoyCoef,
                    formule(
                        "IF($celluleMoyenne=\"\",\"\",ROUND($celluleMoyenne*\$C\$3,2))",
                        ligne.moyenne?.let { ClasseCalculator.arrondir(it * c.coefficient) }, StylesXlsx.NOTE
                    )
                )
                val colonneMoyennes = "${ref(b.colMoyenne, premiere, true, true)}:${ref(b.colMoyenne, derniere, true, true)}"
                f.poser(
                    r, b.colRang,
                    formule("IF($celluleMoyenne=\"\",\"\",RANK($celluleMoyenne,$colonneMoyennes,0))", ligne.rang, StylesXlsx.ENTIER)
                )
            }

            val moyennes = blocs.map { ref(it.colMoyenne, r) }
            val formuleAnnuelle = if (type == TypeEtablissement.PUBLIC) {
                "IF(COUNT(${moyennes[0]},${moyennes[1]})=2,ROUND((2*${moyennes[1]}+${moyennes[0]})/3,2),\"\")"
            } else {
                "IF(COUNT(${moyennes.joinToString(",")})=3,ROUND((${moyennes.joinToString("+")})/3,2),\"\")"
            }
            f.poser(r, colAnnuelle, formule(formuleAnnuelle, annuelles[i], StylesXlsx.NOTE))
            val celluleAnnuelle = ref(colAnnuelle, r)
            val colonneAnnuelles = "${ref(colAnnuelle, premiere, true, true)}:${ref(colAnnuelle, derniere, true, true)}"
            f.poser(
                r, colRangAnnuel,
                formule("IF($celluleAnnuelle=\"\",\"\",RANK($celluleAnnuelle,$colonneAnnuelles,0))", rangsAnnuels[i], StylesXlsx.ENTIER)
            )
        }
        // Classe sans élève : une ligne vierge évite des plages de formules invalides.
        if (c.eleves.isEmpty()) {
            for (cc in 0 until colRangAnnuel + 1) f.poser(premiere, cc, Cellule.Vide(StylesXlsx.TEXTE))
        }

        // Statistiques de la classe
        val ligneStats = derniere + 2
        listOf(
            "Effectif ayant une moyenne", "Moyenne de la classe", "Plus forte moyenne",
            "Plus faible moyenne", "Taux de réussite (moyenne ≥ 10)"
        ).forEachIndexed { k, libelle ->
            f.poser(ligneStats + k, 1, Cellule.Texte(libelle, StylesXlsx.LABEL))
            f.poser(ligneStats + k, 2, Cellule.Vide(StylesXlsx.LABEL))
            f.fusionner(ligneStats + k, 1, ligneStats + k, 2)
        }
        fun poserStatistiques(colonne: Int, s: StatistiquesClasse) {
            val plage = "${ref(colonne, premiere, true, true)}:${ref(colonne, derniere, true, true)}"
            f.poser(ligneStats, colonne, formule("COUNT($plage)", s.effectifNote, StylesXlsx.LABEL_ENTIER))
            f.poser(
                ligneStats + 1, colonne,
                formule("IF(COUNT($plage)=0,\"\",ROUND(AVERAGE($plage),2))", s.moyenneClasse, StylesXlsx.LABEL_NOTE)
            )
            f.poser(
                ligneStats + 2, colonne,
                formule("IF(COUNT($plage)=0,\"\",MAX($plage))", s.plusForte, StylesXlsx.LABEL_NOTE)
            )
            f.poser(
                ligneStats + 3, colonne,
                formule("IF(COUNT($plage)=0,\"\",MIN($plage))", s.plusFaible, StylesXlsx.LABEL_NOTE)
            )
            f.poser(
                ligneStats + 4, colonne,
                formule("IF(COUNT($plage)=0,\"\",COUNTIF($plage,\">=10\")/COUNT($plage))", s.tauxReussite, StylesXlsx.LABEL_POURCENT)
            )
        }
        blocs.forEachIndexed { bi, b -> poserStatistiques(b.colMoyenne, resultats[bi].stats) }
        poserStatistiques(colAnnuelle, statsAnnuelles)

        f.lignesFigees = 5
        f.colonnesFigees = 3

        return f to AdressesClasse(
            nomFeuille = nomFeuille,
            effectif = c.eleves.size,
            colonnesMoyenne = blocs.map { it.colMoyenne },
            colonneAnnuelle = colAnnuelle,
            ligneEffectif = ligneStats,
            statsPeriodes = resultats.map { it.stats },
            statsAnnuelle = statsAnnuelles
        )
    }

    private fun construireSynthese(export: ClasseurExport, adresses: List<AdressesClasse>): FeuilleXlsx {
        val f = FeuilleXlsx("Synthèse")
        val type = export.type
        val nbPeriodes = type.nombrePeriodes

        f.poser(0, 0, Cellule.Texte("Synthèse — ${export.matiere}", StylesXlsx.TITRE))
        val sousTitre = buildString {
            append("Professeur : ${export.professeur} — Année scolaire : ${export.anneeScolaire}")
            if (export.etablissement.isNotBlank()) append(" — ${export.etablissement}")
        }
        f.poser(1, 0, Cellule.Texte(sousTitre, StylesXlsx.DEFAUT))

        val titres = listOf("Moyenne de classe", "Plus forte", "Plus faible", "Taux de réussite")
        f.poser(3, 0, Cellule.Vide(StylesXlsx.ENTETE))
        f.poser(3, 1, Cellule.Vide(StylesXlsx.ENTETE))
        f.poser(4, 0, Cellule.Texte("Classe", StylesXlsx.ENTETE))
        f.poser(4, 1, Cellule.Texte("Effectif", StylesXlsx.ENTETE))
        f.largeur(0, 22.0)
        f.largeur(1, 10.0)
        for (bloc in 0..nbPeriodes) {
            val debut = 2 + bloc * 4
            val nom = if (bloc < nbPeriodes) nomPeriode(type, bloc + 1) else "Année"
            f.poser(3, debut, Cellule.Texte(nom, StylesXlsx.ENTETE))
            for (k in 1..3) f.poser(3, debut + k, Cellule.Vide(StylesXlsx.ENTETE))
            f.fusionner(3, debut, 3, debut + 3)
            titres.forEachIndexed { k, t ->
                f.poser(4, debut + k, Cellule.Texte(t, StylesXlsx.ENTETE))
                f.largeur(debut + k, 12.0)
            }
        }

        fun externe(a: AdressesClasse, col: Int, ligne: Int) =
            "'${a.nomFeuille.replace("'", "''")}'!${ref(col, ligne)}"

        adresses.forEachIndexed { i, a ->
            val r = 5 + i
            f.poser(r, 0, Cellule.Texte(a.nomFeuille))
            f.poser(r, 1, Cellule.Nombre(a.effectif.toDouble(), StylesXlsx.ENTIER))
            for (bloc in 0..nbPeriodes) {
                val debut = 2 + bloc * 4
                val colSource = if (bloc < nbPeriodes) a.colonnesMoyenne[bloc] else a.colonneAnnuelle
                val s = if (bloc < nbPeriodes) a.statsPeriodes[bloc] else a.statsAnnuelle
                f.poser(r, debut, formule(externe(a, colSource, a.ligneMoyenne), s.moyenneClasse, StylesXlsx.NOTE))
                f.poser(r, debut + 1, formule(externe(a, colSource, a.lignePlusForte), s.plusForte, StylesXlsx.NOTE))
                f.poser(r, debut + 2, formule(externe(a, colSource, a.lignePlusFaible), s.plusFaible, StylesXlsx.NOTE))
                f.poser(r, debut + 3, formule(externe(a, colSource, a.ligneTaux), s.tauxReussite, StylesXlsx.POURCENT))
            }
        }
        if (adresses.isEmpty()) {
            f.poser(5, 0, Cellule.Texte("Aucune classe"))
        }
        f.lignesFigees = 5
        f.colonnesFigees = 1
        return f
    }
}
