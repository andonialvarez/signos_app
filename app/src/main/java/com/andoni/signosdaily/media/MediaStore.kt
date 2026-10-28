package com.andoni.signosdaily.media

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.andoni.signosdaily.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Sign videos (`<key>.mp4`) and photos (`<key>.jpg`) from the "media" GitHub release, which the
 * "Vídeos de los signos" workflow builds from DILSE. Each file is downloaded once and kept in the
 * app's private storage, so everything already seen works offline.
 */
class MediaStore(context: Context) {

    private val app = context.applicationContext
    private val dir = File(app.filesDir, "signs")
    private val locks = ConcurrentHashMap<String, Mutex>()

    /** Keys published in the media release with their size in bytes (video + photo); null if unknown. */
    @Volatile var available: Map<String, Long>? = null
        private set

    fun hasVideo(key: String) = File(dir, "$key.mp4").length() > 0

    /** Returns the local video, downloading it first if needed; null when offline or not published yet. */
    suspend fun video(key: String): File? = fetch("$key.mp4")

    suspend fun photo(key: String): File? = fetch("$key.jpg")

    private suspend fun fetch(name: String): File? = withContext(Dispatchers.IO) {
        val target = File(dir, name)
        if (target.length() > 0) return@withContext target
        locks.getOrPut(name) { Mutex() }.withLock {
            if (target.length() > 0) return@withLock target
            runCatching {
                dir.mkdirs()
                val tmp = File(dir, "$name.part")
                download(BuildConfig.MEDIA_BASE_URL + name, tmp)
                if (tmp.length() > 0 && tmp.renameTo(target)) target else null
            }.getOrNull()
        }
    }

    private fun download(url: String, into: File) {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
        }
        try {
            if (conn.responseCode != HttpURLConnection.HTTP_OK) error("HTTP ${conn.responseCode} for $url")
            conn.inputStream.use { input -> into.outputStream().use { input.copyTo(it) } }
        } finally {
            conn.disconnect()
        }
    }

    /** Refreshes the list of published signs (at most every 12 hours unless [force]). */
    suspend fun refreshCatalog(force: Boolean = false): Map<String, Long>? = withContext(Dispatchers.IO) {
        val file = File(dir, "manifest.json")
        val fresh = file.exists() && System.currentTimeMillis() - file.lastModified() < 12 * 60 * 60 * 1000L
        if (force || !fresh) {
            runCatching {
                dir.mkdirs()
                val tmp = File(dir, "manifest.json.part")
                download(BuildConfig.MEDIA_BASE_URL + "manifest.json", tmp)
                tmp.renameTo(file)
            }
        }
        available = runCatching {
            val items = JSONObject(file.readText()).getJSONObject("items")
            items.keys().asSequence().associateWith { key ->
                val o = items.getJSONObject(key)
                o.optLong("mp4") + o.optLong("jpg")
            }
        }.getOrNull() ?: available
        available
    }

    /** True when the catalog is known and the sign is not in it (DILSE had no video for it). */
    fun isMissing(key: String): Boolean = available?.let { key !in it } ?: false

    fun isOnline(): Boolean {
        val cm = app.getSystemService(ConnectivityManager::class.java) ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /** Downloads the given signs in the background, ignoring failures. */
    suspend fun prefetch(keys: Collection<String>) {
        if (!isOnline()) return
        for (key in keys) {
            if (isMissing(key)) continue
            video(key)
            photo(key)
        }
    }

    /** Downloads every published sign, for offline use. Reports (done, total). */
    suspend fun downloadAll(keys: Collection<String>, onProgress: (Int, Int) -> Unit): Int {
        val todo = keys.filter { !isMissing(it) }
        var failed = 0
        todo.forEachIndexed { i, key ->
            if (video(key) == null) failed++
            photo(key)
            onProgress(i + 1, todo.size)
        }
        return failed
    }

    fun downloadedCount(keys: Collection<String>): Int = keys.count(::hasVideo)

    fun bytesOnDisk(): Long = dir.listFiles()?.sumOf { it.length() } ?: 0L

    /** Bytes still to download to have every sign offline, when the catalog is known. */
    fun bytesToDownload(keys: Collection<String>): Long? =
        available?.let { catalog -> keys.filter { !hasVideo(it) }.sumOf { catalog[it] ?: 0L } }

    fun clear() {
        dir.listFiles()?.filter { it.name != "manifest.json" }?.forEach { it.delete() }
    }

    companion object {
        @Volatile private var instance: MediaStore? = null
        fun get(context: Context): MediaStore =
            instance ?: synchronized(this) {
                instance ?: MediaStore(context).also { instance = it }
            }
    }
}
