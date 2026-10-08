package com.fastek.notea.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fastek.notea.data.local.dao.EleveDao
import com.fastek.notea.data.local.dao.EvenementDao
import com.fastek.notea.data.local.dao.MatiereDao
import com.fastek.notea.data.local.dao.NoteDao
import com.fastek.notea.data.local.dao.PeriodeDao
import com.fastek.notea.data.local.entity.Eleve
import com.fastek.notea.data.local.entity.Evenement
import com.fastek.notea.data.local.entity.Matiere
import com.fastek.notea.data.local.entity.Note
import com.fastek.notea.data.local.entity.Periode

@Database(
    entities = [Eleve::class, Matiere::class, Note::class, Periode::class, Evenement::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NoteaDatabase : RoomDatabase() {
    abstract fun eleveDao(): EleveDao
    abstract fun matiereDao(): MatiereDao
    abstract fun noteDao(): NoteDao
    abstract fun periodeDao(): PeriodeDao
    abstract fun evenementDao(): EvenementDao

    companion object {
        const val NOM_BASE = "notea.db"

        /**
         * v3 -> v4 : champs facultatifs du profil pour l'en-tête du bulletin PDF.
         * Les données existantes sont conservées. Les bases v1 et v2 (versions de test
         * d'avant le schéma stable) sont encore réinitialisées, voir NoteaApplication.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE eleve ADD COLUMN dateNaissance TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE eleve ADD COLUMN lieuNaissance TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE eleve ADD COLUMN effectif TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE eleve ADD COLUMN aptitudeEps TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE eleve ADD COLUMN redoublant TEXT NOT NULL DEFAULT ''")
            }
        }
    }
}
