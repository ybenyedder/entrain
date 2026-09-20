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
    /** avancement du train entre le dernier arrêt desservi et le prochain (0..1) */
    val progress: Float? = null,
    /** perturbations touchant ce train */
    val alerts: List<fr.webtvmedia.entrain.domain.model.AlertInfo> = emptyList(),
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

            // avancement entre le dernier arrêt desservi et le prochain
            var progress: Float? = null
            if (nextIdx in 1 until stops.size) {
                fun epochOf(i: Int, sec: Int): Long =
                    stops[i].estimatedEpoch ?: TimeUtils.naiveToEpoch(serviceDate, sec)
                val prevEpoch = epochOf(nextIdx - 1, rows[nextIdx - 1].departureSec)
                val nextEpoch = epochOf(nextIdx, rows[nextIdx].arrivalSec)
                if (nextEpoch > prevEpoch) {
                    progress = ((now - prevEpoch).toDouble() / (nextEpoch - prevEpoch))
                        .coerceIn(0.0, 1.0).toFloat()
                }
            } else if (nextIdx == 0) {
                progress = 0f
            }

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
                progress = progress,
                alerts = container.rtRepo.alerts
                    .filter { it.informedTripIds.contains(trip.tripId) && container.rtRepo.isActiveNow(it) },
            )
        }
    }

    fun stop() {
        viewModelScope.launch { container.stopLiveTrip() }
    }
}
