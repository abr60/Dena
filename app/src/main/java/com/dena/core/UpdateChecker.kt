package com.dena.core

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class ReleaseInfo(
    val tagName: String,
    val name: String,
    val body: String,
    val downloadUrl: String,
    val sizeBytes: Long,
    val htmlUrl: String,
)

object UpdateChecker {
    private const val OWNER = "abr60"
    private const val REPO = "Dena"
    private const val API_URL = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"

    suspend fun fetchLatest(): Result<ReleaseInfo> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("User-Agent", "Dena-Updater")
                connectTimeout = 8000
                readTimeout = 8000
            }
            val code = conn.responseCode
            if (code != 200) {
                val err = conn.errorStream?.bufferedReader()?.readText()?.take(300) ?: "HTTP $code"
                return@withContext Result.failure(Exception("GitHub API $code: $err"))
            }
            val json = conn.inputStream.bufferedReader().readText()
            val obj = JSONObject(json)
            val tag = obj.optString("tag_name", "")
            val name = obj.optString("name", tag)
            val body = obj.optString("body", "")
            val htmlUrl = obj.optString("html_url", "https://github.com/$OWNER/$REPO/releases")
            val assets = obj.optJSONArray("assets")
            var downloadUrl = htmlUrl
            var size: Long = 0
            if (assets != null && assets.length() > 0) {
                // Prefer APK asset, fallback to first asset
                var apk: JSONObject? = null
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    if (a.optString("name").endsWith(".apk", ignoreCase = true)) { apk = a; break }
                }
                val chosen = apk ?: assets.getJSONObject(0)
                downloadUrl = chosen.optString("browser_download_url", htmlUrl)
                size = chosen.optLong("size", 0)
            }
            Result.success(ReleaseInfo(tag, name, body, downloadUrl, size, htmlUrl))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun isNewer(latestTag: String, currentVersion: String): Boolean {
        fun norm(v: String) = v.trim().removePrefix("v").removePrefix("V")
        return compareVersions(norm(latestTag), norm(currentVersion)) > 0
    }

    private fun compareVersions(a: String, b: String): Int {
        // Split on . and - ; compare numerically where possible, else lexicographically
        val pa = a.split('.', '-')
        val pb = b.split('.', '-')
        val len = maxOf(pa.size, pb.size)
        for (i in 0 until len) {
            val sa = pa.getOrNull(i) ?: ""
            val sb = pb.getOrNull(i) ?: ""
            val na = sa.toIntOrNull()
            val nb = sb.toIntOrNull()
            val cmp = when {
                na != null && nb != null -> na.compareTo(nb)
                na != null -> 1 // numeric > pre-release label
                nb != null -> -1
                else -> sa.compareTo(sb)
            }
            if (cmp != 0) return cmp
        }
        return 0
    }

    fun formatSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val mb = bytes / (1024.0 * 1024.0)
        return if (mb >= 1) String.format(java.util.Locale.US, "%.1f MB", mb)
        else String.format(java.util.Locale.US, "%d KB", bytes / 1024)
    }
}
