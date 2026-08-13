package com.example.chat

import android.content.Context
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

data class Sticker(
    val id: String,
    val packId: String,
    val url: String? = null, // Remote URL for built-in stickers
    val localPath: String? = null, // Local absolute path for custom saved stickers
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
    val defaultPacks = listOf(
        StickerPack(
            id = "cats",
            name = "Gatitos 🐱",
            iconUrl = "https://i.imgur.com/uFkObeN.png",
            stickers = listOf(
                Sticker("cat_happy", "cats", "https://media.giphy.com/media/8g7S0O63678031v8Uj/giphy.gif", isGif = true),
                Sticker("cat_dance", "cats", "https://media.giphy.com/media/3oriffitOfuTj9X0Nq/giphy.gif", isGif = true),
                Sticker("cat_shocked", "cats", "https://media.giphy.com/media/P8EonNrrfK83O6663E/giphy.gif", isGif = true),
                Sticker("cat_love", "cats", "https://media.giphy.com/media/vNf16SIsA98S5uA6uL/giphy.gif", isGif = true)
            )
        ),
        StickerPack(
            id = "reactions",
            name = "Reacciones 😂",
            iconUrl = "https://i.imgur.com/M6LgO0U.png",
            stickers = listOf(
                Sticker("react_thumbs", "reactions", "https://i.imgur.com/83p8gK6.png", isGif = false),
                Sticker("react_heart", "reactions", "https://i.imgur.com/M6LgO0U.png", isGif = false),
                Sticker("react_party", "reactions", "https://i.imgur.com/o7R0N2F.png", isGif = false),
                Sticker("react_blown", "reactions", "https://i.imgur.com/rAepuK9.png", isGif = false)
            )
        ),
        StickerPack(
            id = "study",
            name = "Estudio 📚",
            iconUrl = "https://i.imgur.com/M0E2Pto.png",
            stickers = listOf(
                Sticker("study_reading", "study", "https://i.imgur.com/M0E2Pto.png", isGif = false),
                Sticker("study_owl", "study", "https://i.imgur.com/pZ2xV9g.png", isGif = false),
                Sticker("study_aplus", "study", "https://i.imgur.com/Hn2f2iK.png", isGif = false),
                Sticker("study_trophy", "study", "https://i.imgur.com/Z4wH0q6.png", isGif = false)
            )
        )
    )

    fun getStickersDir(context: Context): File {
        val dir = File(context.filesDir, STICKERS_DIR_NAME)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    // Save a received base64 sticker to local file storage
    fun saveReceivedSticker(context: Context, base64Data: String, isGif: Boolean): Sticker? {
        return try {
            val bytes = Base64.decode(base64Data, Base64.DEFAULT)
            val dir = getStickersDir(context)
            val id = "sticker_${System.currentTimeMillis()}"
            val extension = if (isGif) "gif" else "png"
            val file = File(dir, "$id.$extension")
            
            FileOutputStream(file).use { fos ->
                fos.write(bytes)
            }
            
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

    // Load custom saved stickers from the phone's local storage
    fun loadFavorites(context: Context): StickerPack {
        val dir = getStickersDir(context)
        val files = dir.listFiles() ?: emptyArray()
        
        val stickers = files.filter { it.isFile && (it.name.endsWith(".png") || it.name.endsWith(".gif")) }
            .map { file ->
                val isGif = file.name.endsWith(".gif")
                val id = file.nameWithoutExtension
                Sticker(
                    id = id,
                    packId = "favorites",
                    localPath = file.absolutePath,
                    isGif = isGif
                )
            }
            .reversed() // Show newest first
        
        return StickerPack(
            id = "favorites",
            name = "Favoritos ❤️",
            iconUrl = "https://i.imgur.com/o7R0N2F.png", // Thumbnail icon URL for favorites tab
            stickers = stickers
        )
    }
}
