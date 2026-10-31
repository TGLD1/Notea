package com.fastek.notea.enseignant.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fastek.notea.enseignant.data.entity.Classe
import kotlinx.coroutines.flow.Flow

@Dao
interface ClasseDao {
    @Query("SELECT * FROM classe WHERE professeurId = :professeurId ORDER BY ordre, id")
    fun observerClasses(professeurId: Long): Flow<List<Classe>>

    @Query("SELECT * FROM classe WHERE professeurId = :professeurId ORDER BY ordre, id")
    suspend fun obtenirClasses(professeurId: Long): List<Classe>

    @Query("SELECT * FROM classe WHERE id = :id")
    suspend fun obtenir(id: Long): Classe?

    @Insert
    suspend fun inserer(classe: Classe): Long

    @Update
    suspend fun mettreAJour(classe: Classe)

    @Delete
    suspend fun supprimer(classe: Classe)

    @Query("SELECT * FROM classe")
    suspend fun obtenirToutes(): List<Classe>

    @Insert
    suspend fun insererToutes(classes: List<Classe>)

    /** Supprime toutes les classes ; élèves, notes, cours et rappels partent en cascade. */
    @Query("DELETE FROM classe")
    suspend fun toutSupprimer()
}
