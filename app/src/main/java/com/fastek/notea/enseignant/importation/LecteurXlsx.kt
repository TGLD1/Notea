package com.fastek.notea.enseignant.importation

import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Lit la première feuille d'un classeur Excel (.xlsx) en lignes de texte, sans bibliothèque.
 * Gère les textes partagés d'Excel, les textes en ligne (comme ceux que Notea écrit) et les nombres.
 */
object LecteurXlsx {

    private const val NS_PRINCIPAL = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"
    private const val NS_RELATIONS = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"
    private const val TAILLE_MAX_PARTIE = 20_000_000

    fun lireLignes(flux: InputStream): List<List<String>> {
        val fichiers = lireZip(flux)
        val chemin = cheminPremiereFeuille(fichiers)
            ?: throw IllegalArgumentException("Ce fichier ne contient aucune feuille de calcul.")
        val feuille = fichiers[chemin] ?: throw IllegalArgumentException("Feuille introuvable dans le fichier.")
        val partages = fichiers["xl/sharedStrings.xml"]?.let { chainesPartagees(analyser(it)) } ?: emptyList()
        return lignesDeFeuille(analyser(feuille), partages)
    }

    private fun lireZip(flux: InputStream): Map<String, ByteArray> {
        val fichiers = HashMap<String, ByteArray>()
        var total = 0
        ZipInputStream(flux).use { zip ->
            var entree = zip.nextEntry
            while (entree != null) {
                val nom = entree.name
                if (!entree.isDirectory && nom.startsWith("xl/") && (nom.endsWith(".xml") || nom.endsWith(".rels"))) {
                    val octets = zip.readBytes()
                    total += octets.size
                    require(total <= TAILLE_MAX_PARTIE) { "Fichier trop volumineux." }
                    fichiers[nom] = octets
                }
                entree = zip.nextEntry
            }
        }
        return fichiers
    }

    private fun analyser(octets: ByteArray): Document {
        val fabrique = DocumentBuilderFactory.newInstance()
        fabrique.isNamespaceAware = true
        try {
            fabrique.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        } catch (e: Exception) {
            // Option non prise en charge par cet analyseur : on continue sans.
        }
        return fabrique.newDocumentBuilder().parse(ByteArrayInputStream(octets))
    }

    private fun Element.enfants(nom: String): List<Element> {
        val resultat = ArrayList<Element>()
        var noeud: Node? = firstChild
        while (noeud != null) {
            if (noeud is Element && (noeud.localName == nom || noeud.nodeName == nom)) resultat.add(noeud)
            noeud = noeud.nextSibling
        }
        return resultat
    }

    /** Texte d'un élément « si » ou « is » : les morceaux « t », y compris dans les « r » (texte enrichi), sans la phonétique. */
    private fun texteDe(element: Element): String {
        val sb = StringBuilder()
        element.enfants("t").forEach { sb.append(it.textContent) }
        element.enfants("r").forEach { run -> run.enfants("t").forEach { sb.append(it.textContent) } }
        return sb.toString()
    }

    private fun cheminPremiereFeuille(fichiers: Map<String, ByteArray>): String? {
        val classeur = fichiers["xl/workbook.xml"]
        val relations = fichiers["xl/_rels/workbook.xml.rels"]
        if (classeur != null && relations != null) {
            val feuilles = analyser(classeur).getElementsByTagNameNS(NS_PRINCIPAL, "sheet")
            val idPremiere = (feuilles.item(0) as? Element)?.getAttributeNS(NS_RELATIONS, "id").orEmpty()
            val liens = analyser(relations).getElementsByTagName("Relationship")
            for (i in 0 until liens.length) {
                val lien = liens.item(i) as Element
                if (lien.getAttribute("Id") == idPremiere) {
                    val cible = lien.getAttribute("Target")
                    val chemin = if (cible.startsWith("/")) cible.removePrefix("/") else "xl/$cible"
                    if (fichiers.containsKey(chemin)) return chemin
                }
            }
        }
        return fichiers.keys.filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }.minOrNull()
    }

    private fun chainesPartagees(document: Document): List<String> {
        val elements = document.getElementsByTagNameNS(NS_PRINCIPAL, "si")
        return (0 until elements.length).map { texteDe(elements.item(it) as Element) }
    }

    private fun lignesDeFeuille(document: Document, partages: List<String>): List<List<String>> {
        val lignes = document.getElementsByTagNameNS(NS_PRINCIPAL, "row")
        val resultat = ArrayList<List<String>>()
        for (i in 0 until lignes.length) {
            val ligne = lignes.item(i) as Element
            val cellules = HashMap<Int, String>()
            var suivante = 0
            ligne.enfants("c").forEach { cellule ->
                val reference = cellule.getAttribute("r")
                val colonne = if (reference.isNotEmpty()) indexColonne(reference) else suivante
                suivante = colonne + 1
                cellules[colonne] = valeurDe(cellule, partages)
            }
            val taille = (cellules.keys.maxOrNull() ?: -1) + 1
            resultat.add((0 until taille).map { cellules[it].orEmpty() })
        }
        return resultat
    }

    private fun valeurDe(cellule: Element, partages: List<String>): String {
        val type = cellule.getAttribute("t")
        val valeur = cellule.enfants("v").firstOrNull()?.textContent.orEmpty()
        return when (type) {
            "s" -> partages.getOrNull(valeur.trim().toIntOrNull() ?: -1).orEmpty()
            "inlineStr" -> cellule.enfants("is").firstOrNull()?.let { texteDe(it) }.orEmpty()
            "str", "b", "e" -> valeur
            else -> nombreEnTexte(valeur)
        }.trim()
    }

    /** « 12.0 » devient « 12 » : Excel écrit les entiers sans décimale, pas toujours. */
    private fun nombreEnTexte(valeur: String): String {
        val nombre = valeur.toDoubleOrNull() ?: return valeur
        return if (nombre == Math.floor(nombre) && Math.abs(nombre) < 1e15) nombre.toLong().toString() else valeur
    }

    /** « A1 » donne 0, « C7 » donne 2, « AA3 » donne 26. */
    internal fun indexColonne(reference: String): Int {
        var index = 0
        for (c in reference) {
            if (!c.isLetter()) break
            index = index * 26 + (c.uppercaseChar() - 'A' + 1)
        }
        return index - 1
    }
}
