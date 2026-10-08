package com.fastek.notea.diagnostic

import android.content.Context
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.cancellation.CancellationException

/**
 * Journal de diagnostic 100% local : enregistre, fonctionnalité par fonctionnalité, si ça a
 * marché (OK), échoué (ERREUR) ou fait planter l'app (CRASH), pour déboguer sans PC.
 *
 * Aucune donnée personnelle n'y est écrite (ni notes, ni noms) : uniquement le nom de la
 * fonctionnalité, un détail technique et, en cas d'erreur, le type d'exception + les premières
 * lignes de la pile. Le fichier reste sur le téléphone ; l'élève le partage s'il le veut.
 *
 * Utilisation :
 *   Journal.ok("bulletin.pdf", "semestre 1")
 *   Journal.erreur("bulletin.pdf", exception)
 *   val x = Journal.suivre("note.ajout") { ... }   // journalise OK ou ERREUR puis relance l'erreur
 */
object Journal {

    private const val NOM_FICHIER = "journal.log"
    private const val TAILLE_MAX = 200_000L
    private val FORMAT_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val LIGNE = Regex("^(\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}) \\[(OK|ERREUR|CRASH)] (\\S+)")

    private val verrou = Any()
    private var fichier: File? = null

    /** À appeler une seule fois, au tout début de Application.onCreate(). */
    fun init(context: Context) {
        fichier = File(context.filesDir, NOM_FICHIER)
        val ancienGestionnaire = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { fil, erreur ->
            ecrire("CRASH", "plantage", erreur, "thread=${fil.name}")
            ancienGestionnaire?.uncaughtException(fil, erreur)
        }
    }

    fun ok(fonction: String, detail: String = "") = ecrire("OK", fonction, null, detail)

    fun erreur(fonction: String, erreur: Throwable? = null, detail: String = "") =
        ecrire("ERREUR", fonction, erreur, detail)

    /** Exécute [bloc] et journalise OK ou ERREUR (l'erreur est ensuite relancée normalement). */
    inline fun <T> suivre(fonction: String, bloc: () -> T): T {
        try {
            val resultat = bloc()
            ok(fonction)
            return resultat
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            erreur(fonction, e)
            throw e
        }
    }

    fun lire(): String = synchronized(verrou) {
        fichier?.takeIf { it.exists() }?.readText() ?: ""
    }

    fun vider() = synchronized(verrou) {
        fichier?.takeIf { it.exists() }?.delete()
        Unit
    }

    data class EtatFonction(
        val fonction: String,
        val dernierStatut: String,
        val date: String,
        val nbOk: Int,
        val nbErreurs: Int
    ) {
        val fonctionne: Boolean get() = dernierStatut == "OK"
    }

    /** Dernier état connu de chaque fonctionnalité ; celles en échec passent en premier. */
    fun resume(): List<EtatFonction> {
        val etats = LinkedHashMap<String, EtatFonction>()
        lire().lineSequence().forEach { ligne ->
            val m = LIGNE.find(ligne) ?: return@forEach
            val (date, statut, fonction) = m.destructured
            val avant = etats[fonction]
            etats[fonction] = EtatFonction(
                fonction = fonction,
                dernierStatut = statut,
                date = date,
                nbOk = (avant?.nbOk ?: 0) + if (statut == "OK") 1 else 0,
                nbErreurs = (avant?.nbErreurs ?: 0) + if (statut == "OK") 0 else 1
            )
        }
        return etats.values.sortedWith(
            compareBy<EtatFonction> { it.fonctionne }.thenBy { it.fonction }
        )
    }

    private fun ecrire(statut: String, fonction: String, erreur: Throwable?, detail: String) {
        val f = fichier ?: return
        val texte = StringBuilder()
            .append(LocalDateTime.now().format(FORMAT_DATE))
            .append(" [").append(statut).append("] ").append(fonction)
        if (detail.isNotBlank()) texte.append(" — ").append(detail)
        if (erreur != null) {
            texte.append(" | ").append(erreur.javaClass.simpleName).append(": ").append(erreur.message ?: "")
            erreur.stackTrace.take(6).forEach { texte.append("\n    at ").append(it) }
        }
        texte.append('\n')

        synchronized(verrou) {
            try {
                if (f.exists() && f.length() > TAILLE_MAX) {
                    // Garde la seconde moitié du fichier, en repartant d'une ligne entière.
                    val fin = f.readText().takeLast((TAILLE_MAX / 2).toInt())
                    f.writeText(fin.substringAfter('\n', fin))
                }
                f.appendText(texte.toString())
            } catch (_: Exception) {
                // Le journal ne doit jamais faire planter l'app.
            }
        }
    }
}
