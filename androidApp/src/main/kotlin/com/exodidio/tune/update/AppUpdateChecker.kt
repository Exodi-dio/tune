package com.exodidio.tune.update

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import com.exodidio.tune.BuildConfig
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

const val TuneReleasesLatestUrl = "https://api.github.com/repos/Exodi-dio/tune/releases/latest"
private const val UpdateConnectTimeoutMs = 10_000
private const val UpdateReadTimeoutMs = 15_000
const val UpdateMaxResponseBytes = 256 * 1024
const val UpdateApkSubdir = "updates"

data class AppRelease(
    val tag: String,
    val version: String,
    val notes: String,
    val apkUrl: String?,
)

/** Strips a single leading `v`/`V` (when followed by a digit) and trims whitespace. */
fun normalizeVersionTag(tag: String): String {
    var value = tag.trim()
    if (value.length > 1 && (value[0] == 'v' || value[0] == 'V') && value[1].isDigit()) {
        value = value.substring(1).trim()
    }
    return value
}

private fun semverCore(version: String): String {
    val dash = version.indexOf('-')
    val plus = version.indexOf('+')
    val end = when {
        dash >= 0 && plus >= 0 -> minOf(dash, plus)
        dash >= 0 -> dash
        plus >= 0 -> plus
        else -> version.length
    }
    return version.substring(0, end)
}

private fun prereleaseOf(normalized: String): String? {
    val dash = normalized.indexOf('-')
    if (dash < 0) return null
    val plus = normalized.indexOf('+', dash)
    return if (plus < 0) normalized.substring(dash + 1) else normalized.substring(dash + 1, plus)
}

private fun parseSemverParts(normalized: String): List<Int>? {
    val core = semverCore(normalized)
    if (core.isBlank()) return null
    return runCatching {
        core.split('.').map { part ->
            if (part.isBlank() || part.any { !it.isDigit() }) return null
            part.toInt()
        }
    }.getOrNull()
}

/**
 * Numeric semver compare on the normalized form.
 * Prereleases sort before their release; malformed input falls back to lexical order.
 */
fun compareSemver(a: String, b: String): Int {
    val normalizedA = normalizeVersionTag(a)
    val normalizedB = normalizeVersionTag(b)
    val partsA = parseSemverParts(normalizedA)
    val partsB = parseSemverParts(normalizedB)
    if (partsA == null || partsB == null) return normalizedA.compareTo(normalizedB)
    val width = maxOf(partsA.size, partsB.size)
    for (index in 0 until width) {
        val left = partsA.getOrElse(index) { 0 }
        val right = partsB.getOrElse(index) { 0 }
        if (left != right) return left.compareTo(right)
    }
    val preA = prereleaseOf(normalizedA)
    val preB = prereleaseOf(normalizedB)
    if (preA == null && preB == null) return 0
    if (preA == null) return 1
    if (preB == null) return -1
    return preA.compareTo(preB)
}

/** True when [latestTag] is strictly newer than [currentVersion]. */
fun isUpdateAvailable(currentVersion: String, latestTag: String): Boolean =
    compareSemver(latestTag, currentVersion) > 0

/**
 * Allowlist for APK download URLs. Only `https` URLs on GitHub release hosts are accepted;
 * `file:`, `content:`, plain `http:`, and off-allowlist hosts are rejected. Uses java.net.URI
 * so the check is unit-testable on the JVM (no android.net.Uri dependency).
 */
fun isAllowedApkUrl(rawUrl: String): Boolean {
    val uri = runCatching { URI(rawUrl.trim()) }.getOrNull() ?: return false
    if (!uri.scheme.equals("https", ignoreCase = true)) return false
    val host = uri.host?.lowercase() ?: return false
    if (host == "github.com") return true
    if (host == "objects.githubusercontent.com") return true
    if (host == "githubusercontent.com") return true
    if (host.endsWith(".githubusercontent.com")) return true
    return false
}

private val ReleaseJson = Json { ignoreUnknownKeys = true }

/** Parses `GET .../releases/latest` into an [AppRelease]; first allowlisted `.apk` asset wins. */
fun parseLatestReleaseJson(json: String): AppRelease? = runCatching {
    val root = ReleaseJson.parseToJsonElement(json).jsonObject
    val tag = root["tag_name"]?.jsonPrimitive?.contentOrNull?.takeIf(String::isNotBlank) ?: return null
    val notes = root["body"]?.jsonPrimitive?.contentOrNull.orEmpty()
    val assets = root["assets"]?.jsonArray.orEmpty()
    var apkUrl: String? = null
    for (asset in assets) {
        val obj = runCatching { asset.jsonObject }.getOrNull() ?: continue
        val url = obj["browser_download_url"]?.jsonPrimitive?.contentOrNull.orEmpty()
        if (url.isBlank()) continue
        val name = obj["name"]?.jsonPrimitive?.contentOrNull.orEmpty()
        val urlPath = url.substringBefore('?').lowercase()
        if (name.lowercase().endsWith(".apk") || urlPath.endsWith(".apk")) {
            if (!isAllowedApkUrl(url)) continue
            apkUrl = url
            break
        }
    }
    AppRelease(tag = tag, version = normalizeVersionTag(tag), notes = notes, apkUrl = apkUrl)
}.getOrNull()

