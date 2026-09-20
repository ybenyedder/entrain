package fr.webtvmedia.entrain.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.webtvmedia.entrain.data.AppContainer
import fr.webtvmedia.entrain.data.gtfs.ImportProgress
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OnboardingViewModel(val container: AppContainer) : ViewModel() {

    private val _progress = MutableStateFlow<ImportProgress?>(null)
    val progress: StateFlow<ImportProgress?> = _progress.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun startImport() {
        if (_busy.value) return
        _busy.value = true
        viewModelScope.launch {
            container.gtfsImporter.import { p ->
                _progress.value = p
            }
            _busy.value = false
        }
    }
}
