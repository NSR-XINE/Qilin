package com.mydonghua.app.data.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.mydonghua.app.domain.model.Show
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AniListUser(
    val id: Int,
    val name: String,
    val avatarUrl: String?,
    val episodesWatched: Int,
    val animeCount: Int,
    val minutesWatched: Int = 0,
    val meanScore: Double = 0.0
) {
    val daysWatched: String
        get() {
            val days = minutesWatched.toDouble() / 1440.0
            return String.format(java.util.Locale.US, "%.1fd", days)
        }
}

data class AniListMedia(
    val id: Int,
    val titleRomaji: String?,
    val titleEnglish: String?,
    val titleNative: String?,
    val coverImage: String?,
    val bannerImage: String? = null,
    val averageScore: Int? = null,
    val popularity: Int? = null,
    val description: String?,
    val episodes: Int,
    val status: String?,
    val genres: List<String>?,
    val studio: String?,
    val releaseYear: Int?
) {
    val preferredTitle: String
        get() = titleEnglish?.takeIf { it.isNotBlank() }
            ?: titleRomaji?.takeIf { it.isNotBlank() }
            ?: titleNative?.takeIf { it.isNotBlank() }
            ?: "Unknown Title"
}

data class AniListMediaDetail(
    val id: Int,
    val averageScore: Int?,
    val meanScore: Int?,
    val popularity: Int?,
    val favourites: Int?,
    val bannerImage: String?,
    val coverImage: String?,
    val description: String?,
    val format: String?,
    val status: String?,
    val episodes: Int?,
    val userScore: Double?,
    val userProgress: Int?,
    val userStatus: String?
)

data class AniListEntry(
    val id: Int,
    val mediaId: Int,
    val progress: Int,
    val status: String,
    val score: Double,
    val media: AniListMedia
)

object AniListService {
    private const val TAG = "AniListService"
    private const val PREFS_NAME = "anilist_tracking_prefs"
    private const val KEY_TOKEN = "anilist_token"
    private const val KEY_USER_ID = "anilist_user_id"
    private const val KEY_USERNAME = "anilist_username"
    private const val KEY_AVATAR = "anilist_avatar"
    private const val KEY_EPISODES_WATCHED = "anilist_episodes_watched"
    private const val KEY_ANIME_COUNT = "anilist_anime_count"
    private const val KEY_MINUTES_WATCHED = "anilist_minutes_watched"
    private const val KEY_MEAN_SCORE = "anilist_mean_score"
    private const val KEY_AUTO_SYNC = "anilist_auto_sync"

    private const val GRAPHQL_ENDPOINT = "https://graphql.anilist.co/"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .header("User-Agent", "MyDonghua/${com.mydonghua.app.BuildConfig.VERSION_NAME} (Android)")
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
        .build()

    private var prefs: SharedPreferences? = null

    private val _userFlow = MutableStateFlow<AniListUser?>(null)
    val userFlow: StateFlow<AniListUser?> = _userFlow.asStateFlow()

    private val _autoSyncFlow = MutableStateFlow(true)
    val autoSyncFlow: StateFlow<Boolean> = _autoSyncFlow.asStateFlow()

    fun init(context: Context) {
        if (prefs != null) return
        val sp = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs = sp

        val token = sp.getString(KEY_TOKEN, null)
        val userId = sp.getInt(KEY_USER_ID, 0)
        val username = sp.getString(KEY_USERNAME, null)
        val avatar = sp.getString(KEY_AVATAR, null)
        val episodesWatched = sp.getInt(KEY_EPISODES_WATCHED, 0)
        val animeCount = sp.getInt(KEY_ANIME_COUNT, 0)
        val minutesWatched = sp.getInt(KEY_MINUTES_WATCHED, 0)
        val meanScore = sp.getFloat(KEY_MEAN_SCORE, 0f).toDouble()
        val autoSync = sp.getBoolean(KEY_AUTO_SYNC, true)

        _autoSyncFlow.value = autoSync

        if (!token.isNullOrBlank() && userId > 0 && !username.isNullOrBlank()) {
            _userFlow.value = AniListUser(
                id = userId,
                name = username,
                avatarUrl = avatar,
                episodesWatched = episodesWatched,
                animeCount = animeCount,
                minutesWatched = minutesWatched,
                meanScore = meanScore
            )
        }
    }

