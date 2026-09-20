package fr.webtvmedia.entrain

import fr.webtvmedia.entrain.data.prim.PrimRepository
import fr.webtvmedia.entrain.data.prim.PrimPassage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrimParserTest {

    @Test
    fun `parse une reponse siri-lite complete`() {
        val json = """
        {
          "ServiceDelivery": {
            "ResponseTimestamp": "2026-09-20T13:00:00Z",
            "StopMonitoringDelivery": [
              {
                "MonitoringRef": {"value": "IDFM:71371"},
                "MonitoredStopVisit": [
                  {
                    "MonitoredVehicleJourney": {
                      "LineRef": {"value": "IDFM:C01736"},
                      "PublishedLineName": [{"Lang": "fr", "value": "RER A"}],
                      "DestinationName": [{"Lang": "fr", "value": "Marne-la-Vallée Chessy"}],
                      "TrainNumber": "ZB123",
                      "MonitoredCall": {
                        "AimedDepartureTime": "2026-09-20T15:12:00+02:00",
                        "ExpectedDepartureTime": "2026-09-20T15:17:00+02:00"
                      }
                    }
                  },
                  {
                    "MonitoredVehicleJourney": {
                      "LineRef": {"value": "IDFM:C01311"},
                      "PublishedLineName": [{"Lang": "fr", "value": "H"}],
                      "DestinationName": [{"Lang": "fr", "value": "Pontoise"}],
                      "MonitoredCall": {
                        "AimedDepartureTime": "2026-09-20T15:25:00+02:00"
                      }
                    }
                  },
                  {
                    "MonitoredVehicleJourney": {
                      "LineRef": {"value": "IDFM:B01234"},
                      "MonitoredCall": { "AimedDepartureTime": "2026-09-20T15:30:00+02:00" }
                    }
                  }
                ]
              }
            ]
          }
        }
        """.trimIndent()
        val passages = PrimRepository.parseStopMonitoring(json, "IDFM:71371")
        // la ligne de bus B01234 doit être écartée
        assertEquals(2, passages.size)
        val first = passages[0]
        assertEquals("RER A", first.lineLabel)
        assertEquals("Marne-la-Vallée Chessy", first.destination)
        assertEquals("ZB123", first.trainNumber)
        // 2026-09-20T00:00Z = 1789862400 ; 15:12+02:00 = 13:12Z = +47520 s
        assertEquals(1789909920L, first.aimedEpoch)
        assertEquals(1789910220L, first.expectedEpoch)
        // retard 5 min
        assertEquals(300L, first.expectedEpoch!! - first.aimedEpoch!!)
        // le 2e n'a pas d'estimation
        assertTrue(passages[1].expectedEpoch == null)
    }

    @Test
    fun `reponse invalide renvoie vide`() {
        assertTrue(PrimRepository.parseStopMonitoring("{}", "x").isEmpty())
        assertTrue(PrimRepository.parseStopMonitoring("pas du tout du json", "x").isEmpty())
    }

    @Test
    fun `conversion en departures`() {
        val passages = listOf(
            PrimPassage(
                idfmRef = "IDFM:71371",
                lineLabel = "RER A",
                destination = "Chessy",
                aimedEpoch = 1789909920L,
                expectedEpoch = 1789910220L,
                trainNumber = "ZB123",
            ),
        )
        val deps = PrimRepository.toDepartures(passages)
        assertEquals(1, deps.size)
        val d = deps.first()
        assertEquals(fr.webtvmedia.entrain.domain.model.TrainCategory.TRANSILIEN, d.category)
        assertEquals("RER A", d.lineLabel)
        assertEquals(5L, d.delayedMin)
        assertEquals(1789910220L, d.estimatedEpoch)
        assertEquals(15 * 3600 + 12 * 60, d.scheduledSec) // 15:12 heure de Paris
    }
}
