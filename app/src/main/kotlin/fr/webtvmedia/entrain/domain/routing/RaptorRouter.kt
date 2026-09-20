package fr.webtvmedia.entrain.domain.routing

import fr.webtvmedia.entrain.domain.model.Journey
import fr.webtvmedia.entrain.domain.model.Leg
import fr.webtvmedia.entrain.domain.model.StopVisit
import fr.webtvmedia.entrain.domain.model.TrainCategory
import java.time.LocalDate

/**
 * RAPTOR adapté réseau ferré : 1 round = 1 train pris. Les correspondances
 * se font dans une même gare (StopArea) : 0 min en restant sur le même
 * StopPoint, 5 min pour changer de StopPoint dans la gare.
 */
class RaptorRouter(private val idx: TimetableIndex) {

    companion object {
        const val TRANSFER_SEC = 5 * 60
        const val MAX_LEGS = 4
    }

    private class Label(
        var arr: Long,
        var tripIdx: Int = -1,      // -1 = label d'origine ou transfert à pied
        var boardPos: Int = -1,
        var alightPos: Int = -1,
        var boardFromStop: Int = -1, // point marqué depuis lequel on a scanné
        var epochDay: Long = -1,     // jour de service du run emprunté
        var round: Int = 0,
    )

    private class Run(val tripIdx: Int, val epochDay: Long)

    /** Runs actifs pour les dates [date-1 .. date+1], par pattern, triés par départ absolu. */
    private fun buildRuns(date: LocalDate): Array<List<Run>> {
        val days = longArrayOf(date.toEpochDay() - 1, date.toEpochDay(), date.toEpochDay() + 1)
        val ymds = IntArray(3)
        for (d in 0 until 3) {
            val dt = date.plusDays((d - 1).toLong())
            ymds[d] = dt.year * 10000 + dt.monthValue * 100 + dt.dayOfMonth
        }
        val cache = HashMap<String, BooleanArray>()
        fun active(svcId: String): BooleanArray = cache.getOrPut(svcId) {
            val dates = idx.services[svcId] ?: IntArray(0)
            BooleanArray(3) { i -> binaryContains(dates, ymds[i]) }
        }
        val runs = Array(idx.patternStops.size) { ArrayList<Run>() }
        for (p in idx.patternStops.indices) {
            val out = runs[p]
            for (tIdx in idx.patternTrips[p]) {
                val act = active(idx.tripServiceId[tIdx])
                for (d in 0 until 3) if (act[d]) out.add(Run(tIdx, days[d]))
            }
            out.sortBy { r -> r.epochDay * 86400L + (idx.tripTimes[r.tripIdx]?.dep?.firstOrNull() ?: 0) }
        }
        return Array(runs.size) { runs[it].toList() }
    }

    /**
     * Prochains trajets de `originAreaId` vers `destAreaId` à partir de
     * `date` + `timeSec` (secondes locales depuis minuit de `date`, peut
     * dépasser 24 h). Renvoie jusqu'à `maxResults` trajets distincts.
     */
    fun search(
        originAreaId: String,
        destAreaId: String,
        date: LocalDate,
        timeSec: Int,
        maxResults: Int = 8,
        windowSec: Long = 16 * 3600,
    ): List<Journey> {
        val origin = idx.areaIndex(originAreaId)
        val dest = idx.areaIndex(destAreaId)
        if (origin < 0 || dest < 0 || origin == dest) return emptyList()

        val baseDay = date.toEpochDay()
        var searchFrom = baseDay * 86400 + timeSec
        val limit = searchFrom + windowSec

        val runs = buildRuns(date)
        val results = ArrayList<Journey>(maxResults)
        val seen = HashSet<String>()
        var guard = 0
        while (results.size < maxResults && searchFrom < limit && guard++ < 64) {
            val j = searchOne(origin, dest, runs, searchFrom, baseDay) ?: break
            val key = j.legs.joinToString("|") { it.tripId } + "#" + j.totalDeparture
            if (!seen.add(key)) {
                searchFrom = baseDay * 86400 + j.totalDeparture + 60
                continue
            }
            results.add(j)
            searchFrom = baseDay * 86400 + j.totalDeparture + 60
        }
        return results
    }

