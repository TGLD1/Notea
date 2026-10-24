package com.fastek.notea.data.backup

import androidx.room.withTransaction
import com.fastek.notea.data.local.NoteaDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Archivage des années scolaires. « Nouvelle année » enregistre d'abord toutes les données dans
 * un fichier d'archive (même format que la sauvegarde), puis efface les notes et les événements.
 * Le profil, les matières, les coefficients et les objectifs sont conservés.
 */
class ArchiveManager(
    private val dossier: File,
    private val database: NoteaDatabase,
    private val sauvegarde: SauvegardeManager
) {

    /** Archive l'année en cours et prépare la suivante. Retourne le libellé de la nouvelle année. */
    suspend fun nouvelleAnnee(): String {
        val eleve = database.eleveDao().getProfil() ?: throw IllegalStateException("Aucun profil")
        val contenu = sauvegarde.exporter()
        withContext(Dispatchers.IO) {
            dossier.mkdirs()
            val nom = "Archive_" + (eleve.anneeScolaire + "_" + eleve.classe)
                .replace(Regex("[^A-Za-z0-9_-]"), "-")
            var fichier = File(dossier, "$nom.json")
            if (fichier.exists()) fichier = File(dossier, "${nom}_${System.currentTimeMillis()}.json")
            fichier.writeText(contenu, Charsets.UTF_8)
        }
        val suivante = anneeSuivante(eleve.anneeScolaire)
        database.withTransaction {
            database.noteDao().toutSupprimer()
            database.evenementDao().toutSupprimer()
            database.periodeDao().reinitialiserTerminees()
            database.eleveDao().mettreAJour(eleve.copy(anneeScolaire = suivante))
        }
        return suivante
    }

    /** Archives présentes, de la plus récente à la plus ancienne. */
    suspend fun lister(): List<File> = withContext(Dispatchers.IO) {
        dossier.listFiles { f -> f.extension == "json" }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    suspend fun resumer(fichier: File): SauvegardeManager.ResumeArchive? = withContext(Dispatchers.IO) {
        try {
            sauvegarde.resumer(fichier.readText(Charsets.UTF_8))
        } catch (e: Exception) {
            null
        }
    }

    suspend fun supprimer(fichier: File) {
        withContext(Dispatchers.IO) { fichier.delete() }
    }

    companion object {
        /** « 2025-2026 » → « 2026-2027 » (le séparateur est conservé) ; inchangé si le format est inconnu. */
        fun anneeSuivante(annee: String): String {
            val modele = Regex("(\\d{4})(\\D+)(\\d{4})").find(annee) ?: return annee
            val (debut, separateur, fin) = modele.destructured
            return annee.replaceRange(
                modele.range,
                "${debut.toInt() + 1}$separateur${fin.toInt() + 1}"
            )
        }
    }
}
