package com.fastek.notea.enseignant.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fastek.notea.data.local.entity.TypeNote
import com.fastek.notea.enseignant.data.entity.Evaluation
import kotlinx.coroutines.flow.Flow

@Dao
interface EvaluationDao {
    @Query("SELECT * FROM evaluation WHERE periodeId = :periodeId ORDER BY date, id")
    fun observerEvaluations(periodeId: Long): Flow<List<Evaluation>>

    @Query("SELECT * FROM evaluation WHERE classeId = :classeId ORDER BY date, id")
    suspend fun obtenirEvaluationsClasse(classeId: Long): List<Evaluation>

    @Query("SELECT COUNT(*) FROM evaluation WHERE periodeId = :periodeId AND type = :type")
    suspend fun compterParType(periodeId: Long, type: TypeNote): Int

    @Insert
    suspend fun inserer(evaluation: Evaluation): Long

    @Update
    suspend fun mettreAJour(evaluation: Evaluation)

    @Delete
    suspend fun supprimer(evaluation: Evaluation)

    @Query("SELECT * FROM evaluation")
    suspend fun obtenirToutes(): List<Evaluation>

    @Insert
    suspend fun insererToutes(evaluations: List<Evaluation>)
}
