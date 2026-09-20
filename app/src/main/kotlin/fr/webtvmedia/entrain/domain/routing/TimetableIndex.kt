package fr.webtvmedia.entrain.domain.routing

import fr.webtvmedia.entrain.data.db.CalendarDateEntity
import fr.webtvmedia.entrain.data.db.RouteEntity
import fr.webtvmedia.entrain.data.db.StopEntity
import fr.webtvmedia.entrain.data.db.StopTimeEntity
import fr.webtvmedia.entrain.data.db.TripEntity

/**
 * Index horaires compact en mémoire, reconstruit après chaque import GTFS.
 * Tout est en tableau primitif pour rester léger (≈15 Mo pour le réseau national).
 */
class TimetableIndex private constructor(
    val areaIds: List<String>,
    val areaNames: List<String>,
    val stopPointIds: List<String>,
    /** index StopPoint -> index Area */
    val stopPointArea: IntArray,
    /** pour chaque Area : indices de ses StopPoint */
    val areaPoints: Array<IntArray>,
    val tripIds: Array<String>,
    val tripTrainNumbers: Array<String>,
    val tripServiceId: Array<String>,
    val tripRouteId: Array<String?>,
    val tripPattern: IntArray,
    val patternStops: Array<IntArray>,
    val patternTrips: List<IntArray>,
    /** index Trip -> temps alignés sur patternStops (null = trip ignoré) */
    val tripTimes: Array<TimetableIndex.TripTimes?>,
    /** pour chaque StopPoint : patterns le desservant */
    val patternsAtStop: Array<IntArray>,
    /** serviceId -> dates yyyymmdd triées */
    val services: Map<String, IntArray>,
    val routeLabel: Map<String, String>,
    val routeColor: Map<String, String>,
) {
    class TripTimes(
        val arr: IntArray,
        val dep: IntArray,
        val pickup: ByteArray,
        val dropoff: ByteArray,
    )

    fun areaIndex(id: String): Int = areaIds.indexOf(id)

    fun stationName(stopPointIdx: Int): String = areaNames[stopPointArea[stopPointIdx]]
    fun stationId(stopPointIdx: Int): String = areaIds[stopPointArea[stopPointIdx]]

    fun routeLabelOfTrip(tripIdx: Int): String? =
        tripRouteId[tripIdx]?.let { routeLabel[it] }

    fun routeColorOfTrip(tripIdx: Int): String? =
        tripRouteId[tripIdx]?.let { routeColor[it] }

    companion object {
        fun build(
            stops: List<StopEntity>,
            routes: List<RouteEntity>,
            trips: List<TripEntity>,
            stopTimes: List<StopTimeEntity>,
            calendar: List<CalendarDateEntity>,
        ): TimetableIndex {
            // --- Areas & StopPoints ---
            val areaIdToIdx = HashMap<String, Int>()
            val areaIds = ArrayList<String>()
            val areaNames = ArrayList<String>()
            for (s in stops) {
                if (s.parentStation.isNullOrBlank()) {
                    if (!areaIdToIdx.containsKey(s.id)) {
                        areaIdToIdx[s.id] = areaIds.size
                        areaIds.add(s.id)
                        areaNames.add(s.name)
                    }
                }
            }
            val stopPointIdxOf = HashMap<String, Int>()
            val stopPointIds = ArrayList<String>()
            val stopPointArea = ArrayList<Int>()
            val pointsByArea = HashMap<Int, ArrayList<Int>>()
            for (s in stops) {
                val parent = s.parentStation
                if (!parent.isNullOrBlank()) {
                    var area = areaIdToIdx[parent]
                    if (area == null) {
                        areaIdToIdx[parent] = areaIds.size
                        areaIds.add(parent)
                        areaNames.add(parent.removePrefix("StopArea:OCE"))
                        area = areaIds.size - 1
                    }
                    val spIdx = stopPointIds.size
                    stopPointIdxOf[s.id] = spIdx
                    stopPointIds.add(s.id)
                    stopPointArea.add(area)
                    pointsByArea.getOrPut(area) { ArrayList() }.add(spIdx)
                }
            }
            val nPoints = stopPointIds.size
            val stopPointAreaArr = IntArray(nPoints) { stopPointArea[it] }
            val areaPointsArr = Array(areaIds.size) { a ->
                pointsByArea[a]?.toIntArray() ?: IntArray(0)
            }

            // --- Services (calendar_dates, exception_type 1 = service actif) ---
            val calByService = HashMap<String, ArrayList<Int>>()
            for (c in calendar) {
                if (c.exceptionType == 1) calByService.getOrPut(c.serviceId) { ArrayList() }.add(c.date)
            }
            val services = HashMap<String, IntArray>()
            for ((k, v) in calByService) services[k] = v.sorted().toIntArray()

            // --- Routes ---
            val routeLabel = HashMap<String, String>()
            val routeColor = HashMap<String, String>()
            for (r in routes) {
                val lbl = r.shortName?.takeIf { it.isNotBlank() }
                if (lbl != null) routeLabel[r.routeId] = lbl
                if (!r.color.isNullOrBlank()) routeColor[r.routeId] = r.color
            }
            val routeById = HashMap<String, RouteEntity>()
            for (r in routes) routeById[r.routeId] = r

            // --- Trips ---
            val nTrips = trips.size
            val tripIdxById = HashMap<String, Int>(nTrips * 2)
            val tripIdsArr = arrayOfNulls<String>(nTrips)
            val tripNumsArr = arrayOfNulls<String>(nTrips)
            val tripSvcArr = arrayOfNulls<String>(nTrips)
            val tripRouteArr = arrayOfNulls<String?>(nTrips)
            for ((i, t) in trips.withIndex()) {
                tripIdxById[t.tripId] = i
                tripIdsArr[i] = t.tripId
                tripNumsArr[i] = t.headsign ?: ""
                tripSvcArr[i] = t.serviceId
                tripRouteArr[i] = t.routeId
            }

            // --- StopTimes groupés par trip (triés par trip_id, stop_sequence) ---
            val stopSeq = HashMap<Int, ArrayList<StopTimeEntity>>()
            for (st in stopTimes) {
                val tIdx = tripIdxById[st.tripId] ?: continue
                stopSeq.getOrPut(tIdx) { ArrayList() }.add(st)
            }

            val patternMap = HashMap<List<Int>, Int>()
            val patternStopsList = ArrayList<IntArray>()
            val patternTripsList = ArrayList<ArrayList<Int>>()
            val tripPatternArr = IntArray(nTrips)
            val tripTimesArr = arrayOfNulls<TripTimes>(nTrips)
            for (tIdx in 0 until nTrips) {
                val sts = stopSeq[tIdx] ?: continue
                if (sts.size < 2) continue
                val stopsPath = ArrayList<Int>(sts.size)
                var ok = true
                for (st in sts) {
                    val sp = stopPointIdxOf[st.stopId]
                    if (sp == null) { ok = false; break }
                    stopsPath.add(sp)
                }
                if (!ok || stopsPath.size < 2) continue
                val patIdx = patternMap.getOrPut(stopsPath) {
                    patternStopsList.add(stopsPath.toIntArray())
                    patternTripsList.add(ArrayList())
                    patternTripsList.size - 1
                }
                patternTripsList[patIdx].add(tIdx)
                tripPatternArr[tIdx] = patIdx
                tripTimesArr[tIdx] = TripTimes(
                    IntArray(sts.size) { sts[it].arrivalSec },
                    IntArray(sts.size) { sts[it].departureSec },
                    ByteArray(sts.size) { sts[it].pickupType.toByte() },
                    ByteArray(sts.size) { sts[it].dropOffType.toByte() },
                )
            }

            val patTrips = patternTripsList.map { list ->
                list.sortedBy { tIdx -> tripTimesArr[tIdx]?.dep?.firstOrNull() ?: 0 }.toIntArray()
            }

            val atStop = Array(nPoints) { ArrayList<Int>() }
            for (p in patternStopsList.indices) {
                for (sp in patternStopsList[p]) atStop[sp].add(p)
            }
            val patternsAtStopArr = Array(nPoints) { i -> atStop[i].toIntArray() }

            return TimetableIndex(
                areaIds = areaIds,
                areaNames = areaNames,
                stopPointIds = stopPointIds,
                stopPointArea = stopPointAreaArr,
                areaPoints = areaPointsArr,
                tripIds = tripIdsArr.map { it ?: "" }.toTypedArray(),
                tripTrainNumbers = tripNumsArr.map { it ?: "" }.toTypedArray(),
                tripServiceId = tripSvcArr.map { it ?: "" }.toTypedArray(),
                tripRouteId = tripRouteArr.map { it }.toTypedArray(),
                tripPattern = tripPatternArr,
                patternStops = patternStopsList.toTypedArray(),
                patternTrips = patTrips,
                tripTimes = tripTimesArr,
                patternsAtStop = patternsAtStopArr,
                services = services,
                routeLabel = routeLabel,
                routeColor = routeColor,
            )
        }
    }
}
