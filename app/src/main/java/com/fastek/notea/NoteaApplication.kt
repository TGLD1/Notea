package com.fastek.notea

import android.app.Application
import androidx.room.Room
import com.fastek.notea.data.local.NoteaDatabase
import com.fastek.notea.data.repository.NoteaRepository

class NoteaApplication : Application() {

    val database: NoteaDatabase by lazy {
        Room.databaseBuilder(this, NoteaDatabase::class.java, NoteaDatabase.NOM_BASE)
            .build()
    }

    val repository: NoteaRepository by lazy {
        NoteaRepository(
            eleveDao = database.eleveDao(),
            matiereDao = database.matiereDao(),
            noteDao = database.noteDao(),
            periodeDao = database.periodeDao()
        )
    }
}
