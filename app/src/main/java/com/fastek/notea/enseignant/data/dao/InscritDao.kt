package com.fastek.notea.enseignant.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fastek.notea.enseignant.data.entity.Inscrit
import kotlinx.coroutines.flow.Flow

@Dao
interface InscritDao {
    @Query("SELECT * FROM inscrit WHERE classeId = :classeId ORDER BY nom COLLATE NOCASE, prenom COLLATE NOCASE, id")
    fun observerInscrits(classeId: Long): Flow<List<Inscrit>>

    @Query("SELECT * FROM inscrit WHERE classeId = :classeId ORDER BY nom COLLATE NOCASE, prenom COLLATE NOCASE, id")
    suspend fun obtenirInscrits(classeId: Long): List<Inscrit>

    @Insert
    suspend fun inserer(inscrit: Inscrit): Long

    @Insert
    suspend fun insererTous(inscrits: List<Inscrit>)

    @Update
    suspend fun mettreAJour(inscrit: Inscrit)

    @Delete
    suspend fun supprimer(inscrit: Inscrit)
}
