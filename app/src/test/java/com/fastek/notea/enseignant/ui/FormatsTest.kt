package com.fastek.notea.enseignant.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatsTest {

    @Test
    fun `une note ne peut jamais depasser 20`() {
        assertEquals("2", filtrerSaisieNote("2", ""))
        assertEquals("2", filtrerSaisieNote("25", "2"))
        assertEquals("2", filtrerSaisieNote("21", "2"))
        assertEquals("20", filtrerSaisieNote("20", "2"))
        assertEquals("20,", filtrerSaisieNote("20,5", "20,"))
        assertEquals("20", filtrerSaisieNote("200", "20"))
    }

    @Test
    fun `deux decimales au plus et un seul separateur`() {
        assertEquals("12,34", filtrerSaisieNote("12,345", "12,34"))
        assertEquals("12,", filtrerSaisieNote("12,,", "12,"))
        assertEquals("12.5", filtrerSaisieNote("12.5", "12"))
        assertEquals("", filtrerSaisieNote(",", ""))
    }

    @Test
    fun `caracteres invalides ignores et effacement possible`() {
        assertEquals("12,3", filtrerSaisieNote("12,3x", "12,3"))
        assertEquals("", filtrerSaisieNote("abc", ""))
        assertEquals("", filtrerSaisieNote("", "12"))
        assertEquals("0", filtrerSaisieNote("0", ""))
    }

    @Test
    fun `affichage des notes et des moyennes`() {
        assertEquals("12", formaterNote(12.0))
        assertEquals("12,5", formaterNote(12.5))
        assertEquals("10,33", formaterMoyenne(10.3333))
        assertEquals("—", formaterMoyenne(null))
    }

    @Test
    fun `heures et minutes bornees`() {
        assertEquals("2", filtrerNombreBorne("2", "", 23))
        assertEquals("2", filtrerNombreBorne("25", "2", 23))
        assertEquals("23", filtrerNombreBorne("23", "2", 23))
        assertEquals("5", filtrerNombreBorne("5", "", 59))
        assertEquals("12", filtrerNombreBorne("123", "12", 59))
        assertEquals("", filtrerNombreBorne("a", "", 23))
        assertEquals("", filtrerNombreBorne("", "5", 23))
    }
}
