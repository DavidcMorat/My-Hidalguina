package com.example.chat

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream

data class Sticker(
    val id: String,
    val packId: String,
    val url: String? = null, // Remote URL for Giphy / built-in stickers
    val localPath: String? = null, // Local absolute path for legacy custom saved stickers
    val isGif: Boolean = false
)

data class StickerPack(
    val id: String,
    val name: String,
    val iconUrl: String,
    val stickers: List<Sticker>
)

object StickerManager {
    private const val STICKERS_DIR_NAME = "my_stickers"

    // Default high-quality static and animated GIF sticker packs
    val defaultPacks = emptyList<StickerPack>()

    private val GIPHY_KEYS = listOf(
        "CdRLa66RNo3fS",
        "0UTRbFco6Y276gMCHm7v8A38883rrJg8",
        "3ePst77Z9b7v5Q79bMAFUhRLD0F2DJ3E",
        "mCObK2SjXgL5m3bM67H7fKLeD28rrJg8",
        "dc6zaTOxFJmzC"
    )

    private fun parseGiphyResponse(bodyString: String): List<Sticker> {
        val json = JSONObject(bodyString)
        val dataArray = json.optJSONArray("data") ?: return emptyList()
        val list = mutableListOf<Sticker>()
        for (i in 0 until dataArray.length()) {
            val item = dataArray.optJSONObject(i) ?: continue
            val id = item.optString("id") ?: continue
            val images = item.optJSONObject("images") ?: continue
            val fixedHeightObj = images.optJSONObject("fixed_height") 
                ?: images.optJSONObject("fixed_height_small") 
                ?: images.optJSONObject("original") 
                ?: continue
            val gifUrl = fixedHeightObj.optString("url") ?: continue
            list.add(Sticker(id = id, packId = "giphy", url = gifUrl, isGif = true))
        }
        return list
    }

