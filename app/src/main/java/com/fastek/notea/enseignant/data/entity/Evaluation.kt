package com.fastek.notea.enseignant.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.fastek.notea.data.local.entity.TypeNote
import java.time.LocalDate

/** Une colonne de la grille de notes : une interrogation ou un devoir, pour une classe et une période. */
@Entity(
    tableName = "evaluation",
    foreignKeys = [
        ForeignKey(
            entity = Classe::class,
            parentColumns = ["id"],
            childColumns = ["classeId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PeriodeClasse::class,
            parentColumns = ["id"],
            childColumns = ["periodeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("classeId"), Index("periodeId")]
)
data class Evaluation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classeId: Long,
    val periodeId: Long,
    val type: TypeNote,
    val intitule: String = "",
    val date: LocalDate
)
