package com.ringtoneshuffler.app.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object AudioTrimmerUtil {

    suspend fun trimAudio(
        context: Context,
        inputUri: Uri,
        startTimeMs: Long,
        endTimeMs: Long
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            // First, copy the input Uri to a temporary file because FFmpeg handles files better than content URIs
            val tempInputFile = File(context.cacheDir, "temp_trim_input.mp3")
            context.contentResolver.openInputStream(inputUri)?.use { input ->
                tempInputFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            val tempOutputFile = File(context.cacheDir, "trimmed_ringtone_${System.currentTimeMillis()}.mp3")
            if (tempOutputFile.exists()) {
                tempOutputFile.delete()
            }

            // Convert ms to seconds format
            val startSeconds = startTimeMs / 1000.0
            val durationSeconds = (endTimeMs - startTimeMs) / 1000.0

            // FFmpeg command to trim audio
            // -y : overwrite
            // -i : input
            // -ss : start time
            // -t : duration
            // -c copy : copy codec (much faster, no re-encoding)
            val command = "-y -i \"${tempInputFile.absolutePath}\" -ss $startSeconds -t $durationSeconds -c copy \"${tempOutputFile.absolutePath}\""
            
            val session = FFmpegKit.execute(command)
            if (ReturnCode.isSuccess(session.returnCode)) {
                // Once successfully trimmed, copy to MediaStore so RingtoneManager can read it
                val finalUri = RingtoneCopier.copyAndGetSystemReadableUri(context, Uri.fromFile(tempOutputFile))
                
                // Cleanup temp files
                tempInputFile.delete()
                tempOutputFile.delete()
                
                return@withContext finalUri
            } else {
                Log.e("AudioTrimmerUtil", "FFmpeg failed: ${session.failStackTrace}")
                tempInputFile.delete()
                return@withContext null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext null
        }
    }
}
