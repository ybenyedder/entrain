package fr.webtvmedia.entrain.ui.departures

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.webtvmedia.entrain.data.AppContainer
import fr.webtvmedia.entrain.domain.model.Departure
import fr.webtvmedia.entrain.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DeparturesUiState(
    val stationName: String = "",
    val loading: Boolean = true,
    val departures: List<Departure> = emptyList(),
    val arrivals: List<Departure> = emptyList(),
    val isFavorite: Boolean = false,
    val error: String? = null,
)

class DeparturesViewModel(
    private val container: AppContainer,
    savedState: SavedStateHandle,
) : ViewModel() {

    val stationId: String = savedState.get<String>("stationId") ?: ""

    private val _state = MutableStateFlow(DeparturesUiState())
    val state: StateFlow<DeparturesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val st = container.station(stationId)
            _state.value = _state.value.copy(stationName = st?.name ?: "Gare")
            container.db.userPrefsDao().isFavorite(stationId).collect { fav ->
                _state.value = _state.value.copy(isFavorite = fav)
            }
        }
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            try {
                container.rtRepo.refresh()
                val now = java.time.LocalTime.now(TimeUtils.PARIS)
                val sec = now.hour * 3600 + now.minute * 60
                val deps = container.departures(stationId, sec, 6 * 3600, arrivals = false)
                val arrs = container.departures(stationId, sec, 4 * 3600, arrivals = true)

                // temps réel Transilien/RER (PRIM) — si la clé est configurée
                var primDeps = emptyList<Departure>()
                try {
                    if (container.primRepo.isConfigured()) {
                        primDeps = fr.webtvmedia.entrain.data.prim.PrimRepository.toDepartures(
                            container.primRepo.departuresForStation(stationId),
                        )
                    }
                } catch (_: Exception) {
                }

                val today = java.time.LocalDate.now(TimeUtils.PARIS)
                fun effTime(d: Departure): Long =
                    d.estimatedEpoch ?: TimeUtils.naiveToEpoch(today.plusDays(d.dayOffset.toLong()), d.scheduledSec)
                val cutoff = System.currentTimeMillis() / 1000 - 120
                val merged = (deps + primDeps)
                    .distinctBy { it.tripId + "#" + it.stopPointId }
                    .filter { effTime(it) >= cutoff }
                    .sortedBy { effTime(it) }

                _state.value = _state.value.copy(
                    loading = false,
                    departures = merged,
                    arrivals = arrs.filter { effTime(it) >= cutoff }.sortedBy { effTime(it) },
                    error = null,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(loading = false, error = e.message)
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            val dao = container.db.userPrefsDao()
            if (_state.value.isFavorite) {
                dao.removeFavorite(stationId)
            } else {
                val count = dao.favoriteCount()
                dao.addFavorite(
                    fr.webtvmedia.entrain.data.db.FavoriteStationEntity(
                        stationId, count, System.currentTimeMillis(),
                    ),
                )
            }
        }
    }
}
