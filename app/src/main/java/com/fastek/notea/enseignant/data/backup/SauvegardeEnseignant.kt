package com.fastek.notea.enseignant.data.backup

import androidx.room.withTransaction
import com.fastek.notea.data.backup.SauvegardeInvalideException
import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.data.local.entity.TypeNote
import com.fastek.notea.enseignant.data.EnseignantDatabase
import com.fastek.notea.enseignant.data.entity.Classe
import com.fastek.notea.enseignant.data.entity.Evaluation
import com.fastek.notea.enseignant.data.entity.Inscrit
import com.fastek.notea.enseignant.data.entity.NoteEleve
import com.fastek.notea.enseignant.data.entity.PeriodeClasse
import com.fastek.notea.enseignant.data.entity.Professeur
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

/**
 * Sauvegarde et restauration 100 % locales de Notea Enseignant : toutes les classes, tous les élèves
 * et toutes les notes sont écrits dans un fichier JSON. La restauration remplace l'intégralité des
 * données en une seule transaction : si le fichier est invalide, rien n'est modifié.
 */
class SauvegardeEnseignant(private val db: EnseignantDatabase) {

    suspend fun exporter(): String {
        val professeurs = db.professeurDao().obtenirTous()
        val classes = db.classeDao().obtenirToutes()
        val periodes = db.periodeClasseDao().obtenirToutes()
        val inscrits = db.inscritDao().obtenirTous()
        val evaluations = db.evaluationDao().obtenirToutes()
        val notes = db.noteEleveDao().obtenirToutes()
        return JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION_FORMAT)
            put("date", LocalDate.now().toString())
            put("professeurs", JSONArray().apply { professeurs.forEach { put(versJson(it)) } })
            put("classes", JSONArray().apply { classes.forEach { put(versJson(it)) } })
            put("periodes", JSONArray().apply { periodes.forEach { put(versJson(it)) } })
            put("inscrits", JSONArray().apply { inscrits.forEach { put(versJson(it)) } })
            put("evaluations", JSONArray().apply { evaluations.forEach { put(versJson(it)) } })
            put("notes", JSONArray().apply { notes.forEach { put(versJson(it)) } })
        }.toString(2)
    }

    /** Remplace toutes les données par celles du fichier. Lève [SauvegardeInvalideException] si invalide. */
    suspend fun restaurer(contenu: String) {
        val d = lire(contenu)
        db.withTransaction {
            db.professeurDao().toutSupprimer()
            db.professeurDao().insererTous(d.professeurs)
            db.classeDao().insererToutes(d.classes)
            db.periodeClasseDao().insererToutes(d.periodes)
            db.inscritDao().insererTous(d.inscrits)
            db.evaluationDao().insererToutes(d.evaluations)
            db.noteEleveDao().insererToutes(d.notes)
        }
    }

    private class Donnees(
        val professeurs: List<Professeur>,
        val classes: List<Classe>,
        val periodes: List<PeriodeClasse>,
        val inscrits: List<Inscrit>,
        val evaluations: List<Evaluation>,
        val notes: List<NoteEleve>
    )

    private fun lire(contenu: String): Donnees {
        try {
            val racine = JSONObject(contenu)
            if (racine.optString("format") != FORMAT) {
                throw SauvegardeInvalideException("Ce fichier n'est pas une sauvegarde de Notea Enseignant.")
            }
            if (racine.optInt("version", 0) > VERSION_FORMAT) {
                throw SauvegardeInvalideException(
                    "Cette sauvegarde vient d'une version plus récente de Notea Enseignant. Mets l'application à jour."
                )
            }
            val professeurs = racine.getJSONArray("professeurs").objets().map { professeurDepuis(it) }
            val classes = racine.getJSONArray("classes").objets().map { classeDepuis(it) }
            val periodes = racine.getJSONArray("periodes").objets().map { periodeDepuis(it) }
            val inscrits = racine.getJSONArray("inscrits").objets().map { inscritDepuis(it) }
            val evaluations = racine.getJSONArray("evaluations").objets().map { evaluationDepuis(it) }
            val notes = racine.getJSONArray("notes").objets().map { noteDepuis(it) }

            val idProfesseurs = professeurs.map { it.id }.toSet()
            val idClasses = classes.map { it.id }.toSet()
            val idPeriodes = periodes.map { it.id }.toSet()
            val idInscrits = inscrits.map { it.id }.toSet()
            val idEvaluations = evaluations.map { it.id }.toSet()
            val coherent = classes.all { it.professeurId in idProfesseurs } &&
                periodes.all { it.classeId in idClasses } &&
                inscrits.all { it.classeId in idClasses } &&
                evaluations.all { it.classeId in idClasses && it.periodeId in idPeriodes } &&
                notes.all { it.evaluationId in idEvaluations && it.inscritId in idInscrits }
            if (!coherent) throw SauvegardeInvalideException("La sauvegarde est incohérente ou endommagée.")

            return Donnees(professeurs, classes, periodes, inscrits, evaluations, notes)
        } catch (e: SauvegardeInvalideException) {
            throw e
        } catch (e: JSONException) {
            throw SauvegardeInvalideException("Fichier de sauvegarde illisible.")
        } catch (e: RuntimeException) {
            throw SauvegardeInvalideException("Fichier de sauvegarde illisible.")
        }
    }

    private fun JSONArray.objets(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun versJson(p: Professeur) = JSONObject().apply {
        put("id", p.id); put("nom", p.nom); put("prenom", p.prenom); put("matiere", p.matiere)
        put("typeEtablissement", p.typeEtablissement.name); put("anneeScolaire", p.anneeScolaire)
        put("etablissement", p.etablissement)
    }

    private fun professeurDepuis(o: JSONObject) = Professeur(
        id = o.getLong("id"),
        nom = o.getString("nom"),
        prenom = o.getString("prenom"),
        matiere = o.getString("matiere"),
        typeEtablissement = TypeEtablissement.valueOf(o.getString("typeEtablissement")),
        anneeScolaire = o.getString("anneeScolaire"),
        etablissement = o.optString("etablissement", "")
    )

    private fun versJson(c: Classe) = JSONObject().apply {
        put("id", c.id); put("professeurId", c.professeurId); put("nom", c.nom)
        put("coefficient", c.coefficient); put("ordre", c.ordre)
    }

    private fun classeDepuis(o: JSONObject) = Classe(
        id = o.getLong("id"),
        professeurId = o.getLong("professeurId"),
        nom = o.getString("nom"),
        coefficient = o.getInt("coefficient"),
        ordre = o.getInt("ordre")
    )

    private fun versJson(p: PeriodeClasse) = JSONObject().apply {
        put("id", p.id); put("classeId", p.classeId); put("numero", p.numero); put("terminee", p.terminee)
    }

    private fun periodeDepuis(o: JSONObject) = PeriodeClasse(
        id = o.getLong("id"),
        classeId = o.getLong("classeId"),
        numero = o.getInt("numero"),
        terminee = o.getBoolean("terminee")
    )

    private fun versJson(i: Inscrit) = JSONObject().apply {
        put("id", i.id); put("classeId", i.classeId); put("nom", i.nom)
        put("prenom", i.prenom); put("matricule", i.matricule)
    }

    private fun inscritDepuis(o: JSONObject) = Inscrit(
        id = o.getLong("id"),
        classeId = o.getLong("classeId"),
        nom = o.getString("nom"),
        prenom = o.getString("prenom"),
        matricule = o.optString("matricule", "")
    )

    private fun versJson(e: Evaluation) = JSONObject().apply {
        put("id", e.id); put("classeId", e.classeId); put("periodeId", e.periodeId)
        put("type", e.type.name); put("intitule", e.intitule); put("date", e.date.toString())
    }

    private fun evaluationDepuis(o: JSONObject) = Evaluation(
        id = o.getLong("id"),
        classeId = o.getLong("classeId"),
        periodeId = o.getLong("periodeId"),
        type = TypeNote.valueOf(o.getString("type")),
        intitule = o.optString("intitule", ""),
        date = LocalDate.parse(o.getString("date"))
    )

    private fun versJson(n: NoteEleve) = JSONObject().apply {
        put("id", n.id); put("evaluationId", n.evaluationId); put("inscritId", n.inscritId); put("valeur", n.valeur)
    }

    private fun noteDepuis(o: JSONObject) = NoteEleve(
        id = o.getLong("id"),
        evaluationId = o.getLong("evaluationId"),
        inscritId = o.getLong("inscritId"),
        valeur = o.getDouble("valeur")
    )

    companion object {
        const val FORMAT = "notea-enseignant-sauvegarde"
        const val VERSION_FORMAT = 1
    }
}
