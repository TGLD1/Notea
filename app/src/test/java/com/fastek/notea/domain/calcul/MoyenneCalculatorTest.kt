package com.fastek.notea.domain.calcul

import com.fastek.notea.data.local.entity.Note
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
    fun `moyenne matiere B (coeff 3) = 2,666...`() {
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
        val moyenneGenerale = MoyenneCalculator.moyenneGeneralePeriode(moyennes, noteConduite = null)
        assertEquals(2.9, moyenneGenerale!!, 0.01)
    }

    @Test
    fun `moyenne annuelle publique pondere le 2e semestre double`() {
        val annuelle = MoyenneCalculator.moyenneAnnuellePublic(moyenneS1 = 10.0, moyenneS2 = 13.0)
        // (2*13 + 10) / 3 = 12.0
        assertEquals(12.0, annuelle, 0.001)
    }
}
