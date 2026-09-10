package com.fastek.notea.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.fastek.notea.data.local.entity.Note
import com.fastek.notea.data.local.entity.TypeNote
import com.fastek.notea.data.repository.NoteaRepository
import com.fastek.notea.domain.calcul.MoyenneCalculator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class NotesUiState(
    val matiereNom: String = "",
    val notes: List<Note> = emptyList(),
    val moyenne: Double? = null,
    val nouvelleValeur: String = "",
    val nouveauType: TypeNote = TypeNote.DEVOIR,
    val erreur: String? = null
)

private data class SaisieState(
    val valeur: String = "",
    val type: TypeNote = TypeNote.DEVOIR,
    val erreur: String? = null
)

class NotesViewModel(
    private val repository: NoteaRepository,
    private val matiereId: Long,
    private val periodeId: Long
) : ViewModel() {

    private val _saisie = MutableStateFlow(SaisieState())
    private val _matiereNom = MutableStateFlow("")

    init {
        viewModelScope.launch {
            _matiereNom.value = repository.getMatiere(matiereId)?.nom ?: ""
        }
    }

    val uiState: StateFlow<NotesUiState> = combine(
        repository.observerNotes(matiereId, periodeId),
        _saisie,
        _matiereNom
    ) { notes, saisie, nom ->
        val notesTriees = notes.sortedByDescending { it.date }
        NotesUiState(
            matiereNom = nom,
            notes = notesTriees,
            moyenne = MoyenneCalculator.moyenneMatiere(notes),
            nouvelleValeur = saisie.valeur,
            nouveauType = saisie.type,
            erreur = saisie.erreur
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotesUiState())

    fun onValeurChange(valeur: String) {
        if (valeur.isEmpty() || valeur.matches(Regex("^\\d{0,2}([.,]\\d{0,2})?$"))) {
            _saisie.update { it.copy(valeur = valeur, erreur = null) }
        }
    }

    fun onTypeChange(type: TypeNote) {
        _saisie.update { it.copy(type = type) }
    }

    fun ajouterNote() {
        val valeur = _saisie.value.valeur.replace(',', '.').toDoubleOrNull()
        if (valeur == null || valeur < 0.0 || valeur > 20.0) {
            _saisie.update { it.copy(erreur = "Note invalide (0 à 20)") }
            return
        }
        viewModelScope.launch {
            repository.ajouterNote(
                Note(
                    matiereId = matiereId,
                    periodeId = periodeId,
                    type = _saisie.value.type,
                    valeur = valeur,
                    date = LocalDate.now()
                )
            )
            _saisie.update { it.copy(valeur = "", erreur = null) }
        }
    }

    fun supprimerNote(note: Note) {
        viewModelScope.launch { repository.supprimerNote(note) }
    }
}

/** Factory dédiée : NotesViewModel a besoin de matiereId/periodeId en plus du repository. */
class NotesViewModelFactory(
    private val repository: NoteaRepository,
    private val matiereId: Long,
    private val periodeId: Long
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        NotesViewModel(repository, matiereId, periodeId) as T
}
