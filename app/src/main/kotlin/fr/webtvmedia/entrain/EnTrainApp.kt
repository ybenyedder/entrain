package fr.webtvmedia.entrain

import android.app.Application
import fr.webtvmedia.entrain.data.AppContainer

class EnTrainApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
