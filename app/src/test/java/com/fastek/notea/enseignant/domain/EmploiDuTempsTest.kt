package com.fastek.notea.enseignant.domain

import com.fastek.notea.enseignant.data.entity.Creneau
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class EmploiDuTempsTest {

    // Le lundi 26 octobre 2026.
    private val lundi8h = LocalDateTime.of(2026, 10, 26, 8, 0)

    @Test
    fun `prochaine occurrence le jour meme si l'heure n'est pas passee`() {
        assertEquals(LocalDateTime.of(2026, 10, 26, 9, 0), EmploiDuTemps.prochaineOccurrence(1, 9 * 60, lundi8h))
    }

    @Test
    fun `prochaine occurrence la semaine suivante si l'heure est passee ou egale`() {
        assertEquals(LocalDateTime.of(2026, 11, 2, 7, 0), EmploiDuTemps.prochaineOccurrence(1, 7 * 60, lundi8h))
        assertEquals(LocalDateTime.of(2026, 11, 2, 8, 0), EmploiDuTemps.prochaineOccurrence(1, 8 * 60, lundi8h))
    }

    @Test
    fun `prochaine occurrence un autre jour de la semaine`() {
        assertEquals(LocalDateTime.of(2026, 10, 28, 8, 30), EmploiDuTemps.prochaineOccurrence(3, 8 * 60 + 30, lundi8h))
        assertEquals(LocalDateTime.of(2026, 10, 31, 10, 0), EmploiDuTemps.prochaineOccurrence(6, 10 * 60, lundi8h))
        assertEquals(LocalDateTime.of(2026, 11, 1, 17, 0), EmploiDuTemps.prochaineOccurrence(7, 17 * 60, lundi8h))
    }

    @Test
    fun `affichage des heures`() {
        assertEquals("8h", EmploiDuTemps.heure(480))
        assertEquals("8h30", EmploiDuTemps.heure(510))
        assertEquals("8h05", EmploiDuTemps.heure(485))
        assertEquals("0h", EmploiDuTemps.heure(0))
    }

    @Test
    fun `chevauchement de plages horaires`() {
        assertFalse(EmploiDuTemps.chevauchent(480, 600, 600, 720))
        assertTrue(EmploiDuTemps.chevauchent(480, 600, 540, 660))
        assertTrue(EmploiDuTemps.chevauchent(480, 720, 540, 600))
    }

    private val existants = listOf(
        Creneau(id = 1, classeId = 1, jour = 1, debut = 480, fin = 600),
        Creneau(id = 2, classeId = 2, jour = 2, debut = 480, fin = 600)
    )

    @Test
    fun `verification d'un cours`() {
        assertNull(EmploiDuTemps.verifierCours(1, 600, 720, existants, null))
        assertNotNull(EmploiDuTemps.verifierCours(1, 540, 660, existants, null))
        assertNotNull(EmploiDuTemps.verifierCours(1, 700, 700, existants, null))
        assertNotNull(EmploiDuTemps.verifierCours(1, 800, 700, existants, null))
        assertNotNull(EmploiDuTemps.verifierCours(0, 480, 600, existants, null))
        assertNotNull(EmploiDuTemps.verifierCours(1, 480, 1500, existants, null))
    }

    @Test
    fun `modifier un cours ne le fait pas chevaucher avec lui meme`() {
        assertNull(EmploiDuTemps.verifierCours(1, 480, 630, existants, ignorerId = 1))
    }

    @Test
    fun `cours en cours puis prochain cours`() {
        val creneaux = listOf(
            Creneau(id = 1, classeId = 1, jour = 1, debut = 7 * 60, fin = 9 * 60),
            Creneau(id = 2, classeId = 2, jour = 3, debut = 10 * 60, fin = 12 * 60)
        )
        val enCours = EmploiDuTemps.prochainCours(creneaux, lundi8h)!!
        assertTrue(enCours.enCours)
        assertEquals(1L, enCours.creneau.id)

        val apres = EmploiDuTemps.prochainCours(creneaux, LocalDateTime.of(2026, 10, 26, 9, 30))!!
        assertFalse(apres.enCours)
        assertEquals(2L, apres.creneau.id)
        assertEquals(LocalDateTime.of(2026, 10, 28, 10, 0), apres.debut)

        assertNull(EmploiDuTemps.prochainCours(emptyList(), lundi8h))
    }
}
