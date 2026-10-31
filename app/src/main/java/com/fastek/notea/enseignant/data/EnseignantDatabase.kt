package com.fastek.notea.enseignant.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.fastek.notea.data.local.Converters
import com.fastek.notea.enseignant.data.dao.ClasseDao
import com.fastek.notea.enseignant.data.dao.CreneauDao
import com.fastek.notea.enseignant.data.dao.EvaluationDao
import com.fastek.notea.enseignant.data.dao.InscritDao
import com.fastek.notea.enseignant.data.dao.NoteEleveDao
import com.fastek.notea.enseignant.data.dao.PeriodeClasseDao
import com.fastek.notea.enseignant.data.dao.ProfesseurDao
import com.fastek.notea.enseignant.data.dao.RappelDao
import com.fastek.notea.enseignant.data.entity.Classe
import com.fastek.notea.enseignant.data.entity.Creneau
import com.fastek.notea.enseignant.data.entity.Evaluation
import com.fastek.notea.enseignant.data.entity.Inscrit
import com.fastek.notea.enseignant.data.entity.NoteEleve
import com.fastek.notea.enseignant.data.entity.PeriodeClasse
import com.fastek.notea.enseignant.data.entity.Professeur
import com.fastek.notea.enseignant.data.entity.Rappel

@Database(
    entities = [
        Professeur::class, Classe::class, Inscrit::class,
        PeriodeClasse::class, Evaluation::class, NoteEleve::class,
        Creneau::class, Rappel::class
    ],
    version = 2,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class EnseignantDatabase : RoomDatabase() {
    abstract fun professeurDao(): ProfesseurDao
    abstract fun classeDao(): ClasseDao
    abstract fun inscritDao(): InscritDao
    abstract fun periodeClasseDao(): PeriodeClasseDao
    abstract fun evaluationDao(): EvaluationDao
    abstract fun noteEleveDao(): NoteEleveDao
    abstract fun creneauDao(): CreneauDao
    abstract fun rappelDao(): RappelDao

    companion object {
        const val NOM_BASE = "notea_enseignant.db"

        /** Version 2 : emploi du temps (cours) et rappels de cours. Les données existantes sont conservées. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `creneau` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`classeId` INTEGER NOT NULL, `jour` INTEGER NOT NULL, `debut` INTEGER NOT NULL, " +
                        "`fin` INTEGER NOT NULL, `salle` TEXT NOT NULL, " +
                        "FOREIGN KEY(`classeId`) REFERENCES `classe`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_creneau_classeId` ON `creneau` (`classeId`)")
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `rappel` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                        "`creneauId` INTEGER NOT NULL, `texte` TEXT NOT NULL, `minutes` INTEGER NOT NULL, " +
                        "`sonore` INTEGER NOT NULL, `hebdomadaire` INTEGER NOT NULL, `actif` INTEGER NOT NULL, " +
                        "FOREIGN KEY(`creneauId`) REFERENCES `creneau`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )"
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_rappel_creneauId` ON `rappel` (`creneauId`)")
            }
        }
    }
}
