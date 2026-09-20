package fr.webtvmedia.entrain

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import fr.webtvmedia.entrain.ui.AppRoot
import fr.webtvmedia.entrain.ui.theme.EnTrainTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            EnTrainTheme {
                AppRoot()
            }
        }
    }
}
