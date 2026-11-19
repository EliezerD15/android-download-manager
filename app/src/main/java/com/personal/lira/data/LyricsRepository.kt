package com.personal.lira.data

import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class LyricsRepository {
    fun findLyrics(title: String, artist: String): String? {
        val query = "track_name=${encode(title)}&artist_name=${encode(artist)}"
        val connection = URL("https://lrclib.net/api/search?$query")
            .openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 15_000
            connection.readTimeout = 20_000
            connection.setRequestProperty("User-Agent", "LiraAndroid/1.0 (personal-use app)")
            connection.setRequestProperty("Lrclib-Client", "LiraAndroid/1.0")

            if (connection.responseCode !in 200..299) return null
            val payload = connection.inputStream.bufferedReader().use { it.readText() }
            val results = JSONArray(payload)
            val normalizedTitle = title.trim().lowercase()
            val normalizedArtist = artist.trim().lowercase()
            var bestLyrics: String? = null
            var bestScore = -1
            for (index in 0 until results.length()) {
                val record = results.optJSONObject(index) ?: continue
                val synced = record.optString("syncedLyrics").takeUnless { it == "null" || it.isBlank() }
                val plain = record.optString("plainLyrics").takeUnless { it == "null" || it.isBlank() }
                val lyrics = synced ?: plain
                if (lyrics != null) {
                    val resultTitle = record.optString("trackName", record.optString("name"))
                        .trim()
                        .lowercase()
                    val resultArtist = record.optString("artistName").trim().lowercase()
                    val score = (if (resultTitle == normalizedTitle) 2 else 0) +
                        (if (resultArtist == normalizedArtist) 1 else 0)
                    if (score > bestScore) {
                        bestScore = score
                        bestLyrics = lyrics
                    }
                }
            }
            return bestLyrics
        } finally {
            connection.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, Charsets.UTF_8.name())
}
