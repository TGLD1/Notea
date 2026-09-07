package com.fastek.notea.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fastek.notea.data.local.entity.Periode
import kotlinx.coroutines.flow.Flow

@Dao
interface PeriodeDao {

    @Insert
    suspend fun inserer(periode: Periode): Long

    @Update
    suspend fun mettreAJour(periode: Periode)

    @Query("SELECT * FROM periode WHERE eleveId = :eleveId ORDER BY numero")
    fun observerPeriodes(eleveId: Long): Flow<List<Periode>>

    @Query("SELECT * FROM periode WHERE eleveId = :eleveId AND numero = :numero LIMIT 1")
    suspend fun getPeriode(eleveId: Long, numero: Int): Periode?
}
