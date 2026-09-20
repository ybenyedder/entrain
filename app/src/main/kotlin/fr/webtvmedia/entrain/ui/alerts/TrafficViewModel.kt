package fr.webtvmedia.entrain.ui.alerts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.webtvmedia.entrain.data.AppContainer
import fr.webtvmedia.entrain.domain.model.AlertInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrafficUiState(
    val loading: Boolean = true,
    val alerts: List<AlertInfo> = emptyList(),
    val total: Int = 0,
)

class TrafficViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(TrafficUiState())
    val state: StateFlow<TrafficUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            container.rtRepo.refresh(force = true)
            val all = container.rtRepo.alerts
            val disruptionWords = listOf(
                "perturb", "ralenti", "interrompu", "supprim", "modifi", "retard",
                "grève", "greve", "travaux", "ferm", "annul", "adapté", "adapte",
            )
            val active = all.filter { container.rtRepo.isActiveNow(it) }
            val sorted = active.sortedWith(
                compareByDescending<fr.webtvmedia.entrain.domain.model.AlertInfo> { a ->
                    disruptionWords.any { a.header.contains(it, ignoreCase = true) }
                }.thenByDescending { it.activeStartEpoch ?: 0 },
            )
            _state.value = TrafficUiState(false, sorted, all.size)
        }
    }
}
