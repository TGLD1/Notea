package com.fastek.notea.enseignant.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Un élève inscrit dans une classe. */
@Entity(
    tableName = "inscrit",
    foreignKeys = [
        ForeignKey(
            entity = Classe::class,
            parentColumns = ["id"],
            childColumns = ["classeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("classeId")]
)
data class Inscrit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classeId: Long,
    val nom: String,
    val prenom: String,
    val matricule: String = ""
)
