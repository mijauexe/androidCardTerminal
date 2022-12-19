package com.card.terminal

import android.app.AlertDialog
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.smartcardio.hidglobal.Constants.PERMISSION_TO_BIND_BACKEND_SERVICE
import android.smartcardio.hidglobal.PackageManagerQuery
import android.smartcardio.ipc.ICardService
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
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
    private lateinit var db: AppDatabase

    private var workBtnClicked = false
    private var privateBtnClicked = false
    private var coffeeBtnClicked = false
    private var enterBtnClicked = false
    private var exitBtnClicked = false

    var cardScannerActive = false

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

//        thread {
//            db.clearAllTables()
//        }

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

    fun setButtons() {
        val workButton = findViewById<Button>(R.id.ib_work)
        val privateButton = findViewById<Button>(R.id.ib_private)
        val coffeeButton = findViewById<Button>(R.id.ib_coffee)

        val enterButton = findViewById<Button>(R.id.ib_enter)
        val exitButton = findViewById<Button>(R.id.ib_exit)


        workButton.setOnClickListener {
            if (workBtnClicked) {
                workBtnClicked = false
                workButton.setBackgroundColor(Color.parseColor("#1e88e5"))
            } else {
                workBtnClicked = true
                workButton.setBackgroundResource(R.drawable.on_item_select_work)
            }

            privateButton.setBackgroundColor(Color.parseColor("#ff8f00"))
            coffeeButton.setBackgroundColor(Color.parseColor("#ffeb3b"))

            privateBtnClicked = false
            coffeeBtnClicked = false
        }

        privateButton.setOnClickListener {
            if (privateBtnClicked) {
                privateBtnClicked = false
                privateButton.setBackgroundColor(Color.parseColor("#ff8f00"))
            } else {
                privateBtnClicked = true
                privateButton.setBackgroundResource(R.drawable.on_item_select_private)
            }

            workButton.setBackgroundColor(Color.parseColor("#1e88e5"))
            coffeeButton.setBackgroundColor(Color.parseColor("#ffeb3b"))

            workBtnClicked = false
            coffeeBtnClicked = false
        }

        coffeeButton.setOnClickListener {
            if (coffeeBtnClicked) {
                coffeeBtnClicked = false
                coffeeButton.setBackgroundColor(Color.parseColor("#ffeb3b"))
            } else {
                coffeeBtnClicked = true
                coffeeButton.setBackgroundResource(R.drawable.on_item_select_coffee)
            }

            workButton.setBackgroundColor(Color.parseColor("#1e88e5"))
            privateButton.setBackgroundColor(Color.parseColor("#ff8f00"))

            workBtnClicked = false
            privateBtnClicked = false
        }

        enterButton.setOnClickListener {
            if (enterBtnClicked) {
                enterBtnClicked = false
                enterButton.setBackgroundColor(Color.parseColor("#43a047"))
            } else {
                enterBtnClicked = true
                enterButton.setBackgroundResource(R.drawable.on_item_select_enter)
            }
            exitButton.setBackgroundColor(Color.parseColor("#e64a19"))
            exitBtnClicked = false
        }

        exitButton.setOnClickListener {
            if (exitBtnClicked) {
                exitBtnClicked = false
                exitButton.setBackgroundColor(Color.parseColor("#e64a19"))
            } else {
                exitBtnClicked = true
                exitButton.setBackgroundResource(R.drawable.on_item_select_exit)
            }
            enterButton.setBackgroundColor(Color.parseColor("#43a047"))
            enterBtnClicked = false
        }
    }

    private fun cardText(text: String, access: String) {
        Handler(Looper.getMainLooper()).post {
            val cardNumber = findViewById<TextView>(R.id.textview_output)
            cardNumber.text = text
            Toast.makeText(this, access, Toast.LENGTH_LONG)
                .show()
        }
        if (cardScannerActive) {
            resetButtons()
        }
        Thread.sleep(5000)

        Handler(Looper.getMainLooper()).post {
            val cardNumber = findViewById<TextView>(R.id.textview_output)
            cardNumber.text = ""
        }
    }

    private fun setObservers() {
        mutableCardCode.observe(this) {
            if (!cardScannerActive) {
                return@observe
            }
            var accessText = "access denied!"
            if ((workBtnClicked or privateBtnClicked or coffeeBtnClicked) and (enterBtnClicked or exitBtnClicked)) {
                if (it["ErrorCode"].equals("1")) {
                    thread {
                        cardText(it["CardResponse"].toString(), accessText)
                    }
                } else if (it["CardNumber"] != null) {
                    thread {
                        val allowedAccessDao = db.AllowedAccessDao()

                        val dataList = allowedAccessDao.getAll()

                        for (r in dataList) {
                            if (r.cardNumber.equals(it["CardNumber"])) {
                                accessText = "access granted!"
                            }
                        }

                        cardText(it["CardNumber"].toString(), accessText)

                        if (MyHttpClient.isClientReady() and !accessText.equals("access denied!")) {
                            if (exitBtnClicked) {
                                MyHttpClient.postData(mapOf("status" to "exit"))
                            } else MyHttpClient.postData(mapOf("status" to "enter"))
                        }
                    }
                }
            } else {
                val alertDialog: AlertDialog = AlertDialog.Builder(this@MainActivity).create()
                alertDialog.setTitle("Napomena")
                alertDialog.setMessage("Odaberite razlog otvaranja vrata te ulaz ili izlaz")
                alertDialog.setButton(
                    AlertDialog.BUTTON_NEUTRAL, "OK",
                    { dialog, which -> dialog.dismiss() })
                alertDialog.show()
            }
        }
        mutableDateTime.observe(this) {
            if (it != null) {
                val dateText = findViewById<TextView>(R.id.tv_date)
                dateText.text = LocalDateTime.parse(it.toString(), DateTimeFormatter.ISO_DATE_TIME)
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))

                val clockText = findViewById<TextView>(R.id.tv_clock)
                clockText.text = LocalDateTime.parse(it.toString(), DateTimeFormatter.ISO_DATE_TIME)
                    .format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            }
        }
    }

    private fun resetButtons() {
        val workButton = findViewById<Button>(R.id.ib_work)
        val privateButton = findViewById<Button>(R.id.ib_private)
        val coffeeButton = findViewById<Button>(R.id.ib_coffee)
        val enterButton = findViewById<Button>(R.id.ib_enter)
        val exitButton = findViewById<Button>(R.id.ib_exit)

        workBtnClicked = false
        privateBtnClicked = false
        coffeeBtnClicked = false
        exitBtnClicked = false
        enterBtnClicked = false

        workButton.setBackgroundColor(Color.parseColor("#1e88e5"))
        privateButton.setBackgroundColor(Color.parseColor("#ff8f00"))
        coffeeButton.setBackgroundColor(Color.parseColor("#ffeb3b"))
        exitButton.setBackgroundColor(Color.parseColor("#e64a19"))
        enterButton.setBackgroundColor(Color.parseColor("#43a047"))
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

    fun checkPin(text: CharSequence?): Boolean {
        if (text == null) return false
        //thread {
        val pinCodeDao = db.PinCodeDao()

        val dataList = pinCodeDao.getAll()

        for (r in dataList) {
            if (r.pinCode == text) {
                return true
            }
        }
        // }
        return false
    }
}