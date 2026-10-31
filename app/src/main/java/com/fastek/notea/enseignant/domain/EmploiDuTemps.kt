package com.fastek.notea.enseignant.domain

import com.fastek.notea.enseignant.data.entity.Creneau
import java.time.LocalDateTime
import java.time.LocalTime

data class ProchainCours(val creneau: Creneau, val debut: LocalDateTime, val enCours: Boolean)

/** Règles de l'emploi du temps : jours (1 = lundi … 7 = dimanche), heures en minutes depuis minuit. */
object EmploiDuTemps {

    val JOURS = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
    val JOURS_COURTS = listOf("Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim")

    /** 480 donne « 8h », 510 donne « 8h30 », 485 donne « 8h05 ». */
    fun heure(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0) "${h}h" else "${h}h${if (m < 10) "0$m" else "$m"}"
    }

    /** Prochain moment, strictement après [maintenant], où tombe ce jour de semaine à cette heure. */
    fun prochaineOccurrence(jour: Int, minutes: Int, maintenant: LocalDateTime): LocalDateTime {
        require(jour in 1..7) { "Jour invalide : $jour" }
        var candidat = LocalDateTime.of(maintenant.toLocalDate(), LocalTime.of(minutes / 60, minutes % 60))
        while (candidat.dayOfWeek.value != jour || !candidat.isAfter(maintenant)) {
            candidat = candidat.plusDays(1)
        }
        return candidat
    }

    fun chevauchent(debutA: Int, finA: Int, debutB: Int, finB: Int): Boolean = debutA < finB && debutB < finA

    /**
     * Vérifie un cours avant enregistrement. Retourne le message d'erreur à montrer, ou null si le cours
     * est valide. [ignorerId] est le cours en cours de modification.
     */
    fun verifierCours(jour: Int, debut: Int, fin: Int, existants: List<Creneau>, ignorerId: Long?): String? {
        if (jour !in 1..7) return "Choisis un jour."
        if (debut !in 0..1439 || fin !in 1..1439) return "Les heures doivent être comprises entre 0h et 23h59."
        if (fin <= debut) return "L'heure de fin doit être après l'heure de début."
        val autre = existants.firstOrNull {
            it.id != ignorerId && it.jour == jour && chevauchent(debut, fin, it.debut, it.fin)
        }
        if (autre != null) {
            return "Ce cours chevauche un autre cours du ${JOURS[jour - 1].lowercase()} (${heure(autre.debut)} à ${heure(autre.fin)})."
        }
        return null
    }

    /** Le cours en cours s'il y en a un, sinon le prochain à venir dans la semaine. */
    fun prochainCours(creneaux: List<Creneau>, maintenant: LocalDateTime): ProchainCours? {
        val minutesActuelles = maintenant.hour * 60 + maintenant.minute
        val enCours = creneaux.firstOrNull {
            it.jour == maintenant.dayOfWeek.value && minutesActuelles >= it.debut && minutesActuelles < it.fin
        }
        if (enCours != null) {
            val debut = LocalDateTime.of(maintenant.toLocalDate(), LocalTime.of(enCours.debut / 60, enCours.debut % 60))
            return ProchainCours(enCours, debut, true)
        }
        return creneaux
            .map { ProchainCours(it, prochaineOccurrence(it.jour, it.debut, maintenant), false) }
            .minByOrNull { it.debut }
    }
}
