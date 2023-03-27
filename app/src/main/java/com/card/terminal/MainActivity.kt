package com.card.terminal

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.smartcardio.hidglobal.Constants.PERMISSION_TO_BIND_BACKEND_SERVICE
import android.smartcardio.hidglobal.PackageManagerQuery
import android.smartcardio.ipc.ICardService
import android.view.*
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.card.terminal.components.CustomDialog
import com.card.terminal.databinding.ActivityMainBinding
import com.card.terminal.db.AppDatabase
import com.card.terminal.http.MyHttpClient
import com.card.terminal.utils.ContextProvider
import com.card.terminal.utils.ShowDateTime
import com.card.terminal.utils.cardUtils.OmniCard
import kotlinx.coroutines.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val REQUEST_BIND_BACKEND_SERVICE_PERMISSION = 9000
    private var cardService: ICardService? = null

    private var mutableCardCode = MutableLiveData<Map<String, String>>()
    var mutableLarusCode = MutableLiveData<Map<String, String>>()
    private var mutableDateTime = MutableLiveData<LocalDateTime>()
    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private var workBtnClicked = false
    private var privateBtnClicked = false
    private var coffeeBtnClicked = false
    private var doctorBtnBlicked = false
    private var enterBtnClicked = false
    private var exitBtnClicked = false
    var cardScannerActive = false
    private var screensaverShowing = false

    private val SCREENSAVER_DELAY = 30000L

    private val handler = Handler()

    val PREFS_NAME = "MyPrefsFile"
    val IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextProvider.setApplicationContext(this)

        db = AppDatabase.getInstance((this))

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isFirstTime = prefs.getBoolean(IS_FIRST_TIME_LAUNCH, true)
        if (isFirstTime) {
            val editor = prefs.edit()
            editor.putBoolean(IS_FIRST_TIME_LAUNCH, false)
            // Set the preferences if they haven't been set already
            editor.putString("larusIP", "192.168.0.200")
            editor.putInt("larusPort", 8005)
            editor.putString("serverIP", "http://sucic.info/b0pass/b0pass_iftp2.php")
            editor.putInt("serverPort", 80)
            editor.apply()
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            return true // Consume the event to disable volume buttons
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onResume() {
        super.onResume()

        mutableDateTime.postValue(LocalDateTime.now())
        ShowDateTime.setDateAndTime(mutableDateTime)

//        if (PackageManagerQuery().isCardManagerAppInstalled(this)) {
//            if (ContextCompat.checkSelfPermission(
//                    this,
//                    PERMISSION_TO_BIND_BACKEND_SERVICE
//                ) == PackageManager.PERMISSION_GRANTED
//            ) {
//                OmniCard.bindCardBackend(this, mutableCardCode, false)
//                MyHttpClient.bindHttpClient(mutableLarusCode, db)
//            } else {
//                ActivityCompat.requestPermissions(
//                    this,
//                    arrayOf(PERMISSION_TO_BIND_BACKEND_SERVICE),
//                    REQUEST_BIND_BACKEND_SERVICE_PERMISSION
//                )
//            }
//        } else {
//            Toast.makeText(this, "HID OMNIKEY driver is not installed", Toast.LENGTH_LONG).show()
//            MyHttpClient.bindHttpClient(mutableLarusCode, db)
//        }

        MyHttpClient.bindHttpClient(mutableLarusCode)
        setObservers()
    }

    fun setButtons() {
        val workButton = findViewById<Button>(R.id.ib_work)
        val privateButton = findViewById<Button>(R.id.ib_private)
        val coffeeButton = findViewById<Button>(R.id.ib_coffee)
        val doctorButton = findViewById<Button>(R.id.ib_doctor)

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
            doctorButton.setBackgroundColor(Color.TRANSPARENT)

            privateBtnClicked = false
            coffeeBtnClicked = false
            doctorBtnBlicked = false
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
            doctorButton.setBackgroundColor(Color.TRANSPARENT)

            workBtnClicked = false
            coffeeBtnClicked = false
            doctorBtnBlicked = false
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
            doctorButton.setBackgroundColor(Color.TRANSPARENT)

            workBtnClicked = false
            doctorBtnBlicked = false
            privateBtnClicked = false
        }

        doctorButton.setOnClickListener {
            if (doctorBtnBlicked) {
                doctorBtnBlicked = false
                doctorButton.setBackgroundColor(Color.TRANSPARENT)
            } else {
                doctorBtnBlicked = true
                doctorButton.setBackgroundResource(R.drawable.card_button_background)
            }

            workButton.setBackgroundColor(Color.TRANSPARENT)
            privateButton.setBackgroundColor(Color.TRANSPARENT)
            coffeeButton.setBackgroundColor(Color.TRANSPARENT)

            workBtnClicked = false
            coffeeBtnClicked = false
            privateBtnClicked = false

            showSpinningCircle(3)

            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
            val navController = navHostFragment.navController

            when (navHostFragment.navController.currentDestination?.id) {
                R.id.FirstFragment -> {
                    navController.navigate(
                        R.id.action_FirstFragment_to_CheckoutFragment
                    )
                }

            }

        }
    }

    private fun resetButtons() {
        findViewById<Button>(R.id.ib_work).setBackgroundColor(Color.TRANSPARENT)
        findViewById<Button>(R.id.ib_private).setBackgroundColor(Color.TRANSPARENT)
        findViewById<Button>(R.id.ib_coffee).setBackgroundColor(Color.TRANSPARENT)

        workBtnClicked = false
        privateBtnClicked = false
        coffeeBtnClicked = false
        doctorBtnBlicked = false
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
        }
    }

    fun showSpinningCircle(seconds: Long) {
        // create a progress dialog with a circular progress bar
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Molimo pričekajte...")
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressDialog.setCancelable(false)

        GlobalScope.launch {
            // show the progress dialog on the main thread
            withContext(Dispatchers.Main) {
                progressDialog.show()
            }
            // suspend the coroutine for 5 seconds
            delay(seconds * 1000) // 5000 milliseconds = 5 seconds
            // dismiss the progress dialog on the main thread
            withContext(Dispatchers.Main) {
                progressDialog.dismiss()
            }
        }
    }

