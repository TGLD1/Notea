package com.fastek.notea.enseignant.export

import java.io.OutputStream
import java.math.BigDecimal
import java.util.TreeMap
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Contenu d'une cellule d'une feuille Excel. [style] est l'un des indices de [StylesXlsx]. */
sealed class Cellule {
    abstract val style: Int

    data class Texte(val valeur: String, override val style: Int = StylesXlsx.TEXTE) : Cellule()
    data class Nombre(val valeur: Double, override val style: Int = StylesXlsx.NOTE) : Cellule()
    data class Vide(override val style: Int = StylesXlsx.TEXTE) : Cellule()

    /**
     * Formule (sans le « = » initial). [resultat] est la valeur calculée par Notea (Double, String ou null) :
     * elle est écrite en cache pour que les aperçus qui ne recalculent pas affichent déjà les bons chiffres.
     */
    data class Formule(
        val formule: String,
        val resultat: Any? = null,
        override val style: Int = StylesXlsx.NOTE
    ) : Cellule()
}

/** Indices des styles définis dans xl/styles.xml. */
object StylesXlsx {
    const val DEFAUT = 0
    const val ENTETE = 1
    const val TEXTE = 2
    const val NOTE = 3
    const val ENTIER = 4
    const val POURCENT = 5
    const val LABEL = 6
    const val LABEL_NOTE = 7
    const val LABEL_POURCENT = 8
    const val TITRE = 9
    const val LABEL_ENTIER = 10
}

class FeuilleXlsx(val nom: String) {
    internal val lignes = TreeMap<Int, TreeMap<Int, Cellule>>()
    internal val largeurs = TreeMap<Int, Double>()
    internal val fusions = mutableListOf<String>()

    /** Nombre de lignes et de colonnes figées en haut et à gauche (0 = rien de figé). */
    var lignesFigees = 0
    var colonnesFigees = 0

    /** Pose une cellule ; ligne et colonne comptent à partir de 0. */
    fun poser(ligne: Int, colonne: Int, cellule: Cellule) {
        lignes.getOrPut(ligne) { TreeMap() }[colonne] = cellule
    }

    fun largeur(colonne: Int, largeur: Double) {
        largeurs[colonne] = largeur
    }

    fun fusionner(ligne1: Int, col1: Int, ligne2: Int, col2: Int) {
        fusions.add("${XlsxWriter.reference(col1, ligne1)}:${XlsxWriter.reference(col2, ligne2)}")
    }
}

/**
 * Écrit un classeur .xlsx (zip de fichiers XML) sans aucune bibliothèque : le format est simple
 * et l'application reste légère. Les textes sont en « inlineStr » (pas de table de chaînes partagées).
 */
object XlsxWriter {

    private const val NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val NS_PKG_REL = "http://schemas.openxmlformats.org/package/2006/relationships"
    private const val ENTETE_XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n"

    /** Lettres de colonne : 0 -> A, 25 -> Z, 26 -> AA. */
    fun lettres(colonne: Int): String {
        var n = colonne
        val sb = StringBuilder()
        do {
            sb.insert(0, ('A' + n % 26))
            n = n / 26 - 1
        } while (n >= 0)
        return sb.toString()
    }

    /** Référence A1 : colonne et ligne comptées à partir de 0. */
    fun reference(colonne: Int, ligne: Int, colonneFixe: Boolean = false, ligneFixe: Boolean = false): String =
        (if (colonneFixe) "$" else "") + lettres(colonne) + (if (ligneFixe) "$" else "") + (ligne + 1)

    /** Nom de feuille accepté par Excel : sans []:*?/\ , 31 caractères au plus, unique. */
    fun nomFeuilleValide(souhaite: String, dejaUtilises: Collection<String>): String {
        var base = souhaite.replace(Regex("[\\[\\]:*?/\\\\]"), " ").trim().trim('\'')
        if (base.isEmpty()) base = "Feuille"
        base = base.take(31)
        var candidat = base
        var i = 2
        while (dejaUtilises.any { it.equals(candidat, ignoreCase = true) }) {
            val suffixe = " ($i)"
            candidat = base.take(31 - suffixe.length) + suffixe
            i++
        }
        return candidat
    }

