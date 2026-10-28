package com.andoni.signosdaily.update

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.andoni.signosdaily.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val versionCode: Int, val versionName: String, val notes: String, val apkUrl: String)

/** Checks GitHub Releases for a newer APK and installs it. */
object Updater {
    private const val LATEST_RELEASE = "https://api.github.com/repos/andonialvarez/signos_app/releases/latest"

    /** Returns the latest release if it is newer than the installed app, otherwise null. */
    suspend fun check(): UpdateInfo? = withContext(Dispatchers.IO) {
        if (!BuildConfig.SELF_UPDATE) return@withContext null
        runCatching {
            val conn = (URL(LATEST_RELEASE).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/vnd.github+json")
            }
            val json = conn.inputStream.bufferedReader().use { JSONObject(it.readText()) }
            conn.disconnect()
            // Tags look like v1.0.<versionCode>.
            val tag = json.getString("tag_name").removePrefix("v")
            val code = tag.substringAfterLast('.').toIntOrNull() ?: return@runCatching null
            val assets = json.getJSONArray("assets")
            val urls = List(assets.length()) { assets.getJSONObject(it) }
                .filter { it.getString("name").endsWith(".apk") }
                .map { it.getString("browser_download_url") }
            val apk = urls.firstOrNull() ?: return@runCatching null
            if (code > BuildConfig.VERSION_CODE) {
                UpdateInfo(code, tag, json.optString("body").trim(), apk)
            } else null
        }.getOrNull()
    }

    /** Downloads the APK into the cache, reporting progress from 0 to 1. */
    suspend fun download(context: Context, info: UpdateInfo, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val file = File(dir, "SignosDaily-${info.versionName}.apk")
            val conn = (URL(info.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
                instanceFollowRedirects = true
            }
            val total = conn.contentLengthLong
            conn.inputStream.use { input ->
                file.outputStream().use { output ->
                    val buffer = ByteArray(64 * 1024)
                    var read = 0L
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        read += n
                        if (total > 0) onProgress(read.toFloat() / total)
                    }
                }
            }
            conn.disconnect()
            file
        }

    /** Opens the system installer; Android asks once to allow installs from this app. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.files", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
