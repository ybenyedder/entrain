package fr.webtvmedia.entrain.util

import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TextUtils {
    /** minuscules sans accents ni ponctuation, pour la recherche de gares. */
    fun normalize(s: String): String =
        Normalizer.normalize(s.lowercase(Locale.FRENCH), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .replace("œ", "oe")
            .replace("æ", "ae")
            .replace(Regex("[^a-z0-9]+"), " ")
            .trim()

    /** Convertit une description HTML des flux SNCF en texte lisible. */
    fun htmlToText(html: String): String {
        var t = html
            .replace(Regex("(?i)<\\s*br\\s*/?\\s*>"), "\n")
            .replace(Regex("(?i)</\\s*p\\s*>"), "\n")
            .replace(Regex("(?i)</\\s*(div|li|tr|h[1-6])\\s*>"), "\n")
            .replace(Regex("(?i)<\\s*li[^>]*>"), "• ")
            .replace(Regex("<[^>]+>"), "")
        val entities = mapOf(
            "&nbsp;" to " ", "&amp;" to "&", "&lt;" to "<", "&gt;" to ">",
            "&quot;" to "\"", "&#39;" to "'", "&apos;" to "'", "&laquo;" to "«",
            "&raquo;" to "»", "&eacute;" to "é", "&egrave;" to "è", "&agrave;" to "à",
            "&ccedil;" to "ç", "&ecirc;" to "ê", "&ocirc;" to "ô", "&hellip;" to "…",
            "&ndash;" to "–", "&mdash;" to "—",
        )
        for ((k, v) in entities) t = t.replace(k, v)
        t = t.replace(Regex("&#(\\d+);")) { m -> m.groupValues[1].toIntOrNull()?.toChar()?.toString() ?: "" }
        return t.replace(Regex("[ \\t]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
    }
}

object TimeUtils {
    val PARIS: ZoneId = ZoneId.of("Europe/Paris")

    fun hhmm(totalSec: Int): String {
        val s = ((totalSec % 86400) + 86400) % 86400
        return String.format(Locale.FRANCE, "%02d:%02d", s / 3600, (s % 3600) / 60)
    }

    /** "2 h 05" / "45 min" */
    fun duration(totalSec: Int): String {
        val min = (totalSec / 60).coerceAtLeast(0)
        return if (min >= 60) "${min / 60} h ${String.format(Locale.FRANCE, "%02d", min % 60)}"
        else "${min} min"
    }

    fun ymd(date: LocalDate): Int =
        date.year * 10000 + date.monthValue * 100 + date.dayOfMonth

    fun parseYmd(v: Int): LocalDate =
        LocalDate.of(v / 10000, (v % 10000) / 100, v % 100)

    /** Secondes GTFS (00:00:00 à 48:00:00+) depuis "HH:MM:SS". */
    fun parseGtfsSec(s: String): Int? {
        if (s.isBlank()) return null
        val p = s.split(":")
        if (p.size != 3) return null
        val h = p[0].toIntOrNull() ?: return null
        val m = p[1].toIntOrNull() ?: return null
        val sec = p[2].toIntOrNull() ?: return null
        return h * 3600 + m * 60 + sec
    }

    /** epoch d'un horaire GTFS naïf (date + secondes locales) en Europe/Paris. */
    fun naiveToEpoch(date: LocalDate, sec: Int): Long =
        date.atStartOfDay(PARIS).toEpochSecond() + sec

    fun epochToLocalSec(epoch: Long): Int {
        val t = Instant.ofEpochSecond(epoch).atZone(PARIS)
        return t.hour * 3600 + t.minute * 60 + t.second
    }

    fun epochToLocalDate(epoch: Long): LocalDate =
        Instant.ofEpochSecond(epoch).atZone(PARIS).toLocalDate()

    fun dayKey(date: LocalDate): String = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
}
