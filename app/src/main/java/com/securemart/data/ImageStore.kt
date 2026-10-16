package com.securemart.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Product photos are shrunk and re-encoded as JPEG into app-private storage.
 * Re-encoding removes EXIF metadata (such as the GPS location of the photo) before it is saved.
 */
object ImageStore {
    private const val MAX_SIDE = 900

    fun decodeSampled(context: Context, uri: Uri, maxSide: Int = MAX_SIDE): Bitmap? {
        return try {
            val resolver = context.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

            var sample = 1
            while (bounds.outWidth / sample > maxSide * 2 || bounds.outHeight / sample > maxSide * 2) {
                sample *= 2
            }
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            var bitmap = resolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            } ?: return null

            val scale = maxSide.toFloat() / maxOf(bitmap.width, bitmap.height)
            val rotation = readRotation(context, uri)
            if (scale < 1f || rotation != 0) {
                val matrix = Matrix()
                if (scale < 1f) matrix.postScale(scale, scale)
                if (rotation != 0) matrix.postRotate(rotation.toFloat())
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun readRotation(context: Context, uri: Uri): Int {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                when (
                    ExifInterface(stream).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                ) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            0
        }
    }

    /** Saves the picked photo and returns the private file path, or null if it could not be read. */
    fun saveProductImage(context: Context, uri: Uri): String? {
        val bitmap = decodeSampled(context, uri) ?: return null
        return try {
            val dir = File(context.filesDir, "product_images").apply { mkdirs() }
            val file = File(dir, UUID.randomUUID().toString() + ".jpg")
            FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it) }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** Copies a bundled picture (res/drawable) into private storage and returns its file path. */
    fun copyFromResource(context: Context, resId: Int): String? {
        return try {
            val dir = File(context.filesDir, "product_images").apply { mkdirs() }
            val file = File(dir, UUID.randomUUID().toString() + ".jpg")
            context.resources.openRawResource(resId).use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            file.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    fun delete(path: String?) {
        if (path != null) {
            try {
                File(path).delete()
            } catch (e: Exception) {
                // nothing to clean up
            }
        }
    }
}
