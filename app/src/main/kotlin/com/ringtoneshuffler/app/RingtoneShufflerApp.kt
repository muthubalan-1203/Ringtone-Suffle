package com.ringtoneshuffler.app

import android.app.Application
import com.ringtoneshuffler.app.data.ContactManager
import com.ringtoneshuffler.app.data.HistoryManager
import com.ringtoneshuffler.app.data.PrefsHelper

class RingtoneShufflerApp : Application() {

    /** App-wide singleton for SharedPreferences access. */
    lateinit var prefsHelper: PrefsHelper
        private set

    lateinit var historyManager: HistoryManager
        private set

    lateinit var contactManager: ContactManager
        private set

    override fun onCreate() {
        super.onCreate()
        prefsHelper = PrefsHelper(this)
        historyManager = HistoryManager(this)
        contactManager = ContactManager(this)
    }
}