/**
 * Bounded read of the API response body (cap [maxBytes], default 256KB). Throws when the
 * payload exceeds the cap so captive-portal/huge responses cannot OOM the app.
 */
fun readBoundedText(input: InputStream, maxBytes: Int = UpdateMaxResponseBytes): String {
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(8 * 1024)
    var total = 0
    while (true) {
        val read = input.read(buffer)
        if (read < 0) break
        total += read
        if (total > maxBytes) error("Update check failed: response too large")
        out.write(buffer, 0, read)
    }
    return out.toString(Charsets.UTF_8.name())
}

/** Fetches the latest GitHub release; throws on network/parse failure (caller maps to error UI). */
suspend fun fetchLatestRelease(): AppRelease = withContext(Dispatchers.IO) {
    val connection = URL(TuneReleasesLatestUrl).openConnection() as HttpURLConnection
    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = UpdateConnectTimeoutMs
        connection.readTimeout = UpdateReadTimeoutMs
        connection.setRequestProperty("Accept", "application/vnd.github+json")
        connection.setRequestProperty("User-Agent", "Tune-Android/${BuildConfig.VERSION_NAME}")
        if (connection.responseCode !in 200..299) error("Update check failed: HTTP ${connection.responseCode}")
        val text = connection.inputStream.use { readBoundedText(it) }
        parseLatestReleaseJson(text) ?: error("Update check failed: invalid release payload")
    } finally {
        connection.disconnect()
    }
}

fun updateApkFileName(tag: String): String {
    val safe = normalizeVersionTag(tag).ifBlank { "latest" }.replace(Regex("[^A-Za-z0-9._-]+"), "_")
    return "tune-$safe.apk"
}

/** Pure path helper (JVM-testable): APK file under [externalCacheDir]/updates. */
fun updateApkExternalFile(externalCacheDir: File, tag: String): File {
    return File(File(externalCacheDir, UpdateApkSubdir), updateApkFileName(tag))
}

/** Relative destination path under the external cache dir (for DownloadManager/FileProvider). */
fun updateApkRelativePath(tag: String): String = "$UpdateApkSubdir/${updateApkFileName(tag)}"

/**
 * APK destination in the EXTERNAL cache dir (writable by the DownloadManager system uid via
 * `file:` URI, unlike the internal cacheDir). Falls back to the internal cache dir only when
 * externalCacheDir is null (e.g. storage not mounted). Served for install via the
 * `external-cache-path` FileProvider entry.
 */
fun updateApkFile(context: Context, tag: String): File {
    val base = context.externalCacheDir ?: context.cacheDir
    return updateApkExternalFile(base, tag)
}

/** Enqueues the APK via DownloadManager into the external app cache dir; returns the download ID. */
fun enqueueUpdateDownload(context: Context, apkUrl: String, tag: String): Long {
    require(isAllowedApkUrl(apkUrl)) { "Rejected APK host" }
    val target = updateApkFile(context, tag)
    target.parentFile?.mkdirs()
    if (target.isFile) target.delete()
    val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    // Note: DownloadManager.Request has no setDestinationInExternalCacheDir in the public SDK
    // (only setDestinationUri / setDestinationInExternalFilesDir / setDestinationInExternalPublicDir).
    // The external-cache equivalent is setDestinationUri with a file: URI under externalCacheDir,
    // which lives on external storage and is writable by the manager (unlike internal cacheDir).
    val request = DownloadManager.Request(Uri.parse(apkUrl))
        .setTitle("Tune $tag")
        .setDescription(apkUrl.substringAfterLast('/'))
        .setMimeType("application/vnd.android.package-archive")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationUri(Uri.fromFile(target))
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(true)
    return manager.enqueue(request)
}

/**
 * Installs a downloaded APK via FileProvider content URI. Throws when unknown-sources permission
 * is missing (caller surfaces Error with retry) or when the installer cannot be launched.
 */
fun installUpdateApk(context: Context, apkFile: File) {
    require(apkFile.isFile) { "Update APK is missing" }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        if (!context.packageManager.canRequestPackageInstalls()) {
            runCatching {
                val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(settingsIntent)
            }
            error("Unknown sources not granted")
        }
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
    val view = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    try {
        context.startActivity(view)
    } catch (e: Exception) {
        throw IllegalStateException("Could not launch installer", e)
    }
}
