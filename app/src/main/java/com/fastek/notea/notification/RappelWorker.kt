package com.fastek.notea.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fastek.notea.NoteaApplication
import com.fastek.notea.diagnostic.Journal
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import kotlin.coroutines.cancellation.CancellationException

/**
 * Vérifie chaque jour à l'heure choisie (18h par défaut, voir [ReglagesRappel]) les événements du jour, de demain et d'après-demain
 * (ce dernier une seule fois), envoie une notification groupée si besoin, puis planifie sa propre prochaine exécution
 * (plus fiable qu'un simple travail périodique, qui peut dériver dans le temps).
 */
class RappelWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        try {
            val app = applicationContext as NoteaApplication
            val repository = app.repository
            val eleve = repository.getProfil()
            var nbEvenements = 0

            if (eleve != null) {
                val aujourdHui = LocalDate.now()
                // Trois rappels : dans 2 jours (une seule fois), la veille, et le jour même.
                val dansDeuxJours = repository.getEvenementsAvenirNonNotifies(
                    eleve.id, aujourdHui.plusDays(2), aujourdHui.plusDays(2)
                )
                val demain = repository.getEvenementsDuJour(eleve.id, aujourdHui.plusDays(1))
                val duJour = repository.getEvenementsDuJour(eleve.id, aujourdHui)
                nbEvenements = dansDeuxJours.size + demain.size + duJour.size

                val lignes = buildList {
                    if (duJour.isNotEmpty()) add("Aujourd'hui : " + duJour.joinToString(", ") { it.titre })
                    if (demain.isNotEmpty()) add("Demain : " + demain.joinToString(", ") { it.titre })
                    if (dansDeuxJours.isNotEmpty()) add("Dans 2 jours : " + dansDeuxJours.joinToString(", ") { it.titre })
                }
                if (lignes.isNotEmpty()) {
                    if (!NotificationsHelper.afficherRappel(applicationContext, lignes)) {
                        Journal.erreur("notification.rappel", detail = "notifications désactivées dans Android")
                    }
                    dansDeuxJours.forEach { repository.mettreAJourEvenement(it.copy(notifie = true)) }
                }
            }
            Journal.ok("notification.rappel", "$nbEvenements événement(s) notifié(s)")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Une erreur ne doit pas casser la chaîne : on la journalise et on replanifie quand même.
            Journal.erreur("notification.rappel", e)
        }

        planifierProchainRappel(applicationContext)
        return Result.success()
    }

    companion object {
        const val CANAL_ID = "notea_rappels"
        private const val NOM_TRAVAIL = "rappel_planning"

        /** Planifie l'exécution suivante à l'heure du rappel (aujourd'hui si pas encore passée, sinon demain). */
        fun planifierProchainRappel(context: Context) {
            val maintenant = LocalDateTime.now()
            var cible = maintenant.toLocalDate().atTime(ReglagesRappel.heure(context), 0)
            if (!maintenant.isBefore(cible)) {
                cible = cible.plusDays(1)
            }
            val delai = Duration.between(maintenant, cible).toMillis()

            val requete = OneTimeWorkRequestBuilder<RappelWorker>()
                .setInitialDelay(delai, TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                NOM_TRAVAIL,
                ExistingWorkPolicy.REPLACE,
                requete
            )
        }
    }
}
