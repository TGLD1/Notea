package com.fastek.notea.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * Un événement du planning (devoir à venir, interrogation, etc.), optionnellement
 * associé à une matière. [notifie] évite de renvoyer deux fois la même notification.
 */
@Entity(
    tableName = "evenement",
    foreignKeys = [
        ForeignKey(
            entity = Eleve::class,
            parentColumns = ["id"],
            childColumns = ["eleveId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Matiere::class,
            parentColumns = ["id"],
            childColumns = ["matiereId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("eleveId"), Index("matiereId")]
)
data class Evenement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eleveId: Long,
    val matiereId: Long?,
    val titre: String,
    val date: LocalDate,
    val notifie: Boolean = false
)
