package com.fastek.notea.enseignant.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastek.notea.diagnostic.Journal
import com.fastek.notea.enseignant.data.entity.Classe
import com.fastek.notea.enseignant.data.entity.Creneau
import com.fastek.notea.enseignant.data.entity.Rappel
import com.fastek.notea.enseignant.data.repository.EnseignantRepository
import com.fastek.notea.enseignant.domain.EmploiDuTemps
import com.fastek.notea.enseignant.rappel.RappelScheduler
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Emploi du temps du professeur et rappels de cours. */
class EmploiViewModel(private val repo: EnseignantRepository, private val contexte: Context) : ViewModel() {

    private val partage = SharingStarted.WhileSubscribed(5000)

    val creneaux: StateFlow<List<Creneau>> = repo.observerCreneaux().stateIn(viewModelScope, partage, emptyList())

    val rappels: StateFlow<List<Rappel>> = repo.observerRappels().stateIn(viewModelScope, partage, emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val classes: StateFlow<List<Classe>> = repo.observerProfesseur()
        .flatMapLatest { p -> if (p == null) flowOf(emptyList()) else repo.observerClasses(p.id) }
        .stateIn(viewModelScope, partage, emptyList())

    /** Enregistre un cours (nouveau si [existant] est nul). [apres] reçoit le message d'erreur, ou null si réussi. */
    fun enregistrerCours(
        existant: Creneau?, classeId: Long, jour: Int, debut: Int, fin: Int, salle: String,
        apres: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val erreur = try {
                val message = EmploiDuTemps.verifierCours(jour, debut, fin, repo.obtenirCreneaux(), existant?.id)
                    ?: existant?.let { cours ->
                        repo.rappelsDuCreneau(cours.id).firstOrNull { it.minutes < debut || it.minutes > fin }?.let {
                            "Le rappel de ${EmploiDuTemps.heure(it.minutes)} tomberait hors des heures du cours. " +
                                "Supprime-le ou change ses heures d'abord."
                        }
                    }
                if (message != null) {
                    message
                } else {
                    if (existant == null) {
                        repo.creerCreneau(Creneau(classeId = classeId, jour = jour, debut = debut, fin = fin, salle = salle.trim()))
                    } else {
                        repo.mettreAJourCreneau(
                            existant.copy(classeId = classeId, jour = jour, debut = debut, fin = fin, salle = salle.trim())
                        )
                    }
                    RappelScheduler.replanifierTout(contexte)
                    Journal.ok("emploi.cours")
                    null
                }
            } catch (e: Exception) {
                Journal.erreur("emploi.cours", e)
                "Action impossible (${e.message ?: e.javaClass.simpleName})"
            }
            apres(erreur)
        }
    }

    fun supprimerCours(creneau: Creneau) {
        viewModelScope.launch {
            try {
                repo.supprimerCreneau(creneau).forEach { RappelScheduler.annuler(contexte, it) }
                Journal.ok("emploi.suppression")
            } catch (e: Exception) {
                Journal.erreur("emploi.suppression", e)
            }
        }
    }

    /** Ajoute un rappel pendant un cours. [apres] reçoit le message d'erreur, ou null si réussi. */
    fun ajouterRappel(
        creneau: Creneau, texte: String, minutes: Int, sonore: Boolean, hebdomadaire: Boolean,
        apres: (String?) -> Unit
    ) {
        viewModelScope.launch {
            val erreur = when {
                texte.isBlank() -> "Écris ce dont tu veux te souvenir."
                minutes < creneau.debut || minutes > creneau.fin ->
                    "Le rappel doit tomber pendant le cours (${EmploiDuTemps.heure(creneau.debut)} à ${EmploiDuTemps.heure(creneau.fin)})."
                else -> try {
                    val rappel = Rappel(
                        creneauId = creneau.id, texte = texte.trim(), minutes = minutes,
                        sonore = sonore, hebdomadaire = hebdomadaire
                    )
                    val id = repo.creerRappel(rappel)
                    RappelScheduler.planifier(contexte, rappel.copy(id = id), creneau)
                    Journal.ok("rappel.creation")
                    null
                } catch (e: Exception) {
                    Journal.erreur("rappel.creation", e)
                    "Action impossible (${e.message ?: e.javaClass.simpleName})"
                }
            }
            apres(erreur)
        }
    }

    fun supprimerRappel(rappel: Rappel) {
        viewModelScope.launch {
            try {
                repo.supprimerRappel(rappel)
                RappelScheduler.annuler(contexte, rappel.id)
            } catch (e: Exception) {
                Journal.erreur("rappel.suppression", e)
            }
        }
    }

    /** Active ou désactive un rappel (un rappel « une seule fois » devient inactif après sa sonnerie). */
    fun basculerRappel(rappel: Rappel) {
        viewModelScope.launch {
            try {
                val maj = rappel.copy(actif = !rappel.actif)
                repo.mettreAJourRappel(maj)
                val creneau = repo.obtenirCreneau(rappel.creneauId)
                if (maj.actif && creneau != null) RappelScheduler.planifier(contexte, maj, creneau)
                else RappelScheduler.annuler(contexte, rappel.id)
            } catch (e: Exception) {
                Journal.erreur("rappel.bascule", e)
            }
        }
    }
}
