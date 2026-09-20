package fr.webtvmedia.entrain

import fr.webtvmedia.entrain.data.db.CalendarDateEntity
import fr.webtvmedia.entrain.data.db.RouteEntity
import fr.webtvmedia.entrain.data.db.StopEntity
import fr.webtvmedia.entrain.data.db.StopTimeEntity
import fr.webtvmedia.entrain.data.db.TripEntity
import fr.webtvmedia.entrain.domain.routing.RaptorRouter
import fr.webtvmedia.entrain.domain.routing.TimetableIndex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Graphe de test :
 *   A --(T1 10:00→11:00)--> C  (direct)
 *   A --(T2 12:00→12:30)--> B --(T4 13:00→13:30)--> C   (correspondance)
 *   Y --(P1 10:00→11:00)--> X1 ; X2 --(P2 11:20→12:00)--> Z  (correspondance même gare, 5 min)
 */
class RaptorRouterTest {

    private val date: LocalDate = LocalDate.of(2026, 9, 21)
    private lateinit var router: RaptorRouter

    private fun hhmm(h: Int, m: Int) = h * 3600 + m * 60

    @Before
    fun setUp() {
        val stops = listOf(
            StopEntity("StopArea:OCEA", "Gare A", "gare a", 0.0, 0.0, null),
            StopEntity("StopArea:OCEB", "Gare B", "gare b", 0.0, 0.0, null),
            StopEntity("StopArea:OCEC", "Gare C", "gare c", 0.0, 0.0, null),
            StopEntity("StopArea:OCEX", "Gare X", "gare x", 0.0, 0.0, null),
            StopEntity("StopArea:OCEY", "Gare Y", "gare y", 0.0, 0.0, null),
            StopEntity("StopArea:OCEZ", "Gare Z", "gare z", 0.0, 0.0, null),
            StopEntity("StopPoint:OCETRAIN-A", "Gare A", "gare a", 0.0, 0.0, "StopArea:OCEA"),
            StopEntity("StopPoint:OCETRAIN-B", "Gare B", "gare b", 0.0, 0.0, "StopArea:OCEB"),
            StopEntity("StopPoint:OCETRAIN-C", "Gare C", "gare c", 0.0, 0.0, "StopArea:OCEC"),
            StopEntity("StopPoint:OCETRAIN-X1", "Gare X", "gare x", 0.0, 0.0, "StopArea:OCEX"),
            StopEntity("StopPoint:OCETER-X2", "Gare X", "gare x", 0.0, 0.0, "StopArea:OCEX"),
            StopEntity("StopPoint:OCETRAIN-Y", "Gare Y", "gare y", 0.0, 0.0, "StopArea:OCEY"),
            StopEntity("StopPoint:OCETRAIN-Z", "Gare Z", "gare z", 0.0, 0.0, "StopArea:OCEZ"),
        )
        val routes = listOf(
            RouteEntity("R1", "1187", "T1", "A - C", 2, null, null),
            RouteEntity("R2", "1187", "T2", "A - B", 2, null, null),
        )
        val trips = listOf(
            TripEntity("trip-t1", "R1", "S1", "1001", 0, "Gare C"),
            TripEntity("trip-t2", "R2", "S1", "1002", 0, "Gare B"),
            TripEntity("trip-t4", "R2", "S1", "1004", 0, "Gare C"),
            TripEntity("trip-p1", "R1", "S1", "2001", 0, "Gare X"),
            TripEntity("trip-p2", "R2", "S1", "2002", 0, "Gare Z"),
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
            st("trip-p1", 0, "StopPoint:OCETRAIN-Y", hhmm(10, 0), hhmm(10, 0)),
            st("trip-p1", 1, "StopPoint:OCETRAIN-X1", hhmm(11, 0), hhmm(11, 0)),
            st("trip-p2", 0, "StopPoint:OCETER-X2", hhmm(11, 20), hhmm(11, 20)),
            st("trip-p2", 1, "StopPoint:OCETRAIN-Z", hhmm(12, 0), hhmm(12, 0)),
        )
        val calendar = listOf(CalendarDateEntity("S1", 20260921, 1))
        val idx = TimetableIndex.build(stops, routes, trips, stopTimes, calendar)
        router = RaptorRouter(idx)
    }

    @Test
    fun `trajet direct trouve`() {
        val journeys = router.search("StopArea:OCEA", "StopArea:OCEC", date, hhmm(9, 0))
        assertTrue(journeys.isNotEmpty())
        val j = journeys.first()
        assertEquals(1, j.legs.size)
        assertEquals(hhmm(10, 0), j.departureSec)
        assertEquals(hhmm(11, 0), j.arrivalSec)
        assertEquals("1001", j.legs.first().trainNumber)
        assertEquals("Gare A", j.legs.first().boardStation)
        assertEquals("Gare C", j.legs.first().alightStation)
    }

    @Test
    fun `aucun trajet avant le premier depart`() {
        val journeys = router.search("StopArea:OCEA", "StopArea:OCEC", date, hhmm(9, 1))
            .filter { it.totalDeparture >= hhmm(10, 0) }
        // après 10:00 le seul A->C direct est parti ; il reste la correspondance
        assertTrue(journeys.isNotEmpty())
        val withTransfer = journeys.first { it.legs.size == 2 }
        assertEquals(hhmm(12, 0), withTransfer.departureSec)
        assertEquals(hhmm(13, 30), withTransfer.arrivalSec)
        assertEquals("Gare B", withTransfer.legs[0].alightStation)
    }

