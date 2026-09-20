package fr.webtvmedia.entrain.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.webtvmedia.entrain.data.AppContainer
import fr.webtvmedia.entrain.data.db.StationRow
import fr.webtvmedia.entrain.ui.vm.SearchForm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val fromName: String? = null,
    val toName: String? = null,
    val ymd: Int = 0,
    val timeSec: Int = -1,
    val timeMode: Int = 0,
    val activeAlerts: Int = 0,
    val alertWording: String = "infos",
)

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val favorites: StateFlow<List<StationRow>> = container.db.userPrefsDao().favorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recents: StateFlow<List<StationRow>> = container.db.userPrefsDao().recents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val liveTrip: StateFlow<fr.webtvmedia.entrain.data.db.LiveTripEntity?> =
        container.liveTrip
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        refreshForm()
        viewModelScope.launch {
            // précharge l'index horaires pour que la recherche soit instantanée
            container.ensureIndex()
        }
        viewModelScope.launch {
            // horaires : contrôle quotidien, téléchargement hebdo si périmés
            container.maybeAutoUpdateTimetable()
        }
        viewModelScope.launch {
            container.rtRepo.refresh()
            refreshAlerts()
        }
    }

    /** Recompte les perturbations actives depuis le dernier flux. */
    fun refreshAlerts() {
        viewModelScope.launch {
            val active = container.rtRepo.alerts.filter { container.rtRepo.isActiveNow(it) }
            val disruptions = active.count { a ->
                DISRUPTION_WORDS.any { a.header.contains(it, ignoreCase = true) }
            }
            _state.value = _state.value.copy(
                activeAlerts = if (disruptions > 0) disruptions else active.size,
                alertWording = if (disruptions > 0) "perturbation" + if (disruptions > 1) "s" else ""
                else "info" + if (active.size > 1) "s" else "",
            )
        }
    }

    companion object {
        private val DISRUPTION_WORDS = listOf(
            "perturb", "ralenti", "interrompu", "supprim", "modifi", "retard",
            "grève", "greve", "travaux", "ferm", "annul", "adapté", "adapte",
        )
    }

    fun refreshForm() {
        _state.value = _state.value.copy(
            fromName = SearchForm.fromName,
            toName = SearchForm.toName,
            ymd = SearchForm.ymd,
            timeSec = SearchForm.timeSec,
            timeMode = SearchForm.timeMode,
        )
    }

    fun setStation(slot: String, id: String, name: String) {
        if (slot == "from") {
            SearchForm.fromId = id; SearchForm.fromName = name
        } else {
            SearchForm.toId = id; SearchForm.toName = name
        }
        refreshForm()
        viewModelScope.launch {
            container.db.userPrefsDao().upsertRecent(
                fr.webtvmedia.entrain.data.db.RecentStationEntity(id, System.currentTimeMillis(), 1),
            )
        }
    }

    fun swap() {
        val f = SearchForm.fromId; val fn = SearchForm.fromName
        SearchForm.fromId = SearchForm.toId; SearchForm.fromName = SearchForm.toName
        SearchForm.toId = f; SearchForm.toName = fn
        refreshForm()
    }

    fun setDate(ymd: Int) {
        SearchForm.ymd = ymd
        refreshForm()
    }

    fun setTime(sec: Int) {
        SearchForm.timeSec = sec
        refreshForm()
    }

    fun setTimeMode(mode: Int) {
        SearchForm.timeMode = mode
        if (mode == 1 && SearchForm.timeSec < 0) {
            // en mode « arriver à », une heure cible est plus utile que « dès que possible »
            SearchForm.timeSec = nowSecDefault()
        }
        refreshForm()
    }

    private fun nowSecDefault(): Int {
        val t = java.time.LocalTime.now(fr.webtvmedia.entrain.util.TimeUtils.PARIS)
        return t.hour * 3600 + t.minute * 60
    }

    fun defaultYmd(): Int = fr.webtvmedia.entrain.util.TimeUtils.ymd(java.time.LocalDate.now(fr.webtvmedia.entrain.util.TimeUtils.PARIS))
}
