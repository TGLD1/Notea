package com.fastek.notea.enseignant.rappel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fastek.notea.diagnostic.Journal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Reprogramme les rappels quand Android les a effacés : redémarrage, mise à jour de l'application, changement d'heure. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val attente = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                RappelScheduler.replanifierTout(context)
            } catch (e: Exception) {
                Journal.erreur("rappel.reprogrammation", e)
            } finally {
                attente.finish()
            }
        }
    }
}
