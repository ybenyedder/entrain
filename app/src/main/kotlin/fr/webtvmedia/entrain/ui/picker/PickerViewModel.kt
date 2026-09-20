package fr.webtvmedia.entrain.ui.picker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.webtvmedia.entrain.data.AppContainer
import fr.webtvmedia.entrain.data.db.StationRow
import fr.webtvmedia.entrain.domain.model.Station
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PickerUiState(
    val query: String = "",
    val results: List<Station> = emptyList(),
    val searching: Boolean = false,
)

class PickerViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(PickerUiState())
    val state: StateFlow<PickerUiState> = _state.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    val favorites: StateFlow<List<StationRow>> = container.db.userPrefsDao().favorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recents: StateFlow<List<StationRow>> = container.db.userPrefsDao().recents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _popular = MutableStateFlow<List<Station>>(emptyList())
    val popular: StateFlow<List<Station>> = _popular.asStateFlow()

    init {
        viewModelScope.launch {
            queryFlow.debounce(180).collect { q ->
                if (q.isBlank()) {
                    _state.value = _state.value.copy(results = emptyList(), searching = false)
                } else {
                    _state.value = _state.value.copy(searching = true)
                    val res = container.searchStations(q)
                    _state.value = PickerUiState(q, res, false)
                }
            }
        }
        viewModelScope.launch {
            _popular.value = container.popularStations()
        }
    }

    fun setQuery(q: String) {
        _state.value = _state.value.copy(query = q)
        queryFlow.value = q
    }
}
