package com.card.terminal.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.card.terminal.db.AppDatabase
import com.card.terminal.db.entity.Event
import com.card.terminal.http.MyHttpClient
import java.io.ByteArrayOutputStream
import java.io.IOException

object Utils {
    fun newEventImageLogic(context: Context, imageUriString: String?): String? {
        val imageUri = Uri.parse(imageUriString)
        var base64String: String? = null
        try {
            val inputStream = context.contentResolver?.openInputStream(imageUri)
            inputStream?.use { stream ->
                val bitmap = Bitmap.createBitmap(BitmapFactory.decodeStream(stream))
                val byteArrayOutputStream = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
                val byteArray = byteArrayOutputStream.toByteArray()
                base64String =
                    android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return base64String
    }

    fun publishOldBundleEventAndRemoveIt() {
        val oldBundle = Bundle()
        val prefs = ContextProvider.getApplicationContext()
            .getSharedPreferences("MyPrefsFile", AppCompatActivity.MODE_PRIVATE)
        val editor = prefs.edit()

        try {
            for ((key, value) in prefs.all) {
                if (key.contains("oldBundle_")) {
                    when (value) {
                        is String -> {
                            oldBundle.putString(key.substring(key.indexOf("_") + 1), value)
                            editor.remove(key)
                        }

                        is Int -> {
                            oldBundle.putInt(key.substring(key.indexOf("_") + 1), value)
                            editor.remove(key)
                        }

                        is Boolean -> {
                            oldBundle.putBoolean(key.substring(key.indexOf("_") + 1), value)
                            editor.remove(key)
                        }

                        else -> {}
                    }
                }
            }
            editor.commit()
        } catch (e: Exception) {
            println(e)
        }
        MyHttpClient.publishNewEvent(oldBundle)
    }

    fun commitOldBundleToSharedPrefs(bundle: Bundle) {
        val prefs = ContextProvider.getApplicationContext()
            .getSharedPreferences("MyPrefsFile", AppCompatActivity.MODE_PRIVATE)
        val editor = prefs.edit()

        try {
            for (key in bundle.keySet()) {
                when (val value = bundle.get(key)) {
                    is String -> {
                        editor.putString("oldBundle_${key}", value)
                    }

                    is Int -> {
                        editor.putInt("oldBundle_${key}", value)
                    }

                    is Boolean -> {
                        editor.putBoolean("oldBundle_${key}", value)
                    }
                }
            }
            editor.commit()
        } catch (e: Exception) {
            println(e)
        }
    }

    fun updateEvent(bundle: Bundle) {
        val db = AppDatabase.getInstance(
            ContextProvider.getApplicationContext(), Thread.currentThread().stackTrace
        )
        val e = db.EventDao()
            .getLastScanEventWithCardNumber(Integer.valueOf(bundle.get("CardCode") as String))
        val newE = e?.let {
            Event(
                uid = it.uid,
                eventCode = bundle.getInt("eCode"), //TODO
                eventCode2 = bundle.getInt("eCode2", 0),
                cardNumber = it.cardNumber,
                dateTime = it.dateTime,
                published = it.published,
                deviceId = it.deviceId,
                image = it.image
            )
        }
        if (newE != null) {
            db.EventDao().update(newE)
        }
    }
}