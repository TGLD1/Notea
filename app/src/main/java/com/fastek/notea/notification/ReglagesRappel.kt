package com.fastek.notea.notification

import android.content.Context

/** Heure du rappel quotidien du planning (réglage local, 18h par défaut). */
object ReglagesRappel {
    const val HEURE_PAR_DEFAUT = 18
    val HEURES_PROPOSEES = (6..22).toList()

    private const val FICHIER = "notea_reglages"
    private const val CLE_HEURE = "rappel_heure"

    fun heure(context: Context): Int =
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)
            .getInt(CLE_HEURE, HEURE_PAR_DEFAUT)
            .coerceIn(0, 23)

    /** Enregistre la nouvelle heure et replanifie immédiatement le prochain rappel. */
    fun definirHeure(context: Context, heure: Int) {
        context.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)
            .edit().putInt(CLE_HEURE, heure.coerceIn(0, 23)).apply()
        RappelWorker.planifierProchainRappel(context)
    }
}
