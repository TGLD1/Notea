package com.fastek.notea.enseignant.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fastek.notea.enseignant.data.entity.Creneau
import kotlinx.coroutines.flow.Flow

@Dao
interface CreneauDao {
    @Query("SELECT * FROM creneau ORDER BY jour, debut")
    fun observerTous(): Flow<List<Creneau>>

    @Query("SELECT * FROM creneau ORDER BY jour, debut")
    suspend fun obtenirTous(): List<Creneau>

    @Query("SELECT * FROM creneau WHERE id = :id")
    suspend fun obtenir(id: Long): Creneau?

    @Insert
    suspend fun inserer(creneau: Creneau): Long

    @Insert
    suspend fun insererTous(creneaux: List<Creneau>)

    @Update
    suspend fun mettreAJour(creneau: Creneau)

    @Delete
    suspend fun supprimer(creneau: Creneau)
}
