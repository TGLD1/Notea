package com.fastek.notea.enseignant.importation

import com.fastek.notea.enseignant.domain.EleveLu
import com.fastek.notea.enseignant.domain.ListeElevesParser
import com.fastek.notea.enseignant.export.Cellule
import com.fastek.notea.enseignant.export.FeuilleXlsx
import com.fastek.notea.enseignant.export.XlsxWriter
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class LecteurXlsxTest {

    @Test
    fun `un classeur ecrit par Notea est relu`() {
        val feuille = FeuilleXlsx("Liste")
        feuille.poser(0, 0, Cellule.Texte("N°"))
        feuille.poser(0, 1, Cellule.Texte("Nom"))
        feuille.poser(0, 2, Cellule.Texte("Prénoms"))
        feuille.poser(1, 0, Cellule.Nombre(1.0))
        feuille.poser(1, 1, Cellule.Texte("DOSSOU"))
        feuille.poser(1, 2, Cellule.Texte("Jean-Marc"))
        feuille.poser(2, 0, Cellule.Nombre(2.0))
        feuille.poser(2, 1, Cellule.Texte("AGBO & fils <test>"))
        feuille.poser(2, 2, Cellule.Texte("Marie"))
        val sortie = ByteArrayOutputStream()
        XlsxWriter.ecrire(listOf(feuille), sortie)

        val lignes = LecteurXlsx.lireLignes(ByteArrayInputStream(sortie.toByteArray()))
        assertEquals(listOf("N°", "Nom", "Prénoms"), lignes[0])
        assertEquals(listOf("1", "DOSSOU", "Jean-Marc"), lignes[1])
        assertEquals(listOf("2", "AGBO & fils <test>", "Marie"), lignes[2])

        val eleves = ListeElevesParser.analyser(lignes.joinToString("\n") { it.joinToString(";") })
        assertEquals(listOf(EleveLu("DOSSOU", "Jean-Marc"), EleveLu("AGBO & fils <test>", "Marie")), eleves)
    }

    /** Un classeur construit comme Excel : textes partagés (t="s"), cellules vides, deuxième feuille ignorée. */
    @Test
    fun `un classeur au format Excel avec textes partages est lu`() {
        val sortie = ByteArrayOutputStream()
        ZipOutputStream(sortie).use { zip ->
            fun entree(nom: String, contenu: String) {
                zip.putNextEntry(ZipEntry(nom))
                zip.write(contenu.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
            val ns = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
            val rel = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            entree(
                "xl/workbook.xml",
                "<workbook xmlns=\"$ns\" xmlns:r=\"$rel\"><sheets>" +
                    "<sheet name=\"Classe\" sheetId=\"1\" r:id=\"rId2\"/><sheet name=\"Autre\" sheetId=\"2\" r:id=\"rId3\"/></sheets></workbook>"
            )
            entree(
                "xl/_rels/workbook.xml.rels",
                "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId2\" Type=\"$rel/worksheet\" Target=\"worksheets/sheet2.xml\"/>" +
                    "<Relationship Id=\"rId3\" Type=\"$rel/worksheet\" Target=\"worksheets/sheet1.xml\"/></Relationships>"
            )
            entree(
                "xl/sharedStrings.xml",
                "<sst xmlns=\"$ns\"><si><t>Nom</t></si><si><t>Prénoms</t></si><si><t>DOSSOU</t></si>" +
                    "<si><r><t>Jean</t></r><r><t>-Marc</t></r></si></sst>"
            )
            entree(
                "xl/worksheets/sheet2.xml",
                "<worksheet xmlns=\"$ns\"><sheetData>" +
                    "<row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"B1\" t=\"s\"><v>1</v></c></row>" +
                    "<row r=\"2\"><c r=\"A2\" t=\"s\"><v>2</v></c><c r=\"C2\" t=\"s\"><v>3</v></c><c r=\"D2\"><v>12.0</v></c></row>" +
                    "</sheetData></worksheet>"
            )
            entree("xl/worksheets/sheet1.xml", "<worksheet xmlns=\"$ns\"><sheetData><row r=\"1\"><c r=\"A1\" t=\"inlineStr\"><is><t>NE PAS LIRE</t></is></c></row></sheetData></worksheet>")
        }
        val lignes = LecteurXlsx.lireLignes(ByteArrayInputStream(sortie.toByteArray()))
        assertEquals(listOf("Nom", "Prénoms"), lignes[0])
        assertEquals(listOf("DOSSOU", "", "Jean-Marc", "12"), lignes[1])
    }

    @Test
    fun `index de colonne`() {
        assertEquals(0, LecteurXlsx.indexColonne("A1"))
        assertEquals(2, LecteurXlsx.indexColonne("C7"))
        assertEquals(26, LecteurXlsx.indexColonne("AA3"))
    }
}
