package com.github.kr328.clash.update

import com.github.kr328.clash.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import java.io.IOException
import java.util.concurrent.TimeUnit

/** The newest release published on the fork's GitHub Releases page. */
data class Release(val tag: String, val name: String, val notes: String, val pageUrl: String)

/**
 * Checks the fork's GitHub Releases for a newer build.
 *
 * Fork builds share upstream's versionName (debug builds of the same version differ only by
 * tag), so instead of comparing version numbers the APK carries the tag it was released under
 * ([BuildConfig.RELEASE_TAG]) and any newer tag counts as an update. Pre-releases are included,
 * since the fork's debug builds are published as pre-releases.
 */
object UpdateChecker {
    private const val RELEASES_API =
        "https://api.github.com/repos/muzi-xiaoren/ClashMetaForAndroid/releases?per_page=1"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    /** Tag of the running build, or empty for a local build. */
    val currentTag: String
        get() = BuildConfig.RELEASE_TAG

    /** Fetches the newest release. Blocking — call off the main thread. */
    fun latest(): Release {
        val request = Request.Builder()
            .url(RELEASES_API)
            .header("Accept", "application/vnd.github+json")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")

            val releases = JSONArray(response.body?.string().orEmpty())
            if (releases.length() == 0) throw IOException("no release found")

            val release = releases.getJSONObject(0)
            val tag = release.getString("tag_name")

            return Release(
                tag = tag,
                name = release.optString("name").ifEmpty { tag },
                notes = release.optString("body").trim(),
                pageUrl = release.getString("html_url"),
            )
        }
    }

    fun isNewer(release: Release): Boolean = release.tag != currentTag
}
