package com.fastek.notea.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fastek.notea.data.local.entity.Evenement
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface EvenementDao {

    @Insert
    suspend fun inserer(evenement: Evenement): Long

    @Update
    suspend fun mettreAJour(evenement: Evenement)

    @Delete
    suspend fun supprimer(evenement: Evenement)

    @Query("DELETE FROM evenement WHERE id = :id")
    suspend fun supprimerParId(id: Long)

    @Query("SELECT * FROM evenement WHERE eleveId = :eleveId ORDER BY date")
    fun observerEvenements(eleveId: Long): Flow<List<Evenement>>

    /** Utilisé par le rappel WorkManager : événements dans la fenêtre [debut, fin] pas encore notifiés. */
    @Query("SELECT * FROM evenement WHERE eleveId = :eleveId AND date BETWEEN :debut AND :fin AND notifie = 0")
    suspend fun getEvenementsAvenirNonNotifies(eleveId: Long, debut: LocalDate, fin: LocalDate): List<Evenement>
}
