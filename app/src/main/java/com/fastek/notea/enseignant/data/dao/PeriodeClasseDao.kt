package com.fastek.notea.enseignant.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fastek.notea.enseignant.data.entity.PeriodeClasse
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodeClasseDao {
    @Query("SELECT * FROM periode_classe WHERE classeId = :classeId ORDER BY numero")
    fun observerPeriodes(classeId: Long): Flow<List<PeriodeClasse>>

    @Query("SELECT * FROM periode_classe WHERE classeId = :classeId ORDER BY numero")
    suspend fun obtenirPeriodes(classeId: Long): List<PeriodeClasse>

    @Query("SELECT * FROM periode_classe WHERE classeId = :classeId AND numero = :numero")
    suspend fun obtenir(classeId: Long, numero: Int): PeriodeClasse?

    @Insert
    suspend fun inserer(periode: PeriodeClasse): Long

    @Update
    suspend fun mettreAJour(periode: PeriodeClasse)

    @Query("SELECT * FROM periode_classe")
    suspend fun obtenirToutes(): List<PeriodeClasse>

    @Insert
    suspend fun insererToutes(periodes: List<PeriodeClasse>)

    @Query("UPDATE periode_classe SET terminee = 0")
    suspend fun reinitialiserTerminees()
}
