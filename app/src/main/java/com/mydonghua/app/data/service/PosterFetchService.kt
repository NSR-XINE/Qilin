package com.mydonghua.app.data.service

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder

data class AutoFillDetails(
    val titleZh: String?,
    val synopsis: String?,
    val poster: String?,
    val totalEp: Int,
    val status: String?,
    val aniListId: Int? = null
)

object PosterFetchService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val original = chain.request()
            val request = original.newBuilder()
                .header("User-Agent", "Qilin/${com.mydonghua.app.BuildConfig.VERSION_NAME} (Android)")
                .build()
            chain.proceed(request)
        }
        .build()

    suspend fun fetchFromJikan(query: String): AutoFillDetails? = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("https://api.jikan.moe/v4/anime?q=$encodedQuery&limit=1")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                val data = json.optJSONArray("data") ?: return@withContext null
                if (data.length() == 0) return@withContext null
                val entry = data.getJSONObject(0)

                val images = entry.optJSONObject("images")
                val jpg = images?.optJSONObject("jpg")
                val imageUrl = jpg?.optString("large_image_url")

                val titleZh = entry.optString("title_japanese", "")
                val synopsis = entry.optString("synopsis", "")
                val episodes = entry.optInt("episodes", 0)
                val statusStr = entry.optString("status", "").lowercase()

                val status = when {
                    statusStr.contains("currently airing") -> "ongoing"
                    statusStr.contains("finished airing") -> "completed"
                    statusStr.contains("cancelled") -> "stopped"
                    else -> null
                }

                AutoFillDetails(
                    titleZh = titleZh.takeIf { it.isNotEmpty() && it != "null" },
                    synopsis = synopsis.takeIf { it.isNotEmpty() && it != "null" },
                    poster = imageUrl?.takeIf { it.isNotEmpty() && it != "null" },
                    totalEp = episodes,
                    status = status
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun fetchFromAniList(query: String): AutoFillDetails? = withContext(Dispatchers.IO) {
        try {
            val graphQLQuery = """
                query (${'$'}search: String) {
                  Page(page: 1, perPage: 1) {
                    media(search: ${'$'}search, type: ANIME) {
                      id
                      title {
                        native
                        romaji
                        english
                      }
                      coverImage {
                        extraLarge
                        large
                      }
                      description(asHtml: false)
                      episodes
                      status
                    }
                  }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("search", query)
            }
            val requestBodyJson = JSONObject().apply {
                put("query", graphQLQuery)
                put("variables", variables)
            }

            val requestBody = requestBodyJson.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("https://graphql.anilist.co/")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val bodyStr = response.body?.string() ?: return@withContext null
                val json = JSONObject(bodyStr)
                val data = json.optJSONObject("data") ?: return@withContext null
                val page = data.optJSONObject("Page") ?: return@withContext null
                val mediaArr = page.optJSONArray("media") ?: return@withContext null
                if (mediaArr.length() == 0) return@withContext null
                val media = mediaArr.getJSONObject(0)

                val titles = media.optJSONObject("title")
                val titleZh = titles?.optString("native")?.takeIf { it.isNotEmpty() } ?: titles?.optString("romaji")

                val coverImage = media.optJSONObject("coverImage")
                val imageUrl = coverImage?.optString("extraLarge")?.takeIf { it.isNotEmpty() } ?: coverImage?.optString("large")

                val description = media.optString("description", "")
                    .replace(Regex("<[^>]*>"), "")

                val episodes = media.optInt("episodes", 0)
                val statusStr = media.optString("status", "").lowercase()
                val status = when (statusStr) {
                    "releasing" -> "ongoing"
                    "finished" -> "completed"
                    "cancelled" -> "stopped"
                    else -> null
                }

                val mediaId = media.optInt("id", 0).takeIf { it > 0 }

                AutoFillDetails(
                    titleZh = titleZh?.takeIf { it.isNotEmpty() && it != "null" },
                    synopsis = description.takeIf { it.isNotEmpty() && it != "null" },
                    poster = imageUrl?.takeIf { it.isNotEmpty() && it != "null" },
                    totalEp = episodes,
                    status = status,
                    aniListId = mediaId
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun scrapePoster(url: String): String? = withContext(Dispatchers.IO) {
        if (!url.startsWith("http")) return@withContext null
        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val html = response.body?.string() ?: return@withContext null

                val ogImageRegex = Regex("""<meta[^>]+(?:property=["']og:image["'][^>]+content=["']([^"']+)["']|content=["']([^"']+)["'][^>]+property=["']og:image["'])""", RegexOption.IGNORE_CASE)
                val ogMatch = ogImageRegex.find(html)
                var imageUrl = ogMatch?.groupValues?.getOrNull(1)?.takeIf { it.isNotEmpty() }
                    ?: ogMatch?.groupValues?.getOrNull(2)?.takeIf { it.isNotEmpty() }

                if (imageUrl == null) {
                    val twitterRegex = Regex("""<meta[^>]+(?:name=["']twitter:image["'][^>]+content=["']([^"']+)["']|content=["']([^"']+)["'][^>]+name=["']twitter:image["'])""", RegexOption.IGNORE_CASE)
                    val twMatch = twitterRegex.find(html)
                    imageUrl = twMatch?.groupValues?.getOrNull(1)?.takeIf { it.isNotEmpty() }
                        ?: twMatch?.groupValues?.getOrNull(2)?.takeIf { it.isNotEmpty() }
                }

                if (imageUrl == null) {
                    val linkImageRegex = Regex("""<link[^>]+rel=["']image_src["'][^>]+href=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
                    imageUrl = linkImageRegex.find(html)?.groupValues?.getOrNull(1)
                }

                if (imageUrl != null) {
                    imageUrl = imageUrl.replace("&amp;", "&")
                    if (imageUrl.startsWith("//")) {
                        imageUrl = "https:$imageUrl"
                    }
                }
                imageUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    data class AniListSearchResult(
        val id: Int? = null,
        val title: String,
        val titleZh: String?,
        val synopsis: String?,
        val poster: String?,
        val totalEp: Int,
        val status: String?
    )

    suspend fun searchAniList(query: String, limit: Int = 10): List<AniListSearchResult> = withContext(Dispatchers.IO) {
        try {
            val graphQLQuery = """
                query (${'$'}search: String) {
                  Page(page: 1, perPage: $limit) {
                    media(search: ${'$'}search, type: ANIME) {
                      id
                      title {
                        native
                        romaji
                        english
                      }
                      coverImage {
                        extraLarge
                        large
                      }
                      description(asHtml: false)
                      episodes
                      status
                    }
                  }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("search", query)
            }
            val requestBodyJson = JSONObject().apply {
                put("query", graphQLQuery)
                put("variables", variables)
            }

            val requestBody = requestBodyJson.toString()
                .toRequestBody("application/json; charset=utf-8".toMediaType())

            val request = Request.Builder()
                .url("https://graphql.anilist.co/")
                .post(requestBody)
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyStr)
                val data = json.optJSONObject("data") ?: return@withContext emptyList()
                val page = data.optJSONObject("Page") ?: return@withContext emptyList()
                val mediaArr = page.optJSONArray("media") ?: return@withContext emptyList()

                val results = mutableListOf<AniListSearchResult>()
                for (i in 0 until mediaArr.length()) {
                    val media = mediaArr.getJSONObject(i)
                    val mediaId = media.optInt("id", 0).takeIf { it > 0 }
                    val titles = media.optJSONObject("title")
                    val english = titles?.optString("english")?.takeIf { it.isNotEmpty() && it != "null" }
                    val romaji = titles?.optString("romaji")?.takeIf { it.isNotEmpty() && it != "null" }
                    val native = titles?.optString("native")?.takeIf { it.isNotEmpty() && it != "null" }

                    val mainTitle = english ?: romaji ?: native ?: "Unknown Title"

                    val coverImage = media.optJSONObject("coverImage")
                    val imageUrl = coverImage?.optString("extraLarge")?.takeIf { it.isNotEmpty() } ?: coverImage?.optString("large")

                    val description = media.optString("description", "")
                        .replace(Regex("<[^>]*>"), "")

                    val episodes = media.optInt("episodes", 0)
                    val statusStr = media.optString("status", "").lowercase()
                    val status = when (statusStr) {
                        "releasing" -> "ongoing"
                        "finished" -> "completed"
                        "cancelled" -> "stopped"
                        else -> "ongoing"
                    }

                    results.add(
                        AniListSearchResult(
                            id = mediaId,
                            title = mainTitle,
                            titleZh = native,
                            synopsis = description.takeIf { it.isNotEmpty() && it != "null" },
                            poster = imageUrl?.takeIf { it.isNotEmpty() && it != "null" },
                            totalEp = episodes,
                            status = status
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    data class MyAnimeListSearchResult(
        val title: String,
        val titleZh: String?,
        val synopsis: String?,
        val poster: String?,
        val totalEp: Int,
        val status: String?
    )

    suspend fun searchMyAnimeList(query: String, limit: Int = 10): List<MyAnimeListSearchResult> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("https://api.jikan.moe/v4/anime?q=$encodedQuery&limit=$limit")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyStr)
                val data = json.optJSONArray("data") ?: return@withContext emptyList()

                val results = mutableListOf<MyAnimeListSearchResult>()
                for (i in 0 until data.length()) {
                    val entry = data.getJSONObject(i)
                    val title = entry.optString("title", "Unknown Title")
                    val titleZh = entry.optString("title_japanese", "").takeIf { it.isNotEmpty() && it != "null" }

                    val images = entry.optJSONObject("images")
                    val jpg = images?.optJSONObject("jpg")
                    val imageUrl = jpg?.optString("large_image_url")?.takeIf { it.isNotEmpty() && it != "null" }

                    val synopsis = entry.optString("synopsis", "").takeIf { it.isNotEmpty() && it != "null" }
                    val episodes = entry.optInt("episodes", 0)

                    val statusStr = entry.optString("status", "").lowercase()
                    val status = when {
                        statusStr.contains("currently airing") -> "ongoing"
                        statusStr.contains("finished airing") -> "completed"
                        statusStr.contains("cancelled") -> "stopped"
                        else -> "ongoing"
                    }

                    results.add(
                        MyAnimeListSearchResult(
                            title = title,
                            titleZh = titleZh,
                            synopsis = synopsis,
                            poster = imageUrl,
                            totalEp = episodes,
                            status = status
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    data class KitsuSearchResult(
        val title: String,
        val titleZh: String?,
        val synopsis: String?,
        val poster: String?,
        val totalEp: Int,
        val status: String?
    )

    suspend fun searchKitsu(query: String, limit: Int = 10): List<KitsuSearchResult> = withContext(Dispatchers.IO) {
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url("https://kitsu.io/api/edge/anime?filter[text]=$encodedQuery&page[limit]=$limit")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyStr)
                val data = json.optJSONArray("data") ?: return@withContext emptyList()

                val results = mutableListOf<KitsuSearchResult>()
                for (i in 0 until data.length()) {
                    val entry = data.getJSONObject(i)
                    val attr = entry.optJSONObject("attributes") ?: continue

                    val titlesObj = attr.optJSONObject("titles")
                    val en = titlesObj?.optString("en")?.takeIf { it.isNotEmpty() && it != "null" }
                    val jaJp = titlesObj?.optString("ja_jp")?.takeIf { it.isNotEmpty() && it != "null" }
                    val canonical = attr.optString("canonicalTitle", "Unknown Title")

                    val mainTitle = en ?: canonical
                    val posterObj = attr.optJSONObject("posterImage")
                    val imageUrl = posterObj?.optString("large")?.takeIf { it.isNotEmpty() && it != "null" }
                        ?: posterObj?.optString("original")?.takeIf { it.isNotEmpty() && it != "null" }

                    val synopsis = attr.optString("synopsis", "").takeIf { it.isNotEmpty() && it != "null" }
                    val episodes = attr.optInt("episodeCount", 0)
                    val statusStr = attr.optString("status", "").lowercase()
                    val status = when {
                        statusStr.contains("current") -> "ongoing"
                        statusStr.contains("finished") -> "completed"
                        else -> "ongoing"
                    }

                    results.add(
                        KitsuSearchResult(
                            title = mainTitle,
                            titleZh = jaJp,
                            synopsis = synopsis,
                            poster = imageUrl,
                            totalEp = episodes,
                            status = status
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    fun getDonghuaStreamUrl(query: String): String {
        return try {
            "https://donghuastream.org/?s=" + URLEncoder.encode(query, "UTF-8")
        } catch (e: Exception) {
            "https://donghuastream.org/?s=$query"
        }
    }

    fun getLuciferDonghuaUrl(query: String): String {
        return try {
            "https://luciferdonghua.in/?s=" + URLEncoder.encode(query, "UTF-8")
        } catch (e: Exception) {
            "https://luciferdonghua.in/?s=$query"
        }
    }

    fun resolveWatchUrl(showTitle: String, customWatchUrl: String?, providerPreference: String): String {
        if (providerPreference.lowercase() == "direct" && !customWatchUrl.isNullOrBlank()) {
            return customWatchUrl
        }
        return when (providerPreference.lowercase()) {
            "luciferdonghua" -> getLuciferDonghuaUrl(showTitle)
            "donghuastream" -> getDonghuaStreamUrl(showTitle)
            else -> if (!customWatchUrl.isNullOrBlank()) customWatchUrl else getDonghuaStreamUrl(showTitle)
        }
    }
}
