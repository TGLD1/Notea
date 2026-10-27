package com.fastek.notea.enseignant.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fastek.notea.data.local.entity.TypeEtablissement

/** Profil du professeur (une seule ligne) : sa matière et le type d'établissement. */
@Entity(tableName = "professeur")
data class Professeur(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val prenom: String,
    val matiere: String,
    val typeEtablissement: TypeEtablissement,
    val anneeScolaire: String,
    val etablissement: String = ""
)
