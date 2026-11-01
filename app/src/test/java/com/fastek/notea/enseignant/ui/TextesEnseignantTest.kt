package com.fastek.notea.enseignant.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextesEnseignantTest {

    private fun tout(page: PageTexte) = (listOf(page.titre, page.sousTitre) + page.sections.flatMap { listOfNotNull(it.titre, it.texte) })
        .joinToString("\n")

    @Test
    fun `toutes les pages existent et ont du contenu`() {
        TextesEnseignant.IDENTIFIANTS.forEach { id ->
            val page = TextesEnseignant.page(id)
            assertNotNull("page $id", page)
            assertTrue(page!!.sections.isNotEmpty())
            assertTrue(page.sections.all { it.texte.isNotBlank() })
        }
        assertNull(TextesEnseignant.page("inconnue"))
    }

    @Test
    fun `le prestataire de paiement cite est FedaPay`() {
        TextesEnseignant.IDENTIFIANTS.forEach { id ->
            assertFalse(tout(TextesEnseignant.page(id)!!).contains("KkiaPay", ignoreCase = true))
        }
        assertTrue(tout(TextesEnseignant.page("confidentialite")!!).contains("FedaPay"))
    }

    @Test
    fun `les regles de calcul annoncees sont celles de Notea`() {
        val calcul = tout(TextesEnseignant.page("calcul")!!)
        assertTrue(calcul.contains("(2 × moyenne du 2ᵉ semestre + moyenne du 1ᵉʳ semestre) ÷ 3"))
        assertTrue(calcul.contains("÷ (1 + nombre de devoirs)"))
        assertTrue(calcul.contains("2 au maximum"))
    }

    @Test
    fun `contact`() {
        assertEquals("https://wa.me/2290164409445", TextesEnseignant.WHATSAPP_LIEN)
        assertTrue(TextesEnseignant.EMAIL.contains("@"))
    }
}
