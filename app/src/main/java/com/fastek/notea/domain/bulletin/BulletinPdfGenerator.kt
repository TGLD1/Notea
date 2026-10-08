package com.fastek.notea.domain.bulletin

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Génère le bulletin PDF d'une période avec l'API native [PdfDocument] (aucune dépendance
 * ajoutée, donc aucun poids en plus). Format A4 portrait.
 *
 * Colonnes inspirées du bulletin officiel : Disciplines, Coef, Moy. interro., Devoir 1,
 * Devoir 2, Moy/20, Moy Coef, Rang, Appréciation. Rang, appréciation, moyenne/rang de classe
 * ne sont pas calculables (app mono-élève) : ils restent vides, à remplir à la main.
 */
object BulletinPdfGenerator {

    private const val LARGEUR_PAGE = 595
    private const val HAUTEUR_PAGE = 842
    private const val MARGE = 36f
    private const val HAUTEUR_LIGNE = 22f
    private const val BAS_UTILISABLE = 780f
    private const val BLEU = 0xFF1565C0.toInt()
    private const val GRIS_CLAIR = 0xFFF1F3F6.toInt()
    private const val GRIS_TEXTE = 0xFF6B7280.toInt()
    private const val POINTILLES = "........................"

    private val TITRES = listOf(
        "Disciplines", "Coef", "M. interro", "Devoir 1", "Devoir 2",
        "Moy/20", "Moy Coef", "Rang", "Appréciation"
    )
    private val LARGEURS = listOf(135f, 30f, 50f, 45f, 45f, 45f, 50f, 35f, 88f)
    private val LARGEUR_TABLEAU = LARGEURS.sum()

