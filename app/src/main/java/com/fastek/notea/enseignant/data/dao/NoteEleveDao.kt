package com.fastek.notea.enseignant.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.fastek.notea.enseignant.data.entity.NoteEleve
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteEleveDao {
    @Query(
        "SELECT n.* FROM note_eleve n INNER JOIN evaluation e ON e.id = n.evaluationId " +
            "WHERE e.periodeId = :periodeId"
    )
    fun observerNotesPeriode(periodeId: Long): Flow<List<NoteEleve>>

    @Query(
        "SELECT n.* FROM note_eleve n INNER JOIN evaluation e ON e.id = n.evaluationId " +
            "WHERE e.classeId = :classeId"
    )
    suspend fun obtenirNotesClasse(classeId: Long): List<NoteEleve>

    /** Remplace la note existante grâce à l'index unique (évaluation, élève). */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enregistrer(note: NoteEleve)

    @Query("DELETE FROM note_eleve WHERE evaluationId = :evaluationId AND inscritId = :inscritId")
    suspend fun effacer(evaluationId: Long, inscritId: Long)
}
