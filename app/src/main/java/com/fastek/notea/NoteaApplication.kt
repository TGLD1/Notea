package com.fastek.notea

import android.app.Application
import java.io.File
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.room.Room
import co.opensi.kkiapay.uikit.Kkiapay
import co.opensi.kkiapay.uikit.SdkConfig
import com.fastek.notea.data.backup.ArchiveManager
import com.fastek.notea.data.backup.SauvegardeManager
import com.fastek.notea.data.local.NoteaDatabase
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.diagnostic.Journal
import com.fastek.notea.enseignant.data.EnseignantDatabase
import com.fastek.notea.enseignant.data.backup.ArchivesEnseignant
import com.fastek.notea.enseignant.data.backup.SauvegardeEnseignant
import com.fastek.notea.enseignant.data.repository.EnseignantRepository
import com.fastek.notea.notification.RappelWorker

class NoteaApplication : Application() {

    val database: NoteaDatabase by lazy {
        Room.databaseBuilder(this, NoteaDatabase::class.java, NoteaDatabase.NOM_BASE)
            .addMigrations(NoteaDatabase.MIGRATION_3_4, NoteaDatabase.MIGRATION_4_5, NoteaDatabase.MIGRATION_5_6)
            .fallbackToDestructiveMigrationFrom(1, 2)
            .build()
    }

    val enseignantBase: EnseignantDatabase by lazy {
        Room.databaseBuilder(this, EnseignantDatabase::class.java, EnseignantDatabase.NOM_BASE)
            .addMigrations(EnseignantDatabase.MIGRATION_1_2)
            .build()
    }

    val enseignantRepository: EnseignantRepository by lazy { EnseignantRepository(enseignantBase) }

    val sauvegardeEnseignant: SauvegardeEnseignant by lazy { SauvegardeEnseignant(enseignantBase) }

    val archivesEnseignant: ArchivesEnseignant by lazy {
        ArchivesEnseignant(File(filesDir, "archives_enseignant"), sauvegardeEnseignant, enseignantRepository)
    }

    val sauvegarde: SauvegardeManager by lazy { SauvegardeManager(database) }

    val archives: ArchiveManager by lazy { ArchiveManager(File(filesDir, "archives"), database, sauvegarde) }

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
        Journal.init(this)
        Journal.ok("app.demarrage")
        if (BuildConfig.KKIAPAY_PUBLIC_KEY.isNotBlank()) {
            // Une panne du SDK de paiement ne doit jamais empêcher l'application de démarrer.
            try {
                Journal.suivre("don.initialisation") {
                    Kkiapay.init(
                        applicationContext,
                        BuildConfig.KKIAPAY_PUBLIC_KEY,
                        SdkConfig(themeColor = R.color.kkiapay_theme, enableSandbox = BuildConfig.KKIAPAY_SANDBOX)
                    )
                }
            } catch (e: Exception) {
                // déjà journalisé par Journal.suivre
            }
        }
        Journal.suivre("notification.canal") { creerCanalNotification() }
        Journal.suivre("notification.planification") { RappelWorker.planifierProchainRappel(this) }
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
