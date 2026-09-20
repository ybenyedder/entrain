package fr.webtvmedia.entrain.ui.vm

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import fr.webtvmedia.entrain.EnTrainApp
import fr.webtvmedia.entrain.ui.alerts.TrafficViewModel
import fr.webtvmedia.entrain.ui.departures.DeparturesViewModel
import fr.webtvmedia.entrain.ui.home.HomeViewModel
import fr.webtvmedia.entrain.ui.onboarding.OnboardingViewModel
import fr.webtvmedia.entrain.ui.picker.PickerViewModel
import fr.webtvmedia.entrain.ui.results.ResultsViewModel
import fr.webtvmedia.entrain.ui.settings.SettingsViewModel
import fr.webtvmedia.entrain.ui.stations.StationsTabViewModel

class VmFactory(private val app: EnTrainApp) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T = create(modelClass, CreationExtras.Empty)

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val savedState = extras.createSavedStateHandle()
        val container = app.container
        return when {
            modelClass.isAssignableFrom(AppViewModel::class.java) -> AppViewModel(container)
            modelClass.isAssignableFrom(HomeViewModel::class.java) -> HomeViewModel(container)
            modelClass.isAssignableFrom(OnboardingViewModel::class.java) -> OnboardingViewModel(container)
            modelClass.isAssignableFrom(PickerViewModel::class.java) -> PickerViewModel(container)
            modelClass.isAssignableFrom(ResultsViewModel::class.java) -> ResultsViewModel(container, savedState)
            modelClass.isAssignableFrom(DeparturesViewModel::class.java) -> DeparturesViewModel(container, savedState)
            modelClass.isAssignableFrom(TrafficViewModel::class.java) -> TrafficViewModel(container)
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> SettingsViewModel(container)
            modelClass.isAssignableFrom(StationsTabViewModel::class.java) -> StationsTabViewModel(container)
            modelClass.isAssignableFrom(fr.webtvmedia.entrain.ui.live.LiveTripViewModel::class.java) ->
                fr.webtvmedia.entrain.ui.live.LiveTripViewModel(container)
            else -> throw IllegalArgumentException("ViewModel inconnu : $modelClass")
        } as T
    }
}

/** Formulaire de recherche persistant entre les écrans (en mémoire). */
object SearchForm {
    var fromId: String? = null
    var fromName: String? = null
    var toId: String? = null
    var toName: String? = null
    var ymd: Int = 0
    var timeSec: Int = -1 // -1 = dès que possible
    var timeMode: Int = 0 // 0 = partir à, 1 = arriver à
}

class AppViewModel(private val container: fr.webtvmedia.entrain.data.AppContainer) : ViewModel() {
    suspend fun checkData(): Boolean = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        container.db.metaDao().get("gtfs_version") != null
    }
}
