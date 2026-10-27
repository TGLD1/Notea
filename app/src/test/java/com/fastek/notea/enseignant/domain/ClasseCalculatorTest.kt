package com.fastek.notea.enseignant.domain

import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.data.local.entity.TypeNote
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClasseCalculatorTest {

    private fun interro(v: Double) = NoteSaisie(TypeNote.INTERROGATION, v)
    private fun devoir(v: Double) = NoteSaisie(TypeNote.DEVOIR, v)

    @Test
    fun `moyenne = (moyenne des interros + devoirs) divisee par 1 + nombre de devoirs`() {
        // interros 4,3,3,1 -> 2,75 ; devoirs 3 et 4 -> (2,75 + 3 + 4) / 3 = 3,25 (exemple validé de Notea)
        val notes = listOf(interro(4.0), interro(3.0), interro(3.0), interro(1.0), devoir(3.0), devoir(4.0))
        assertEquals(3.25, ClasseCalculator.moyenneEleve(notes)!!, 0.0001)
        assertEquals(2.75, ClasseCalculator.moyenneInterro(notes)!!, 0.0001)
    }

    @Test
    fun `un devoir absent ne penalise pas l'eleve`() {
        val notes = listOf(interro(10.0), interro(14.0), devoir(8.0))
        // M_intero = 12 ; (12 + 8) / 2 = 10
        assertEquals(10.0, ClasseCalculator.moyenneEleve(notes)!!, 0.0001)
    }

    @Test
    fun `interros seules et devoirs seuls`() {
        assertEquals(12.0, ClasseCalculator.moyenneEleve(listOf(interro(10.0), interro(14.0)))!!, 0.0001)
        assertEquals(12.0, ClasseCalculator.moyenneEleve(listOf(devoir(8.0), devoir(16.0)))!!, 0.0001)
        assertNull(ClasseCalculator.moyenneEleve(emptyList()))
    }

    @Test
    fun `la moyenne est arrondie a 2 decimales`() {
        // (10 + 11 + 12) / 3 = 11 ; 3 devoirs impossibles, on vérifie l'arrondi avec 1/3
        val notes = listOf(interro(10.0), devoir(10.0), devoir(11.0)) // (10+10+11)/3 = 10,3333
        assertEquals(10.33, ClasseCalculator.moyenneEleve(notes)!!, 0.0001)
    }

    @Test
    fun `rangs a la francaise avec ex aequo`() {
        val rangs = ClasseCalculator.rangs(listOf(15.0, 12.0, 15.0, null, 9.0))
        assertEquals(listOf(1, 3, 1, null, 4), rangs)
    }

    @Test
    fun `statistiques de classe`() {
        val s = ClasseCalculator.statistiques(listOf(4.0, 9.99, 10.0, 12.5, 15.0, 20.0, null))
        assertEquals(6, s.effectifNote)
        assertEquals(11.92, s.moyenneClasse!!, 0.0001)
        assertEquals(20.0, s.plusForte!!, 0.0001)
        assertEquals(4.0, s.plusFaible!!, 0.0001)
        assertEquals(4.0 / 6.0, s.tauxReussite!!, 0.0001)
        assertEquals(listOf(1, 1, 2, 2), s.repartition)
    }

    @Test
    fun `statistiques d'une classe sans note`() {
        val s = ClasseCalculator.statistiques(listOf(null, null))
        assertEquals(0, s.effectifNote)
        assertNull(s.moyenneClasse)
        assertNull(s.tauxReussite)
    }

    @Test
    fun `calculerPeriode associe rang et moyenne a chaque eleve`() {
        val resultat = ClasseCalculator.calculerPeriode(
            listOf(
                EleveNotes(1, listOf(interro(8.0), devoir(10.0))),   // 9
                EleveNotes(2, listOf(interro(16.0), devoir(14.0))),  // 15
                EleveNotes(3, emptyList())
            )
        )
        assertEquals(listOf(2, 1, null), resultat.lignes.map { it.rang })
        assertEquals(9.0, resultat.lignes[0].moyenne!!, 0.0001)
        assertEquals(2, resultat.stats.effectifNote)
        assertEquals(0.5, resultat.stats.tauxReussite!!, 0.0001)
    }

    @Test
    fun `moyenne annuelle public (2 x S2 + S1) sur 3 et prive moyenne des 3 trimestres`() {
        assertEquals(
            12.0,
            ClasseCalculator.moyenneAnnuelle(TypeEtablissement.PUBLIC, listOf(10.0, 13.0))!!,
            0.0001
        )
        assertEquals(
            12.0,
            ClasseCalculator.moyenneAnnuelle(TypeEtablissement.PRIVE, listOf(10.0, 12.0, 14.0))!!,
            0.0001
        )
        assertNull(ClasseCalculator.moyenneAnnuelle(TypeEtablissement.PUBLIC, listOf(10.0, null)))
        assertNull(ClasseCalculator.moyenneAnnuelle(TypeEtablissement.PRIVE, listOf(10.0, 12.0, null)))
    }
}
