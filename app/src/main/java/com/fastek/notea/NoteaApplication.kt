package com.fastek.notea

import android.app.Application
import androidx.room.Room
import com.fastek.notea.data.local.NoteaDatabase

class NoteaApplication : Application() {

    val database: NoteaDatabase by lazy {
        Room.databaseBuilder(this, NoteaDatabase::class.java, NoteaDatabase.NOM_BASE)
            .build()
    }
}
