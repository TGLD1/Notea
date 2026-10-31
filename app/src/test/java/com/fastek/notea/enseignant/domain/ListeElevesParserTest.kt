package com.fastek.notea.enseignant.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ListeElevesParserTest {

    @Test
    fun `nom en majuscules puis prenoms`() {
        val liste = ListeElevesParser.analyser("DOSSOU Jean-Marc\nAGBO Marie Claire\n\nHOUNKPATIN Paul")
        assertEquals(
            listOf(
                EleveLu("DOSSOU", "Jean-Marc"),
                EleveLu("AGBO", "Marie Claire"),
                EleveLu("HOUNKPATIN", "Paul")
            ),
            liste
        )
    }

    @Test
    fun `numeros de debut de ligne ignores`() {
        val liste = ListeElevesParser.analyser("1. DOSSOU Jean\n2) AGBO Marie\n3 - ZINSOU Luc")
        assertEquals(listOf("DOSSOU", "AGBO", "ZINSOU"), liste.map { it.nom })
        assertEquals(listOf("Jean", "Marie", "Luc"), liste.map { it.prenom })
    }

    @Test
    fun `separateurs point-virgule tabulation et virgule`() {
        val liste = ListeElevesParser.analyser("Dossou;Jean;M123\nAgbo\tMarie\nZinsou, Luc")
        assertEquals(
            listOf(EleveLu("Dossou", "Jean", "M123"), EleveLu("Agbo", "Marie"), EleveLu("Zinsou", "Luc")),
            liste
        )
    }

    @Test
    fun `sans majuscules le premier mot est le nom`() {
        assertEquals(listOf(EleveLu("Dossou", "Jean Marc")), ListeElevesParser.analyser("Dossou Jean Marc"))
    }

    @Test
    fun `un seul mot`() {
        assertEquals(listOf(EleveLu("DOSSOU", "")), ListeElevesParser.analyser("DOSSOU"))
    }

    @Test
    fun `ligne d'en-tete ignoree`() {
        val liste = ListeElevesParser.analyser("N°;Nom;Prénoms\n1;DOSSOU;Jean\n2;AGBO;Marie")
        assertEquals(listOf(EleveLu("DOSSOU", "Jean"), EleveLu("AGBO", "Marie")), liste)
        assertEquals(listOf(EleveLu("DOSSOU", "Jean")), ListeElevesParser.analyser("Nom et Prénoms\nDOSSOU Jean"))
        assertEquals(listOf(EleveLu("Dossou", "Jean")), ListeElevesParser.analyser("Nom;Prénoms\nDossou;Jean"))
    }

    @Test
    fun `premiere colonne numerique numero d'ordre ou matricule`() {
        val liste = ListeElevesParser.analyser("12;DOSSOU;Jean\n20250123;AGBO;Marie")
        assertEquals(EleveLu("DOSSOU", "Jean"), liste[0])
        assertEquals(EleveLu("AGBO", "Marie", "20250123"), liste[1])
    }

    @Test
    fun `un eleve nomme comme un mot d'en-tete n'est pas ignore s'il n'est pas seul sur la ligne`() {
        assertEquals(
            listOf(EleveLu("DOSSOU", "Jean"), EleveLu("NOM", "Paul")),
            ListeElevesParser.analyser("DOSSOU Jean\nNOM Paul")
        )
    }
}