    fun generer(data: BulletinData, fichier: File) {
        val document = PdfDocument()
        val rendu = Rendu(document)
        try {
            rendu.nouvellePage()
            dessinerEntete(rendu, data)
            dessinerTitresTableau(rendu)
            data.lignes.forEachIndexed { index, ligne ->
                rendu.assurerPlace(HAUTEUR_LIGNE) { dessinerTitresTableau(rendu) }
                dessinerLigneMatiere(rendu, ligne, index % 2 == 1)
            }
            dessinerLigne(
                rendu,
                listOf(
                    "TOTAL", data.totalCoefficients.toString(), "", "", "",
                    "", format(data.totalMoyCoef.takeIf { data.totalCoefficients > 0 }), "", ""
                ),
                gras = true, fond = GRIS_CLAIR
            )
            rendu.y += 16f
            dessinerResultats(rendu, data)
            dessinerSignatures(rendu)
            rendu.terminerPage()
            FileOutputStream(fichier).use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }

    // ---------------------------------------------------------------- dessin

    private fun dessinerEntete(r: Rendu, data: BulletinData) {
        val titre = peinture(18f, gras = true, couleur = BLEU)
        r.canvas.drawText("BULLETIN DE NOTES", MARGE, r.y + 16f, titre)
        val sousTitre = peinture(11f, couleur = GRIS_TEXTE)
        r.canvas.drawText(
            "${data.libellePeriode} ${data.numeroPeriode} — Année scolaire ${data.anneeScolaire}",
            MARGE, r.y + 34f, sousTitre
        )
        r.y += 46f
        trait(r, BLEU, 1.5f)
        r.y += 14f

        val moitie = MARGE + LARGEUR_TABLEAU / 2f + 10f
        ligneInfo(r, MARGE, "Nom", data.nom.uppercase(Locale.FRANCE))
        ligneInfo(r, moitie, "Classe", data.classe)
        r.y += 16f
        ligneInfo(r, MARGE, "Prénoms", data.prenom)
        ligneInfo(r, moitie, "Matricule", data.matricule.ifBlank { POINTILLES })
        r.y += 16f
        ligneInfo(r, MARGE, "Né(e) le / à", naissance(data))
        ligneInfo(r, moitie, "Effectif", data.effectif.ifBlank { "........" })
        r.y += 16f
        ligneInfo(r, MARGE, "Aptitude EPS", data.aptitudeEps.ifBlank { "........" })
        ligneInfo(r, moitie, "Redoublant / Abandon", "${data.redoublant.ifBlank { "......" }} / ......")
        r.y += 24f
    }

    private fun naissance(d: BulletinData): String =
        if (d.dateNaissance.isBlank() && d.lieuNaissance.isBlank()) {
            POINTILLES
        } else {
            "${d.dateNaissance.ifBlank { "......" }} à ${d.lieuNaissance.ifBlank { "......" }}"
        }

    private fun ligneInfo(r: Rendu, x: Float, libelle: String, valeur: String) {
        val p = peinture(10f, couleur = GRIS_TEXTE)
        r.canvas.drawText("$libelle :", x, r.y, p)
        val decalage = p.measureText("$libelle : ") + 2f
        r.canvas.drawText(valeur, x + decalage, r.y, peinture(10.5f, gras = true))
    }

    private fun trait(r: Rendu, couleur: Int, epaisseur: Float) {
        val p = Paint().apply {
            color = couleur
            strokeWidth = epaisseur
            style = Paint.Style.STROKE
        }
        r.canvas.drawLine(MARGE, r.y, MARGE + LARGEUR_TABLEAU, r.y, p)
    }

    private fun dessinerTitresTableau(r: Rendu) {
        dessinerLigne(r, TITRES, gras = true, fond = BLEU, couleurTexte = Color.WHITE, taille = 8.5f)
    }

    private fun dessinerLigneMatiere(r: Rendu, l: LigneBulletin, alterner: Boolean) {
        val cellules = listOf(
            l.matiere,
            l.coefficient.toString(),
            if (l.estConduite) "" else format(l.moyenneInterro),
            if (l.estConduite) "" else format(l.devoir1),
            if (l.estConduite) "" else format(l.devoir2),
            format(l.moyenne).ifEmpty { "—" },
            format(l.moyenneCoef).ifEmpty { "—" },
            "",
            ""
        )
        dessinerLigne(r, cellules, gras = false, fond = if (alterner) GRIS_CLAIR else null)
    }

    /** Dessine une ligne de tableau : première colonne alignée à gauche, les autres centrées. */
    private fun dessinerLigne(
        r: Rendu,
        cellules: List<String>,
        gras: Boolean,
        fond: Int?,
        couleurTexte: Int = Color.BLACK,
        taille: Float = 9f
    ) {
        r.assurerPlace(HAUTEUR_LIGNE)
        val haut = r.y
        val bas = haut + HAUTEUR_LIGNE
        if (fond != null) {
            val pf = Paint().apply { color = fond; style = Paint.Style.FILL }
            r.canvas.drawRect(MARGE, haut, MARGE + LARGEUR_TABLEAU, bas, pf)
        }
        val bordure = Paint().apply {
            color = 0xFF9CA3AF.toInt()
            strokeWidth = 0.6f
            style = Paint.Style.STROKE
        }
        r.canvas.drawRect(MARGE, haut, MARGE + LARGEUR_TABLEAU, bas, bordure)

        val texte = peinture(taille, gras = gras, couleur = couleurTexte)
        var x = MARGE
        cellules.forEachIndexed { i, contenu ->
            val largeur = LARGEURS[i]
            if (i > 0) r.canvas.drawLine(x, haut, x, bas, bordure)
            val affiche = tronquer(contenu, texte, largeur - 8f)
            val base = haut + HAUTEUR_LIGNE / 2f + taille / 3f
            if (i == 0) {
                r.canvas.drawText(affiche, x + 4f, base, texte)
            } else {
                r.canvas.drawText(affiche, x + (largeur - texte.measureText(affiche)) / 2f, base, texte)
            }
            x += largeur
        }
        r.y = bas
    }

    private fun dessinerResultats(r: Rendu, data: BulletinData) {
        r.assurerPlace(120f)
        val gros = peinture(13f, gras = true, couleur = BLEU)
        val normal = peinture(10.5f)
        val moyenne = format(data.moyenneGenerale).ifEmpty { "—" }
        r.canvas.drawText(
            "Moyenne générale du ${data.libellePeriode.lowercase(Locale.FRANCE)} ${data.numeroPeriode} : $moyenne / 20",
            MARGE, r.y + 12f, gros
        )
        r.y += 28f

        if (data.moyenneAnnuelle != null) {
            val s1 = format(data.moyennePeriode1)
            r.canvas.drawText("Moyenne du semestre 1 : $s1 / 20", MARGE, r.y, normal)
            r.y += 16f
            r.canvas.drawText(
                "Moyenne annuelle : ${format(data.moyenneAnnuelle)} / 20  (2 × S2 + S1) ÷ 3",
                MARGE, r.y, peinture(11f, gras = true)
            )
            r.y += 20f
        }

        r.canvas.drawText("Moyenne de la classe : $POINTILLES", MARGE, r.y, normal)
        r.canvas.drawText("Rang : ........ / ........", MARGE + 300f, r.y, normal)
        r.y += 16f
        r.canvas.drawText("Plus forte moyenne : ........", MARGE, r.y, normal)
        r.canvas.drawText("Plus faible moyenne : ........", MARGE + 300f, r.y, normal)
        r.y += 22f

        if (data.finAnnee) {
            r.canvas.drawText("Décision du conseil (Passe / Redouble) : $POINTILLES", MARGE, r.y, normal)
            r.y += 18f
            r.canvas.drawText("Appréciation générale : $POINTILLES$POINTILLES", MARGE, r.y, normal)
            r.y += 22f
        }
    }

    private fun dessinerSignatures(r: Rendu) {
        r.assurerPlace(70f)
        r.y += 10f
        val p = peinture(10f, couleur = GRIS_TEXTE)
        r.canvas.drawText("Signature de l'élève / du parent", MARGE, r.y, p)
        val droite = "Cachet et signature de l'établissement"
        r.canvas.drawText(droite, MARGE + LARGEUR_TABLEAU - p.measureText(droite), r.y, p)
        r.y += 50f
    }

    // --------------------------------------------------------------- outils

    private fun peinture(taille: Float, gras: Boolean = false, couleur: Int = Color.BLACK) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = taille
            color = couleur
            typeface = if (gras) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        }

