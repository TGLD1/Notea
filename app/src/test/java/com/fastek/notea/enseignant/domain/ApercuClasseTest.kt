package com.fastek.notea.enseignant.domain

import com.fastek.notea.data.local.entity.TypeNote
import com.fastek.notea.enseignant.data.entity.Evaluation
import com.fastek.notea.enseignant.data.entity.Inscrit
import com.fastek.notea.enseignant.data.entity.NoteEleve
import com.fastek.notea.enseignant.data.entity.PeriodeClasse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class ApercuClasseTest {

    private val inscrits = listOf(
        Inscrit(id = 1, classeId = 1, nom = "AGBO", prenom = "Marie"),
        Inscrit(id = 2, classeId = 1, nom = "DOSSOU", prenom = "Jean"),
        Inscrit(id = 3, classeId = 1, nom = "ZINSOU", prenom = "Luc")
    )
    private val periodes = listOf(
        PeriodeClasse(id = 10, classeId = 1, numero = 1),
        PeriodeClasse(id = 11, classeId = 1, numero = 2)
    )

    private fun evaluation(id: Long, periodeId: Long) =
        Evaluation(id = id, classeId = 1, periodeId = periodeId, type = TypeNote.INTERROGATION, date = LocalDate.of(2026, 10, 1))

    @Test
    fun `classe sans note`() {
        val apercu = calculerApercu(inscrits, periodes, emptyList(), emptyList())
        assertEquals(3, apercu.effectif)
        assertNull(apercu.moyenne)
        assertNull(apercu.numeroPeriode)
        assertEquals(0, apercu.elevesNotes)
    }

    @Test
    fun `la periode la plus recente avec des notes est retenue`() {
        val evaluations = listOf(evaluation(100, 10), evaluation(101, 11))
        val notes = listOf(
            NoteEleve(evaluationId = 100, inscritId = 1, valeur = 10.0),
            NoteEleve(evaluationId = 100, inscritId = 2, valeur = 12.0),
            NoteEleve(evaluationId = 101, inscritId = 1, valeur = 14.0),
            NoteEleve(evaluationId = 101, inscritId = 2, valeur = 8.0)
        )
        val apercu = calculerApercu(inscrits, periodes, evaluations, notes)
        assertEquals(2, apercu.numeroPeriode)
        assertEquals(11.0, apercu.moyenne!!, 0.0001)
        assertEquals(2, apercu.elevesNotes)
    }

    @Test
    fun `retour a la periode precedente quand la derniere est vide`() {
        val evaluations = listOf(evaluation(100, 10), evaluation(101, 11))
        val notes = listOf(NoteEleve(evaluationId = 100, inscritId = 3, valeur = 16.0))
        val apercu = calculerApercu(inscrits, periodes, evaluations, notes)
        assertEquals(1, apercu.numeroPeriode)
        assertEquals(16.0, apercu.moyenne!!, 0.0001)
        assertEquals(1, apercu.elevesNotes)
    }
}
