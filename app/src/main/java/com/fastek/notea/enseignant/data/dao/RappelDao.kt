package com.fastek.notea.enseignant.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fastek.notea.enseignant.data.entity.Rappel
import kotlinx.coroutines.flow.Flow

@Dao
interface RappelDao {
    @Query("SELECT * FROM rappel ORDER BY minutes, id")
    fun observerTous(): Flow<List<Rappel>>

    @Query("SELECT * FROM rappel")
    suspend fun obtenirTous(): List<Rappel>

    @Query("SELECT * FROM rappel WHERE creneauId = :creneauId ORDER BY minutes, id")
    suspend fun obtenirDuCreneau(creneauId: Long): List<Rappel>

    @Query("SELECT * FROM rappel WHERE id = :id")
    suspend fun obtenir(id: Long): Rappel?

    @Insert
    suspend fun inserer(rappel: Rappel): Long

    @Insert
    suspend fun insererTous(rappels: List<Rappel>)

    @Update
    suspend fun mettreAJour(rappel: Rappel)

    @Delete
    suspend fun supprimer(rappel: Rappel)
}