    fun ecrire(feuilles: List<FeuilleXlsx>, sortie: OutputStream) {
        require(feuilles.isNotEmpty()) { "Un classeur contient au moins une feuille." }
        ZipOutputStream(sortie).use { zip ->
            fun entree(chemin: String, contenu: String) {
                zip.putNextEntry(ZipEntry(chemin))
                zip.write(contenu.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            entree("[Content_Types].xml", contentTypes(feuilles.size))
            entree("_rels/.rels", relsRacine())
            entree("xl/workbook.xml", workbook(feuilles))
            entree("xl/_rels/workbook.xml.rels", relsClasseur(feuilles.size))
            entree("xl/styles.xml", styles())
            feuilles.forEachIndexed { i, f -> entree("xl/worksheets/sheet${i + 1}.xml", feuille(f)) }
        }
    }

    private fun contentTypes(nb: Int): String = buildString {
        append(ENTETE_XML)
        append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
        append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
        append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
        append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
        append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>")
        for (i in 1..nb) {
            append("<Override PartName=\"/xl/worksheets/sheet$i.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>")
        }
        append("</Types>")
    }

    private fun relsRacine(): String =
        ENTETE_XML +
            "<Relationships xmlns=\"$NS_PKG_REL\">" +
            "<Relationship Id=\"rId1\" Type=\"$NS_REL/officeDocument\" Target=\"xl/workbook.xml\"/>" +
            "</Relationships>"

    private fun workbook(feuilles: List<FeuilleXlsx>): String = buildString {
        append(ENTETE_XML)
        append("<workbook xmlns=\"$NS\" xmlns:r=\"$NS_REL\">")
        append("<bookViews><workbookView/></bookViews>")
        append("<sheets>")
        feuilles.forEachIndexed { i, f ->
            append("<sheet name=\"${echapper(f.nom)}\" sheetId=\"${i + 1}\" r:id=\"rId${i + 1}\"/>")
        }
        append("</sheets>")
        // Recalcul complet à l'ouverture : Excel et LibreOffice contrôlent ainsi les valeurs en cache.
        append("<calcPr calcId=\"191029\" fullCalcOnLoad=\"1\"/>")
        append("</workbook>")
    }

    private fun relsClasseur(nb: Int): String = buildString {
        append(ENTETE_XML)
        append("<Relationships xmlns=\"$NS_PKG_REL\">")
        for (i in 1..nb) {
            append("<Relationship Id=\"rId$i\" Type=\"$NS_REL/worksheet\" Target=\"worksheets/sheet$i.xml\"/>")
        }
        append("<Relationship Id=\"rId${nb + 1}\" Type=\"$NS_REL/styles\" Target=\"styles.xml\"/>")
        append("</Relationships>")
    }

    private fun styles(): String =
        ENTETE_XML +
            "<styleSheet xmlns=\"$NS\">" +
            // polices : 0 normale, 1 grasse, 2 grasse blanche, 3 grasse grande
            "<fonts count=\"4\">" +
            "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
            "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>" +
            "<font><b/><sz val=\"11\"/><color rgb=\"FFFFFFFF\"/><name val=\"Calibri\"/></font>" +
            "<font><b/><sz val=\"14\"/><name val=\"Calibri\"/></font>" +
            "</fonts>" +
            // remplissages : 0 aucun, 1 trame obligatoire, 2 violet FASTEK, 3 lavande
            "<fills count=\"4\">" +
            "<fill><patternFill patternType=\"none\"/></fill>" +
            "<fill><patternFill patternType=\"gray125\"/></fill>" +
            "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FF4C1D95\"/><bgColor indexed=\"64\"/></patternFill></fill>" +
            "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFEDE9FE\"/><bgColor indexed=\"64\"/></patternFill></fill>" +
            "</fills>" +
            "<borders count=\"2\">" +
            "<border><left/><right/><top/><bottom/><diagonal/></border>" +
            "<border><left style=\"thin\"><color rgb=\"FFB8B0D0\"/></left><right style=\"thin\"><color rgb=\"FFB8B0D0\"/></right>" +
            "<top style=\"thin\"><color rgb=\"FFB8B0D0\"/></top><bottom style=\"thin\"><color rgb=\"FFB8B0D0\"/></bottom><diagonal/></border>" +
            "</borders>" +
            "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>" +
            "<cellXfs count=\"11\">" +
            "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>" +                                   // 0 défaut
            "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\" vertical=\"center\" wrapText=\"1\"/></xf>" + // 1 en-tête
            "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\"/>" +                  // 2 texte
            "<xf numFmtId=\"2\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>" + // 3 note 0,00
            "<xf numFmtId=\"1\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>" + // 4 entier
            "<xf numFmtId=\"9\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>" + // 5 pourcentage
            "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\"/>" + // 6 libellé
            "<xf numFmtId=\"2\" fontId=\"1\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>" + // 7 libellé 0,00
            "<xf numFmtId=\"9\" fontId=\"1\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>" + // 8 libellé %
            "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>" +                    // 9 titre
            "<xf numFmtId=\"1\" fontId=\"1\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyNumberFormat=\"1\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>" + // 10 libellé entier
            "</cellXfs>" +
            "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>" +
            "</styleSheet>"

    private fun feuille(f: FeuilleXlsx): String = buildString {
        append(ENTETE_XML)
        append("<worksheet xmlns=\"$NS\">")
        append("<sheetViews><sheetView workbookViewId=\"0\">")
        if (f.lignesFigees > 0 || f.colonnesFigees > 0) {
            val haut = reference(f.colonnesFigees, f.lignesFigees)
            val volet = when {
                f.lignesFigees > 0 && f.colonnesFigees > 0 -> "bottomRight"
                f.lignesFigees > 0 -> "bottomLeft"
                else -> "topRight"
            }
            append("<pane")
            if (f.colonnesFigees > 0) append(" xSplit=\"${f.colonnesFigees}\"")
            if (f.lignesFigees > 0) append(" ySplit=\"${f.lignesFigees}\"")
            append(" topLeftCell=\"$haut\" activePane=\"$volet\" state=\"frozen\"/>")
        }
        append("</sheetView></sheetViews>")
        append("<sheetFormatPr defaultRowHeight=\"15\"/>")
        if (f.largeurs.isNotEmpty()) {
            append("<cols>")
            f.largeurs.forEach { (col, l) ->
                append("<col min=\"${col + 1}\" max=\"${col + 1}\" width=\"${BigDecimal.valueOf(l).toPlainString()}\" customWidth=\"1\"/>")
            }
            append("</cols>")
        }
        append("<sheetData>")
        f.lignes.forEach { (ligne, cellules) ->
            append("<row r=\"${ligne + 1}\">")
            cellules.forEach { (col, c) -> append(cellule(reference(col, ligne), c)) }
            append("</row>")
        }
        append("</sheetData>")
        if (f.fusions.isNotEmpty()) {
            append("<mergeCells count=\"${f.fusions.size}\">")
            f.fusions.forEach { append("<mergeCell ref=\"$it\"/>") }
            append("</mergeCells>")
        }
        append("</worksheet>")
    }

    private fun cellule(ref: String, c: Cellule): String = when (c) {
        is Cellule.Texte ->
            "<c r=\"$ref\" s=\"${c.style}\" t=\"inlineStr\"><is><t xml:space=\"preserve\">${echapper(c.valeur)}</t></is></c>"
        is Cellule.Nombre ->
            "<c r=\"$ref\" s=\"${c.style}\"><v>${nombre(c.valeur)}</v></c>"
        is Cellule.Vide ->
            "<c r=\"$ref\" s=\"${c.style}\"/>"
        is Cellule.Formule -> when (val r = c.resultat) {
            is Double -> "<c r=\"$ref\" s=\"${c.style}\"><f>${echapper(c.formule)}</f><v>${nombre(r)}</v></c>"
            is Int -> "<c r=\"$ref\" s=\"${c.style}\"><f>${echapper(c.formule)}</f><v>$r</v></c>"
            is String -> "<c r=\"$ref\" s=\"${c.style}\" t=\"str\"><f>${echapper(c.formule)}</f><v>${echapper(r)}</v></c>"
            else -> "<c r=\"$ref\" s=\"${c.style}\" t=\"str\"><f>${echapper(c.formule)}</f><v></v></c>"
        }
    }

    /** Écriture décimale simple (point), jamais en notation scientifique. */
    private fun nombre(v: Double): String = BigDecimal.valueOf(v).toPlainString()

    fun echapper(texte: String): String = buildString(texte.length + 8) {
        for (ch in texte) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> if (ch >= ' ' || ch == '\t' || ch == '\n' || ch == '\r') append(ch)
            }
        }
    }
}
