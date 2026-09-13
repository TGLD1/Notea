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

    @Query("UPDATE eleve SET objectifAnnuel = :objectif WHERE id = :eleveId")
    suspend fun mettreAJourObjectifAnnuel(eleveId: Long, objectif: Double)

    @Query("UPDATE eleve SET nom = :nom WHERE id = :eleveId")
    suspend fun mettreAJourNom(eleveId: Long, nom: String)

    @Query("UPDATE eleve SET prenom = :prenom WHERE id = :eleveId")
    suspend fun mettreAJourPrenom(eleveId: Long, prenom: String)

    @Query("UPDATE eleve SET matricule = :matricule WHERE id = :eleveId")
    suspend fun mettreAJourMatricule(eleveId: Long, matricule: String)
}
