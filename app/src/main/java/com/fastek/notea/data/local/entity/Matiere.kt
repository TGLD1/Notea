package com.fastek.notea.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Une matière choisie par l'élève, avec son coefficient.
 * Le coefficient est saisi librement (pas de table officielle) car il varie selon la classe/série.
 */
@Entity(
    tableName = "matiere",
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
data class Matiere(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val eleveId: Long,
    val nom: String,
    val coefficient: Int
)
