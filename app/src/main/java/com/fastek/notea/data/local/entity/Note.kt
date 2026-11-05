package com.fastek.notea.data.local.entity

import java.time.LocalDate

/** Une note individuelle (devoir ou interrogation), toujours sur 20, telle que la lit le moteur de calcul. */
data class Note(
    val id: Long = 0,
    val matiereId: Long,
    val periodeId: Long,
    val type: TypeNote,
    val valeur: Double,
    val date: LocalDate
)
