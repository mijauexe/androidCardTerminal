package com.card.terminal.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Environment
import com.card.terminal.db.AppDatabase
import com.card.terminal.utils.ContextProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.File


class CleanUpReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent) {
        if (intent.action != null && intent.action.equals("com.cleanup.pics")) {
            cleanUp()
        }
    }

    private fun cleanUp() {
        val scope3 = CoroutineScope(Dispatchers.IO)
        scope3.launch {
            val db = AppDatabase.getInstance(
                ContextProvider.getApplicationContext(), Thread.currentThread().stackTrace
            )
            val eventList = db.EventDao().getPublishedEvents()

            if (eventList != null) {
                for (event in eventList) {
                    try {
                        val picUUID = event.image

                        val path =
                            "${Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)}" + "/" + picUUID + ".jpg"

                        val deleted = File(path).delete()
                        db.EventDao().deleteEventByImageUUID(picUUID)
                    } catch (e: Exception) {
                        Timber.d(e.stackTraceToString())
                    }
                }
            }
        }
    }
}