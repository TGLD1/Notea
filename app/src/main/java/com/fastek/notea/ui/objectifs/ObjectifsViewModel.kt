package com.fastek.notea.ui.objectifs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fastek.notea.data.repository.NoteaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PeriodeEditable(
    val id: Long,
    val numero: Int,
    val objectif: String
)

data class ObjectifsUiState(
    val chargement: Boolean = true,
    val eleveId: Long? = null,
    val objectifAnnuel: String = "",
    val periodes: List<PeriodeEditable> = emptyList()
)

/** Valeurs en cours de frappe — priment sur ce qui vient de la base tant qu'elles existent. */
private data class Brouillon(
    val objectifAnnuel: String? = null,
    val objectifsPeriode: Map<Long, String> = emptyMap()
)

private val REGEX_NOTE = Regex("^\\d{0,2}([.,]\\d{0,2})?$")

class ObjectifsViewModel(private val repository: NoteaRepository) : ViewModel() {

    private val _brouillon = MutableStateFlow(Brouillon())

    val uiState: StateFlow<ObjectifsUiState> = repository.observerProfil()
        .flatMapLatest { profil ->
            if (profil == null) {
                flowOf(ObjectifsUiState(chargement = false))
            } else {
                repository.observerPeriodes(profil.id).flatMapLatest { periodes ->
                    val periodesTriees = periodes.sortedBy { it.numero }
                    _brouillon.map { brouillon ->
                        ObjectifsUiState(
                            chargement = false,
                            eleveId = profil.id,
                            objectifAnnuel = brouillon.objectifAnnuel ?: profil.objectifAnnuel.toString(),
                            periodes = periodesTriees.map { periode ->
                                PeriodeEditable(
                                    id = periode.id,
                                    numero = periode.numero,
                                    objectif = brouillon.objectifsPeriode[periode.id]
                                        ?: periode.objectifCible.toString()
                                )
                            }
                        )
                    }
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ObjectifsUiState())

    fun onObjectifAnnuelChange(eleveId: Long, valeur: String) {
        if (valeur.isNotEmpty() && !valeur.matches(REGEX_NOTE)) return
        _brouillon.update { it.copy(objectifAnnuel = valeur) }
        valeurValide(valeur)?.let { v ->
            viewModelScope.launch { repository.mettreAJourObjectifAnnuel(eleveId, v) }
        }
    }

    fun onObjectifPeriodeChange(periodeId: Long, valeur: String) {
        if (valeur.isNotEmpty() && !valeur.matches(REGEX_NOTE)) return
        _brouillon.update { it.copy(objectifsPeriode = it.objectifsPeriode + (periodeId to valeur)) }
        valeurValide(valeur)?.let { v ->
            viewModelScope.launch { repository.definirObjectifPeriode(periodeId, v) }
        }
    }

    private fun valeurValide(valeur: String): Double? =
        valeur.replace(',', '.').toDoubleOrNull()?.takeIf { it in 0.0..20.0 }
}