private fun setObservers() {
    mutableLarusCode.observe(this) {
        Toast.makeText(this, it.toString(), Toast.LENGTH_LONG).show()
        if (!it["CardCode"].equals("0")) {
            it["CardCode"]?.let { it1 -> MyHttpClient.pingy(it1) }
            //TODO navHostFragment.navController.currentDestination ako ocemo vidjet u kojem smo


            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
            val navController = navHostFragment.navController

            when (navHostFragment.navController.currentDestination?.id) {
                R.id.MainFragment -> {
                    val bundle = Bundle()
                    bundle.putString("CardCode", it["CardCode"])
                    bundle.putString("DateTime", it["DateTime"])
                    try {
                        val cardOwner = db.CardDao().get(it["CardCode"]!!.toInt()).owner
                        val person = db.PersonDao().get(cardOwner)
                        bundle.putString("name", person.firstName + " " + person.lastName)
                        navController.navigate(R.id.action_mainFragment_to_FirstFragment, bundle)


                    } catch (e : Exception) {
                        Toast.makeText(this, "KARTICA NE POSTOJI U SUSTAVU!", Toast.LENGTH_LONG).show() //TODO dodat neki dijalog il nes
                    }

                }

                R.id.SettingsFragment -> {
                    Toast.makeText(
                        this,
                        "skenirana kartica ali nije inicijaliziran prolaz",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    mutableCardCode.observe(this) {
        if (!cardScannerActive) {
            return@observe
        }
        var accessGranted = false
        if ((workBtnClicked or privateBtnClicked or coffeeBtnClicked) /*and (enterBtnClicked or exitBtnClicked)?????*/) {
            if (it["ErrorCode"].equals("1")) {
                thread {
                    cardText(
                        it["CardResponse"].toString(),
                        accessGranted
                    ) //TODO INA TREBA FIXAT OVO
                }
            } else if (it["CardNumber"] != null) {
                thread {
                    val allowedAccessDao = db.CardDao()

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
            if (dateText != null) {
                dateText.text = LocalDateTime.parse(it.toString(), DateTimeFormatter.ISO_DATE_TIME)
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            }

            val clockText = findViewById<TextView>(R.id.tv_clock)
            if (clockText != null) {
                clockText.text = LocalDateTime.parse(it.toString(), DateTimeFormatter.ISO_DATE_TIME)
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
            }
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
}