package com.fastek.notea.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fastek.notea.data.local.entity.Eleve
import kotlinx.coroutines.flow.Flow

@Dao
interface EleveDao {

    @Insert
    suspend fun inserer(eleve: Eleve): Long

    @Update
    suspend fun mettreAJour(eleve: Eleve)

    /** L'app est mono-utilisateur : on lit toujours le premier (et unique) profil. */
    @Query("SELECT * FROM eleve LIMIT 1")
    fun observerProfil(): Flow<Eleve?>

    @Query("SELECT * FROM eleve LIMIT 1")
    suspend fun getProfil(): Eleve?
}
