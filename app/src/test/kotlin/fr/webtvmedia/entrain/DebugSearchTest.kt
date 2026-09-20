package fr.webtvmedia.entrain

import fr.webtvmedia.entrain.data.db.CalendarDateEntity
import fr.webtvmedia.entrain.data.db.RouteEntity
import fr.webtvmedia.entrain.data.db.StopEntity
import fr.webtvmedia.entrain.data.db.StopTimeEntity
import fr.webtvmedia.entrain.data.db.TripEntity
import fr.webtvmedia.entrain.domain.routing.RaptorRouter
import fr.webtvmedia.entrain.domain.routing.TimetableIndex
import org.junit.Test
import java.time.LocalDate

class DebugSearchTest {

    private val date: LocalDate = LocalDate.of(2026, 9, 21)
    private fun hhmm(h: Int, m: Int) = h * 3600 + m * 60

    private fun router(): RaptorRouter {
        val stops = listOf(
            StopEntity("StopArea:OCEA", "Gare A", "gare a", 0.0, 0.0, null),
            StopEntity("StopArea:OCEB", "Gare B", "gare b", 0.0, 0.0, null),
            StopEntity("StopArea:OCEC", "Gare C", "gare c", 0.0, 0.0, null),
            StopEntity("StopPoint:OCETRAIN-A", "Gare A", "gare a", 0.0, 0.0, "StopArea:OCEA"),
            StopEntity("StopPoint:OCETRAIN-B", "Gare B", "gare b", 0.0, 0.0, "StopArea:OCEB"),
            StopEntity("StopPoint:OCETRAIN-C", "Gare C", "gare c", 0.0, 0.0, "StopArea:OCEC"),
        )
        val routes = listOf(RouteEntity("R1", "1187", "T1", "A - C", 2, null, null))
        val trips = listOf(
            TripEntity("trip-t1", "R1", "S1", "1001", 0, "Gare C"),
            TripEntity("trip-t2", "R1", "S1", "1002", 0, "Gare B"),
            TripEntity("trip-t4", "R1", "S1", "1004", 0, "Gare C"),
        )
        fun st(trip: String, seq: Int, stop: String, arr: Int, dep: Int) =
            StopTimeEntity(trip, seq, stop, arr, dep, 0, 0)
        val stopTimes = listOf(
            st("trip-t1", 0, "StopPoint:OCETRAIN-A", hhmm(10, 0), hhmm(10, 0)),
            st("trip-t1", 1, "StopPoint:OCETRAIN-C", hhmm(11, 0), hhmm(11, 0)),
            st("trip-t2", 0, "StopPoint:OCETRAIN-A", hhmm(12, 0), hhmm(12, 0)),
            st("trip-t2", 1, "StopPoint:OCETRAIN-B", hhmm(12, 30), hhmm(12, 30)),
            st("trip-t4", 0, "StopPoint:OCETRAIN-B", hhmm(13, 0), hhmm(13, 0)),
            st("trip-t4", 1, "StopPoint:OCETRAIN-C", hhmm(13, 30), hhmm(13, 30)),
        )
        val calendar = listOf(CalendarDateEntity("S1", 20260921, 1))
        return RaptorRouter(TimetableIndex.build(stops, routes, trips, stopTimes, calendar))
    }

    @Test
    fun debug() {
        val r = router()
        val js = r.search("StopArea:OCEA", "StopArea:OCEC", date, hhmm(9, 0), maxResults = 8)
        println("RÉSULTATS: ${js.size}")
        js.forEachIndexed { i, j ->
            println(
                "#$i dep=${j.departureSec / 60}min arr=${j.arrivalSec / 60}min legs=" +
                    j.legs.joinToString(" -> ") { "${it.trainNumber}(${it.boardStation}→${it.alightStation})" },
            )
        }
        val js2 = r.search("StopArea:OCEA", "StopArea:OCEC", date, hhmm(10, 1), maxResults = 8)
        println("APRÈS 10:01: ${js2.size}")
        js2.forEachIndexed { i, j ->
            println("#$i dep=${j.departureSec / 60}min legs=${j.legs.map { it.trainNumber }}")
        }
    }
}
