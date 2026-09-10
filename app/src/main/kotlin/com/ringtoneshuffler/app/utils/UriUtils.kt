package com.ringtoneshuffler.app.utils

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

fun getDisplayNameFromUri(context: Context, uri: Uri): String {
    var displayName: String? = null
    try {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    displayName = cursor.getString(index)
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    if (displayName.isNullOrEmpty()) {
        displayName = uri.lastPathSegment
            ?.substringAfterLast("/")
            ?.substringAfterLast("%2F")
            ?.substringBeforeLast(".")
            ?: uri.toString().takeLast(24)
    } else {
        // remove extension if present
        displayName = displayName!!.substringBeforeLast(".")
    }
    return displayName!!
}

@Composable
fun rememberDisplayName(uri: Uri): String {
    val context = LocalContext.current
    return remember(uri) { getDisplayNameFromUri(context, uri) }
}
