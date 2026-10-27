package com.fastek.notea.enseignant.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Période d'une classe (semestre ou trimestre). Chaque classe se clôture indépendamment. */
@Entity(
    tableName = "periode_classe",
    foreignKeys = [
        ForeignKey(
            entity = Classe::class,
            parentColumns = ["id"],
            childColumns = ["classeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["classeId", "numero"], unique = true)]
)
data class PeriodeClasse(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classeId: Long,
    val numero: Int,
    val terminee: Boolean = false
)