    val isLoggedIn: Boolean
        get() = !getToken().isNullOrBlank() && _userFlow.value != null

    val isAutoSyncEnabled: Boolean
        get() = _autoSyncFlow.value

    fun getToken(): String? {
        return prefs?.getString(KEY_TOKEN, null)
    }

    fun sanitizeToken(raw: String): String {
        var token = raw.trim()
        if (token.contains("access_token=")) {
            token = token.substringAfter("access_token=").substringBefore("&")
        }
        if (token.startsWith("Bearer ", ignoreCase = true)) {
            token = token.substring(7).trim()
        }
        return token.trim()
    }

    fun saveSession(token: String, user: AniListUser) {
        val cleanToken = sanitizeToken(token)
        prefs?.edit()
            ?.putString(KEY_TOKEN, cleanToken)
            ?.putInt(KEY_USER_ID, user.id)
            ?.putString(KEY_USERNAME, user.name)
            ?.putString(KEY_AVATAR, user.avatarUrl)
            ?.putInt(KEY_EPISODES_WATCHED, user.episodesWatched)
            ?.putInt(KEY_ANIME_COUNT, user.animeCount)
            ?.putInt(KEY_MINUTES_WATCHED, user.minutesWatched)
            ?.putFloat(KEY_MEAN_SCORE, user.meanScore.toFloat())
            ?.apply()
        _userFlow.value = user
    }

    fun clearSession() {
        prefs?.edit()
            ?.remove(KEY_TOKEN)
            ?.remove(KEY_USER_ID)
            ?.remove(KEY_USERNAME)
            ?.remove(KEY_AVATAR)
            ?.remove(KEY_EPISODES_WATCHED)
            ?.remove(KEY_ANIME_COUNT)
            ?.remove(KEY_MINUTES_WATCHED)
            ?.remove(KEY_MEAN_SCORE)
            ?.apply()
        _userFlow.value = null
    }

    fun setAutoSync(enabled: Boolean) {
        prefs?.edit()?.putBoolean(KEY_AUTO_SYNC, enabled)?.apply()
        _autoSyncFlow.value = enabled
    }

