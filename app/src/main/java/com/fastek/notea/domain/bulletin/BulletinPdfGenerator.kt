package com.fastek.notea.domain.bulletin

import android.graphics.Canvas
import android.graphics.RectF
import android.graphics.Rect
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Génère le bulletin PDF d'une période avec l'API native [PdfDocument] (aucune dépendance
 * ajoutée). Mise en page calquée sur le bulletin officiel : bandeau titre, cadre d'identité,
 * tableau des disciplines, bilan (+ sanction, ou décision du conseil en fin d'année) et
 * cadre d'appréciation. Rang, moyenne de classe, plus forte/faible moyenne et appréciations
 * ne sont pas calculables (app mono-élève) : ils restent vides, à remplir à la main.
 */
object BulletinPdfGenerator {

    private const val LARGEUR_PAGE = 595
    private const val HAUTEUR_PAGE = 842
    private const val MARGE = 30f
    private const val LARGEUR = 535f
    private const val BAS_UTILISABLE = 790f
    private const val HAUTEUR_TITRES = 24f
    private const val HAUTEUR_LIGNE = 26f
    private const val MARINE = 0xFF1E2235.toInt()
    private const val BORDURE = 0xFF4B5563.toInt()
    private const val GRIS_CLAIR = 0xFFEEF0F5.toInt()
    private const val GRIS_TEXTE = 0xFF4B5563.toInt()
    private const val POINTS = "........"

    private val TITRES = listOf(
        "Disciplines", "Coef", "Moy. interro.", "Devoir 1", "Devoir 2",
        "Moy/20", "Moy Coef.", "Rang", "Appréciation"
    )
    private val LARGEURS = listOf(120f, 30f, 55f, 45f, 45f, 45f, 50f, 40f, 105f)

    fun generer(data: BulletinData, fichier: File) {
        val document = PdfDocument()
        val rendu = Rendu(document)
        try {
            rendu.nouvellePage()
            dessinerEntete(rendu, data)
            // Le bulletin tient sur une seule page : la hauteur des lignes s'adapte au nombre de
            // matières. La Conduite est toujours placée en dernier.
            val lignes = data.lignes.sortedBy { it.estConduite }
            val reserve = HAUTEUR_TITRES + 22f + 14f + hauteurBilan(data) + 142f
            val hauteurLigne = ((BAS_UTILISABLE - rendu.y - reserve) / lignes.size.coerceAtLeast(1))
                .coerceIn(12f, HAUTEUR_LIGNE)
            dessinerTitresTableau(rendu)
            lignes.forEach { ligne -> dessinerLigneMatiere(rendu, ligne, hauteurLigne) }
            dessinerTotal(rendu, data)
            rendu.y += 14f
            dessinerBilan(rendu, data)
            dessinerAppreciation(rendu)
            rendu.terminerPage()
            FileOutputStream(fichier).use { document.writeTo(it) }
        } finally {
            document.close()
        }
    }

    // ------------------------------------------------------------- en-tête

    private fun dessinerEntete(r: Rendu, d: BulletinData) {
        val c = r.canvas
        fond(c, MARGE, r.y, LARGEUR, 22f, MARINE)
        centre(c, "BULLETIN DE NOTES", MARGE, LARGEUR, r.y + 15f, peinture(11f, gras = true, couleur = Color.WHITE))
        r.y += 22f

        val base = r.y + 19f
        c.drawText("Établissement :", MARGE, base, peinture(9f, gras = true))
        ligneTiretee(c, MARGE + 82f, MARGE + LARGEUR, base + 1f)
        r.y += 30f

        val h = 26f
        val top = r.y
        val xMid = MARGE + 265f
        val xPhoto = xMid + 190f
        val libelle = if (d.libellePeriode == "Semestre") "Sem. :" else "Trim. :"

        cadre(c, MARGE, top, LARGEUR, 4 * h)
        trait(c, xMid, top, xMid, top + 4 * h)
        trait(c, xPhoto, top, xPhoto, top + 3 * h)
        for (i in 1..3) trait(c, MARGE, top + i * h, xMid, top + i * h)
        trait(c, xMid, top + h, xPhoto, top + h)
        trait(c, xMid, top + 2 * h, xPhoto, top + 2 * h)
        trait(c, xMid, top + 3 * h, MARGE + LARGEUR, top + 3 * h)

        fun ligneBase(ligne: Int) = top + ligne * h + 17f
        val xl = MARGE + 6f
        val xv = MARGE + 100f
        info(c, xl, xv, ligneBase(0), "Matricule :", d.matricule.ifBlank { POINTS }, 160f)
        info(c, xl, xv, ligneBase(1), "Nom :", d.nom.uppercase(Locale.FRANCE), 160f)
        info(c, xl, xv, ligneBase(2), "Prénoms :", d.prenom, 160f)
        info(c, xl, xv, ligneBase(3), "Date et lieu naiss. :", naissance(d), 160f)

        info(c, xMid + 6f, xMid + 88f, ligneBase(0), "Année scolaire :", d.anneeScolaire, 98f)
        info(c, xMid + 6f, xMid + 46f, ligneBase(1), "Classe :", d.classe, 60f)
        info(c, xMid + 112f, xMid + 140f, ligneBase(1), "Eff. :", d.effectif.ifBlank { POINTS }, 46f)
        info(c, xMid + 6f, xMid + 46f, ligneBase(2), libelle, d.numeroPeriode.toString(), 60f)
        info(c, xMid + 112f, xMid + 140f, ligneBase(2), "EPS :", d.aptitudeEps.ifBlank { POINTS }, 46f)

        val redoublant = d.redoublant.trim()
        ligneOuiNon(c, xMid + 6f, top + 3 * h + 11f, "Redoublant :", redoublant.equals("oui", true), redoublant.equals("non", true))
        ligneOuiNon(c, xMid + 6f, top + 3 * h + 23f, "Abandon :", false, false)

        val cadrePhoto = RectF(xPhoto + 8f, top + 6f, xPhoto + 8f + 64f, top + 6f + 3 * h - 12f)
        if (!dessinerPhoto(c, d.photo, cadrePhoto)) {
            cadre(c, cadrePhoto.left, cadrePhoto.top, cadrePhoto.width(), cadrePhoto.height(), GRIS_TEXTE, 0.6f)
            centre(c, "Photo", cadrePhoto.left, cadrePhoto.width(), top + 3 * h / 2f + 3f, peinture(7f, couleur = GRIS_TEXTE))
        }

        r.y = top + 4 * h + 12f
    }

    /** Dessine la photo recadrée au centre du cadre ; false si absente ou illisible. */
    private fun dessinerPhoto(c: Canvas, chemin: String, zone: RectF): Boolean {
        if (chemin.isBlank()) return false
        val bitmap = try {
            BitmapFactory.decodeFile(chemin)
        } catch (e: Exception) {
            null
        } ?: return false
        val ratioCadre = zone.width() / zone.height()
        val ratioImage = bitmap.width.toFloat() / bitmap.height
        val source = if (ratioImage > ratioCadre) {
            val largeur = (bitmap.height * ratioCadre).toInt()
            val x = (bitmap.width - largeur) / 2
            Rect(x, 0, x + largeur, bitmap.height)
        } else {
            val hauteur = (bitmap.width / ratioCadre).toInt()
            val y = (bitmap.height - hauteur) / 2
            Rect(0, y, bitmap.width, y + hauteur)
        }
        c.drawBitmap(bitmap, source, zone, Paint(Paint.FILTER_BITMAP_FLAG))
        cadre(c, zone.left, zone.top, zone.width(), zone.height(), GRIS_TEXTE, 0.6f)
        bitmap.recycle()
        return true
    }

    private fun naissance(d: BulletinData): String =
        if (d.dateNaissance.isBlank() && d.lieuNaissance.isBlank()) {
            POINTS
        } else {
            "${d.dateNaissance.ifBlank { "......" }} à ${d.lieuNaissance.ifBlank { "......" }}"
        }

    private fun info(c: Canvas, xLibelle: Float, xValeur: Float, base: Float, libelle: String, valeur: String, largeurMax: Float) {
        c.drawText(libelle, xLibelle, base, peinture(8f, gras = true, couleur = GRIS_TEXTE))
        val p = peinture(9.5f)
        c.drawText(tronquer(valeur, p, largeurMax), xValeur, base, p)
    }

    private fun ligneOuiNon(c: Canvas, x: Float, base: Float, libelle: String, oui: Boolean, non: Boolean) {
        c.drawText(libelle, x, base, peinture(8f, gras = true, couleur = GRIS_TEXTE))
        val p = peinture(8.5f)
        c.drawText("Oui", x + 84f, base, p)
        caseACocher(c, x + 102f, base, oui)
        c.drawText("Non", x + 124f, base, p)
        caseACocher(c, x + 142f, base, non)
    }

    private fun caseACocher(c: Canvas, x: Float, base: Float, cochee: Boolean) {
        cadre(c, x, base - 8f, 8f, 8f, BORDURE, 0.8f)
        if (cochee) {
            trait(c, x + 1.5f, base - 6.5f, x + 6.5f, base - 1.5f, Color.BLACK, 1.2f)
            trait(c, x + 6.5f, base - 6.5f, x + 1.5f, base - 1.5f, Color.BLACK, 1.2f)
        }
    }

    // ------------------------------------------------------------- tableau

    private fun dessinerTitresTableau(r: Rendu) {
        r.assurerPlace(HAUTEUR_TITRES)
        val c = r.canvas
        fond(c, MARGE, r.y, LARGEUR, HAUTEUR_TITRES, MARINE)
        val p = peinture(8f, gras = true, couleur = Color.WHITE)
        var x = MARGE
        TITRES.forEachIndexed { i, titre ->
            centre(c, titre, x, LARGEURS[i], r.y + 15f, p)
            x += LARGEURS[i]
        }
        r.y += HAUTEUR_TITRES
    }

    private fun hauteurBilan(d: BulletinData): Float {
        val gauche = if (d.finAnnee) d.moyennesPrecedentes.size + 4 else 3
        val droite = if (d.finAnnee) d.numeroPeriode + 2 else 2
        return 22f + maxOf(gauche, droite) * 18f + 12f + (if (d.finAnnee) 30f else 0f)
    }

    private fun dessinerLigneMatiere(r: Rendu, l: LigneBulletin, hauteur: Float) {
        val c = r.canvas
        val haut = r.y
        val bas = haut + hauteur
        cadre(c, MARGE, haut, LARGEUR, hauteur, BORDURE, 0.7f)

        val cellules = listOf(
            l.matiere.uppercase(Locale.FRANCE),
            l.coefficient.toString(),
            if (l.estConduite) "" else formatNote(l.moyenneInterro),
            if (l.estConduite) "" else formatNote(l.devoir1),
            if (l.estConduite) "" else formatNote(l.devoir2),
            format2(l.moyenne).ifEmpty { "—" },
            format2(l.moyenneCoef).ifEmpty { "—" },
            "",
            l.appreciation
        )
        val tailleTexte = minOf(9f, hauteur * 0.4f)
        val texte = peinture(tailleTexte)
        val gras = peinture(minOf(8.5f, hauteur * 0.38f), gras = true)
        val base = haut + hauteur / 2f + tailleTexte / 3f
        var x = MARGE
        cellules.forEachIndexed { i, contenu ->
            val largeur = LARGEURS[i]
            // Conduite : les colonnes interro / devoirs sont fusionnées (vides).
            val fusionnee = l.estConduite && (i == 3 || i == 4)
            if (i > 0 && !fusionnee) trait(c, x, haut, x, bas, BORDURE, 0.7f)
            if (i == 0) {
                c.drawText(tronquer(contenu, gras, largeur - 8f), x + 5f, base, gras)
            } else if (i == 8) {
                val petit = peinture(minOf(8f, hauteur * 0.36f))
                centre(c, tronquer(contenu, petit, largeur - 6f), x, largeur, base, petit)
            } else {
                centre(c, contenu, x, largeur, base, texte)
            }
            x += largeur
        }
        r.y = bas
    }

    private fun dessinerTotal(r: Rendu, data: BulletinData) {
        r.assurerPlace(22f)
        val c = r.canvas
        val haut = r.y
        fond(c, MARGE, haut, LARGEUR, 22f, GRIS_CLAIR)
        cadre(c, MARGE, haut, LARGEUR, 22f, BORDURE, 0.7f)
        val p = peinture(9f, gras = true)
        val base = haut + 15f
        c.drawText("TOTAL", MARGE + 5f, base, p)
        centre(c, data.totalCoefficients.toString(), MARGE + LARGEURS[0], LARGEURS[1], base, p)
        val xMoyCoef = MARGE + LARGEURS.take(6).sum()
        centre(c, format2(data.totalMoyCoef.takeIf { data.totalCoefficients > 0 }), xMoyCoef, LARGEURS[6], base, p)
        r.y = haut + 22f
    }

    // --------------------------------------------------------------- bilan

    private fun dessinerBilan(r: Rendu, d: BulletinData) {
        val libelle = d.libellePeriode
        val gauche = mutableListOf<Pair<String, String>>()
        if (d.finAnnee) {
            d.moyennesPrecedentes.forEachIndexed { i, m -> gauche += "Moy. $libelle ${i + 1}" to valeurOuPoints(m) }
        }
        gauche += "Moy. $libelle ${d.numeroPeriode}" to valeurOuPoints(d.moyenneGenerale)
        gauche += "Plus forte Moy." to POINTS
        gauche += "Plus faible Moy." to POINTS
        if (d.finAnnee) gauche += "Moy. Annuelle" to valeurOuPoints(d.moyenneAnnuelle)

        val droite = mutableListOf<Pair<String, String>>()
        repeat(if (d.finAnnee) d.numeroPeriode else 1) { droite += "Rang" to POINTS }
        droite += "Moy. Classe" to POINTS
        if (d.finAnnee) droite += "Rang Annuel" to POINTS

        val largeur = if (d.finAnnee) LARGEUR else 300f
        val hauteurCorps = maxOf(gauche.size, droite.size) * 18f + 12f
        r.assurerPlace(22f + hauteurCorps + (if (d.finAnnee) 30f else 0f))

        val c = r.canvas
        val haut = r.y
        fond(c, MARGE, haut, largeur, 22f, MARINE)
        centre(c, "BILAN", MARGE, largeur, haut + 15f, peinture(10.5f, gras = true, couleur = Color.WHITE))
        cadre(c, MARGE, haut + 22f, largeur, hauteurCorps)

        val libellePaint = peinture(8.5f, gras = true, couleur = GRIS_TEXTE)
        val valeurPaint = peinture(10f, gras = true)
        val xDroite = MARGE + largeur * 0.58f
        val xValeurDroite = xDroite + (if (d.finAnnee) 85f else 72f)
        gauche.forEachIndexed { i, (nom, valeur) ->
            val base = haut + 22f + 17f + i * 18f
            c.drawText("$nom :", MARGE + 10f, base, libellePaint)
            c.drawText(valeur, MARGE + 118f, base, valeurPaint)
            if (nom == "Moy. Annuelle" && d.moyenneAnnuelle != null) {
                c.drawText(d.formuleAnnuelle, MARGE + 160f, base, peinture(7f, couleur = GRIS_TEXTE))
            }
        }
        droite.forEachIndexed { i, (nom, valeur) ->
            val base = haut + 22f + 17f + i * 18f
            c.drawText("$nom :", xDroite, base, libellePaint)
            c.drawText(valeur, xValeurDroite, base, valeurPaint)
        }

        if (!d.finAnnee) {
            val xSanction = MARGE + 310f
            fond(c, xSanction, haut, 225f, 22f, MARINE)
            centre(c, "SANCTION", xSanction, 225f, haut + 15f, peinture(10.5f, gras = true, couleur = Color.WHITE))
            cadre(c, xSanction, haut + 22f, 225f, hauteurCorps)
        }
        r.y = haut + 22f + hauteurCorps

        if (d.finAnnee) {
            r.y += 8f
            val top = r.y
            fond(c, MARGE, top, LARGEUR, 22f, MARINE)
            c.drawText("DÉCISION DU CONSEIL", MARGE + 12f, top + 15f, peinture(10.5f, gras = true, couleur = Color.WHITE))
            val blanc = peinture(9f, gras = true, couleur = Color.WHITE)
            val xPasse = MARGE + LARGEUR - 175f
            c.drawText("Passe", xPasse, top + 15f, blanc)
            cadre(c, xPasse + 34f, top + 7f, 8f, 8f, Color.WHITE, 0.9f)
            c.drawText("Redouble", xPasse + 60f, top + 15f, blanc)
            cadre(c, xPasse + 108f, top + 7f, 8f, 8f, Color.WHITE, 0.9f)
            r.y = top + 22f
        }
    }

    private fun dessinerAppreciation(r: Rendu) {
        r.y += 12f
        r.assurerPlace(130f)
        val c = r.canvas
        val haut = r.y
        cadre(c, MARGE, haut, LARGEUR, 128f)
        c.drawText("Appréciation", MARGE + 12f, haut + 20f, peinture(11f, gras = true))
        ligneTiretee(c, MARGE + 12f, MARGE + 290f, haut + 44f)
        ligneTiretee(c, MARGE + 12f, MARGE + 290f, haut + 62f)

        val gris = peinture(8.5f, couleur = GRIS_TEXTE)
        centre(c, "Cachet et signature de l'établissement", MARGE + 300f, 225f, haut + 20f, gris)
        centre(c, "Le Directeur", MARGE + 300f, 225f, haut + 116f, peinture(9.5f, gras = true, italique = true))
        c.drawText("Signature de l'élève / du parent", MARGE + 12f, haut + 116f, gris)
        r.y = haut + 128f
    }

    // --------------------------------------------------------------- outils

    private fun peinture(taille: Float, gras: Boolean = false, italique: Boolean = false, couleur: Int = Color.BLACK) =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = taille
            color = couleur
            val style = when {
                gras && italique -> Typeface.BOLD_ITALIC
                gras -> Typeface.BOLD
                italique -> Typeface.ITALIC
                else -> Typeface.NORMAL
            }
            typeface = Typeface.create(Typeface.DEFAULT, style)
        }

    private fun fond(c: Canvas, x: Float, y: Float, largeur: Float, hauteur: Float, couleur: Int) {
        val p = Paint().apply { color = couleur; style = Paint.Style.FILL }
        c.drawRect(x, y, x + largeur, y + hauteur, p)
    }

    private fun cadre(c: Canvas, x: Float, y: Float, largeur: Float, hauteur: Float, couleur: Int = BORDURE, epaisseur: Float = 0.9f) {
        val p = Paint().apply { color = couleur; strokeWidth = epaisseur; style = Paint.Style.STROKE }
        c.drawRect(x, y, x + largeur, y + hauteur, p)
    }

    private fun trait(c: Canvas, x1: Float, y1: Float, x2: Float, y2: Float, couleur: Int = BORDURE, epaisseur: Float = 0.8f) {
        val p = Paint().apply { color = couleur; strokeWidth = epaisseur; style = Paint.Style.STROKE }
        c.drawLine(x1, y1, x2, y2, p)
    }

    private fun ligneTiretee(c: Canvas, x1: Float, x2: Float, y: Float) {
        val p = Paint().apply {
            color = GRIS_TEXTE
            strokeWidth = 0.7f
            style = Paint.Style.STROKE
            pathEffect = DashPathEffect(floatArrayOf(2f, 3f), 0f)
        }
        c.drawLine(x1, y, x2, y, p)
    }

    /** Texte centré dans une cellule de [largeur] commençant en [x], tronqué si trop long. */
    private fun centre(c: Canvas, texte: String, x: Float, largeur: Float, base: Float, p: Paint) {
        val affiche = tronquer(texte, p, largeur - 6f)
        c.drawText(affiche, x + (largeur - p.measureText(affiche)) / 2f, base, p)
    }

    private fun tronquer(texte: String, p: Paint, largeurMax: Float): String {
        if (p.measureText(texte) <= largeurMax) return texte
        val ellipse = "…"
        val place = largeurMax - p.measureText(ellipse)
        val n = p.breakText(texte, true, place, null)
        return texte.take(n).trimEnd() + ellipse
    }

    /** Note sans zéros inutiles (15 ; 13.75 ; 1.5), comme sur le bulletin officiel. */
    private fun formatNote(valeur: Double?): String =
        if (valeur == null) "" else String.format(Locale.US, "%.2f", valeur).trimEnd('0').trimEnd('.')

    /** Moyenne sur 2 décimales ; chaîne vide si absente. */
    private fun format2(valeur: Double?): String =
        if (valeur == null) "" else String.format(Locale.US, "%.2f", valeur)

    private fun valeurOuPoints(valeur: Double?): String = format2(valeur).ifEmpty { POINTS }

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
            canvas.drawText("Page $numero", MARGE + LARGEUR - 32f, HAUTEUR_PAGE - 24f, pied)
            document.finishPage(p)
            page = null
        }
    }
}