    /** Raccourcit le texte avec « … » s'il dépasse la largeur disponible. */
    private fun tronquer(texte: String, p: Paint, largeurMax: Float): String {
        if (p.measureText(texte) <= largeurMax) return texte
        val ellipse = "…"
        val place = largeurMax - p.measureText(ellipse)
        val n = p.breakText(texte, true, place, null)
        return texte.take(n).trimEnd() + ellipse
    }

    /** Nombre au format français, 2 décimales (virgule) ; chaîne vide si absent. */
    private fun format(valeur: Double?): String =
        if (valeur == null) "" else String.format(Locale.FRANCE, "%.2f", valeur)

    /** État de dessin : document, page courante, position verticale, numérotation. */
    private class Rendu(private val document: PdfDocument) {
        private var page: PdfDocument.Page? = null
        private var numero = 0
        lateinit var canvas: Canvas
        var y = MARGE

        fun nouvellePage() {
            terminerPage()
            numero++
            val info = PdfDocument.PageInfo.Builder(LARGEUR_PAGE, HAUTEUR_PAGE, numero).create()
            val p = document.startPage(info)
            page = p
            canvas = p.canvas
            y = MARGE
        }

        /** Ajoute une page si la hauteur demandée ne tient pas ; [apres] redessine les titres. */
        fun assurerPlace(hauteur: Float, apres: (() -> Unit)? = null) {
            if (y + hauteur > BAS_UTILISABLE) {
                nouvellePage()
                apres?.invoke()
            }
        }

        fun terminerPage() {
            val p = page ?: return
            val pied = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 7.5f
                color = GRIS_TEXTE
            }
            canvas.drawText(
                "Généré par Notea (FASTEK) — calculs indicatifs ; se référer au bulletin officiel de l'établissement.",
                MARGE, HAUTEUR_PAGE - 24f, pied
            )
            canvas.drawText("Page $numero", MARGE + LARGEUR_TABLEAU - 32f, HAUTEUR_PAGE - 24f, pied)
            document.finishPage(p)
            page = null
        }
    }
}
