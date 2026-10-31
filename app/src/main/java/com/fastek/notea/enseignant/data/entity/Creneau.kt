package com.fastek.notea.enseignant.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un cours de l'emploi du temps : une classe, un jour de semaine (1 = lundi … 7 = dimanche)
 * et des heures exprimées en minutes depuis minuit (480 = 8h00).
 */
@Entity(
    tableName = "creneau",
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
data class Creneau(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classeId: Long,
    val jour: Int,
    val debut: Int,
    val fin: Int,
    val salle: String = ""
)
