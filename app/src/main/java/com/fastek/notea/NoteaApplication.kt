package com.fastek.notea

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import com.fastek.notea.data.local.NoteaDatabase
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.notification.RappelWorker

class NoteaApplication : Application() {

    val database: NoteaDatabase by lazy {
        Room.databaseBuilder(this, NoteaDatabase::class.java, NoteaDatabase.NOM_BASE)
            .fallbackToDestructiveMigration()
            .build()
    }

    val repository: NoteaRepository by lazy {
        NoteaRepository(
            eleveDao = database.eleveDao(),
            matiereDao = database.matiereDao(),
            noteDao = database.noteDao(),
            periodeDao = database.periodeDao(),
            evenementDao = database.evenementDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        creerCanalNotification()
        RappelWorker.planifierProchainRappel(this)
    }

    private fun creerCanalNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val canal = NotificationChannel(
                RappelWorker.CANAL_ID,
                "Rappels Notea",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Rappels pour les événements du planning" }
            getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
        }
    }
}
