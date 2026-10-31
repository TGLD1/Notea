package com.fastek.notea.enseignant.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un rappel personnel pendant un cours : un texte, une heure comprise dans les heures du cours,
 * silencieux ou sonore, chaque semaine ou une seule fois (il devient alors inactif après sa sonnerie).
 */
@Entity(
    tableName = "rappel",
    foreignKeys = [
        ForeignKey(
            entity = Creneau::class,
            parentColumns = ["id"],
            childColumns = ["creneauId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("creneauId")]
)
data class Rappel(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val creneauId: Long,
    val texte: String,
    val minutes: Int,
    val sonore: Boolean,
    val hebdomadaire: Boolean,
    val actif: Boolean = true
)