    /**
     * Recherche « arriver à » : renvoie les trajets arrivant avant `arriveSec`
     * le jour `date`, du départ le plus tardif au plus tôt. L'arrivée au plus tôt
     * étant croissante en fonction de l'heure de départ, une dichotomie sur
     * l'heure de départ trouve le dernier départ encore à l'heure.
     */
    fun searchArriveBy(
        originAreaId: String,
        destAreaId: String,
        date: LocalDate,
        arriveSec: Int,
        maxResults: Int = 5,
        windowSec: Long = 14 * 3600,
    ): List<Journey> {
        val origin = idx.areaIndex(originAreaId)
        val dest = idx.areaIndex(destAreaId)
        if (origin < 0 || dest < 0 || origin == dest) return emptyList()

        val baseDay = date.toEpochDay()
        val deadline = baseDay * 86400 + arriveSec
        val runs = buildRuns(date)

        fun arrivalFrom(t: Long): Long? =
            searchOne(origin, dest, runs, t, baseDay)
                ?.let { baseDay * 86400 + it.totalArrival }

        val earliest = deadline - windowSec
        if (arrivalFrom(earliest) == null) return emptyList()
        if ((arrivalFrom(earliest) ?: Long.MAX_VALUE) > deadline) return emptyList()

        var lo = earliest
        var hi = deadline
        while (hi - lo > 60) {
            val mid = lo + (hi - lo) / 2
            val arr = arrivalFrom(mid)
            if (arr != null && arr <= deadline) lo = mid else hi = mid - 60
        }

        val out = ArrayList<Journey>(maxResults)
        val seen = HashSet<String>()
        var t = lo
        var lastDep = Long.MAX_VALUE
        var guard = 0
        while (out.size < maxResults && t >= earliest && guard++ < 1000) {
            val j = searchOne(origin, dest, runs, t, baseDay) ?: break
            if (j.totalDeparture >= lastDep) {
                // même trajet que le précédent : on descend d'un cran
                t -= 300L
                continue
            }
            lastDep = j.totalDeparture.toLong()
            val key = j.legs.joinToString("|") { it.tripId } + "#" + j.totalDeparture
            if (j.totalArrival <= deadline && seen.add(key)) out.add(j)
            t = baseDay * 86400 + j.totalDeparture - 60
        }
        return out.sortedByDescending { it.totalDeparture }
    }

