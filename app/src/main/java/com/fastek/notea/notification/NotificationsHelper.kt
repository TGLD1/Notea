package com.fastek.notea.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fastek.notea.R
import com.fastek.notea.ui.MainActivity

/** Autorisation, réglages système et envoi des notifications de Notea. */
object NotificationsHelper {

    private const val FICHIER = "notea_reglages"
    private const val CLE_DEMANDEE = "permission_notifications_demandee"
    private const val ID_RAPPEL = 1001
    private const val ID_TEST = 1002

    /** true si l'utilisateur a autorisé les notifications ET que le canal de rappels n'est pas coupé. */
    fun activees(context: Context): Boolean {
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = context.getSystemService(NotificationManager::class.java)
                .getNotificationChannel(RappelWorker.CANAL_ID)
            if (canal != null && canal.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }

    /** La fenêtre d'autorisation Android n'est proposée qu'une fois par l'application : on s'en souvient. */
    fun dejaDemandee(context: Context): Boolean =
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).getBoolean(CLE_DEMANDEE, false)

    fun marquerDemandee(context: Context) {
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE).edit().putBoolean(CLE_DEMANDEE, true).apply()
    }

    /** Ouvre les réglages de notifications de Notea dans Android. */
    fun ouvrirReglages(context: Context) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** Ouvre les réglages d'économie de batterie (certains téléphones bloquent les rappels en arrière-plan). */
    fun ouvrirReglagesBatterie(context: Context) {
        try {
            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            context.startActivity(
                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    /** Affiche la notification de rappel du planning. Retourne false si les notifications sont désactivées. */
    fun afficherRappel(context: Context, lignes: List<String>): Boolean = afficher(context, ID_RAPPEL, "Notea — Rappel du planning", lignes)

    /** Notification d'essai pour vérifier que tout fonctionne. */
    fun afficherTest(context: Context): Boolean =
        afficher(context, ID_TEST, "Notea — Test", listOf("Les rappels du planning fonctionnent sur cet appareil."))

    private fun afficher(context: Context, id: Int, titre: String, lignes: List<String>): Boolean {
        if (!activees(context)) return false
        val ouvrir = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, RappelWorker.CANAL_ID)
            .setContentTitle(titre)
            .setContentText(lignes.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lignes.joinToString("\n")))
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(ouvrir)
            .setAutoCancel(true)
            .build()
        return try {
            NotificationManagerCompat.from(context).notify(id, notification)
            true
        } catch (e: SecurityException) {
            false
        }
    }
}
