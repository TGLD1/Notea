package com.fastek.notea.enseignant.rappel

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fastek.notea.R
import com.fastek.notea.enseignant.data.entity.Creneau
import com.fastek.notea.enseignant.data.entity.Rappel
import com.fastek.notea.enseignant.domain.EmploiDuTemps
import com.fastek.notea.enseignant.ui.EnseignantActivity

/** Notifications des rappels de cours : un canal silencieux et un canal sonore. */
object NotificationsRappel {

    const val CANAL_SILENCIEUX = "notea_ens_silencieux"
    const val CANAL_SONORE = "notea_ens_sonore"

    fun creerCanaux(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val gestionnaire = context.getSystemService(NotificationManager::class.java)
        val silencieux = NotificationChannel(
            CANAL_SILENCIEUX, "Rappels de cours silencieux", NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notification discrète, sans son ni vibration"
            setSound(null, null)
            enableVibration(false)
        }
        val sonore = NotificationChannel(
            CANAL_SONORE, "Rappels de cours sonores", NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notification avec son et vibration"
        }
        gestionnaire.createNotificationChannels(listOf(silencieux, sonore))
    }

    /** Affiche le rappel. Retourne false si les notifications sont désactivées pour l'application. */
    fun afficher(context: Context, rappel: Rappel, nomClasse: String?, creneau: Creneau?): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        creerCanaux(context)
        val ouvrir = PendingIntent.getActivity(
            context, 0,
            Intent(context, EnseignantActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val titre = listOfNotNull(
            nomClasse,
            creneau?.let { "${EmploiDuTemps.heure(it.debut)} – ${EmploiDuTemps.heure(it.fin)}" }
        ).joinToString(" · ").ifBlank { "Rappel de cours" }

        val constructeur = NotificationCompat.Builder(
            context, if (rappel.sonore) CANAL_SONORE else CANAL_SILENCIEUX
        )
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(titre)
            .setContentText(rappel.texte)
            .setStyle(NotificationCompat.BigTextStyle().bigText(rappel.texte))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(ouvrir)
            .setAutoCancel(true)
        if (rappel.sonore) {
            constructeur.setPriority(NotificationCompat.PRIORITY_HIGH).setDefaults(NotificationCompat.DEFAULT_ALL)
        } else {
            // Avant Android 8 il n'y a pas de canaux : on coupe le son et la vibration ici.
            constructeur.setPriority(NotificationCompat.PRIORITY_LOW).setSound(null).setVibrate(null)
        }
        return try {
            NotificationManagerCompat.from(context).notify((10_000 + rappel.id).toInt(), constructeur.build())
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
