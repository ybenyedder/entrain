package fr.webtvmedia.entrain

import fr.webtvmedia.entrain.data.gtfs.CsvReader
import fr.webtvmedia.entrain.util.TextUtils
import fr.webtvmedia.entrain.util.TimeUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.io.ByteArrayInputStream

class CsvReaderTest {
    @Test
    fun `parse en-tete et lignes simples`() {
        val csv = "a,b,c\n1,2,3\n4,,6\n"
        val r = CsvReader(ByteArrayInputStream(csv.toByteArray()))
        assertEquals(listOf("a", "b", "c"), r.readHeader())
        assertEquals(mapOf("a" to "1", "b" to "2", "c" to "3"), r.readRow())
        assertEquals(mapOf("a" to "4", "b" to "", "c" to "6"), r.readRow())
        assertNull(r.readRow())
    }

    @Test
    fun `guillemets et virgules internes`() {
        val csv = "name,desc\n\"DUPONT, Jean\",\"il dit \"\"bonjour\"\"\"\n"
        val r = CsvReader(ByteArrayInputStream(csv.toByteArray()))
        r.readHeader()
        val row = r.readRow()!!
        assertEquals("DUPONT, Jean", row["name"])
        assertEquals("il dit \"bonjour\"", row["desc"])
    }

    @Test
    fun `ligne vide ignoree`() {
        val csv = "a\n\n1\n"
        val r = CsvReader(ByteArrayInputStream(csv.toByteArray()))
        r.readHeader()
        assertEquals(mapOf("a" to "1"), r.readRow())
        assertNull(r.readRow())
    }
}

class TextUtilsTest {
    @Test
    fun `normalisation accents`() {
        assertEquals("gare de montpellier", TextUtils.normalize("Gare de MÓntpellïer"))
        assertEquals("paris gare de lyon", TextUtils.normalize("Paris Gare de Lyon"))
    }

    @Test
    fun `html vers texte`() {
        val html = "<p>Bonjour&nbsp;le <b>monde</b></p><p>Suite</p>"
        assertEquals("Bonjour le monde\nSuite", TextUtils.htmlToText(html))
    }

    @Test
    fun `entites numeriques`() {
        assertEquals("é è", TextUtils.htmlToText("&#233; &#232;"))
    }
}

class TimeUtilsTest {
    @Test
    fun `secondes gtfs`() {
        assertEquals(3600 + 5 * 60, TimeUtils.parseGtfsSec("01:05:00"))
        assertEquals(25 * 3600 + 10 * 60, TimeUtils.parseGtfsSec("25:10:00"))
        assertNull(TimeUtils.parseGtfsSec(""))
        assertNull(TimeUtils.parseGtfsSec("xx:00:00"))
    }

    @Test
    fun `format hhmm`() {
        assertEquals("10:05", TimeUtils.hhmm(10 * 3600 + 5 * 60))
        assertEquals("01:10", TimeUtils.hhmm(25 * 3600 + 10 * 60)) // 25:10 -> 01:10
    }

    @Test
    fun `duree`() {
        assertEquals("45 min", TimeUtils.duration(45 * 60))
        assertEquals("2 h 05", TimeUtils.duration(2 * 3600 + 5 * 60))
    }

    @Test
    fun `epoch paris`() {
        // 2026-09-21 10:00 Europe/Paris = 08:00 UTC (UTC+2 en septembre)
        val epoch = TimeUtils.naiveToEpoch(java.time.LocalDate.of(2026, 9, 21), 10 * 3600)
        val utc = java.time.Instant.ofEpochSecond(epoch).atZone(java.time.ZoneOffset.UTC)
        assertEquals(8, utc.hour)
    }
}
