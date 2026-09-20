package com.moodnotes.app.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * 图片导入与清理：把相册图片复制到应用私有目录并压缩，避免占用过大空间。
 */
object ImageStore {

    suspend fun import(
        context: Context,
        uri: Uri,
        prefix: String,
        maxDim: Int = 1600,
        square: Boolean = false,
    ): String? = withContext(Dispatchers.IO) { importSync(context, uri, prefix, maxDim, square) }

    /**
     * 视频导入：直接复制到应用私有目录（不做转码），保留原始扩展名。
     */
    suspend fun importVideo(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val dir = File(context.filesDir, "videos").apply { mkdirs() }
            val ext = run {
                val displayName = resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                displayName?.substringAfterLast('.', "")?.lowercase()?.takeIf { it.length in 1..5 }
                    ?: resolver.getType(uri)?.substringAfterLast('/')?.takeIf { it.length in 1..5 }
                    ?: "mp4"
            }
            val dest = File(dir, "video_${System.currentTimeMillis()}_${(1000..9999).random()}.$ext")
            resolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            } ?: return@runCatching null
            dest.absolutePath
        }.getOrNull()
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    /**
     * 生成调起系统视频播放器的 Intent。本应用私有文件经 FileProvider 暴露为 content:// URI，
     * 避免 Android 7.0+ 的 FileUriExposedException。若设备上没有可播放的应用则返回 null。
     */
    fun openVideoIntent(context: Context, path: String): Intent? = runCatching {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(path))
        Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }.getOrNull()

    private fun importSync(
        context: Context,
        uri: Uri,
        prefix: String,
        maxDim: Int,
        square: Boolean,
    ): String? = runCatching {
        val resolver = context.contentResolver
        val dir = File(context.filesDir, "images").apply { mkdirs() }
        val dest = File(dir, "${prefix}_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg")

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@runCatching null

        var sample = 1
        while (bounds.outWidth / sample > maxDim * 2 || bounds.outHeight / sample > maxDim * 2) sample *= 2
        val opts = BitmapFactory.Options().apply { inSampleSize = sample }
        val src = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
            ?: return@runCatching null

        val cropped = if (square) {
            val s = minOf(src.width, src.height)
            val x = (src.width - s) / 2
            val y = (src.height - s) / 2
            Bitmap.createBitmap(src, x, y, s, s)
        } else src

        val fitted = scaleToFit(cropped, maxDim)
        dest.outputStream().use { fitted.compress(Bitmap.CompressFormat.JPEG, 88, it) }
        if (fitted !== cropped) fitted.recycle()
        if (cropped !== src) cropped.recycle()
        src.recycle()
        dest.absolutePath
    }.getOrNull()

    private fun scaleToFit(bitmap: Bitmap, maxDim: Int): Bitmap {
        val w = bitmap.width
        val h = bitmap.height
        val max = maxOf(w, h)
        if (max <= maxDim) return bitmap
        val scale = maxDim.toFloat() / max
        return Bitmap.createScaledBitmap(bitmap, (w * scale).toInt(), (h * scale).toInt(), true)
    }
}