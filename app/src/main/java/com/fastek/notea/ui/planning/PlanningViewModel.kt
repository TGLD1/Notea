package com.fastek.notea.ui.planning

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastek.notea.data.local.entity.Evenement
import com.fastek.notea.data.local.entity.Matiere
import com.fastek.notea.data.repository.NoteaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

private val FORMAT_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy")

data class EvenementUi(
    val id: Long,
    val titre: String,
    val date: LocalDate,
    val matiereNom: String?
) {
    val dateAffichee: String get() = date.format(FORMAT_DATE)
}

private data class FormState(
    val titre: String = "",
    val dateTexte: String = LocalDate.now().format(FORMAT_DATE),
    val matiereId: Long? = null,
    val erreur: String? = null
)

data class PlanningUiState(
    val chargement: Boolean = true,
    val eleveId: Long? = null,
    val evenements: List<EvenementUi> = emptyList(),
    val matieresDisponibles: List<Matiere> = emptyList(),
    val titre: String = "",
    val dateTexte: String = "",
    val matiereId: Long? = null,
    val erreur: String? = null
)

class PlanningViewModel(private val repository: NoteaRepository) : ViewModel() {

    private val _form = MutableStateFlow(FormState())

    val uiState: StateFlow<PlanningUiState> = repository.observerProfil()
        .flatMapLatest { profil ->
            if (profil == null) {
                flowOf(PlanningUiState(chargement = false))
            } else {
                combine(
                    repository.observerEvenements(profil.id),
                    repository.observerMatieres(profil.id),
                    _form
                ) { evenements, matieres, form ->
                    val matieresParId = matieres.associateBy { it.id }
                    PlanningUiState(
                        chargement = false,
                        eleveId = profil.id,
                        evenements = evenements.sortedBy { it.date }.map { ev ->
                            EvenementUi(
                                id = ev.id,
                                titre = ev.titre,
                                date = ev.date,
                                matiereNom = ev.matiereId?.let { matieresParId[it]?.nom }
                            )
                        },
                        matieresDisponibles = matieres,
                        titre = form.titre,
                        dateTexte = form.dateTexte,
                        matiereId = form.matiereId,
                        erreur = form.erreur
                    )
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PlanningUiState())

    fun onTitreChange(valeur: String) = _form.update { it.copy(titre = valeur, erreur = null) }
    fun onDateTexteChange(valeur: String) = _form.update { it.copy(dateTexte = valeur, erreur = null) }
    fun onMatiereChange(matiereId: Long?) = _form.update { it.copy(matiereId = matiereId) }

    fun ajouterEvenement(eleveId: Long) {
        val form = _form.value
        if (form.titre.isBlank()) {
            _form.update { it.copy(erreur = "Le titre est obligatoire") }
            return
        }
        val date = try {
            LocalDate.parse(form.dateTexte, FORMAT_DATE)
        } catch (e: DateTimeParseException) {
            _form.update { it.copy(erreur = "Date invalide (format JJ/MM/AAAA)") }
            return
        }
        viewModelScope.launch {
            repository.ajouterEvenement(
                Evenement(eleveId = eleveId, matiereId = form.matiereId, titre = form.titre.trim(), date = date)
            )
            _form.value = FormState()
        }
    }

    fun supprimerEvenement(evenementUi: EvenementUi) {
        viewModelScope.launch { repository.supprimerEvenementParId(evenementUi.id) }
    }
}
