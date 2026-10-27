package com.fastek.notea.enseignant.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Note d'un élève à une évaluation (sur 20). Une case vide correspond à l'absence de ligne :
 * l'élève n'est alors ni pénalisé ni compté pour cette évaluation.
 */
@Entity(
    tableName = "note_eleve",
    foreignKeys = [
        ForeignKey(
            entity = Evaluation::class,
            parentColumns = ["id"],
            childColumns = ["evaluationId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Inscrit::class,
            parentColumns = ["id"],
            childColumns = ["inscritId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["evaluationId", "inscritId"], unique = true),
        Index("inscritId")
    ]
)
data class NoteEleve(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val evaluationId: Long,
    val inscritId: Long,
    val valeur: Double
)
