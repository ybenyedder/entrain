package fr.webtvmedia.entrain.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.webtvmedia.entrain.data.AppContainer
import fr.webtvmedia.entrain.data.db.StationRow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StationsTabViewModel(val container: AppContainer) : ViewModel() {
    val favorites: StateFlow<List<StationRow>> = container.db.userPrefsDao().favorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recents: StateFlow<List<StationRow>> = container.db.userPrefsDao().recents()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _popular = kotlinx.coroutines.flow.MutableStateFlow<List<fr.webtvmedia.entrain.domain.model.Station>>(emptyList())
    val popular: StateFlow<List<fr.webtvmedia.entrain.domain.model.Station>> = _popular

    init {
        viewModelScope.launch { _popular.value = container.popularStations() }
    }
}
