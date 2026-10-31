package com.fastek.notea.enseignant.data.backup

import com.fastek.notea.enseignant.data.entity.Professeur
import com.fastek.notea.enseignant.data.repository.EnseignantRepository
import com.fastek.notea.enseignant.export.ClasseurEnseignant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Archivage des années scolaires : chaque archive est un fichier de sauvegarde (JSON, restaurable)
 * accompagné du classeur Excel de l'année, écrits dans le dossier de l'application.
 */
class ArchivesEnseignant(
    private val dossier: File,
    private val sauvegarde: SauvegardeEnseignant,
    private val repo: EnseignantRepository
) {

    data class Archive(val json: File, val excel: File?, val resume: SauvegardeEnseignant.Resume?)

    /** Écrit l'archive de l'année en cours. Lève une exception si elle n'a pas pu être écrite. */
    suspend fun archiver(professeur: Professeur): Archive {
        val contenu = sauvegarde.exporter()
        val export = repo.preparerExport(professeur)
        return withContext(Dispatchers.IO) {
            dossier.mkdirs()
            val base = nomArchive(professeur.anneeScolaire, professeur.matiere)
            var nom = base
            if (File(dossier, "$nom.json").exists()) nom = "${base}_${System.currentTimeMillis()}"
            val json = File(dossier, "$nom.json")
            json.writeText(contenu, Charsets.UTF_8)
            check(json.length() > 0L) { "L'archive est vide." }
            val excel = if (export.classes.isEmpty()) {
                null
            } else {
                File(dossier, "$nom.xlsx").also { fichier ->
                    FileOutputStream(fichier).use { ClasseurEnseignant.ecrire(export, it) }
                }
            }
            Archive(json, excel, sauvegarde.resumer(contenu))
        }
    }

    /** Archives présentes, de la plus récente à la plus ancienne. */
    suspend fun lister(): List<Archive> = withContext(Dispatchers.IO) {
        dossier.listFiles { f -> f.extension == "json" }
            ?.sortedByDescending { it.lastModified() }
            ?.map { json ->
                Archive(
                    json = json,
                    excel = File(dossier, json.nameWithoutExtension + ".xlsx").takeIf { it.exists() },
                    resume = try {
                        sauvegarde.resumer(json.readText(Charsets.UTF_8))
                    } catch (e: Exception) {
                        null
                    }
                )
            }
            ?: emptyList()
    }

    suspend fun lire(archive: Archive): String = withContext(Dispatchers.IO) { archive.json.readText(Charsets.UTF_8) }

    suspend fun supprimer(archive: Archive) {
        withContext(Dispatchers.IO) {
            archive.json.delete()
            archive.excel?.delete()
        }
    }

    companion object {
        /** « 2026-2027 » et « PCT » donnent « Archive_2026-2027_PCT ». */
        fun nomArchive(annee: String, matiere: String): String =
            "Archive_" + "${annee}_$matiere".replace(Regex("[^\\p{L}\\p{N}_-]+"), "-")
    }
}
