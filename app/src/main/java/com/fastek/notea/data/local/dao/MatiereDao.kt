package com.fastek.notea.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fastek.notea.data.local.entity.Matiere
import kotlinx.coroutines.flow.Flow

@Dao
interface MatiereDao {

    @Insert
    suspend fun inserer(matiere: Matiere): Long

    @Update
    suspend fun mettreAJour(matiere: Matiere)

    @Delete
    suspend fun supprimer(matiere: Matiere)

    @Query("SELECT * FROM matiere WHERE eleveId = :eleveId ORDER BY nom")
    fun observerMatieres(eleveId: Long): Flow<List<Matiere>>

    @Query("SELECT * FROM matiere WHERE id = :matiereId")
    suspend fun getMatiere(matiereId: Long): Matiere?
}
