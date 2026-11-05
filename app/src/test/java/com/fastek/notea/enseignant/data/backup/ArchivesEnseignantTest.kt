package com.fastek.notea.enseignant.data.backup

import com.fastek.notea.enseignant.domain.AnneeScolaire
import org.junit.Assert.assertEquals
import org.junit.Test

class ArchivesEnseignantTest {

    @Test
    fun `nom de fichier d'archive`() {
        assertEquals("Archive_2026-2027_PCT", ArchivesEnseignant.nomArchive("2026-2027", "PCT"))
        assertEquals("Archive_2026-2027_Histoire-Géographie", ArchivesEnseignant.nomArchive("2026-2027", "Histoire/Géographie"))
        assertEquals("Archive_2026-2027_Éducation-Physique-et-Sportive", ArchivesEnseignant.nomArchive("2026-2027", "Éducation Physique et Sportive"))
    }

    @Test
    fun `annee suivante`() {
        assertEquals("2027-2028", AnneeScolaire.suivante("2026-2027"))
        assertEquals("2027/2028", AnneeScolaire.suivante("2026/2027"))
        assertEquals("annee inconnue", AnneeScolaire.suivante("annee inconnue"))
    }
}
