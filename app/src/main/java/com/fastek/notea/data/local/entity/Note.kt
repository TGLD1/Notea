package com.fastek.notea.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/** Une note individuelle (devoir ou interrogation), toujours saisie sur 20. */
@Entity(
    tableName = "note",
    foreignKeys = [
        ForeignKey(
            entity = Matiere::class,
            parentColumns = ["id"],
            childColumns = ["matiereId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Periode::class,
            parentColumns = ["id"],
            childColumns = ["periodeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("matiereId"), Index("periodeId")]
)
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val matiereId: Long,
    val periodeId: Long,
    val type: TypeNote,
    val valeur: Double,
    val date: LocalDate
)
