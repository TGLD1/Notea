package com.fastek.notea.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fastek.notea.data.local.dao.EleveDao
import com.fastek.notea.data.local.dao.MatiereDao
import com.fastek.notea.data.local.dao.NoteDao
import com.fastek.notea.data.local.dao.PeriodeDao
import com.fastek.notea.data.local.entity.Eleve
import com.fastek.notea.data.local.entity.Matiere
import com.fastek.notea.data.local.entity.Note
import com.fastek.notea.data.local.entity.Periode

@Database(
    entities = [Eleve::class, Matiere::class, Note::class, Periode::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NoteaDatabase : RoomDatabase() {
    abstract fun eleveDao(): EleveDao
    abstract fun matiereDao(): MatiereDao
    abstract fun noteDao(): NoteDao
    abstract fun periodeDao(): PeriodeDao

    companion object {
        const val NOM_BASE = "notea.db"
    }
}
