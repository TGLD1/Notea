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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/** Une matière proposée à l'onboarding, avec son état de sélection et son coefficient. */
data class MatiereChoix(
    val nom: String,
    val selectionnee: Boolean = false,
    val coefficient: Int = 1
)

data class OnboardingUiState(
    val nom: String = "",
    val prenom: String = "",
    val classe: String = "",
    val typeEtablissement: TypeEtablissement = TypeEtablissement.PUBLIC,
    val matieres: List<MatiereChoix> = MatieresReference.LISTE.map { MatiereChoix(nom = it) },
    val enCours: Boolean = false,
    val profilCreeAvecId: Long? = null,
    val erreur: String? = null
) {
    val peutValider: Boolean
        get() = nom.isNotBlank() && prenom.isNotBlank() && classe.isNotBlank() &&
            matieres.any { it.selectionnee }
}

class OnboardingViewModel(private val repository: NoteaRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onNomChange(valeur: String) = _uiState.update { it.copy(nom = valeur) }
    fun onPrenomChange(valeur: String) = _uiState.update { it.copy(prenom = valeur) }
    fun onClasseChange(valeur: String) = _uiState.update { it.copy(classe = valeur) }
    fun onTypeEtablissementChange(type: TypeEtablissement) =
        _uiState.update { it.copy(typeEtablissement = type) }

    fun onToggleMatiere(nom: String) = _uiState.update { etat ->
        etat.copy(matieres = etat.matieres.map {
            if (it.nom == nom) it.copy(selectionnee = !it.selectionnee) else it
        })
    }

    fun onCoefficientChange(nom: String, coefficient: Int) = _uiState.update { etat ->
        etat.copy(matieres = etat.matieres.map {
            if (it.nom == nom) it.copy(coefficient = coefficient) else it
        })
    }

    /** Crée le profil, ses périodes (2 ou 3 selon l'établissement) et ses matières. */
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
                        Matiere(eleveId = eleveId, nom = choix.nom, coefficient = choix.coefficient)
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
