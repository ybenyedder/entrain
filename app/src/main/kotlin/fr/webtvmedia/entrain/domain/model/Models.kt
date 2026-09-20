package fr.webtvmedia.entrain.domain.model

/** Gare = StopArea GTFS (regroupe les StopPoint par type de train). */
data class Station(
    val id: String,
    val name: String,
    val lat: Double,
    val lon: Double,
    val stopPointIds: List<String> = emptyList(),
)

/** Un arrêt desservi à l'intérieur d'une étape de trajet. */
data class StopVisit(
    val stopPointId: String,
    val stationId: String,
    val stationName: String,
    val arrivalSec: Int,
    val departureSec: Int,
    val dayOffset: Int,
    val pickupAllowed: Boolean,
    val dropOffAllowed: Boolean,
)

/** Une étape en train (ou car) d'un trajet. */
data class Leg(
    val tripId: String,
    val trainNumber: String,
    val destination: String,
    val category: TrainCategory,
    val lineLabel: String?,
    val departureSec: Int,
    val arrivalSec: Int,
    val departureDayOffset: Int,
    val arrivalDayOffset: Int,
    val boardStation: String,
    val alightStation: String,
    val boardStationId: String,
    val alightStationId: String,
    val boardStopPointId: String,
    val alightStopPointId: String,
    val intermediateStops: List<StopVisit>,
    // --- enrichissement temps réel (rempli par le repository) ---
    val estDepartureEpoch: Long? = null,
    val estArrivalEpoch: Long? = null,
    val depDelaySec: Int? = null,
    val arrDelaySec: Int? = null,
    val cancelled: Boolean = false,
    val stopEstimates: Map<String, Long> = emptyMap(),
)

data class Journey(
    val departureSec: Int,
    val arrivalSec: Int,
    val departureDayOffset: Int,
    val arrivalDayOffset: Int,
    val legs: List<Leg>,
) {
    val durationSec: Int get() = totalArrival - totalDeparture
    val totalDeparture: Int get() = departureSec + departureDayOffset * 86400
    val totalArrival: Int get() = arrivalSec + arrivalDayOffset * 86400
    val transfers: Int get() = legs.size - 1
    val isDirect: Boolean get() = legs.size == 1
}

/** Temps réel appliqué à un arrêt précis (epoch secondes, UTC). */
data class RtStop(
    val arrivalEpoch: Long?,
    val departureEpoch: Long?,
    val skipped: Boolean = false,
)

data class RtTrip(
    val tripId: String,
    val startDate: String,
    val stopUpdates: Map<String, RtStop>,
    val cancelled: Boolean = false,
)

data class Departure(
    val tripId: String,
    val trainNumber: String,
    val destination: String,
    val category: TrainCategory,
    val scheduledSec: Int,
    val dayOffset: Int,
    val estimatedEpoch: Long?,
    val cancelled: Boolean,
    val delayedMin: Long?,
    val stopPointId: String,
    /** libellé de ligne (ex. « RER A », « J ») pour les passages PRIM */
    val lineLabel: String? = null,
    /** quai/voie quand la source le fournit (PRIM) */
    val platform: String? = null,
)

data class AlertInfo(
    val id: String,
    val header: String,
    val description: String,
    val severity: String,
    val activeStartEpoch: Long?,
    val activeEndEpoch: Long?,
    val informedTripIds: Set<String>,
    val informedStationNames: List<String>,
)
