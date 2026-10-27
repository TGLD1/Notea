package com.fastek.notea.enseignant.export

import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.data.local.entity.TypeNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

class ClasseurEnseignantTest {

    private fun exemple(type: TypeEtablissement = TypeEtablissement.PUBLIC): ClasseurExport {
        val interro1 = EvaluationExport(1, TypeNote.INTERROGATION, "")
        val interro2 = EvaluationExport(2, TypeNote.INTERROGATION, "Interro <1> & co")
        val devoir1 = EvaluationExport(3, TypeNote.DEVOIR, "")
        val eleves = listOf(EleveExport(10, "AGBO", "Marie"), EleveExport(11, "DOSSOU", "Jean"))
        return ClasseurExport(
            professeur = "Paul Houngbo",
            matiere = "PCT",
            type = type,
            anneeScolaire = "2026-2027",
            etablissement = "CEG Pahou",
            classes = listOf(
                ClasseExport(
                    nom = "3ème A",
                    coefficient = 2,
                    eleves = eleves,
                    periodes = listOf(PeriodeExport(1, listOf(interro1, interro2, devoir1))),
                    notes = mapOf(
                        1L to mapOf(10L to 10.0, 11L to 16.0),
                        2L to mapOf(10L to 14.0),
                        3L to mapOf(10L to 8.0, 11L to 12.0)
                    )
                ),
                ClasseExport("3ème A", 1, emptyList(), emptyList(), emptyMap())
            )
        )
    }

    private fun entrees(octets: ByteArray): Map<String, String> {
        val resultat = LinkedHashMap<String, String>()
        ZipInputStream(ByteArrayInputStream(octets)).use { zip ->
            var e = zip.nextEntry
            while (e != null) {
                resultat[e.name] = String(zip.readBytes(), Charsets.UTF_8)
                e = zip.nextEntry
            }
        }
        return resultat
    }

    @Test
    fun `le classeur contient les fichiers obligatoires et du XML bien forme`() {
        val sortie = ByteArrayOutputStream()
        ClasseurEnseignant.ecrire(exemple(), sortie)
        val fichiers = entrees(sortie.toByteArray())

        val attendus = listOf(
            "[Content_Types].xml", "_rels/.rels", "xl/workbook.xml", "xl/_rels/workbook.xml.rels",
            "xl/styles.xml", "xl/worksheets/sheet1.xml", "xl/worksheets/sheet2.xml", "xl/worksheets/sheet3.xml"
        )
        assertEquals(attendus, fichiers.keys.toList())

        val fabrique = DocumentBuilderFactory.newInstance()
        fichiers.forEach { (nom, contenu) ->
            // Lève une exception si le XML est mal formé.
            fabrique.newDocumentBuilder().parse(ByteArrayInputStream(contenu.toByteArray(Charsets.UTF_8)))
            assertTrue("$nom doit commencer par la déclaration XML", contenu.startsWith("<?xml"))
        }
    }

    @Test
    fun `feuilles Synthese puis classes avec noms uniques`() {
        val feuilles = ClasseurEnseignant.construire(exemple())
        assertEquals(listOf("Synthèse", "3ème A", "3ème A (2)"), feuilles.map { it.nom })
    }

    @Test
    fun `les formules et les valeurs en cache reproduisent le moteur de Notea`() {
        val feuilles = ClasseurEnseignant.construire(exemple())
        val classe = feuilles[1]

        // Disposition : N°, Nom, Prénoms, puis D:G interros, H moy. interros, I:J devoirs, K moy., L moy. × coef., M rang.
        fun cellule(ligne: Int, colonne: Int) = classe.lignes.getValue(ligne).getValue(colonne) as Cellule.Formule

        // Marie (ligne 6) : interros 10 et 14 -> 12 ; devoir 8 -> (12 + 8) / 2 = 10
        assertEquals("IF(COUNT(D6:G6)=0,\"\",AVERAGE(D6:G6))", cellule(5, 7).formule)
        assertEquals(12.0, cellule(5, 7).resultat as Double, 0.0001)
        assertEquals("IF(COUNT(H6:J6)=0,\"\",ROUND(SUM(H6:J6)/COUNT(H6:J6),2))", cellule(5, 10).formule)
        assertEquals(10.0, cellule(5, 10).resultat as Double, 0.0001)
        assertEquals(20.0, cellule(5, 11).resultat as Double, 0.0001) // coefficient 2
        assertEquals(2, cellule(5, 12).resultat)

        // Jean (ligne 7) : une seule interro (16), devoir 12 -> (16 + 12) / 2 = 14
        assertEquals(14.0, cellule(6, 10).resultat as Double, 0.0001)
        assertEquals(1, cellule(6, 12).resultat)
        assertTrue(cellule(6, 12).formule.contains("RANK(K7,\$K\$6:\$K\$7,0)"))
    }

    @Test
    fun `les textes sont echappes et les notes ecrites en nombres`() {
        val sortie = ByteArrayOutputStream()
        ClasseurEnseignant.ecrire(exemple(), sortie)
        val feuille = entrees(sortie.toByteArray()).getValue("xl/worksheets/sheet2.xml")
        assertTrue(feuille.contains("Interro &lt;1&gt; &amp; co"))
        assertTrue(feuille.contains("<v>14.0</v>"))
        assertTrue(feuille.contains("COUNTIF("))
    }

    @Test
    fun `en prive la moyenne annuelle utilise les trois trimestres`() {
        val feuilles = ClasseurEnseignant.construire(exemple(TypeEtablissement.PRIVE))
        val classe = feuilles[1]
        val formules = classe.lignes.values.flatMap { it.values }
            .filterIsInstance<Cellule.Formule>().map { it.formule }
        assertTrue(formules.any { it.contains("/3,2)") && it.contains("=3") })
    }

    @Test
    fun `lettres de colonne et references`() {
        assertEquals("A", XlsxWriter.lettres(0))
        assertEquals("Z", XlsxWriter.lettres(25))
        assertEquals("AA", XlsxWriter.lettres(26))
        assertEquals("AB", XlsxWriter.lettres(27))
        assertEquals("C4", XlsxWriter.reference(2, 3))
        assertEquals("\$C\$4", XlsxWriter.reference(2, 3, true, true))
    }

    @Test
    fun `noms de feuille valides`() {
        assertEquals("3ème A B", XlsxWriter.nomFeuilleValide("3ème A/B", emptyList()))
        assertEquals("Feuille", XlsxWriter.nomFeuilleValide("   ", emptyList()))
        assertEquals(31, XlsxWriter.nomFeuilleValide("x".repeat(50), emptyList()).length)
    }
}
