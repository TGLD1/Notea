package com.fastek.notea.ui.parametres

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.data.repository.NoteaRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfilUiState(
    val chargement: Boolean = true,
    val eleveId: Long? = null,
    val nom: String = "",
    val prenom: String = "",
    val matricule: String = "",
    val classe: String = "",
    val typeEtablissement: TypeEtablissement = TypeEtablissement.PUBLIC,
    val anneeScolaire: String = "",
    val objectifAnnuel: Double = 12.0,
    val dateNaissance: String = "",
    val lieuNaissance: String = "",
    val effectif: String = "",
    val aptitudeEps: String = "",
    val redoublant: String = ""
)

class ProfilViewModel(private val repository: NoteaRepository) : ViewModel() {

    val uiState: StateFlow<ProfilUiState> = repository.observerProfil()
        .map { eleve ->
            if (eleve == null) {
                ProfilUiState(chargement = false)
            } else {
                ProfilUiState(
                    chargement = false,
                    eleveId = eleve.id,
                    nom = eleve.nom,
                    prenom = eleve.prenom,
                    matricule = eleve.matricule,
                    classe = eleve.classe,
                    typeEtablissement = eleve.typeEtablissement,
                    anneeScolaire = eleve.anneeScolaire,
                    objectifAnnuel = eleve.objectifAnnuel,
                    dateNaissance = eleve.dateNaissance,
                    lieuNaissance = eleve.lieuNaissance,
                    effectif = eleve.effectif,
                    aptitudeEps = eleve.aptitudeEps,
                    redoublant = eleve.redoublant
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilUiState())

    fun onNomChange(eleveId: Long, valeur: String) {
        viewModelScope.launch { repository.mettreAJourNom(eleveId, valeur) }
    }

    fun onPrenomChange(eleveId: Long, valeur: String) {
        viewModelScope.launch { repository.mettreAJourPrenom(eleveId, valeur) }
    }

    fun onMatriculeChange(eleveId: Long, valeur: String) {
        viewModelScope.launch { repository.mettreAJourMatricule(eleveId, valeur) }
    }

    fun onDateNaissanceChange(eleveId: Long, valeur: String) {
        viewModelScope.launch { repository.mettreAJourDateNaissance(eleveId, valeur) }
    }

    fun onLieuNaissanceChange(eleveId: Long, valeur: String) {
        viewModelScope.launch { repository.mettreAJourLieuNaissance(eleveId, valeur) }
    }

    fun onEffectifChange(eleveId: Long, valeur: String) {
        viewModelScope.launch { repository.mettreAJourEffectif(eleveId, valeur) }
    }

    fun onAptitudeEpsChange(eleveId: Long, valeur: String) {
        viewModelScope.launch { repository.mettreAJourAptitudeEps(eleveId, valeur) }
    }

    fun onRedoublantChange(eleveId: Long, valeur: String) {
        viewModelScope.launch { repository.mettreAJourRedoublant(eleveId, valeur) }
    }
}