    private fun searchOne(
        origin: Int,
        dest: Int,
        runs: Array<List<Run>>,
        t0: Long,
        baseDay: Long,
    ): Journey? {
        val nPoints = idx.stopPointIds.size
        val best = arrayOfNulls<Label>(nPoints)
        var marked = ArrayList<Int>()
        for (sp in idx.areaPoints[origin]) {
            best[sp] = Label(t0).also { it.round = 0 }
            marked.add(sp)
        }

        for (round in 1..MAX_LEGS) {
            val improved = ArrayList<Int>()
            for (s in marked) {
                val tau = best[s]!!.arr
                for (p in idx.patternsAtStop[s]) {
                    val stopsP = idx.patternStops[p]
                    var boardPos = -1
                    for (k in stopsP.indices) {
                        if (stopsP[k] == s) { boardPos = k; break }
                    }
                    if (boardPos < 0) continue
                    var chosen: Run? = null
                    for (r in runs[p]) {
                        val tt = idx.tripTimes[r.tripIdx] ?: continue
                        if (tt.dep.size <= boardPos) continue
                        if (tt.pickup[boardPos].toInt() == 1) continue
                        val depAbs = r.epochDay * 86400 + tt.dep[boardPos]
                        if (depAbs >= tau) { chosen = r; break }
                    }
                    val run = chosen ?: continue
                    val tt = idx.tripTimes[run.tripIdx] ?: continue
                    for (k in (boardPos + 1) until stopsP.size) {
                        if (tt.dropoff[k].toInt() == 1) continue
                        val arrAbs = run.epochDay * 86400 + tt.arr[k]
                        val cur = best[stopsP[k]]
                        if (cur == null || arrAbs < cur.arr) {
                            best[stopsP[k]] = Label(arrAbs, run.tripIdx, boardPos, k, s, run.epochDay, round)
                            improved.add(stopsP[k])
                        }
                    }
                }
            }
            if (improved.isEmpty()) { marked = ArrayList(); break }

            // transferts à pied dans la même gare
            val queue = ArrayList(improved)
            var qi = 0
            while (qi < queue.size) {
                val d = queue[qi++]
                val arrD = best[d]!!.arr
                val area = idx.stopPointArea[d]
                for (r in idx.areaPoints[area]) {
                    if (r == d) continue
                    val cand = arrD + TRANSFER_SEC
                    val cur = best[r]
                    if (cur == null || cand < cur.arr) {
                        best[r] = Label(cand, -1, -1, -1, d, -1, best[d]!!.round)
                        queue.add(r)
                    }
                }
            }
            marked = ArrayList(queue.distinct())
            if (marked.isEmpty()) break
        }

        // meilleur point d'arrivée réellement desservi dans la gare destination
        var bestLabel: Label? = null
        for (dp in idx.areaPoints[dest]) {
            val l = best[dp] ?: continue
            if (l.tripIdx == -1) continue
            if (bestLabel == null || l.arr < bestLabel.arr) bestLabel = l
        }
        val finalLabel = bestLabel ?: return null

        // --- reconstruction en remontant les labels ---
        val legsRaw = ArrayList<IntArray>() // [tripIdx, boardPos, alightPos]
        val epochDays = ArrayList<Long>()
        var lab: Label? = finalLabel
        var guard = 0
        while (lab != null && guard++ < MAX_LEGS * 2 + 4) {
            if (lab.tripIdx == -1) {
                // label d'origine (round 0, boardFromStop -1) ou transfert pied
                if (lab.boardFromStop == -1) break
                lab = best[lab.boardFromStop]
                continue
            }
            legsRaw.add(intArrayOf(lab.tripIdx, lab.boardPos, lab.alightPos))
            epochDays.add(lab.epochDay)
            lab = best[lab.boardFromStop]
        }
        if (lab == null || lab.tripIdx != -1 || lab.round != 0) return null
        if (legsRaw.isEmpty()) return null
        legsRaw.reverse()
        epochDays.reverse()

        // --- assemblage des étapes ---
        val legs = ArrayList<Leg>(legsRaw.size)
        var prevAlightAbs = -1L
        for (i in legsRaw.indices) {
            val (tripIdx, boardPos, alightPos) = legsRaw[i].let { Triple(it[0], it[1], it[2]) }
            val tt = idx.tripTimes[tripIdx] ?: return null
            val stopsP = idx.patternStops[idx.tripPattern[tripIdx]]
            val runDay = epochDays[i]

            val depAbs = runDay * 86400 + tt.dep[boardPos]
            val arrAbs = runDay * 86400 + tt.arr[alightPos]
            if (prevAlightAbs != -1L && depAbs < prevAlightAbs) return null
            prevAlightAbs = arrAbs

            fun toSecOff(abs: Long): Pair<Int, Int> {
                val rel = abs - baseDay * 86400
                val off = Math.floorDiv(rel, 86400L).toInt()
                val sec = (rel - off * 86400L).toInt()
                return sec to off
            }
            val (depSec, depOff) = toSecOff(depAbs)
            val (arrSec, arrOff) = toSecOff(arrAbs)

            val inter = ArrayList<StopVisit>()
            for (k in (boardPos + 1) until alightPos) {
                val (sSec, sOff) = toSecOff(runDay * 86400 + tt.arr[k])
                inter.add(
                    StopVisit(
                        stopPointId = idx.stopPointIds[stopsP[k]],
                        stationId = idx.stationId(stopsP[k]),
                        stationName = idx.stationName(stopsP[k]),
                        arrivalSec = sSec,
                        departureSec = sSec,
                        dayOffset = sOff,
                        pickupAllowed = tt.pickup[k].toInt() != 1,
                        dropOffAllowed = tt.dropoff[k].toInt() != 1,
                    ),
                )
            }

            legs.add(
                Leg(
                    tripId = idx.tripIds[tripIdx],
                    trainNumber = idx.tripTrainNumbers[tripIdx],
                    destination = idx.stationName(stopsP.last()),
                    category = TrainCategory.fromStopPointId(idx.stopPointIds[stopsP.first()]),
                    lineLabel = idx.routeLabelOfTrip(tripIdx),
                    departureSec = depSec,
                    arrivalSec = arrSec,
                    departureDayOffset = depOff,
                    arrivalDayOffset = arrOff,
                    boardStation = idx.stationName(stopsP[boardPos]),
                    alightStation = idx.stationName(stopsP[alightPos]),
                    boardStationId = idx.stationId(stopsP[boardPos]),
                    alightStationId = idx.stationId(stopsP[alightPos]),
                    boardStopPointId = idx.stopPointIds[stopsP[boardPos]],
                    alightStopPointId = idx.stopPointIds[stopsP[alightPos]],
                    intermediateStops = inter,
                ),
            )
        }
        // tri par chronologie (un transfert pied peut donner board au même point)
        if (legs.size > 1) {
            for (i in 1 until legs.size) {
                val prev = legs[i - 1]
                val cur = legs[i]
                if (cur.boardStationId != prev.alightStationId) return null
            }
        }
        val first = legs.first()
        val last = legs.last()
        if (first.boardStationId != idx.areaIds[origin] || last.alightStationId != idx.areaIds[dest]) {
            return null
        }
        return Journey(
            departureSec = first.departureSec,
            arrivalSec = last.arrivalSec,
            departureDayOffset = first.departureDayOffset,
            arrivalDayOffset = last.arrivalDayOffset,
            legs = legs,
        )
    }

    private fun binaryContains(arr: IntArray, v: Int): Boolean {
        var lo = 0
        var hi = arr.size - 1
        while (lo <= hi) {
            val mid = (lo + hi) ushr 1
            when {
                arr[mid] == v -> return true
                arr[mid] < v -> lo = mid + 1
                else -> hi = mid - 1
            }
        }
        return false
    }
}
