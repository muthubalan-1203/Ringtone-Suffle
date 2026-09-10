package com.ringtoneshuffler.app.data

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * PrefsHelper — single source of truth for persisted ringtone pool state.
 *
 * Serialization format: URIs joined with "|;|" delimiter stored in SharedPreferences.
 * Keys:
 *   - saved_uris          → "|;|"-joined URI string list
 *   - starred_ids         → comma-separated indices of starred tracks
 *   - current_index       → active pool index (Int)
 *   - algorithm           → "random" | "sequential" | "starred"
 *   - call_was_active     → Boolean flag to reject phantom IDLE events
 *   - skip_repeated       → Boolean — prevent back-to-back repeats
 *   - last_index          → previous index (for skip-repeated check)
 *   - shuffle_enabled     → master enable/disable
 *   - trigger_moment      → "idle" | "outgoing" | "ringing"
 *   - audition_volume     → Int 0–100
 *   - uri_permission      → Boolean takePersistableUriPermission
 *   - cache_private       → Boolean
 */
class PrefsHelper(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Observable state ──────────────────────────────────────────────────────

    private val _uriList = MutableStateFlow(getSavedUris())
    val uriList: StateFlow<List<Uri>> = _uriList

    private val _currentIndex = MutableStateFlow(prefs.getInt(KEY_CURRENT_INDEX, 0))
    val currentIndex: StateFlow<Int> = _currentIndex

    private val _shuffleEnabled = MutableStateFlow(prefs.getBoolean(KEY_SHUFFLE_ENABLED, true))
    val shuffleEnabled: StateFlow<Boolean> = _shuffleEnabled

    private val _algorithm = MutableStateFlow(prefs.getString(KEY_ALGORITHM, ALGO_RANDOM) ?: ALGO_RANDOM)
    val algorithm: StateFlow<String> = _algorithm

    // ── URI Pool ──────────────────────────────────────────────────────────────

    fun getSavedUris(): List<Uri> {
        val raw = prefs.getString(KEY_SAVED_URIS, "") ?: ""
        if (raw.isBlank()) return emptyList()

        val all = raw.split(DELIMITER).map { it.toUri() }

        // Deduplicate by resolved path (fixes duplicates from external/external_primary volumes)
        val seen = mutableSetOf<String>()
        return all.filter { uri ->
            val key = uri.lastPathSegment?.toLongOrNull()?.toString()
                ?: uri.toString()
            seen.add(key) // returns false if already present → filtered out
        }
    }

    fun setSavedUris(uris: List<Uri>) {
        val joined = uris.joinToString(DELIMITER) { it.toString() }
        prefs.edit().putString(KEY_SAVED_URIS, joined).apply()
        _uriList.value = uris
    }

    /**
     * Adds a URI to the pool only if the actual file is not already present.
     *
     * Why path-based dedup?
     * Android MediaStore can return the same physical file under two different
     * content:// URIs (e.g. external vs external_primary volume). A simple
     * URI.equals() check misses these — we resolve to the real file path first.
     */
    fun addUri(uri: Uri): List<Uri> {
        val current = getSavedUris().toMutableList()

        // Already have this exact URI? Skip.
        if (current.any { it == uri }) return current

        // Resolve incoming URI to a real file path (if possible)
        val incomingPath = uri.resolvedPath()

        // Check if any existing URI resolves to the same path
        val alreadyExists = incomingPath != null &&
                current.any { existing -> existing.resolvedPath() == incomingPath }

        if (!alreadyExists) {
            current.add(uri)
            setSavedUris(current)
        }
        return current
    }

    /**
     * Batch-add multiple URIs, deduplicating against the existing pool.
     * Returns the final list after all additions.
     */
    fun addUris(uris: List<Uri>): List<Uri> {
        var result = getSavedUris().toMutableList()
        val existingPaths = result.mapNotNull { it.resolvedPath() }.toMutableSet()

        for (uri in uris) {
            if (result.any { it == uri }) continue          // exact URI duplicate
            val path = uri.resolvedPath()
            if (path != null && existingPaths.contains(path)) continue  // same file, diff URI
            result.add(uri)
            if (path != null) existingPaths.add(path)
        }
        setSavedUris(result)
        return result
    }

    /**
     * Resolves a content:// or file:// URI to its actual file-system path.
     * Used for duplicate detection across different URI schemes for the same file.
     *
     * Returns null if the path cannot be resolved (URI from a cloud source, etc.)
     */
    private fun Uri.resolvedPath(): String? {
        return when (scheme) {
            "file" -> path
            "content" -> {
                // Extract the numeric media ID from the URI (last path segment)
                // and normalise to just the ID so "external" and "external_primary"
                // volumes both compare equal for the same MediaStore entry.
                lastPathSegment?.toLongOrNull()?.toString()
                    ?: pathSegments.lastOrNull()
            }
            else -> null
        }
    }

    fun removeUri(uri: Uri): List<Uri> {
        val current = getSavedUris().toMutableList()
        current.remove(uri)
        setSavedUris(current)
        return current
    }

    // ── Starred tracks ────────────────────────────────────────────────────────

    fun getStarredIndices(): Set<Int> {
        val raw = prefs.getString(KEY_STARRED_IDS, "") ?: ""
        return if (raw.isBlank()) emptySet()
        else raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    fun toggleStar(index: Int): Set<Int> {
        val starred = getStarredIndices().toMutableSet()
        if (starred.contains(index)) starred.remove(index) else starred.add(index)
        prefs.edit().putString(KEY_STARRED_IDS, starred.joinToString(",")).apply()
        return starred
    }

    // ── Advance to next ringtone ──────────────────────────────────────────────────

    /**
     * Core rotation logic — Exhausted-Pool model.
     *
     * Random / Starred modes:
     *   - Maintains a "played this cycle" set.
     *   - Picks only from UNPLAYED songs in the current pool.
     *   - When every song in the pool has played once, resets the cycle
     *     and starts a fresh round (like shuffling a deck of cards again).
     *
     * Sequential mode:
     *   - Already naturally exhausts the pool: 1→2→…→100→1.
     *   - No extra tracking needed.
     *
     * @return the URI that was set, or null if pool is empty.
     */
    fun advanceToNextRingtone(context: Context): Uri? {
        val uris = getSavedUris()
        if (uris.isEmpty()) return null

        val algo       = prefs.getString(KEY_ALGORITHM, ALGO_RANDOM) ?: ALGO_RANDOM
        val currentIdx = prefs.getInt(KEY_CURRENT_INDEX, 0)
        val starred    = getStarredIndices()

        // ── Build the full candidate pool for this algorithm ────────────────
        val fullPool: List<Int> = when (algo) {
            ALGO_STARRED    -> if (starred.isNotEmpty()) starred.sorted() else uris.indices.toList()
            ALGO_SEQUENTIAL -> uris.indices.toList()
            else            -> uris.indices.toList()  // ALGO_RANDOM
        }

        val nextIdx: Int = when (algo) {

            // ── Sequential: simple wraparound ─────────────────────────────
            ALGO_SEQUENTIAL -> {
                val pos = fullPool.indexOf(currentIdx).takeIf { it >= 0 } ?: -1
                fullPool[(pos + 1) % fullPool.size]
            }

            // ── Random / Starred: exhausted-pool shuffle ───────────────────
            else -> {
                // Load the set of indices already played this cycle
                var playedThisCycle = getPlayedIndices()

                // Remove any indices that no longer exist in the pool
                // (e.g. user deleted a song mid-cycle)
                playedThisCycle = playedThisCycle.intersect(fullPool.toSet()).toMutableSet()

                // Compute the unplayed songs remaining this cycle
                var unplayed = fullPool.filter { it !in playedThisCycle }

                // If every song has been played — NEW CYCLE 🆕
                if (unplayed.isEmpty()) {
                    playedThisCycle = mutableSetOf()
                    unplayed = fullPool.toList()
                    savePlayedIndices(emptySet())  // reset stored set
                }

                // Pick a random song from the remaining unplayed ones
                val pick = if (unplayed.size == 1) {
                    unplayed[0]
                } else {
                    // Avoid the same song back-to-back even within a new cycle
                    var candidate = unplayed.random()
                    var attempts  = 0
                    while (candidate == currentIdx && unplayed.size > 1 && attempts < 10) {
                        candidate = unplayed.random()
                        attempts++
                    }
                    candidate
                }

                // Mark this song as played and persist
                playedThisCycle.add(pick)
                savePlayedIndices(playedThisCycle)

                pick
            }
        }

        val nextUri = uris.getOrNull(nextIdx) ?: uris[0]
        setCurrentIndex(nextIdx)
        prefs.edit().putInt(KEY_LAST_INDEX, currentIdx).apply()
        setRingtone(context, nextUri)
        return nextUri
    }

    // ── RingtoneManager ───────────────────────────────────────────────────────

    fun setRingtone(context: Context, uri: Uri) {
        // WRITE_SETTINGS is a special permission — verify before calling.
        // Without it, setActualDefaultRingtoneUri() silently does nothing.
        if (!Settings.System.canWrite(context)) {
            Log.e("PrefsHelper", "❌ WRITE_SETTINGS not granted — ringtone NOT changed. " +
                    "Open app to grant permission.")
            return
        }
        RingtoneManager.setActualDefaultRingtoneUri(
            context,
            RingtoneManager.TYPE_RINGTONE,
            uri
        )
        Log.d("PrefsHelper", "✅ Ringtone set to: $uri")
    }

    fun getCurrentRingtoneUri(): Uri? {
        return try {
            val uris = getSavedUris()
            val idx = prefs.getInt(KEY_CURRENT_INDEX, 0)
            uris.getOrNull(idx)
        } catch (e: Exception) { null }
    }

    // ── Setters ───────────────────────────────────────────────────────────────

    fun setCurrentIndex(idx: Int) {
        prefs.edit().putInt(KEY_CURRENT_INDEX, idx).apply()
        _currentIndex.value = idx
    }

    fun setShuffleEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SHUFFLE_ENABLED, enabled).apply()
        _shuffleEnabled.value = enabled
    }

    fun setAlgorithm(algo: String) {
        prefs.edit().putString(KEY_ALGORITHM, algo).apply()
        _algorithm.value = algo
    }

    fun setCallWasActive(active: Boolean) {
        prefs.edit().putBoolean(KEY_CALL_WAS_ACTIVE, active).apply()
    }

    fun getCallWasActive(): Boolean = prefs.getBoolean(KEY_CALL_WAS_ACTIVE, false)

    fun isShuffleEnabled(): Boolean = prefs.getBoolean(KEY_SHUFFLE_ENABLED, true)

    fun getRequireCallWasActive(): Boolean = prefs.getBoolean(KEY_REQUIRE_CALL_ACTIVE, true)
    // ⚠️ Fix: was putString before — must be putBoolean to match getBoolean() above
    fun setRequireCallWasActive(v: Boolean) = prefs.edit().putBoolean(KEY_REQUIRE_CALL_ACTIVE, v).apply()

    // ── Missed call tracking ──────────────────────────────────────────────────
    /** True if an incoming call started ringing (even if never answered = missed call). */
    fun getCallWasRinging(): Boolean = prefs.getBoolean(KEY_CALL_WAS_RINGING, false)
    fun setCallWasRinging(v: Boolean) = prefs.edit().putBoolean(KEY_CALL_WAS_RINGING, v).apply()

    fun getSkipRepeated(): Boolean = prefs.getBoolean(KEY_SKIP_REPEATED, true)
    fun setSkipRepeated(v: Boolean) = prefs.edit().putBoolean(KEY_SKIP_REPEATED, v).apply()

    fun getTriggerMoment(): String = prefs.getString(KEY_TRIGGER_MOMENT, TRIGGER_IDLE) ?: TRIGGER_IDLE
    fun setTriggerMoment(v: String) = prefs.edit().putString(KEY_TRIGGER_MOMENT, v).apply()

    fun getAuditionVolume(): Int = prefs.getInt(KEY_AUDITION_VOLUME, 82)
    fun setAuditionVolume(v: Int) = prefs.edit().putInt(KEY_AUDITION_VOLUME, v).apply()

    fun getUriPermission(): Boolean = prefs.getBoolean(KEY_URI_PERMISSION, true)
    fun setUriPermission(v: Boolean) = prefs.edit().putBoolean(KEY_URI_PERMISSION, v).apply()

    fun getCachePrivate(): Boolean = prefs.getBoolean(KEY_CACHE_PRIVATE, false)
    fun setCachePrivate(v: Boolean) = prefs.edit().putBoolean(KEY_CACHE_PRIVATE, v).apply()

    fun incrementCallsLogged() {
        val n = prefs.getInt(KEY_CALLS_LOGGED, 0) + 1
        prefs.edit().putInt(KEY_CALLS_LOGGED, n).apply()
    }

    fun getCallsLogged(): Int = prefs.getInt(KEY_CALLS_LOGGED, 0)

    fun isFadeInEnabled(): Boolean = prefs.getBoolean(KEY_FADE_IN_ENABLED, false)
    fun setFadeInEnabled(v: Boolean) = prefs.edit().putBoolean(KEY_FADE_IN_ENABLED, v).apply()

    fun getFadeInDuration(): Int = prefs.getInt(KEY_FADE_IN_DURATION, 3) // Default 3 seconds
    fun setFadeInDuration(seconds: Int) = prefs.edit().putInt(KEY_FADE_IN_DURATION, seconds).apply()

    // ── Played-indices persistence (exhausted-pool shuffle) ───────────────────

    /** Returns the set of pool indices already played in the current cycle. */
    fun getPlayedIndices(): MutableSet<Int> {
        val raw = prefs.getString(KEY_PLAYED_INDICES, "") ?: ""
        return if (raw.isBlank()) mutableSetOf()
        else raw.split(",").mapNotNull { it.trim().toIntOrNull() }.toMutableSet()
    }

    fun savePlayedIndices(indices: Set<Int>) {
        prefs.edit().putString(KEY_PLAYED_INDICES, indices.joinToString(",")).apply()
    }

    /** Call this when songs are added/removed to reset the current cycle. */
    fun resetPlayedCycle() {
        savePlayedIndices(emptySet())
    }

    companion object {
        const val PREFS_NAME = "ringtone_shuffler_prefs"
        const val DELIMITER  = "|;|"

        const val KEY_SAVED_URIS         = "saved_uris"
        const val KEY_STARRED_IDS        = "starred_ids"
        const val KEY_PLAYED_INDICES     = "played_indices"
        const val KEY_CURRENT_INDEX      = "current_index"
        const val KEY_LAST_INDEX         = "last_index"
        const val KEY_ALGORITHM          = "algorithm"
        const val KEY_CALL_WAS_ACTIVE    = "call_was_active"
        const val KEY_CALL_WAS_RINGING   = "call_was_ringing"
        const val KEY_REQUIRE_CALL_ACTIVE = "require_call_was_active"
        const val KEY_SKIP_REPEATED      = "skip_repeated"
        const val KEY_SHUFFLE_ENABLED    = "shuffle_enabled"
        const val KEY_TRIGGER_MOMENT     = "trigger_moment"
        const val KEY_AUDITION_VOLUME    = "audition_volume"
        const val KEY_URI_PERMISSION     = "uri_permission"
        const val KEY_CACHE_PRIVATE      = "cache_private"
        const val KEY_CALLS_LOGGED       = "calls_logged"
        const val KEY_FADE_IN_ENABLED    = "fade_in_enabled"
        const val KEY_FADE_IN_DURATION   = "fade_in_duration"

        const val ALGO_RANDOM     = "random"
        const val ALGO_SEQUENTIAL = "sequential"
        const val ALGO_STARRED    = "starred"

        const val TRIGGER_IDLE     = "idle"
        const val TRIGGER_OUTGOING = "outgoing"
        const val TRIGGER_RINGING  = "ringing"
    }
}
