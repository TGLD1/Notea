package com.fastek.notea.enseignant.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Une classe du professeur (ex. « 3ème A »). Le coefficient est celui de la matière dans cette classe. */
@Entity(
    tableName = "classe",
    foreignKeys = [
        ForeignKey(
            entity = Professeur::class,
            parentColumns = ["id"],
            childColumns = ["professeurId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("professeurId")]
)
data class Classe(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val professeurId: Long,
    val nom: String,
    val coefficient: Int = 1,
    val ordre: Int = 0
)
