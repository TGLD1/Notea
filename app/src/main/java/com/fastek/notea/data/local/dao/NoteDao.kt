package com.fastek.notea.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.fastek.notea.data.local.entity.Note
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Insert
    suspend fun inserer(note: Note): Long

    @Update
    suspend fun mettreAJour(note: Note)

    @Delete
    suspend fun supprimer(note: Note)

    /** Toutes les notes d'une matière pour une période donnée — utilisées par MoyenneCalculator. */
    @Query("SELECT * FROM note WHERE matiereId = :matiereId AND periodeId = :periodeId")
    fun observerNotes(matiereId: Long, periodeId: Long): Flow<List<Note>>

    @Query("SELECT * FROM note WHERE matiereId = :matiereId AND periodeId = :periodeId")
    suspend fun getNotes(matiereId: Long, periodeId: Long): List<Note>
}
