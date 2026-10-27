package com.fastek.notea.enseignant.data

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fastek.notea.data.local.Converters
import com.fastek.notea.enseignant.data.dao.ClasseDao
import com.fastek.notea.enseignant.data.dao.EvaluationDao
import com.fastek.notea.enseignant.data.dao.InscritDao
import com.fastek.notea.enseignant.data.dao.NoteEleveDao
import com.fastek.notea.enseignant.data.dao.PeriodeClasseDao
import com.fastek.notea.enseignant.data.dao.ProfesseurDao
import com.fastek.notea.enseignant.data.entity.Classe
import com.fastek.notea.enseignant.data.entity.Evaluation
import com.fastek.notea.enseignant.data.entity.Inscrit
import com.fastek.notea.enseignant.data.entity.NoteEleve
import com.fastek.notea.enseignant.data.entity.PeriodeClasse
import com.fastek.notea.enseignant.data.entity.Professeur

@Database(
    entities = [
        Professeur::class, Classe::class, Inscrit::class,
        PeriodeClasse::class, Evaluation::class, NoteEleve::class
    ],
    version = 1,
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

    companion object {
        const val NOM_BASE = "notea_enseignant.db"
    }
}
