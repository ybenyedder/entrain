package fr.webtvmedia.entrain.ui.results

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.webtvmedia.entrain.data.AppContainer
import fr.webtvmedia.entrain.domain.model.Journey
import fr.webtvmedia.entrain.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

sealed interface ResultsState {
    data object Loading : ResultsState
    data class Ready(
        val journeys: List<Journey>,
        val fromName: String,
        val toName: String,
        val date: LocalDate,
        val timeSec: Int,
        val refreshedAt: Long,
    ) : ResultsState
    data object Empty : ResultsState
    data class Error(val message: String) : ResultsState
}

class ResultsViewModel(
    private val container: AppContainer,
    savedState: SavedStateHandle,
) : ViewModel() {

    private val fromId: String = savedState.get<String>("from") ?: ""
    private val toId: String = savedState.get<String>("to") ?: ""
    private val ymd: Int = savedState.get<String>("ymd")?.toIntOrNull() ?: 0
    private val timeSecArg: Int = savedState.get<String>("sec")?.toIntOrNull() ?: 0
    private val arriveBy: Boolean = (savedState.get<String>("ab")?.toIntOrNull() ?: 0) == 1

    private val _state = MutableStateFlow<ResultsState>(ResultsState.Loading)
    val state: StateFlow<ResultsState> = _state.asStateFlow()

    private var date: LocalDate =
        if (ymd > 0) TimeUtils.parseYmd(ymd) else LocalDate.now(TimeUtils.PARIS)

    init {
        load()
    }

    fun load() {
        _state.value = ResultsState.Loading
        viewModelScope.launch {
            try {
                container.ensureIndex()
                container.rtRepo.refresh()
                val from = container.station(fromId)
                val to = container.station(toId)
                if (from == null || to == null) {
                    _state.value = ResultsState.Error("Gare introuvable")
                    return@launch
                }
                val time = if (timeSecArg >= 0) timeSecArg else {
                    val t = java.time.LocalTime.now(TimeUtils.PARIS)
                    t.hour * 3600 + t.minute * 60
                }
                val journeys = if (arriveBy) {
                    container.searchJourneysArriveBy(fromId, toId, date, time)
                } else {
                    container.searchJourneys(fromId, toId, date, time)
                }
                _state.value = if (journeys.isEmpty()) ResultsState.Empty else {
                    ResultsState.Ready(journeys, from.name, to.name, date, time, System.currentTimeMillis())
                }
            } catch (e: Exception) {
                _state.value = ResultsState.Error(e.message ?: "Erreur inconnue")
            }
        }
    }

    /** Re-applique le temps réel aux résultats déjà calculés. */
    fun refreshRealtime() {
        if (_state.value !is ResultsState.Ready) {
            load()
            return
        }
        val cur = _state.value as ResultsState.Ready
        _state.value = ResultsState.Loading
        viewModelScope.launch {
            container.rtRepo.refresh(force = true)
            val journeys = if (arriveBy) {
                container.searchJourneysArriveBy(fromId, toId, date, cur.timeSec)
            } else {
                container.searchJourneys(fromId, toId, date, cur.timeSec)
            }
            _state.value = if (journeys.isEmpty()) ResultsState.Empty else {
                ResultsState.Ready(
                    journeys, cur.fromName, cur.toName, cur.date, cur.timeSec,
                    System.currentTimeMillis(),
                )
            }
        }
    }
}
