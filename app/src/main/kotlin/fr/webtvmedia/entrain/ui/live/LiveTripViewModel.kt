package fr.webtvmedia.entrain.ui.live

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.webtvmedia.entrain.data.AppContainer
import fr.webtvmedia.entrain.domain.model.TrainCategory
import fr.webtvmedia.entrain.util.TimeUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LiveStop(
    val stopPointId: String,
    val stationName: String,
    val scheduledSec: Int,
    val estimatedEpoch: Long?,
    val skipped: Boolean,
    val passed: Boolean,
    val isNext: Boolean,
    val isMine: Boolean,
)

data class LiveTripUiState(
    val loading: Boolean = true,
    val active: Boolean = false,
    val trainNumber: String = "",
    val category: TrainCategory = TrainCategory.UNKNOWN,
    val destination: String = "",
    val delaySec: Int? = null,
    val cancelled: Boolean = false,
    val stops: List<LiveStop> = emptyList(),
    val nextStopName: String? = null,
    val arrivalEtaSec: Int? = null,
    val arrivalDelaySec: Int? = null,
    val finished: Boolean = false,
)

class LiveTripViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(LiveTripUiState())
    val state: StateFlow<LiveTripUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            container.liveTrip.collect { trip ->
                if (trip == null) {
                    _state.value = LiveTripUiState(loading = false, active = false)
                } else {
                    refresh()
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val trip = container.db.liveTripDao().get() ?: run {
                _state.value = LiveTripUiState(loading = false)
                return@launch
            }
            container.rtRepo.refresh()
            val rows = container.db.liveTripDao().tripStops(trip.tripId)
            if (rows.isEmpty()) {
                _state.value = LiveTripUiState(loading = false, active = true, trainNumber = "—")
                return@launch
            }
            val rt = container.rtRepo.trips[trip.tripId]
            val now = System.currentTimeMillis() / 1000
            val serviceDate = TimeUtils.parseYmd(trip.dateYmd)
            var nextIdx = -1
            val stops = rows.mapIndexed { i, r ->
                val upd = rt?.stopUpdates?.get(r.stopId)
                val est = upd?.departureEpoch ?: upd?.arrivalEpoch
                val schedEpoch = TimeUtils.naiveToEpoch(serviceDate, r.departureSec)
                val passed = (est ?: schedEpoch) + 60 < now
                if (!passed && nextIdx == -1) nextIdx = i
                LiveStop(
                    stopPointId = r.stopId,
                    stationName = r.parentName ?: r.stopName,
                    scheduledSec = r.departureSec,
                    estimatedEpoch = est,
                    skipped = upd?.skipped == true,
                    passed = passed,
                    isNext = false,
                    isMine = r.stopId == trip.alightStopPointId,
                )
            }.mapIndexed { i, s -> s.copy(isNext = i == nextIdx) }

            val boardRt = rt?.stopUpdates?.get(trip.boardStopPointId)
            val boardSched = TimeUtils.naiveToEpoch(
                serviceDate,
                rows.firstOrNull { it.stopId == trip.boardStopPointId }?.departureSec ?: 0,
            )
            val delaySec = boardRt?.departureEpoch?.let { (it - boardSched).toInt() }
                ?: boardRt?.arrivalEpoch?.let { (it - boardSched).toInt() }

            val mineIdx = stops.indexOfFirst { it.isMine }
            val mine = if (mineIdx >= 0) stops[mineIdx] else null
            val alightRt = rt?.stopUpdates?.get(trip.alightStopPointId)
            val alightSched = TimeUtils.naiveToEpoch(
                serviceDate,
                rows.firstOrNull { it.stopId == trip.alightStopPointId }?.arrivalSec ?: 0,
            )
            val arrDelay = alightRt?.arrivalEpoch?.let { (it - alightSched).toInt() }

            _state.value = LiveTripUiState(
                loading = false,
                active = true,
                trainNumber = container.trainNumberOf(trip.tripId),
                category = TrainCategory.fromStopPointId(rows.first().stopId),
                destination = stops.lastOrNull()?.stationName ?: "",
                delaySec = delaySec,
                cancelled = rt?.cancelled == true,
                stops = stops,
                nextStopName = stops.getOrNull(nextIdx)?.stationName,
                arrivalEtaSec = mine?.estimatedEpoch?.let { TimeUtils.epochToLocalSec(it) }
                    ?: mine?.scheduledSec,
                arrivalDelaySec = arrDelay,
                finished = nextIdx == -1,
            )
        }
    }

    fun stop() {
        viewModelScope.launch { container.stopLiveTrip() }
    }
}
