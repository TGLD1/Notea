package com.fastek.notea.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Une période de l'année scolaire (semestre 1/2 pour le public, trimestre 1/2/3 pour le privé).
 *
 * @param numero 1, 2 (public) ou 1, 2, 3 (privé)
 * @param objectifCible objectif de moyenne pour cette période, sur 20 (12.0 par défaut)
 */
@Entity(
    tableName = "periode",
    foreignKeys = [
        ForeignKey(
            entity = Eleve::class,
            parentColumns = ["id"],
            childColumns = ["eleveId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("eleveId")]
)
data class Periode(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eleveId: Long,
    val numero: Int,
    val objectifCible: Double = 12.0
)
