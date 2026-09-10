package com.ringtoneshuffler.app.utils

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object RingtoneCopier {

    /**
     * Copies the content from the provided URI to the system's public ringtones directory (API 29+)
     * or the app's external ringtones directory (API < 29), making it accessible to RingtoneManager.
     * This fixes the issue where RingtoneManager plays a default beep because it cannot read
     * scoped app storage.
     */
    suspend fun copyAndGetSystemReadableUri(context: Context, sourceUri: Uri): Uri? = withContext(Dispatchers.IO) {
        try {
            val resolver = context.contentResolver
            
            // Extract the original display name from the source URI
            var fileName = "ringtone_${System.currentTimeMillis()}.mp3"
            resolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                }
            }

            // Fallback for file name if it doesn't have an extension
            if (!fileName.contains(".")) {
                fileName += ".mp3"
            }

            // Get mime type based on extension
            val extension = fileName.substringAfterLast('.', "")
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase()) ?: "audio/mpeg"

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // API 29+ - Use MediaStore to write directly to the public Ringtones directory
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_RINGTONES + "/RingtoneShuffler")
                    put(MediaStore.Audio.Media.IS_RINGTONE, 1)
                    put(MediaStore.Audio.Media.IS_ALARM, 0)
                    put(MediaStore.Audio.Media.IS_NOTIFICATION, 0)
                    put(MediaStore.Audio.Media.IS_MUSIC, 0)
                }

                val destUri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: return@withContext null

                // Copy the file contents
                resolver.openInputStream(sourceUri)?.use { input ->
                    resolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
                
                return@withContext destUri
            } else {
                // API 28 and below - Use app's external files dir (RingtoneManager can read it on older Android)
                val ringtonesDir = context.getExternalFilesDir(Environment.DIRECTORY_RINGTONES)
                    ?: return@withContext null

                if (!ringtonesDir.exists()) {
                    ringtonesDir.mkdirs()
                }

                val destFile = File(ringtonesDir, fileName)

                // Copy the file contents
                resolver.openInputStream(sourceUri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                } ?: return@withContext null

                // Scan the newly copied file into MediaStore
                return@withContext scanFileAndGetUri(context, destFile) ?: Uri.fromFile(destFile)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Uses MediaScannerConnection to register the file with Android's MediaStore.
     * This converts our absolute file path into a robust content:// URI.
     */
    private suspend fun scanFileAndGetUri(context: Context, file: File): Uri? = withContext(Dispatchers.IO) {
        var scannedUri: Uri? = null
        val latch = CountDownLatch(1)
        
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            null
        ) { _, uri ->
            scannedUri = uri
            latch.countDown()
        }
        
        // Wait up to 5 seconds for the scan to complete
        latch.await(5, TimeUnit.SECONDS)
        return@withContext scannedUri
    }
}