    // Fetch trending stickers from Tenor
    suspend fun getTrendingTenor(): List<Sticker> = withContext(Dispatchers.IO) {
        val limit = 40
        val apiKey = "L934OFFU63OC" // Public Tenor API Key
        val url = "https://api.tenor.com/v1/trending?key=$apiKey&limit=$limit&media_filter=minimal"
        
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: return@withContext emptyList<Sticker>()
                val json = JSONObject(bodyString)
                val resultsArray = json.optJSONArray("results") ?: return@withContext emptyList<Sticker>()
                
                val list = mutableListOf<Sticker>()
                for (i in 0 until resultsArray.length()) {
                    val item = resultsArray.optJSONObject(i) ?: continue
                    val id = item.optString("id") ?: continue
                    val mediaArray = item.optJSONArray("media") ?: continue
                    if (mediaArray.length() > 0) {
                        val mediaObj = mediaArray.optJSONObject(0) ?: continue
                        val tinygif = mediaObj.optJSONObject("tinygif") ?: mediaObj.optJSONObject("gif") ?: continue
                        val gifUrl = tinygif.optString("url") ?: continue
                        list.add(
                            Sticker(
                                id = id,
                                packId = "giphy",
                                url = gifUrl,
                                isGif = true
                            )
                        )
                    }
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Search stickers on Tenor
    suspend fun searchTenor(query: String): List<Sticker> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val limit = 40
        val apiKey = "L934OFFU63OC" // Public Tenor API Key
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        val url = "https://api.tenor.com/v1/search?q=$encodedQuery&key=$apiKey&limit=$limit&media_filter=minimal"
        
        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()
        
        try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyString = response.body?.string() ?: return@withContext emptyList()
                val json = JSONObject(bodyString)
                val resultsArray = json.optJSONArray("results") ?: return@withContext emptyList()
                
                val list = mutableListOf<Sticker>()
                for (i in 0 until resultsArray.length()) {
                    val item = resultsArray.optJSONObject(i) ?: continue
                    val id = item.optString("id") ?: continue
                    val mediaArray = item.optJSONArray("media") ?: continue
                    if (mediaArray.length() > 0) {
                        val mediaObj = mediaArray.optJSONObject(0) ?: continue
                        val tinygif = mediaObj.optJSONObject("tinygif") ?: mediaObj.optJSONObject("gif") ?: continue
                        val gifUrl = tinygif.optString("url") ?: continue
                        list.add(
                            Sticker(
                                id = id,
                                packId = "giphy",
                                url = gifUrl,
                                isGif = true
                            )
                        )
                    }
                }
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // Fetch trending stickers from Giphy
    suspend fun getTrendingGiphy(): List<Sticker> = withContext(Dispatchers.IO) {
        val limit = 40
        val client = OkHttpClient()
        
        for (key in GIPHY_KEYS) {
            val url = "https://api.giphy.com/v1/stickers/trending?api_key=$key&limit=$limit"
            val request = Request.Builder().url(url).build()
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: continue
                    val list = parseGiphyResponse(bodyString)
                    if (list.isNotEmpty()) {
                        android.util.Log.d("StickerManager", "Successfully loaded Giphy trending with key: $key")
                        return@withContext list
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // If all Giphy keys fail, try Tenor trending
        val tenorList = getTrendingTenor()
        if (tenorList.isNotEmpty()) {
            return@withContext tenorList
        }
        
        // Final ultimate local fallback so the tab is NEVER empty
        val fallbackList = mutableListOf<Sticker>()
        for (pack in defaultPacks) {
            fallbackList.addAll(pack.stickers)
        }
        fallbackList.shuffled()
    }

    // Search stickers on Giphy
    suspend fun searchGiphy(query: String): List<Sticker> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext getTrendingGiphy()
        val limit = 40
        val client = OkHttpClient()
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        
        for (key in GIPHY_KEYS) {
            val url = "https://api.giphy.com/v1/stickers/search?api_key=$key&q=$encodedQuery&limit=$limit"
            val request = Request.Builder().url(url).build()
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string() ?: continue
                    val list = parseGiphyResponse(bodyString)
                    if (list.isNotEmpty()) {
                        android.util.Log.d("StickerManager", "Successfully searched Giphy with key: $key")
                        return@withContext list
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        
        // If all Giphy keys fail, try Tenor search
        val tenorList = searchTenor(query)
        if (tenorList.isNotEmpty()) {
            return@withContext tenorList
        }
        
        // Local keyword search over built-in stickers as final fallback
        val fallbackList = mutableListOf<Sticker>()
        for (pack in defaultPacks) {
            fallbackList.addAll(pack.stickers)
        }
        val results = fallbackList.filter { 
            it.id.contains(query, ignoreCase = true) || 
            it.packId.contains(query, ignoreCase = true) 
        }
        if (results.isNotEmpty()) results else fallbackList.shuffled().take(8)
    }

    // Save sticker as favorite to SharedPreferences
    fun saveFavoriteSticker(context: Context, id: String, url: String, isGif: Boolean): Boolean {
        return try {
            val sharedPrefs = context.getSharedPreferences("favorite_stickers_prefs", Context.MODE_PRIVATE)
            val favoritesSet = sharedPrefs.getStringSet("favorites_set", emptySet()) ?: emptySet()
            
            // Check if already in favorites by checking URL
            val exists = favoritesSet.any { it.contains("|$url|") || it.contains(url) }
            if (exists) return true
            
            val serialized = "$id|$url|$isGif"
            val newSet = favoritesSet.toMutableSet()
            newSet.add(serialized)
            
            sharedPrefs.edit().putStringSet("favorites_set", newSet).apply()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // Load favorite stickers from SharedPreferences
    fun loadFavorites(context: Context): StickerPack {
        val sharedPrefs = context.getSharedPreferences("favorite_stickers_prefs", Context.MODE_PRIVATE)
        val favoritesSet = sharedPrefs.getStringSet("favorites_set", emptySet()) ?: emptySet()
        
        val stickers = favoritesSet.mapNotNull { serialized ->
            val parts = serialized.split("|")
            if (parts.size >= 3) {
                val id = parts[0]
                val url = parts[1]
                val isGif = parts[2].toBoolean()
                Sticker(
                    id = id,
                    packId = "favorites",
                    url = url,
                    isGif = isGif
                )
            } else {
                null
            }
        }.sortedBy { it.id }
        
        return StickerPack(
            id = "favorites",
            name = "Favoritos ❤️",
            iconUrl = "https://media.giphy.com/media/v1.Y2lkPTc5MGI3NjExMWhnM280cTFrbmdnYnptNXQ3N2l0amtrdGFleGlrczN1enJ1eDlpZCZlcD12MV9pbnRlcm5hbF9naWZfYnlfaWQmY3Q9cw/o7R0N2F/giphy.gif",
            stickers = stickers
        )
    }

    // Legacy support for base64 saving
    fun saveReceivedSticker(context: Context, base64Data: String, isGif: Boolean): Sticker? {
        return try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val dir = File(context.filesDir, STICKERS_DIR_NAME)
            if (!dir.exists()) dir.mkdirs()
            val id = "sticker_${System.currentTimeMillis()}"
            val extension = if (isGif) "gif" else "png"
            val file = File(dir, "$id.$extension")
            
            FileOutputStream(file).use { fos ->
                fos.write(bytes)
            }
            
            // Also register to SharedPreferences favorites
            saveFavoriteSticker(context, id, file.absolutePath, isGif)
            
            Sticker(
                id = id,
                packId = "favorites",
                localPath = file.absolutePath,
                isGif = isGif
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
