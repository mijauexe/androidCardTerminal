package com.card.terminal

import android.app.AlertDialog
import android.app.Dialog
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
import android.view.View
import android.view.Window
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.room.Room
import com.card.terminal.components.CustomDialog
import com.card.terminal.databinding.ActivityMainBinding
import com.card.terminal.db.AppDatabase
import com.card.terminal.http.MyHttpClient
import com.card.terminal.utils.ContextProvider
import com.card.terminal.utils.ShowDateTime
import com.card.terminal.utils.cardUtils.OmniCard
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
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

    private fun disableButtons() {
        val decorView: View = this.window.decorView
        val uiOptions: Int = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        val timer = Timer()
        val task: TimerTask = object : TimerTask() {
            override fun run() {
                runOnUiThread { decorView.systemUiVisibility = uiOptions }
            }
        }
        timer.scheduleAtFixedRate(task, 1, 2)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
//        disableButtons()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        disableButtons()
        ContextProvider.setApplicationContext(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        MyHttpClient.setDoorTime(1000, 1000, 1000, 1000)
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
            MyHttpClient.bindHttpClient(mutableServerCode, db)
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
                workButton.setBackgroundColor(Color.TRANSPARENT)
            } else {
                workBtnClicked = true
                workButton.setBackgroundResource(R.drawable.card_button_background)
            }

            privateButton.setBackgroundColor(Color.TRANSPARENT)
            coffeeButton.setBackgroundColor(Color.TRANSPARENT)

            privateBtnClicked = false
            coffeeBtnClicked = false
        }

        privateButton.setOnClickListener {
            if (privateBtnClicked) {
                privateBtnClicked = false
                privateButton.setBackgroundColor(Color.TRANSPARENT)
            } else {
                privateBtnClicked = true
                privateButton.setBackgroundResource(R.drawable.card_button_background)
            }

            workButton.setBackgroundColor(Color.TRANSPARENT)
            coffeeButton.setBackgroundColor(Color.TRANSPARENT)

            workBtnClicked = false
            coffeeBtnClicked = false
        }

        coffeeButton.setOnClickListener {
            if (coffeeBtnClicked) {
                coffeeBtnClicked = false
                coffeeButton.setBackgroundColor(Color.TRANSPARENT)
            } else {
                coffeeBtnClicked = true
                coffeeButton.setBackgroundResource(R.drawable.card_button_background)
            }

            workButton.setBackgroundColor(Color.TRANSPARENT)
            privateButton.setBackgroundColor(Color.TRANSPARENT)

            workBtnClicked = false
            privateBtnClicked = false
        }

        enterButton.setOnClickListener {
            if (enterBtnClicked) {
                enterBtnClicked = false
                enterButton.setBackgroundColor(Color.TRANSPARENT)
            } else {
                enterBtnClicked = true
                enterButton.setBackgroundResource(R.drawable.button_background_green)
            }
            exitButton.setBackgroundColor(Color.TRANSPARENT)
            exitBtnClicked = false
        }

        exitButton.setOnClickListener {
            if (exitBtnClicked) {
                exitBtnClicked = false
                exitButton.setBackgroundColor(Color.TRANSPARENT)
            } else {
                exitBtnClicked = true
                exitButton.setBackgroundResource(R.drawable.button_background_red)
            }
            enterButton.setBackgroundColor(Color.TRANSPARENT)
            enterBtnClicked = false
        }
    }

    private fun resetButtons() {
        findViewById<Button>(R.id.ib_work).setBackgroundColor(Color.TRANSPARENT)
        findViewById<Button>(R.id.ib_private).setBackgroundColor(Color.TRANSPARENT)
        findViewById<Button>(R.id.ib_coffee).setBackgroundColor(Color.TRANSPARENT)
        findViewById<Button>(R.id.ib_enter).setBackgroundColor(Color.TRANSPARENT)
        findViewById<Button>(R.id.ib_exit).setBackgroundColor(Color.TRANSPARENT)

        workBtnClicked = false
        privateBtnClicked = false
        coffeeBtnClicked = false
        exitBtnClicked = false
        enterBtnClicked = false
    }


    private fun cardText(text: String, access: Boolean) {
        Handler(Looper.getMainLooper()).post {
            resetButtons()
            val dialog = this.let { CustomDialog(it, "Card number: $text", access) }
            dialog.setOnShowListener {
                Thread.sleep(3000)
                it.dismiss()
            }
            dialog.show()
//            if (access) {
//                binding.root.findNavController().navigate(R.id.MainFragment) ovo kurca ne radi
//            }
        }
    }

    private fun setObservers() {
        mutableCardCode.observe(this) {
            if (!cardScannerActive) {
                return@observe
            }
            var accessGranted = false
            if ((workBtnClicked or privateBtnClicked or coffeeBtnClicked) and (enterBtnClicked or exitBtnClicked)) {
                if (it["ErrorCode"].equals("1")) {
                    thread {
                        cardText(it["CardResponse"].toString(), accessGranted)
                    }
                } else if (it["CardNumber"] != null) {
                    thread {
                        val allowedAccessDao = db.AllowedAccessDao()

                        val dataList = allowedAccessDao.getAll()

                        for (r in dataList) {
                            if (r.cardNumber.equals(it["CardNumber"])) {
                                accessGranted = true
                            }
                        }

                        cardText(it["CardNumber"].toString(), accessGranted)
                        //resetButtons() //!!!!!
//                        TODO rijesi to kad ce trebat
//                        if (MyHttpClient.isClientReady() and accessGranted) {
//                            if (exitBtnClicked) {
//                                MyHttpClient.postData(mapOf("status" to "exit"))
//                            } else {
//                                MyHttpClient.postData(mapOf("status" to "enter"))
//                            }
//                        }
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
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
            }
        }
    }

    fun getDateTime(): LocalDateTime? {
        return mutableDateTime.value
    }

    override fun onPause() {
        super.onPause()
        MyHttpClient.stop()
        cardService?.releaseService()
    }

    public override fun onStop() {
        super.onStop()
//        MyHttpClient.stop()
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
        val queuee = LinkedBlockingQueue<Boolean>()
        thread {
            val pinCodeDao = db.PinCodeDao()

            val dataList = pinCodeDao.getAll()

            for (r in dataList) {
                if (r.pinCode == text) {
                    queuee.add(true)
                    return@thread
                }
            }
            queuee.add(false)
        }
        return queuee.take()
    }
}