package com.ringtoneshuffler.app.data

import android.content.ContentValues
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VipContact(
    val id: String,
    val name: String,
    val ringtoneUri: String?
)

class ContactManager(private val context: Context) {

    /**
     * Updates the custom ringtone for a specific contact URI (returned from PickContact).
     * Set ringtoneUri to null to clear it.
     */
    suspend fun setContactRingtone(contactLookupUri: Uri, ringtoneUri: Uri?) = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                if (ringtoneUri == null) {
                    putNull(ContactsContract.Contacts.CUSTOM_RINGTONE)
                } else {
                    put(ContactsContract.Contacts.CUSTOM_RINGTONE, ringtoneUri.toString())
                }
            }
            context.contentResolver.update(contactLookupUri, values, null, null)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Clears the custom ringtone using the contact ID.
     */
    suspend fun clearContactRingtoneById(contactId: String) = withContext(Dispatchers.IO) {
        try {
            val values = ContentValues().apply {
                putNull(ContactsContract.Contacts.CUSTOM_RINGTONE)
            }
            context.contentResolver.update(
                ContactsContract.Contacts.CONTENT_URI,
                values,
                "${ContactsContract.Contacts._ID} = ?",
                arrayOf(contactId)
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Returns a Flow of VIP contacts (contacts that have a custom ringtone set).
     * It observes changes in the Contacts database.
     */
    fun getVipContacts(): Flow<List<VipContact>> = callbackFlow {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                trySend(Unit)
            }
        }
        
        context.contentResolver.registerContentObserver(
            ContactsContract.Contacts.CONTENT_URI,
            true,
            observer
        )

        // Ensure we fetch immediately upon collection
        awaitClose {
            context.contentResolver.unregisterContentObserver(observer)
        }
    }.onStart { emit(Unit) }.map {
        fetchVipContacts()
    }

    private suspend fun fetchVipContacts(): List<VipContact> = withContext(Dispatchers.IO) {
        val vipList = mutableListOf<VipContact>()
        
        try {
            val projection = arrayOf(
                ContactsContract.Contacts._ID,
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.Contacts.CUSTOM_RINGTONE
            )

            // Query contacts where CUSTOM_RINGTONE is not null
            val selection = "${ContactsContract.Contacts.CUSTOM_RINGTONE} IS NOT NULL"

            context.contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                projection,
                selection,
                null,
                "${ContactsContract.Contacts.DISPLAY_NAME_PRIMARY} ASC"
            )?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
                val nameIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME_PRIMARY)
                val ringtoneIndex = cursor.getColumnIndexOrThrow(ContactsContract.Contacts.CUSTOM_RINGTONE)

                while (cursor.moveToNext()) {
                    val id = cursor.getString(idIndex)
                    val name = cursor.getString(nameIndex) ?: "Unknown"
                    val ringtone = cursor.getString(ringtoneIndex)
                    
                    if (!ringtone.isNullOrEmpty()) {
                        vipList.add(VipContact(id, name, ringtone))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        vipList
    }
}