    suspend fun validateTokenAndFetchViewer(token: String): Result<AniListUser> = withContext(Dispatchers.IO) {
        try {
            val cleanToken = sanitizeToken(token)
            if (cleanToken.isBlank()) {
                return@withContext Result.failure(Exception("Token cannot be empty"))
            }

            val query = """
                query {
                  Viewer {
                    id
                    name
                    avatar {
                      large
                      medium
                    }
                    statistics {
                      anime {
                        count
                        episodesWatched
                        minutesWatched
                        meanScore
                      }
                    }
                  }
                }
            """.trimIndent()

            val requestBodyJson = JSONObject().apply {
                put("query", query)
            }

            val request = Request.Builder()
                .url(GRAPHQL_ENDPOINT)
                .addHeader("Authorization", "Bearer $cleanToken")
                .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response from AniList"))
                val json = JSONObject(bodyStr)
                if (json.has("errors")) {
                    val errorsArr = json.getJSONArray("errors")
                    val msg = if (errorsArr.length() > 0) errorsArr.getJSONObject(0).optString("message", "Authentication error") else "Invalid token"
                    return@withContext Result.failure(Exception(msg))
                }

                val data = json.optJSONObject("data") ?: return@withContext Result.failure(Exception("Invalid data returned"))
                val viewer = data.optJSONObject("Viewer") ?: return@withContext Result.failure(Exception("Viewer not found"))

                val id = viewer.getInt("id")
                val name = viewer.getString("name")
                val avatarObj = viewer.optJSONObject("avatar")
                val avatarUrl = avatarObj?.optString("large")?.takeIf { it.isNotEmpty() && it != "null" }
                    ?: avatarObj?.optString("medium")

                val stats = viewer.optJSONObject("statistics")
                val animeStats = stats?.optJSONObject("anime")
                val count = animeStats?.optInt("count", 0) ?: 0
                val epsWatched = animeStats?.optInt("episodesWatched", 0) ?: 0
                val minutesWatched = animeStats?.optInt("minutesWatched", 0) ?: 0
                val meanScore = animeStats?.optDouble("meanScore", 0.0) ?: 0.0

                val user = AniListUser(
                    id = id,
                    name = name,
                    avatarUrl = avatarUrl,
                    episodesWatched = epsWatched,
                    animeCount = count,
                    minutesWatched = minutesWatched,
                    meanScore = meanScore
                )

                Result.success(user)
            }
        } catch (e: Exception) {
            Log.e(TAG, "validateTokenAndFetchViewer error", e)
            Result.failure(e)
        }
    }

    suspend fun searchAnime(query: String, limit: Int = 10): List<AniListMedia> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        try {
            val graphQLQuery = """
                query (${'$'}search: String, ${'$'}limit: Int) {
                  Page(page: 1, perPage: ${'$'}limit) {
                    media(search: ${'$'}search, type: ANIME) {
                      id
                      title {
                        romaji
                        english
                        native
                      }
                      coverImage {
                        extraLarge
                        large
                      }
                      description(asHtml: false)
                      episodes
                      status
                      genres
                      studios(isMain: true) {
                        nodes {
                          name
                        }
                      }
                      startDate {
                        year
                      }
                    }
                  }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("search", query)
                put("limit", limit)
            }
            val requestBodyJson = JSONObject().apply {
                put("query", graphQLQuery)
                put("variables", variables)
            }

            val request = Request.Builder()
                .url(GRAPHQL_ENDPOINT)
                .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyStr)
                val data = json.optJSONObject("data") ?: return@withContext emptyList()
                val page = data.optJSONObject("Page") ?: return@withContext emptyList()
                val mediaArr = page.optJSONArray("media") ?: return@withContext emptyList()

                val results = mutableListOf<AniListMedia>()
                for (i in 0 until mediaArr.length()) {
                    val media = mediaArr.getJSONObject(i)
                    val id = media.getInt("id")
                    val titleObj = media.optJSONObject("title")
                    val romaji = titleObj?.optString("romaji")?.takeIf { it.isNotEmpty() && it != "null" }
                    val english = titleObj?.optString("english")?.takeIf { it.isNotEmpty() && it != "null" }
                    val native = titleObj?.optString("native")?.takeIf { it.isNotEmpty() && it != "null" }

                    val coverObj = media.optJSONObject("coverImage")
                    val cover = coverObj?.optString("extraLarge")?.takeIf { it.isNotEmpty() && it != "null" }
                        ?: coverObj?.optString("large")

                    val desc = media.optString("description", "")
                        .replace(Regex("<[^>]*>"), "")
                        .trim()

                    val episodes = media.optInt("episodes", 0)
                    val status = media.optString("status", "").lowercase()

                    val genresArr = media.optJSONArray("genres")
                    val genres = mutableListOf<String>()
                    if (genresArr != null) {
                        for (g in 0 until genresArr.length()) {
                            genres.add(genresArr.getString(g))
                        }
                    }

                    val studiosObj = media.optJSONObject("studios")
                    val studioNodes = studiosObj?.optJSONArray("nodes")
                    val studio = if (studioNodes != null && studioNodes.length() > 0) {
                        studioNodes.getJSONObject(0).optString("name")
                    } else null

                    val startDate = media.optJSONObject("startDate")
                    val releaseYear = startDate?.optInt("year", 0)?.takeIf { it > 0 }

                    results.add(
                        AniListMedia(
                            id = id,
                            titleRomaji = romaji,
                            titleEnglish = english,
                            titleNative = native,
                            coverImage = cover,
                            description = desc.takeIf { it.isNotEmpty() },
                            episodes = episodes,
                            status = status,
                            genres = genres.takeIf { it.isNotEmpty() },
                            studio = studio?.takeIf { it.isNotEmpty() && it != "null" },
                            releaseYear = releaseYear
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            Log.e(TAG, "searchAnime error", e)
            emptyList()
        }
    }

    suspend fun findBestMatchId(title: String, titleZh: String?): Int? = withContext(Dispatchers.IO) {
        val candidates = searchAnime(title, limit = 5)
        if (candidates.isNotEmpty()) {
            val normalizedTitle = title.lowercase().trim()
            for (candidate in candidates) {
                val cTitleEng = candidate.titleEnglish?.lowercase()?.trim()
                val cTitleRom = candidate.titleRomaji?.lowercase()?.trim()
                val cTitleNat = candidate.titleNative?.lowercase()?.trim()
                if (cTitleEng == normalizedTitle || cTitleRom == normalizedTitle || cTitleNat == normalizedTitle) {
                    return@withContext candidate.id
                }
            }
            return@withContext candidates[0].id
        }

        if (!titleZh.isNullOrBlank()) {
            val zhCandidates = searchAnime(titleZh, limit = 5)
            if (zhCandidates.isNotEmpty()) {
                return@withContext zhCandidates[0].id
            }
        }

        null
    }

    suspend fun updateProgress(
        token: String,
        mediaId: Int,
        progress: Int,
        totalEpisodes: Int = 0
    ): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val status = when {
                totalEpisodes > 0 && progress >= totalEpisodes -> "COMPLETED"
                progress > 0 -> "CURRENT"
                else -> "PLANNING"
            }

            val mutation = """
                mutation (${'$'}mediaId: Int, ${'$'}progress: Int, ${'$'}status: MediaListStatus) {
                  SaveMediaListEntry(mediaId: ${'$'}mediaId, progress: ${'$'}progress, status: ${'$'}status) {
                    id
                    mediaId
                    progress
                    status
                  }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("mediaId", mediaId)
                put("progress", progress)
                put("status", status)
            }
            val requestBodyJson = JSONObject().apply {
                put("query", mutation)
                put("variables", variables)
            }

            val request = Request.Builder()
                .url(GRAPHQL_ENDPOINT)
                .addHeader("Authorization", "Bearer ${token.trim()}")
                .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty AniList response"))
                val json = JSONObject(bodyStr)
                if (json.has("errors")) {
                    val errorsArr = json.getJSONArray("errors")
                    val msg = if (errorsArr.length() > 0) errorsArr.getJSONObject(0).optString("message", "Sync error") else "Failed to update AniList"
                    return@withContext Result.failure(Exception(msg))
                }

                val data = json.optJSONObject("data")
                val entry = data?.optJSONObject("SaveMediaListEntry")
                val id = entry?.optInt("id", 0) ?: 0
                Result.success(id)
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateProgress error", e)
            Result.failure(e)
        }
    }

    suspend fun fetchUserAnimeCollection(token: String, userId: Int): Result<List<AniListEntry>> = withContext(Dispatchers.IO) {
        try {
            val query = """
                query (${'$'}userId: Int) {
                  MediaListCollection(userId: ${'$'}userId, type: ANIME) {
                    lists {
                      name
                      isCustomList
                      status
                      entries {
                        id
                        progress
                        status
                        score
                        media {
                          id
                          title {
                            romaji
                            english
                            native
                          }
                          coverImage {
                            extraLarge
                            large
                          }
                          description(asHtml: false)
                          episodes
                          status
                          genres
                          studios(isMain: true) {
                            nodes {
                              name
                            }
                          }
                          startDate {
                            year
                          }
                        }
                      }
                    }
                  }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("userId", userId)
            }
            val requestBodyJson = JSONObject().apply {
                put("query", query)
                put("variables", variables)
            }

            val request = Request.Builder()
                .url(GRAPHQL_ENDPOINT)
                .addHeader("Authorization", "Bearer ${token.trim()}")
                .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty collection response"))
                val json = JSONObject(bodyStr)
                if (json.has("errors")) {
                    val errorsArr = json.getJSONArray("errors")
                    val msg = if (errorsArr.length() > 0) errorsArr.getJSONObject(0).optString("message", "Import error") else "Failed to fetch collection"
                    return@withContext Result.failure(Exception(msg))
                }

                val data = json.optJSONObject("data") ?: return@withContext Result.failure(Exception("No data returned"))
                val collection = data.optJSONObject("MediaListCollection") ?: return@withContext Result.failure(Exception("No collection found"))
                val listsArr = collection.optJSONArray("lists") ?: JSONArray()

                val entryMap = mutableMapOf<Int, AniListEntry>()
                for (l in 0 until listsArr.length()) {
                    val listObj = listsArr.getJSONObject(l)
                    val entriesArr = listObj.optJSONArray("entries") ?: continue

                    for (e in 0 until entriesArr.length()) {
                        val entryObj = entriesArr.getJSONObject(e)
                        val id = entryObj.getInt("id")
                        val progress = entryObj.optInt("progress", 0)
                        val status = entryObj.optString("status", "CURRENT")
                        val score = entryObj.optDouble("score", 0.0)

                        val mediaObj = entryObj.optJSONObject("media") ?: continue
                        val mediaId = mediaObj.getInt("id")

                        val titles = mediaObj.optJSONObject("title")
                        val romaji = titles?.optString("romaji")?.takeIf { it.isNotEmpty() && it != "null" }
                        val english = titles?.optString("english")?.takeIf { it.isNotEmpty() && it != "null" }
                        val native = titles?.optString("native")?.takeIf { it.isNotEmpty() && it != "null" }

                        val cover = mediaObj.optJSONObject("coverImage")
                        val coverUrl = cover?.optString("extraLarge")?.takeIf { it.isNotEmpty() && it != "null" }
                            ?: cover?.optString("large")

                        val desc = mediaObj.optString("description", "")
                            .replace(Regex("<[^>]*>"), "")
                            .trim()

                        val episodes = mediaObj.optInt("episodes", 0)
                        val mediaStatus = mediaObj.optString("status", "").lowercase()

                        val genresArr = mediaObj.optJSONArray("genres")
                        val genres = mutableListOf<String>()
                        if (genresArr != null) {
                            for (g in 0 until genresArr.length()) {
                                genres.add(genresArr.getString(g))
                            }
                        }

                        val studiosObj = mediaObj.optJSONObject("studios")
                        val studioNodes = studiosObj?.optJSONArray("nodes")
                        val studio = if (studioNodes != null && studioNodes.length() > 0) {
                            studioNodes.getJSONObject(0).optString("name")
                        } else null

                        val startDate = mediaObj.optJSONObject("startDate")
                        val releaseYear = startDate?.optInt("year", 0)?.takeIf { it > 0 }

                        val media = AniListMedia(
                            id = mediaId,
                            titleRomaji = romaji,
                            titleEnglish = english,
                            titleNative = native,
                            coverImage = coverUrl,
                            description = desc.takeIf { it.isNotEmpty() },
                            episodes = episodes,
                            status = mediaStatus,
                            genres = genres.takeIf { it.isNotEmpty() },
                            studio = studio?.takeIf { it.isNotEmpty() && it != "null" },
                            releaseYear = releaseYear
                        )

                        // If already seen, prioritize entry with higher progress
                        if (!entryMap.containsKey(mediaId) || entryMap[mediaId]!!.progress < progress) {
                            entryMap[mediaId] = AniListEntry(
                                id = id,
                                mediaId = mediaId,
                                progress = progress,
                                status = status,
                                score = score,
                                media = media
                            )
                        }
                    }
                }

                Result.success(entryMap.values.toList())
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchUserAnimeCollection error", e)
            Result.failure(e)
        }
    }

    suspend fun syncEpisodeProgress(
        show: Show,
        progress: Int,
        onShowLinked: (suspend (Show) -> Unit)? = null
    ) = withContext(Dispatchers.IO) {
        if (!isLoggedIn || !isAutoSyncEnabled) return@withContext
        val token = getToken() ?: return@withContext

        try {
            var targetAniListId = show.aniListId
            if (targetAniListId == null) {
                // Attempt to resolve match
                targetAniListId = findBestMatchId(show.title, show.titleZh)
                if (targetAniListId != null) {
                    val linkedShow = show.copy(aniListId = targetAniListId)
                    onShowLinked?.invoke(linkedShow)
                }
            }

            if (targetAniListId != null) {
                val res = updateProgress(token, targetAniListId, progress, show.totalEp)
                if (res.isSuccess) {
                    Log.d(TAG, "Successfully synced episode $progress for ${show.title} (AniList #$targetAniListId)")
                } else {
                    Log.w(TAG, "Failed to sync episode $progress for ${show.title}: ${res.exceptionOrNull()?.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in syncEpisodeProgress", e)
        }
    }

    suspend fun fetchMediaDetails(mediaId: Int): Result<AniListMediaDetail> = withContext(Dispatchers.IO) {
        try {
            val query = """
                query (${'$'}id: Int) {
                  Media(id: ${'$'}id, type: ANIME) {
                    id
                    averageScore
                    meanScore
                    popularity
                    favourites
                    format
                    status
                    episodes
                    bannerImage
                    coverImage {
                      extraLarge
                      large
                    }
                    description(asHtml: false)
                    mediaListEntry {
                      id
                      status
                      score
                      progress
                    }
                  }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("id", mediaId)
            }
            val requestBodyJson = JSONObject().apply {
                put("query", query)
                put("variables", variables)
            }

            val builder = Request.Builder()
                .url(GRAPHQL_ENDPOINT)
                .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))

            val token = getToken()
            if (!token.isNullOrBlank()) {
                builder.addHeader("Authorization", "Bearer ${token.trim()}")
            }

            client.newCall(builder.build()).execute().use { response ->
                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty media response"))
                val json = JSONObject(bodyStr)
                if (json.has("errors")) {
                    val errorsArr = json.getJSONArray("errors")
                    val msg = if (errorsArr.length() > 0) errorsArr.getJSONObject(0).optString("message", "Error") else "Media query error"
                    return@withContext Result.failure(Exception(msg))
                }

                val data = json.optJSONObject("data") ?: return@withContext Result.failure(Exception("No data returned"))
                val media = data.optJSONObject("Media") ?: return@withContext Result.failure(Exception("Media not found"))

                val avgScore = media.optInt("averageScore", 0).takeIf { it > 0 }
                val meanScore = media.optInt("meanScore", 0).takeIf { it > 0 }
                val popularity = media.optInt("popularity", 0).takeIf { it > 0 }
                val favourites = media.optInt("favourites", 0).takeIf { it > 0 }
                val banner = media.optString("bannerImage").takeIf { it.isNotEmpty() && it != "null" }
                val coverObj = media.optJSONObject("coverImage")
                val cover = coverObj?.optString("extraLarge")?.takeIf { it.isNotEmpty() && it != "null" }
                    ?: coverObj?.optString("large")
                val desc = media.optString("description", "")
                    .replace(Regex("<[^>]*>"), "")
                    .trim()
                    .takeIf { it.isNotEmpty() }
                val format = media.optString("format").takeIf { it.isNotEmpty() && it != "null" }
                val status = media.optString("status").takeIf { it.isNotEmpty() && it != "null" }
                val eps = media.optInt("episodes", 0).takeIf { it > 0 }

                val entryObj = media.optJSONObject("mediaListEntry")
                val userStatus = entryObj?.optString("status")?.takeIf { it.isNotEmpty() && it != "null" }
                val userScore = entryObj?.optDouble("score", 0.0)?.takeIf { it > 0.0 }
                val userProgress = entryObj?.optInt("progress", 0)

                Result.success(
                    AniListMediaDetail(
                        id = mediaId,
                        averageScore = avgScore,
                        meanScore = meanScore,
                        popularity = popularity,
                        favourites = favourites,
                        bannerImage = banner,
                        coverImage = cover,
                        description = desc,
                        format = format,
                        status = status,
                        episodes = eps,
                        userScore = userScore,
                        userProgress = userProgress,
                        userStatus = userStatus
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchMediaDetails error", e)
            Result.failure(e)
        }
    }

    suspend fun updateMediaScoreAndStatus(
        token: String,
        mediaId: Int,
        score: Double?,
        status: String?
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val mutation = """
                mutation (${'$'}mediaId: Int, ${'$'}score: Float, ${'$'}status: MediaListStatus) {
                  SaveMediaListEntry(mediaId: ${'$'}mediaId, score: ${'$'}score, status: ${'$'}status) {
                    id
                    score
                    status
                  }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("mediaId", mediaId)
                if (score != null) put("score", score)
                if (!status.isNullOrBlank()) put("status", status)
            }
            val requestBodyJson = JSONObject().apply {
                put("query", mutation)
                put("variables", variables)
            }

            val request = Request.Builder()
                .url(GRAPHQL_ENDPOINT)
                .addHeader("Authorization", "Bearer ${token.trim()}")
                .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val json = JSONObject(bodyStr)
                if (json.has("errors")) {
                    val errorsArr = json.getJSONArray("errors")
                    val msg = if (errorsArr.length() > 0) errorsArr.getJSONObject(0).optString("message", "Error") else "Mutation error"
                    return@withContext Result.failure(Exception(msg))
                }
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateMediaScoreAndStatus error", e)
            Result.failure(e)
        }
    }

    suspend fun toggleFavorite(animeId: Int): Result<Boolean> = withContext(Dispatchers.IO) {
        val token = getToken() ?: return@withContext Result.failure(Exception("Not logged in"))
        try {
            val mutation = """
                mutation (${'$'}animeId: Int) {
                  ToggleFavourite(animeId: ${'$'}animeId) {
                    anime {
                      nodes {
                        id
                      }
                    }
                  }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("animeId", animeId)
            }
            val requestBodyJson = JSONObject().apply {
                put("query", mutation)
                put("variables", variables)
            }

            val request = Request.Builder()
                .url(GRAPHQL_ENDPOINT)
                .addHeader("Authorization", "Bearer ${token.trim()}")
                .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: return@withContext Result.failure(Exception("Empty response"))
                val json = JSONObject(bodyStr)
                if (json.has("errors")) {
                    val errorsArr = json.getJSONArray("errors")
                    val msg = if (errorsArr.length() > 0) errorsArr.getJSONObject(0).optString("message", "Error") else "Favorite error"
                    return@withContext Result.failure(Exception(msg))
                }
                Result.success(true)
            }
        } catch (e: Exception) {
            Log.e(TAG, "toggleFavorite error", e)
            Result.failure(e)
        }
    }

    suspend fun fetchTrendingDonghua(page: Int = 1, perPage: Int = 12): List<AniListMedia> = withContext(Dispatchers.IO) {
        try {
            val query = """
                query (${'$'}page: Int, ${'$'}perPage: Int) {
                  Page(page: ${'$'}page, perPage: ${'$'}perPage) {
                    media(type: ANIME, countryOfOrigin: "CN", sort: [TRENDING_DESC, POPULARITY_DESC]) {
                      id
                      title {
                        romaji
                        english
                        native
                      }
                      coverImage {
                        extraLarge
                        large
                      }
                      bannerImage
                      averageScore
                      popularity
                      description(asHtml: false)
                      episodes
                      status
                      genres
                      studios(isMain: true) {
                        nodes {
                          name
                        }
                      }
                      startDate {
                        year
                      }
                    }
                  }
                }
            """.trimIndent()

            val variables = JSONObject().apply {
                put("page", page)
                put("perPage", perPage)
            }
            val requestBodyJson = JSONObject().apply {
                put("query", query)
                put("variables", variables)
            }

            val request = Request.Builder()
                .url(GRAPHQL_ENDPOINT)
                .post(requestBodyJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                val bodyStr = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyStr)
                val data = json.optJSONObject("data") ?: return@withContext emptyList()
                val pageObj = data.optJSONObject("Page") ?: return@withContext emptyList()
                val mediaArr = pageObj.optJSONArray("media") ?: return@withContext emptyList()

                val results = mutableListOf<AniListMedia>()
                for (i in 0 until mediaArr.length()) {
                    val media = mediaArr.getJSONObject(i)
                    val id = media.getInt("id")
                    val titleObj = media.optJSONObject("title")
                    val romaji = titleObj?.optString("romaji")?.takeIf { it.isNotEmpty() && it != "null" }
                    val english = titleObj?.optString("english")?.takeIf { it.isNotEmpty() && it != "null" }
                    val native = titleObj?.optString("native")?.takeIf { it.isNotEmpty() && it != "null" }

                    val coverObj = media.optJSONObject("coverImage")
                    val cover = coverObj?.optString("extraLarge")?.takeIf { it.isNotEmpty() && it != "null" }
                        ?: coverObj?.optString("large")
                    val banner = media.optString("bannerImage").takeIf { it.isNotEmpty() && it != "null" }
                    val avgScore = media.optInt("averageScore", 0).takeIf { it > 0 }
                    val pop = media.optInt("popularity", 0).takeIf { it > 0 }

                    val desc = media.optString("description", "")
                        .replace(Regex("<[^>]*>"), "")
                        .trim()
                        .takeIf { it.isNotEmpty() }

                    val episodes = media.optInt("episodes", 0)
                    val status = media.optString("status", "").lowercase()

                    val genresArr = media.optJSONArray("genres")
                    val genres = mutableListOf<String>()
                    if (genresArr != null) {
                        for (g in 0 until genresArr.length()) {
                            genres.add(genresArr.getString(g))
                        }
                    }

                    val studiosObj = media.optJSONObject("studios")
                    val studioNodes = studiosObj?.optJSONArray("nodes")
                    val studio = if (studioNodes != null && studioNodes.length() > 0) {
                        studioNodes.getJSONObject(0).optString("name")
                    } else null

                    val startDate = media.optJSONObject("startDate")
                    val releaseYear = startDate?.optInt("year", 0)?.takeIf { it > 0 }

                    results.add(
                        AniListMedia(
                            id = id,
                            titleRomaji = romaji,
                            titleEnglish = english,
                            titleNative = native,
                            coverImage = cover,
                            bannerImage = banner,
                            averageScore = avgScore,
                            popularity = pop,
                            description = desc,
                            episodes = episodes,
                            status = status,
                            genres = genres.takeIf { it.isNotEmpty() },
                            studio = studio?.takeIf { it.isNotEmpty() && it != "null" },
                            releaseYear = releaseYear
                        )
                    )
                }
                results
            }
        } catch (e: Exception) {
            Log.e(TAG, "fetchTrendingDonghua error", e)
            emptyList()
        }
    }
}
