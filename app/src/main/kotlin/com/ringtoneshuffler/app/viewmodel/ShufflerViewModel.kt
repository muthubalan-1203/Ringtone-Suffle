package com.ringtoneshuffler.app.viewmodel

import android.content.Context
import android.net.Uri
import android.media.MediaPlayer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ringtoneshuffler.app.data.ContactManager
import com.ringtoneshuffler.app.data.HistoryItem
import com.ringtoneshuffler.app.data.HistoryManager
import com.ringtoneshuffler.app.data.PrefsHelper
import com.ringtoneshuffler.app.data.VipContact
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.Flow

class ShufflerViewModel(
    private val prefsHelper: PrefsHelper,
    private val historyManager: HistoryManager,
    private val contactManager: ContactManager
) : ViewModel() {

    // ── Exposed state ──────────────────────────────────────────────────────────

    val uriList: StateFlow<List<Uri>>    = prefsHelper.uriList
    val currentIndex: StateFlow<Int>     = prefsHelper.currentIndex
    val shuffleEnabled: StateFlow<Boolean> = prefsHelper.shuffleEnabled
    val algorithm: StateFlow<String>     = prefsHelper.algorithm

    private val _starredIndices = MutableStateFlow(prefsHelper.getStarredIndices())
    val starredIndices: StateFlow<Set<Int>> = _starredIndices.asStateFlow()

    private val _triggerMoment = MutableStateFlow(prefsHelper.getTriggerMoment())
    val triggerMoment: StateFlow<String> = _triggerMoment.asStateFlow()

    private val _skipRepeated = MutableStateFlow(prefsHelper.getSkipRepeated())
    val skipRepeated: StateFlow<Boolean> = _skipRepeated.asStateFlow()

    private val _requireCallActive = MutableStateFlow(prefsHelper.getRequireCallWasActive())
    val requireCallActive: StateFlow<Boolean> = _requireCallActive.asStateFlow()

    private val _callsLogged = MutableStateFlow(prefsHelper.getCallsLogged())
    val callsLogged: StateFlow<Int> = _callsLogged.asStateFlow()

    private val _fadeInEnabled = MutableStateFlow(prefsHelper.isFadeInEnabled())
    val fadeInEnabled: StateFlow<Boolean> = _fadeInEnabled.asStateFlow()

    private val _fadeInDuration = MutableStateFlow(prefsHelper.getFadeInDuration())
    val fadeInDuration: StateFlow<Int> = _fadeInDuration.asStateFlow()

    private val _lastShuffledUri = MutableStateFlow<Uri?>(prefsHelper.getCurrentRingtoneUri())
    val lastShuffledUri: StateFlow<Uri?> = _lastShuffledUri.asStateFlow()

    private val _playingUri = MutableStateFlow<Uri?>(null)
    val playingUri: StateFlow<Uri?> = _playingUri.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null

    val historyList: StateFlow<List<HistoryItem>> = historyManager.history

    // VIP Contacts list
    val vipContactsList: Flow<List<VipContact>> = contactManager.getVipContacts()

    // ── Actions ────────────────────────────────────────────────────────────────

    fun addUrisFromPicker(context: Context, uris: List<Uri>) {
        viewModelScope.launch {
            val validUris = mutableListOf<Uri>()
            for (uri in uris) {
                val systemReadableUri = com.ringtoneshuffler.app.utils.RingtoneCopier.copyAndGetSystemReadableUri(context, uri)
                if (systemReadableUri != null) {
                    validUris.add(systemReadableUri)
                }
            }
            if (validUris.isNotEmpty()) {
                prefsHelper.addUris(validUris)
                prefsHelper.resetPlayedCycle()
            }
        }
    }

    fun addUri(uri: Uri) {
        prefsHelper.addUri(uri)
        // New song added — reset cycle so it joins immediately
        prefsHelper.resetPlayedCycle()
    }

    fun addUris(uris: List<Uri>) {
        prefsHelper.addUris(uris)
        prefsHelper.resetPlayedCycle()
    }

    fun removeUri(uri: Uri) {
        val newList = prefsHelper.removeUri(uri)
        // Reset cycle — removed song should be excluded from tracking
        prefsHelper.resetPlayedCycle()
        if (currentIndex.value >= newList.size && newList.isNotEmpty()) {
            prefsHelper.setCurrentIndex(0)
        }
    }

    fun toggleStar(index: Int) {
        _starredIndices.value = prefsHelper.toggleStar(index)
    }

    fun setAlgorithm(algo: String) {
        prefsHelper.setAlgorithm(algo)
    }

    fun setShuffleEnabled(context: Context, enabled: Boolean) {
        prefsHelper.setShuffleEnabled(enabled)
        if (enabled) {
            com.ringtoneshuffler.app.service.ShufflerService.start(context)
        } else {
            com.ringtoneshuffler.app.service.ShufflerService.stop(context)
        }
    }

    fun setTriggerMoment(moment: String) {
        prefsHelper.setTriggerMoment(moment)
        _triggerMoment.value = moment
    }

    fun setSkipRepeated(v: Boolean) {
        prefsHelper.setSkipRepeated(v)
        _skipRepeated.value = v
    }

    fun setRequireCallActive(v: Boolean) {
        prefsHelper.setRequireCallWasActive(v)
        _requireCallActive.value = v
    }

    fun setFadeInEnabled(v: Boolean) {
        prefsHelper.setFadeInEnabled(v)
        _fadeInEnabled.value = v
    }

    fun setFadeInDuration(seconds: Int) {
        prefsHelper.setFadeInDuration(seconds)
        _fadeInDuration.value = seconds
    }

    fun manualShuffle(context: Context) {
        val uri = prefsHelper.advanceToNextRingtone(context)
        _lastShuffledUri.value = uri
        _callsLogged.value = prefsHelper.getCallsLogged()
    }

    fun refreshCallsLogged() {
        _callsLogged.value = prefsHelper.getCallsLogged()
    }

    fun clearPool() {
        prefsHelper.setSavedUris(emptyList())
        prefsHelper.setCurrentIndex(0)
        prefsHelper.resetPlayedCycle()
        _starredIndices.value = emptySet()
    }

    fun clearHistory() {
        historyManager.clearHistory()
    }

    // ── VIP Contacts ─────────────────────────────────────────────────────────

    fun setContactRingtone(context: Context, contactUri: Uri, ringtoneUri: Uri?) {
        viewModelScope.launch {
            val finalUri = if (ringtoneUri != null) {
                com.ringtoneshuffler.app.utils.RingtoneCopier.copyAndGetSystemReadableUri(context, ringtoneUri)
            } else null
            contactManager.setContactRingtone(contactUri, finalUri)
        }
    }

    fun clearContactRingtone(contactId: String) {
        viewModelScope.launch {
            contactManager.clearContactRingtoneById(contactId)
        }
    }

    // ── Playback ─────────────────────────────────────────────────────────────

    fun playPreview(context: Context, uri: Uri) {
        stopPreview()
        try {
            mediaPlayer = MediaPlayer.create(context, uri)?.apply {
                setOnCompletionListener {
                    _playingUri.value = null
                    it.release()
                }
                start()
            }
            _playingUri.value = uri
        } catch (e: Exception) {
            _playingUri.value = null
        }
    }

    fun stopPreview() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            // MediaPlayer may be in an invalid state — safe to ignore
        }
        mediaPlayer = null
        _playingUri.value = null
    }

    override fun onCleared() {
        super.onCleared()
        stopPreview()
    }

    // ── Factory ───────────────────────────────────────────────────────────────

    class Factory(
        private val prefsHelper: PrefsHelper,
        private val historyManager: HistoryManager,
        private val contactManager: ContactManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ShufflerViewModel::class.java)) {
                return ShufflerViewModel(prefsHelper, historyManager, contactManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
