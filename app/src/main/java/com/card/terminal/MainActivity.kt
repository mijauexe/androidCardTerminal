package com.card.terminal

import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.smartcardio.hidglobal.Constants.PERMISSION_TO_BIND_BACKEND_SERVICE
import android.smartcardio.hidglobal.PackageManagerQuery
import android.smartcardio.ipc.ICardService
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.room.Room
import com.card.terminal.databinding.ActivityMainBinding
import com.card.terminal.db.AppDatabase
import com.card.terminal.http.MyHttpClient
import com.card.terminal.utils.ShowDateTime
import com.card.terminal.utils.cardUtils.OmniCard
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {
    private val REQUEST_BIND_BACKEND_SERVICE_PERMISSION = 9000
    private var cardService: ICardService? = null


    private var mutableCardCode = MutableLiveData<Map<String, String>>()
    private var mutableServerCode = MutableLiveData<Map<String, String>>()
    private var mutableDateTime = MutableLiveData<LocalDateTime>()
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var db : AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onResume() {
        super.onResume()
        mutableDateTime.postValue(LocalDateTime.now())
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "AppDatabase"
        ).build()

        thread {
            db.clearAllTables()
        }
        ShowDateTime.setDateAndTime(mutableDateTime)

        if (PackageManagerQuery().isCardManagerAppInstalled(this)) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    PERMISSION_TO_BIND_BACKEND_SERVICE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                OmniCard.bindCardBackend(this, mutableCardCode, false)
                MyHttpClient.bindHttpClient(mutableServerCode, db)




            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(PERMISSION_TO_BIND_BACKEND_SERVICE),
                    REQUEST_BIND_BACKEND_SERVICE_PERMISSION
                )
            }
        } else {
            Toast.makeText(this, "HID OMNIKEY driver is not installed", Toast.LENGTH_LONG).show()
        }
        setObservers()
    }

    private fun setObservers() {
        mutableCardCode.observe(this) {
            //val textic = findViewById<TextView>(R.id.textview_output)
            //textic.text = it.toString()

            Handler(Looper.getMainLooper()).post {
                Toast.makeText(this, it.toString(), Toast.LENGTH_LONG)
                    .show()
            }

            if (it["CardNumber"] != null) {
                thread {
                    val allowedAccessDao = db.AllowedAccessDao()

                    val dataList = allowedAccessDao.getAll()
                    var text = "access denied!"

                    for (r in dataList) {
                        if (r.cardNumber.equals(it["CardNumber"])) {
                            text = "access granted!"
                        }
                    }
                    Handler(Looper.getMainLooper()).post {
                        Toast.makeText(this, text, Toast.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }
        mutableDateTime.observe(this) {
            if(it != null) {
                val dateText = findViewById<TextView>(R.id.tv_date)
                dateText.text = LocalDateTime.parse(it.toString(), DateTimeFormatter.ISO_DATE_TIME).format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

                val clockText = findViewById<TextView>(R.id.tv_clock)
                clockText.text = LocalDateTime.parse(it.toString(), DateTimeFormatter.ISO_DATE_TIME).format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            }
        }
    }

    override fun onPause() {
        super.onPause()
        cardService?.releaseService()
    }

    public override fun onStop() {
        super.onStop()
        OmniCard.release()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}