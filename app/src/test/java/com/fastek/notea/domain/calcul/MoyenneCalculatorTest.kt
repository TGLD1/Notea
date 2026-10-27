package com.fastek.notea.domain.calcul

import com.fastek.notea.data.local.entity.Note
import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.data.local.entity.TypeNote
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

/** Reproduit l'exemple chiffré validé par Divin (collège public, 1er semestre). */
class MoyenneCalculatorTest {

    private fun note(type: TypeNote, valeur: Double) =
        Note(matiereId = 1, periodeId = 1, type = type, valeur = valeur, date = LocalDate.now())

    @Test
    fun `moyenne matiere A (coeff 2) = 3,25`() {
        val notes = listOf(
            note(TypeNote.INTERROGATION, 4.0),
            note(TypeNote.INTERROGATION, 3.0),
            note(TypeNote.INTERROGATION, 3.0),
            note(TypeNote.INTERROGATION, 1.0),
            note(TypeNote.DEVOIR, 3.0),
            note(TypeNote.DEVOIR, 4.0)
        )
        val moyenne = MoyenneCalculator.moyenneMatiere(notes)
        assertEquals(3.25, moyenne!!, 0.001)
    }

    @Test
    fun `moyenne matiere B (coeff 3) = 2,666 periodique`() {
        val notes = listOf(
            note(TypeNote.INTERROGATION, 5.0),
            note(TypeNote.INTERROGATION, 1.0),
            note(TypeNote.INTERROGATION, 3.0),
            note(TypeNote.DEVOIR, 3.0),
            note(TypeNote.DEVOIR, 2.0)
        )
        val moyenne = MoyenneCalculator.moyenneMatiere(notes)
        assertEquals(2.6667, moyenne!!, 0.001)
    }

    @Test
    fun `moyenne generale du 1er semestre (A + B, sans conduite) = 2,9`() {
        val moyennes = listOf(
            MoyenneCalculator.MoyennePonderee(moyenne = 3.25, coefficient = 2),
            MoyenneCalculator.MoyennePonderee(moyenne = 2.6667, coefficient = 3)
        )
        val moyenneGenerale = MoyenneCalculator.moyenneGeneralePeriode(moyennes)
        assertEquals(2.9, moyenneGenerale!!, 0.01)
    }

    @Test
    fun `conduite est une matiere coefficient 1`() {
        val moyennes = listOf(
            MoyenneCalculator.MoyennePonderee(14.0, 2),
            MoyenneCalculator.MoyennePonderee(12.0, 1),
            MoyenneCalculator.MoyennePonderee(16.0, 1)
        )
        val moyenneGenerale = MoyenneCalculator.moyenneGeneralePeriode(moyennes)
        assertEquals(14.0, moyenneGenerale!!, 0.0001)
    }

    @Test
    fun `moyenne annuelle publique pondere le 2e semestre double`() {
        val annuelle = MoyenneCalculator.moyenneAnnuellePublic(moyenneS1 = 10.0, moyenneS2 = 13.0)
        // (2*13 + 10) / 3 = 12.0
        assertEquals(12.0, annuelle, 0.001)
    }

    @Test
    fun `moyenne annuelle privee = somme des 3 trimestres divisee par 3`() {
        val annuelle = MoyenneCalculator.moyenneAnnuellePrive(10.0, 12.0, 14.0)
        assertEquals(12.0, annuelle, 0.001)
    }

    @Test
    fun `devoirs seuls ou interrogations seules`() {
        assertEquals(13.0, MoyenneCalculator.moyenneMatiere(listOf(note(TypeNote.DEVOIR, 12.0), note(TypeNote.DEVOIR, 14.0)))!!, 0.001)
        assertEquals(8.0, MoyenneCalculator.moyenneMatiere(listOf(note(TypeNote.INTERROGATION, 6.0), note(TypeNote.INTERROGATION, 10.0)))!!, 0.001)
    }

    @Test
    fun `aucune note donne une moyenne nulle`() {
        assertEquals(null, MoyenneCalculator.moyenneMatiere(emptyList()))
        assertEquals(null, MoyenneCalculator.moyenneGeneralePeriode(emptyList()))
    }
    @Test
    fun `note requise a l'interro pour atteindre la moyenne generale visee`() {
        // Matière notée : interro 8 (coeff 2) ; autre matière : 12 (coeff 2). Visée : 12 de moyenne générale.
        val notes = listOf(note(TypeNote.INTERROGATION, 8.0))
        val autres = listOf(MoyenneCalculator.MoyennePonderee(12.0, 2))
        val requis = MoyenneCalculator.noteRequisePourMoyenneGenerale(
            notes, TypeNote.INTERROGATION, 2, autres, 12.0
        )
        // M_intero = (8 + x)/2 ; général = (12×2 + M×2)/4 = 12 -> M = 12 -> x = 16
        assertEquals(MoyenneCalculator.StatutRequis.ATTEIGNABLE, requis.statut)
        assertEquals(16.0, requis.valeur, 0.01)
    }

    @Test
    fun `note requise deja acquise ou impossible`() {
        val notes = listOf(note(TypeNote.INTERROGATION, 15.0))
        val autres = listOf(MoyenneCalculator.MoyennePonderee(15.0, 2))
        val acquis = MoyenneCalculator.noteRequisePourMoyenneGenerale(
            notes, TypeNote.DEVOIR, 2, autres, 5.0
        )
        assertEquals(MoyenneCalculator.StatutRequis.DEJA_ATTEINT, acquis.statut)

        val impossible = MoyenneCalculator.noteRequisePourMoyenneGenerale(
            listOf(note(TypeNote.INTERROGATION, 2.0)), TypeNote.INTERROGATION, 2,
            listOf(MoyenneCalculator.MoyennePonderee(2.0, 6)), 19.0
        )
        assertEquals(MoyenneCalculator.StatutRequis.IMPOSSIBLE, impossible.statut)
    }

    @Test
    fun `passage public - moyenne requise au semestre 2`() {
        // (2×S2 + S1)/3 = 10 avec S1 = 8 -> S2 = 11
        val requis = MoyenneCalculator.moyenneRequiseDernierePeriode(TypeEtablissement.PUBLIC, listOf(8.0), 10.0)
        assertEquals(MoyenneCalculator.StatutRequis.ATTEIGNABLE, requis.statut)
        assertEquals(11.0, requis.valeur, 0.001)
    }

    @Test
    fun `passage prive - moyenne requise au trimestre 3`() {
        // (T1 + T2 + T3)/3 = 10 avec T1 = 9, T2 = 8 -> T3 = 13
        val requis = MoyenneCalculator.moyenneRequiseDernierePeriode(TypeEtablissement.PRIVE, listOf(9.0, 8.0), 10.0)
        assertEquals(13.0, requis.valeur, 0.001)
    }

    @Test
    fun `passage impossible ou deja acquis`() {
        val impossible = MoyenneCalculator.moyenneRequiseDernierePeriode(TypeEtablissement.PUBLIC, listOf(2.0), 18.0)
        assertEquals(MoyenneCalculator.StatutRequis.IMPOSSIBLE, impossible.statut)
        val acquis = MoyenneCalculator.moyenneRequiseDernierePeriode(TypeEtablissement.PUBLIC, listOf(20.0), 6.0)
        assertEquals(MoyenneCalculator.StatutRequis.DEJA_ATTEINT, acquis.statut)
    }
}
