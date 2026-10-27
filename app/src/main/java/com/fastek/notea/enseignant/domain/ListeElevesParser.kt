package com.fastek.notea.enseignant.domain

/** Un élève lu dans une liste collée ou importée. */
data class EleveLu(val nom: String, val prenom: String, val matricule: String = "")

/**
 * Lit une liste d'élèves collée depuis WhatsApp, Excel ou un fichier CSV, une ligne par élève.
 *
 * - Avec un séparateur « ; », une tabulation ou une virgule : nom ; prénoms [; matricule].
 * - Sinon, les premiers mots entièrement en majuscules forment le nom (convention « DOSSOU Jean-Marc »),
 *   le reste forme les prénoms. S'il n'y en a pas, le premier mot est le nom.
 * - Les numéros de début de ligne (« 12. », « 3) », « 4 - ») et les lignes vides sont ignorés.
 */
object ListeElevesParser {

    private val NUMERO_DEBUT = Regex("^\\s*\\d+\\s*[.)\\-:]\\s*")

    fun analyser(texte: String): List<EleveLu> =
        texte.lineSequence()
            .map { it.replace(NUMERO_DEBUT, "").trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull { analyserLigne(it) }
            .toList()

    private fun analyserLigne(ligne: String): EleveLu? {
        val separateur = listOf(';', '\t', ',').firstOrNull { ligne.contains(it) }
        if (separateur != null) {
            val parties = ligne.split(separateur).map { it.trim() }
            val nom = parties.getOrNull(0).orEmpty()
            if (nom.isEmpty()) return null
            return EleveLu(nom, parties.getOrNull(1).orEmpty(), parties.getOrNull(2).orEmpty())
        }
        val mots = ligne.split(Regex("\\s+")).filter { it.isNotEmpty() }
        if (mots.isEmpty()) return null
        val nbMajuscules = mots.takeWhile { estMajuscules(it) }.size
        val coupure = if (nbMajuscules in 1 until mots.size) nbMajuscules else 1
        return EleveLu(
            nom = mots.take(coupure).joinToString(" "),
            prenom = mots.drop(coupure).joinToString(" ")
        )
    }

    private fun estMajuscules(mot: String): Boolean {
        val lettres = mot.filter { it.isLetter() }
        return lettres.isNotEmpty() && lettres.all { it.isUpperCase() }
    }
}