    @Test
    fun `recherche apres le direct donne la correspondance`() {
        val journeys = router.search("StopArea:OCEA", "StopArea:OCEC", date, hhmm(10, 1))
        assertTrue(journeys.isNotEmpty())
        assertEquals(hhmm(12, 0), journeys.first().departureSec)
        assertEquals(2, journeys.first().legs.size)
    }

    @Test
    fun `correspondance meme gare avec 5 minutes`() {
        val journeys = router.search("StopArea:OCEY", "StopArea:OCEZ", date, hhmm(9, 30))
        assertEquals(1, journeys.size)
        val j = journeys.first()
        assertEquals(2, j.legs.size)
        assertEquals(hhmm(10, 0), j.departureSec)
        assertEquals(hhmm(12, 0), j.arrivalSec)
        assertEquals("Gare X", j.legs[0].alightStation)
        assertEquals("Gare X", j.legs[1].boardStation)
    }

    @Test
    fun `correspondance trop courte refusee`() {
        // arrivée X-TER 11:00 sur le point X3 ; départ du point X4 (même gare) à 11:02 :
        // il faut 5 min pour changer de point dans la gare -> connexion impossible.
        val stops = listOf(
            StopEntity("StopArea:OCEX2", "Gare X", "gare x", 0.0, 0.0, null),
            StopEntity("StopPoint:OCETER-X3", "Gare X", "gare x", 0.0, 0.0, "StopArea:OCEX2"),
            StopEntity("StopPoint:OCETGV-X4", "Gare X", "gare x", 0.0, 0.0, "StopArea:OCEX2"),
            StopEntity("StopArea:OCEY2", "Gare Y", "gare y", 0.0, 0.0, null),
            StopEntity("StopPoint:OCETRAIN-Y2", "Gare Y", "gare y", 0.0, 0.0, "StopArea:OCEY2"),
            StopEntity("StopArea:OCEZ2", "Gare Z", "gare z", 0.0, 0.0, null),
            StopEntity("StopPoint:OCETRAIN-Z2", "Gare Z", "gare z", 0.0, 0.0, "StopArea:OCEZ2"),
        )
        val trips = listOf(
            TripEntity("q1", "R1", "S1", "3001", 0, null),
            TripEntity("q2", "R1", "S1", "3002", 0, null),
        )
        val stopTimes = listOf(
            StopTimeEntity("q1", 0, "StopPoint:OCETRAIN-Y2", hhmm(10, 0), hhmm(10, 0), 0, 0),
            StopTimeEntity("q1", 1, "StopPoint:OCETER-X3", hhmm(11, 0), hhmm(11, 0), 0, 0),
            StopTimeEntity("q2", 0, "StopPoint:OCETGV-X4", hhmm(11, 2), hhmm(11, 2), 0, 0),
            StopTimeEntity("q2", 1, "StopPoint:OCETRAIN-Z2", hhmm(11, 30), hhmm(11, 30), 0, 0),
        )
        val idx = TimetableIndex.build(
            stops, emptyList(), trips, stopTimes,
            listOf(CalendarDateEntity("S1", 20260921, 1)),
        )
        val r = RaptorRouter(idx)
        val journeys = r.search("StopArea:OCEY2", "StopArea:OCEZ2", date, hhmm(9, 0))
        assertTrue(journeys.isEmpty())
    }

    @Test
    fun `jour different ignore les trains`() {
        val journeys = router.search("StopArea:OCEA", "StopArea:OCEC", date.plusDays(1), hhmm(9, 0))
        assertTrue(journeys.isEmpty())
    }

    @Test
    fun `plusieurs resultats successifs`() {
        val journeys = router.search("StopArea:OCEA", "StopArea:OCEC", date, hhmm(9, 0), maxResults = 8)
        assertEquals(2, journeys.size)
        assertEquals(hhmm(10, 0), journeys[0].departureSec)
        assertEquals(hhmm(12, 0), journeys[1].departureSec)
    }

    @Test
    fun `meme origine et destination`() {
        val journeys = router.search("StopArea:OCEA", "StopArea:OCEA", date, hhmm(9, 0))
        assertTrue(journeys.isEmpty())
    }

    // ---- recherche « arriver à » ----

    @Test
    fun `arriver avant 13h30 garde la correspondance tardive`() {
        val js = router.searchArriveBy("StopArea:OCEA", "StopArea:OCEC", date, hhmm(13, 30))
        assertEquals(2, js.size)
        // du départ le plus tardif au plus tôt
        assertEquals(hhmm(12, 0), js[0].departureSec)
        assertEquals(hhmm(13, 30), js[0].arrivalSec)
        assertEquals(hhmm(10, 0), js[1].departureSec)
        assertEquals(hhmm(11, 0), js[1].arrivalSec)
    }

    @Test
    fun `arriver avant 11h30 ne garde que le direct`() {
        val js = router.searchArriveBy("StopArea:OCEA", "StopArea:OCEC", date, hhmm(11, 30))
        assertEquals(1, js.size)
        assertEquals(hhmm(10, 0), js[0].departureSec)
        assertEquals(hhmm(11, 0), js[0].arrivalSec)
    }

    @Test
    fun `arriver avant 10h30 impossible`() {
        val js = router.searchArriveBy("StopArea:OCEA", "StopArea:OCEC", date, hhmm(10, 30))
        assertTrue(js.isEmpty())
    }

    @Test
    fun `arriver avant minuit garde tout`() {
        val js = router.searchArriveBy("StopArea:OCEA", "StopArea:OCEC", date, hhmm(23, 59))
        // fenêtre 14 h : depuis 9 h 59, on retrouve direct + correspondance
        assertTrue(js.size >= 2)
        assertEquals(hhmm(12, 0), js[0].departureSec)
    }
}
