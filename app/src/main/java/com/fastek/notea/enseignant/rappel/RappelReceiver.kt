package com.fastek.notea.enseignant.rappel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fastek.notea.NoteaApplication
import com.fastek.notea.diagnostic.Journal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Reçoit l'alarme d'un rappel : affiche la notification puis reprogramme la semaine suivante. */
class RappelReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val rappelId = intent.getLongExtra(RappelScheduler.EXTRA_RAPPEL, -1L)
        if (rappelId < 0) return
        val attente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val base = (context.applicationContext as NoteaApplication).enseignantBase
                val rappel = base.rappelDao().obtenir(rappelId)
                if (rappel != null && rappel.actif) {
                    val creneau = base.creneauDao().obtenir(rappel.creneauId)
                    val classe = creneau?.let { base.classeDao().obtenir(it.classeId) }
                    NotificationsRappel.afficher(context, rappel, classe?.nom, creneau)
                    Journal.ok("rappel.declenche", rappel.texte)
                    if (rappel.hebdomadaire && creneau != null) {
                        RappelScheduler.planifier(context, rappel, creneau)
                    } else {
                        base.rappelDao().mettreAJour(rappel.copy(actif = false))
                    }
                }
            } catch (e: Exception) {
                Journal.erreur("rappel.declenchement", e)
            } finally {
                attente.finish()
            }
        }
    }
}
