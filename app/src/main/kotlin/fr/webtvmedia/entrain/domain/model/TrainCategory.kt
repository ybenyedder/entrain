package fr.webtvmedia.entrain.domain.model

import androidx.compose.ui.graphics.Color

/**
 * Catégorie de train déduite du préfixe du StopPoint GTFS SNCF
 * (ex. "StopPoint:OCETGV INOUI-87773002" → TGV_INOUI).
 */
enum class TrainCategory(val label: String, val color: Color) {
    TGV_INOUI("TGV INOUI", Color(0xFFB01218)),
    OUIGO("OUIGO", Color(0xFFE2007A)),
    INTERCITES("INTERCITÉS", Color(0xFF7C2234)),
    INTERCITES_NUIT("INTERCITÉS de nuit", Color(0xFF1F2E5A)),
    TER("TER", Color(0xFF127BBF)),
    CAR_TER("Car TER", Color(0xFF7B8794)),
    CAR_RESERVATION("Car à réservation", Color(0xFF7B8794)),
    TRAM_TRAIN("Tram-Train", Color(0xFF00897B)),
    TRANSILIEN("Transilien", Color(0xFF1D4E89)),
    EUROSTAR("Eurostar", Color(0xFF002E6D)),
    THALYS("Thalys", Color(0xFFBE1414)),
    LYRIA("Lyria", Color(0xFFC8102E)),
    ICE("ICE", Color(0xFFC62828)),
    NAVETTE("Navette", Color(0xFF6D4C41)),
    TRAIN("Train", Color(0xFF546E7A)),
    UNKNOWN("Train", Color(0xFF546E7A));

    val isCar: Boolean get() = this == CAR_TER || this == CAR_RESERVATION

    companion object {
        fun fromStopPointId(stopPointId: String): TrainCategory {
            if (!stopPointId.startsWith("StopPoint:")) return UNKNOWN
            val body = stopPointId.removePrefix("StopPoint:OCE")
            val dash = body.lastIndexOf('-')
            if (dash <= 0) return UNKNOWN
            return when (body.substring(0, dash)) {
                "TGV INOUI" -> TGV_INOUI
                "OUIGO" -> OUIGO
                "INTERCITES" -> INTERCITES
                "INTERCITES de nuit" -> INTERCITES_NUIT
                "Train TER" -> TER
                "Car TER" -> CAR_TER
                "Car à réservation" -> CAR_RESERVATION
                "TramTrain" -> TRAM_TRAIN
                "Eurostar" -> EUROSTAR
                "THALYS" -> THALYS
                "Lyria" -> LYRIA
                "ICE" -> ICE
                "Navette" -> NAVETTE
                "Train" -> TRAIN
                else -> UNKNOWN
            }
        }
    }
}
