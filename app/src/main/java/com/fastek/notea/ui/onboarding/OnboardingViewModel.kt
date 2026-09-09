package com.fastek.notea.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastek.notea.data.local.entity.Eleve
import com.fastek.notea.data.local.entity.Matiere
import com.fastek.notea.data.local.entity.Periode
import com.fastek.notea.data.local.entity.TypeEtablissement
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.domain.calcul.MoyenneCalculator
import com.fastek.notea.domain.reference.MatieresReference
import com.fastek.notea.domain.reference.NiveauxReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class MatiereChoix(
    val nom: String,
    val selectionnee: Boolean = false,
    val coefficient: String = "1"
)

data class OnboardingUiState(
    val nom: String = "",
    val prenom: String = "",
    val niveau: String = "",
    val serie: String? = null,
    val typeEtablissement: TypeEtablissement = TypeEtablissement.PUBLIC,
    val matieres: List<MatiereChoix> = MatieresReference.LISTE.map { MatiereChoix(nom = it) },
    val enCours: Boolean = false,
    val profilCreeAvecId: Long? = null,
    val erreur: String? = null
) {
    val classe: String
        get() = if (serie != null) "$niveau $serie" else niveau

    val peutValider: Boolean
        get() = nom.isNotBlank() && prenom.isNotBlank() && niveau.isNotBlank() &&
            (!NiveauxReference.necessiteSerie(niveau) || serie != null) &&
            matieres.any { it.selectionnee }
}

class OnboardingViewModel(private val repository: NoteaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNomChange(valeur: String) = _uiState.update { it.copy(nom = valeur) }
    fun onPrenomChange(valeur: String) = _uiState.update { it.copy(prenom = valeur) }

    fun onNiveauChange(valeur: String) = _uiState.update {
        it.copy(niveau = valeur, serie = if (NiveauxReference.necessiteSerie(valeur)) it.serie else null)
    }

    fun onSerieChange(valeur: String) = _uiState.update { it.copy(serie = valeur) }

    fun onTypeEtablissementChange(type: TypeEtablissement) =
        _uiState.update { it.copy(typeEtablissement = type) }

    fun onToggleMatiere(nom: String) = _uiState.update { etat ->
        etat.copy(matieres = etat.matieres.map {
            if (it.nom == nom) it.copy(selectionnee = !it.selectionnee) else it
        })
    }

    fun onCoefficientChange(nom: String, valeur: String) = _uiState.update { etat ->
        etat.copy(matieres = etat.matieres.map {
            if (it.nom == nom) it.copy(coefficient = valeur) else it
        })
    }

    fun validerEtCreerProfil() {
        val etat = _uiState.value
        if (!etat.peutValider || etat.enCours) return

        _uiState.update { it.copy(enCours = true, erreur = null) }

        viewModelScope.launch {
            try {
                val eleve = Eleve(
                    nom = etat.nom.trim(),
                    prenom = etat.prenom.trim(),
                    classe = etat.classe.trim(),
                    typeEtablissement = etat.typeEtablissement,
                    anneeScolaire = anneeScolaireParDefaut(),
                    objectifAnnuel = MoyenneCalculator.OBJECTIF_PAR_DEFAUT
                )
                val eleveId = repository.creerProfil(eleve)

                repeat(etat.typeEtablissement.nombrePeriodes) { index ->
                    repository.creerPeriode(
                        Periode(
                            eleveId = eleveId,
                            numero = index + 1,
                            objectifCible = MoyenneCalculator.OBJECTIF_PAR_DEFAUT
                        )
                    )
                }

                etat.matieres.filter { it.selectionnee }.forEach { choix ->
                    repository.ajouterMatiere(
                        Matiere(
                            eleveId = eleveId,
                            nom = choix.nom,
                            coefficient = choix.coefficient.toIntOrNull() ?: 1
                        )
                    )
                }

                _uiState.update { it.copy(enCours = false, profilCreeAvecId = eleveId) }
            } catch (e: Exception) {
                _uiState.update { it.copy(enCours = false, erreur = e.message ?: "Erreur inconnue") }
            }
        }
    }

    private fun anneeScolaireParDefaut(): String {
        val aujourdHui = LocalDate.now()
        return if (aujourdHui.monthValue >= 9) {
            "${aujourdHui.year}-${aujourdHui.year + 1}"
        } else {
            "${aujourdHui.year - 1}-${aujourdHui.year}"
        }
    }
}
