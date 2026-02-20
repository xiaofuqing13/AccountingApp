package com.loveapp.accountbook.util

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.util.*

object DiaryMediaManager {

    private val sdf = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.getDefault())

    fun getImageDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "diary_images")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getAudioDir(context: Context): File {
        val dir = File(context.getExternalFilesDir(null), "diary_audio")
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun generateImageFileName(): String = "diary_${sdf.format(Date())}.jpg"

    fun generateAudioFileName(): String = "diary_${sdf.format(Date())}.m4a"

    fun copyImageToPrivate(context: Context, sourceUri: Uri): String? {
        val fileName = generateImageFileName()
        val destFile = File(getImageDir(context), fileName)
        return try {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                destFile.outputStream().use { output -> input.copyTo(output) }
            }
            fileName
        } catch (e: Exception) {
            null
        }
    }

    fun getImageFile(context: Context, fileName: String): File =
        File(getImageDir(context), fileName)

    fun getAudioFile(context: Context, fileName: String): File =
        File(getAudioDir(context), fileName)

    fun normalizeStoredFileName(rawValue: String): String {
        val trimmed = rawValue.trim()
        if (trimmed.isEmpty()) return trimmed
        val decoded = runCatching { Uri.decode(trimmed) }.getOrDefault(trimmed)
        val noQuery = decoded
            .substringBefore('?')
            .substringBefore('#')
            .removePrefix("file://")
            .removePrefix("FILE://")
            .removeSurrounding("\"")
            .trim()
        val normalized = File(noQuery).name.trim()
        return if (normalized.isNotEmpty()) normalized else trimmed
    }

    fun resolveImageFile(context: Context, rawValue: String): File {
        return resolveMediaFile(context, rawValue, isAudio = false)
    }

    fun resolveAudioFile(context: Context, rawValue: String): File {
        return resolveMediaFile(context, rawValue, isAudio = true)
    }

    private fun resolveMediaFile(context: Context, rawValue: String, isAudio: Boolean): File {
        val trimmed = rawValue.trim()
        val absoluteCandidate = File(trimmed)
        if (absoluteCandidate.isAbsolute && absoluteCandidate.exists()) {
            return absoluteCandidate
        }

        val fileName = normalizeStoredFileName(rawValue)
        return if (isAudio) getAudioFile(context, fileName) else getImageFile(context, fileName)
    }

    fun configurePlayerDataSource(mediaPlayer: MediaPlayer, file: File): Boolean {
        return try {
            mediaPlayer.setDataSource(file.absolutePath)
            true
        } catch (_: Exception) {
            try {
                FileInputStream(file).use { input ->
                    mediaPlayer.setDataSource(input.fd)
                }
                true
            } catch (_: Exception) {
                false
            }
        }
    }

    val IMG_PATTERN = Regex("\\[IMG\\s*:(.+?)]", setOf(RegexOption.IGNORE_CASE))
    val AUDIO_PATTERN = Regex("\\[AUDIO\\s*:(.+?)]", setOf(RegexOption.IGNORE_CASE))
}
