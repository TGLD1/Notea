package com.fastek.notea.data.backup

import androidx.room.withTransaction
import com.fastek.notea.data.local.NoteaDatabase
import com.fastek.notea.data.local.entity.Eleve
import com.fastek.notea.data.local.entity.Evenement
import com.fastek.notea.data.local.entity.Matiere
import com.fastek.notea.data.local.entity.Note
import com.fastek.notea.data.local.entity.Periode
import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.data.local.entity.TypeNote
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

/** Erreur lisible renvoyée quand le fichier choisi n'est pas une sauvegarde Notea valide. */
class SauvegardeInvalideException(message: String) : Exception(message)

/**
 * Sauvegarde / restauration 100% locale : toutes les données de l'élève sont sérialisées dans
 * un fichier JSON que l'élève range où il veut (aucun serveur, aucune permission spéciale).
 * La restauration remplace l'intégralité des données actuelles, en une seule transaction :
 * si le fichier est invalide, rien n'est modifié.
 */
class SauvegardeManager(private val database: NoteaDatabase) {

    suspend fun exporter(): String {
        val eleves = database.eleveDao().getTous()
        val periodes = database.periodeDao().getToutes()
        val matieres = database.matiereDao().getToutes()
        val notes = database.noteDao().getToutes()
        val evenements = database.evenementDao().getTous()

        return JSONObject().apply {
            put("format", FORMAT)
            put("version", VERSION_FORMAT)
            put("date", LocalDate.now().toString())
            put("eleves", JSONArray().apply { eleves.forEach { put(versJson(it)) } })
            put("periodes", JSONArray().apply { periodes.forEach { put(versJson(it)) } })
            put("matieres", JSONArray().apply { matieres.forEach { put(versJson(it)) } })
            put("notes", JSONArray().apply { notes.forEach { put(versJson(it)) } })
            put("evenements", JSONArray().apply { evenements.forEach { put(versJson(it)) } })
        }.toString(2)
    }

    /** Remplace toutes les données par celles du fichier. Lève [SauvegardeInvalideException] si invalide. */
    suspend fun restaurer(contenu: String) {
        val donnees = lire(contenu)
        database.withTransaction {
            database.eleveDao().toutSupprimer()
            database.eleveDao().insererTous(donnees.eleves)
            database.periodeDao().insererToutes(donnees.periodes)
            database.matiereDao().insererToutes(donnees.matieres)
            database.noteDao().insererToutes(donnees.notes)
            database.evenementDao().insererTous(donnees.evenements)
        }
    }

    private class Donnees(
        val eleves: List<Eleve>,
        val periodes: List<Periode>,
        val matieres: List<Matiere>,
        val notes: List<Note>,
        val evenements: List<Evenement>
    )

    private fun lire(contenu: String): Donnees {
        try {
            val racine = JSONObject(contenu)
            if (racine.optString("format") != FORMAT) {
                throw SauvegardeInvalideException("Ce fichier n'est pas une sauvegarde Notea.")
            }
            if (racine.optInt("version", 0) > VERSION_FORMAT) {
                throw SauvegardeInvalideException(
                    "Cette sauvegarde vient d'une version plus récente de Notea. Mets l'application à jour."
                )
            }
            val eleves = racine.getJSONArray("eleves").objets().map { eleveDepuis(it) }
            val periodes = racine.getJSONArray("periodes").objets().map { periodeDepuis(it) }
            val matieres = racine.getJSONArray("matieres").objets().map { matiereDepuis(it) }
            val notes = racine.getJSONArray("notes").objets().map { noteDepuis(it) }
            val evenements = racine.getJSONArray("evenements").objets().map { evenementDepuis(it) }

            val idEleves = eleves.map { it.id }.toSet()
            val idMatieres = matieres.map { it.id }.toSet()
            val idPeriodes = periodes.map { it.id }.toSet()
            val coherent = periodes.all { it.eleveId in idEleves } &&
                matieres.all { it.eleveId in idEleves } &&
                notes.all { it.matiereId in idMatieres && it.periodeId in idPeriodes } &&
                evenements.all { it.eleveId in idEleves && (it.matiereId == null || it.matiereId in idMatieres) }
            if (!coherent) throw SauvegardeInvalideException("La sauvegarde est incohérente ou endommagée.")

            return Donnees(eleves, periodes, matieres, notes, evenements)
        } catch (e: SauvegardeInvalideException) {
            throw e
        } catch (e: JSONException) {
            throw SauvegardeInvalideException("Fichier de sauvegarde illisible.")
        } catch (e: RuntimeException) {
            throw SauvegardeInvalideException("Fichier de sauvegarde illisible.")
        }
    }

