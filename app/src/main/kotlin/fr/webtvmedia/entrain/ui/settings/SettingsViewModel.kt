package fr.webtvmedia.entrain.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.webtvmedia.entrain.data.AppContainer
import fr.webtvmedia.entrain.data.gtfs.ImportProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val gtfsVersion: String? = null,
    val importDate: String? = null,
    val updating: Boolean = false,
    val updateStatus: String? = null,
    val primKey: String = "",
    val primKeySaved: Boolean = false,
)

class SettingsViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val v = container.db.metaDao().get("gtfs_version")
            val ts = container.db.metaDao().get("import_ts")?.toLongOrNull()
            _state.value = _state.value.copy(
                gtfsVersion = v,
                importDate = ts?.let {
                    java.time.Instant.ofEpochMilli(it).atZone(fr.webtvmedia.entrain.util.TimeUtils.PARIS)
                        .format(java.time.format.DateTimeFormatter.ofPattern("d MMMM yyyy 'à' HH:mm", java.util.Locale.FRENCH))
                },
                primKey = container.primRepo.apiKey(),
            )
        }
    }

    fun setPrimKey(key: String) {
        _state.value = _state.value.copy(primKey = key, primKeySaved = false)
    }

    fun savePrimKey() {
        viewModelScope.launch {
            container.primRepo.setKey(_state.value.primKey)
            _state.value = _state.value.copy(primKeySaved = true)
        }
    }

    fun updateTimetable() {
        if (_state.value.updating) return
        _state.value = _state.value.copy(updating = true, updateStatus = "Téléchargement…")
        viewModelScope.launch {
            container.gtfsImporter.import { p ->
                val msg = when (p) {
                    is ImportProgress.Downloading -> "Téléchargement… ${p.percent} %"
                    ImportProgress.Parsing -> "Lecture…"
                    is ImportProgress.Importing -> "Import… ${p.percent} %"
                    ImportProgress.Finishing -> "Finalisation…"
                    is ImportProgress.Done -> "Terminé"
                    is ImportProgress.Failed -> "Échec : ${p.message}"
                }
                _state.value = _state.value.copy(updateStatus = msg)
            }
            container.invalidateIndex()
            container.rebuildIndex()
            val v = container.db.metaDao().get("gtfs_version")
            _state.value = _state.value.copy(updating = false, gtfsVersion = v)
        }
    }
}
