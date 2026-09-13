package com.fastek.notea.notification

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fastek.notea.NoteaApplication
import com.fastek.notea.data.local.entity.Evenement
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * Vérifie chaque jour à 18h les événements dans les 48h à venir pas encore notifiés,
 * envoie une notification si besoin, puis planifie sa propre prochaine exécution
 * (plus fiable qu'un simple travail périodique, qui peut dériver dans le temps).
 */
class RappelWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as NoteaApplication
        val repository = app.repository
        val eleve = repository.getProfil()

        if (eleve != null) {
            val aujourdHui = LocalDate.now()
            val dansDeuxJours = aujourdHui.plusDays(2)
            val evenements = repository.getEvenementsAvenirNonNotifies(eleve.id, aujourdHui, dansDeuxJours)

            if (evenements.isNotEmpty()) {
                afficherNotification(evenements)
                evenements.forEach { repository.mettreAJourEvenement(it.copy(notifie = true)) }
            }
        }

        planifierProchainRappel(applicationContext)
        return Result.success()
    }

    private fun afficherNotification(evenements: List<Evenement>) {
        val texte = if (evenements.size == 1) {
            evenements.first().titre
        } else {
            evenements.joinToString(", ") { it.titre }
        }

        val notification = NotificationCompat.Builder(applicationContext, CANAL_ID)
            .setContentTitle("Notea — Rappel")
            .setContentText(texte)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // icône système temporaire
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val permissionOk = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

        if (permissionOk) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.notify(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val CANAL_ID = "notea_rappels"
        private const val NOTIFICATION_ID = 1001
        private const val NOM_TRAVAIL = "rappel_planning"

        /** Planifie l'exécution suivante à 18h (aujourd'hui si pas encore passé, sinon demain). */
        fun planifierProchainRappel(context: Context) {
            val maintenant = LocalDateTime.now()
            var cible = maintenant.toLocalDate().atTime(18, 0)
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