    // ------------------------------------------------------------ conversions

    private fun JSONArray.objets(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

    private fun versJson(e: Eleve) = JSONObject().apply {
        put("id", e.id); put("nom", e.nom); put("prenom", e.prenom); put("classe", e.classe)
        put("typeEtablissement", e.typeEtablissement.name); put("anneeScolaire", e.anneeScolaire)
        put("objectifAnnuel", e.objectifAnnuel); put("matricule", e.matricule)
        put("dateNaissance", e.dateNaissance); put("lieuNaissance", e.lieuNaissance)
        put("effectif", e.effectif); put("aptitudeEps", e.aptitudeEps); put("redoublant", e.redoublant)
    }

    private fun eleveDepuis(o: JSONObject) = Eleve(
        id = o.getLong("id"),
        nom = o.getString("nom"),
        prenom = o.getString("prenom"),
        classe = o.getString("classe"),
        typeEtablissement = TypeEtablissement.valueOf(o.getString("typeEtablissement")),
        anneeScolaire = o.getString("anneeScolaire"),
        objectifAnnuel = o.getDouble("objectifAnnuel"),
        matricule = o.optString("matricule", ""),
        dateNaissance = o.optString("dateNaissance", ""),
        lieuNaissance = o.optString("lieuNaissance", ""),
        effectif = o.optString("effectif", ""),
        aptitudeEps = o.optString("aptitudeEps", ""),
        redoublant = o.optString("redoublant", "")
    )

    private fun versJson(p: Periode) = JSONObject().apply {
        put("id", p.id); put("eleveId", p.eleveId); put("numero", p.numero); put("objectifCible", p.objectifCible)
    }

    private fun periodeDepuis(o: JSONObject) = Periode(
        id = o.getLong("id"),
        eleveId = o.getLong("eleveId"),
        numero = o.getInt("numero"),
        objectifCible = o.getDouble("objectifCible")
    )

    private fun versJson(m: Matiere) = JSONObject().apply {
        put("id", m.id); put("eleveId", m.eleveId); put("nom", m.nom); put("coefficient", m.coefficient)
    }

    private fun matiereDepuis(o: JSONObject) = Matiere(
        id = o.getLong("id"),
        eleveId = o.getLong("eleveId"),
        nom = o.getString("nom"),
        coefficient = o.getInt("coefficient")
    )

    private fun versJson(n: Note) = JSONObject().apply {
        put("id", n.id); put("matiereId", n.matiereId); put("periodeId", n.periodeId)
        put("type", n.type.name); put("valeur", n.valeur); put("date", n.date.toEpochDay())
    }

    private fun noteDepuis(o: JSONObject) = Note(
        id = o.getLong("id"),
        matiereId = o.getLong("matiereId"),
        periodeId = o.getLong("periodeId"),
        type = TypeNote.valueOf(o.getString("type")),
        valeur = o.getDouble("valeur"),
        date = LocalDate.ofEpochDay(o.getLong("date"))
    )

    private fun versJson(e: Evenement) = JSONObject().apply {
        put("id", e.id); put("eleveId", e.eleveId)
        put("matiereId", e.matiereId ?: JSONObject.NULL)
        put("titre", e.titre); put("date", e.date.toEpochDay()); put("notifie", e.notifie)
    }

    private fun evenementDepuis(o: JSONObject) = Evenement(
        id = o.getLong("id"),
        eleveId = o.getLong("eleveId"),
        matiereId = if (o.isNull("matiereId")) null else o.getLong("matiereId"),
        titre = o.getString("titre"),
        date = LocalDate.ofEpochDay(o.getLong("date")),
        notifie = o.optBoolean("notifie", false)
    )

    companion object {
        const val FORMAT = "notea-sauvegarde"
        const val VERSION_FORMAT = 1
    }
}
