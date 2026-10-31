package com.fastek.notea.enseignant.rappel

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import com.fastek.notea.NoteaApplication
import com.fastek.notea.diagnostic.Journal
import com.fastek.notea.enseignant.data.entity.Creneau
import com.fastek.notea.enseignant.data.entity.Rappel
import com.fastek.notea.enseignant.domain.EmploiDuTemps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import java.time.ZoneId

/** Programme les rappels de cours avec le service d'alarmes d'Android (une alarme par rappel). */
object RappelScheduler {

    const val EXTRA_RAPPEL = "rappelId"

    private fun intentRappel(context: Context, rappelId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context, rappelId.toInt(),
            Intent(context, RappelReceiver::class.java).putExtra(EXTRA_RAPPEL, rappelId),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

    /** Android 12+ : l'autorisation « Alarmes et rappels » est nécessaire pour sonner à la minute près. */
    fun alarmesPrecisesAutorisees(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    /** Écran d'autorisation des alarmes précises (Android 12+), ou null si inutile. */
    fun intentAutorisation(context: Context): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } else {
            null
        }

    /** Programme la prochaine sonnerie du rappel (le jour du cours, à l'heure du rappel). */
    fun planifier(context: Context, rappel: Rappel, creneau: Creneau, maintenant: LocalDateTime = LocalDateTime.now()) {
        val moment = EmploiDuTemps.prochaineOccurrence(creneau.jour, rappel.minutes, maintenant)
        val millis = moment.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val alarmes = context.getSystemService(AlarmManager::class.java)
        val intention = intentRappel(context, rappel.id)
        try {
            if (alarmesPrecisesAutorisees(context)) {
                alarmes.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intention)
            } else {
                alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intention)
            }
        } catch (e: SecurityException) {
            Journal.erreur("rappel.planification", e)
            alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis, intention)
        }
    }

    fun annuler(context: Context, rappelId: Long) {
        val alarmes = context.getSystemService(AlarmManager::class.java)
        val intention = intentRappel(context, rappelId)
        alarmes.cancel(intention)
        intention.cancel()
    }

    /** Reprogramme tous les rappels actifs (démarrage, redémarrage du téléphone, changement d'heure, restauration). */
    suspend fun replanifierTout(context: Context) = withContext(Dispatchers.IO) {
        val base = (context.applicationContext as NoteaApplication).enseignantBase
        val creneaux = base.creneauDao().obtenirTous().associateBy { it.id }
        base.rappelDao().obtenirTous().forEach { rappel ->
            val creneau = creneaux[rappel.creneauId]
            if (rappel.actif && creneau != null) planifier(context, rappel, creneau) else annuler(context, rappel.id)
        }
    }
}
