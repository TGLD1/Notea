package com.fastek.notea

import android.app.Application
import androidx.room.Room
import com.fastek.notea.diagnostic.Journal
import com.fastek.notea.enseignant.data.EnseignantDatabase
import com.fastek.notea.enseignant.data.backup.ArchivesEnseignant
import com.fastek.notea.enseignant.data.backup.SauvegardeEnseignant
import com.fastek.notea.enseignant.data.repository.EnseignantRepository
import java.io.File

class NoteaApplication : Application() {

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

    override fun onCreate() {
        super.onCreate()
        Journal.init(this)
        Journal.ok("app.demarrage")
    }
}
