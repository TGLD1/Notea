package com.fastek.notea.enseignant.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fastek.notea.enseignant.data.entity.Professeur
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfesseurDao {
    @Query("SELECT * FROM professeur LIMIT 1")
    fun observer(): Flow<Professeur?>

    @Query("SELECT * FROM professeur LIMIT 1")
    suspend fun obtenir(): Professeur?

    @Insert
    suspend fun inserer(professeur: Professeur): Long

    @Update
    suspend fun mettreAJour(professeur: Professeur)

    @Query("SELECT * FROM professeur")
    suspend fun obtenirTous(): List<Professeur>

    @Insert
    suspend fun insererTous(professeurs: List<Professeur>)

    /** Supprime aussi, en cascade, les classes, élèves, évaluations et notes. */
    @Query("DELETE FROM professeur")
    suspend fun toutSupprimer()
}
