package com.card.terminal

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.ProgressDialog
import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.media.MediaPlayer
import android.os.*
import android.provider.Settings
import android.smartcardio.ipc.ICardService
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import com.card.terminal.components.CustomDialog
import com.card.terminal.databinding.ActivityMainBinding
import com.card.terminal.db.AppDatabase
import com.card.terminal.db.entity.OperationSchedule
import com.card.terminal.http.MyHttpClient
import com.card.terminal.log.CustomLogFormatter
import com.card.terminal.receivers.USBReceiver
import com.card.terminal.utils.ContextProvider
import com.card.terminal.utils.MiroConverter
import fr.bipi.tressence.file.FileLoggerTree
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*


class MainActivity : AppCompatActivity() {

    private val REQUEST_BIND_BACKEND_SERVICE_PERMISSION = 9000
    private var mutableCardCode = MutableLiveData<Map<String, String>>()

    private var cardService: ICardService? = null
    var mutableLarusCode = MutableLiveData<Map<String, String>>()

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase
    private val usbReceiver: USBReceiver? = null

    private var workBtnClicked = false
    private var privateBtnClicked = false
    private var coffeeBtnClicked = false
    private var doctorBtnBlicked = false
    private var extraBtnClicked = false
    private var enterBtnClicked = false
    private var exitBtnClicked = false
    var cardScannerActive = true

    private var mediaPlayer: MediaPlayer? = null

    private lateinit var mAdminComponentName: ComponentName
    lateinit var mDevicePolicyManager: DevicePolicyManager

    val PREFS_NAME = "MyPrefsFile"
    val IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch"

    companion object {
        const val LOCK_ACTIVITY_KEY = "com.card.terminal.MainActivity"
    }

    private var timerHandler: Handler? = null
    private val delayMillis: Long = 500 // 0.5 second delay

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextProvider.setApplicationContext(this)

//        CameraUtils.init(this)

        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        registerReceiver(usbReceiver, filter)

        val croatianLocale = Locale("hr", "HR")
        Locale.setDefault(croatianLocale)

        mAdminComponentName = AdminReceiver.getComponentName(this)
        mDevicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        val permission = READ_EXTERNAL_STORAGE
        val requestCode = 123 // You can choose any integer value for the request code
        if (ContextCompat.checkSelfPermission(
                this, permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
        } else {
            startLogger()
        }

        Timber.d("Msg: MainActivity OnCreate called")
        Thread.setDefaultUncaughtExceptionHandler(
            UEHandler(
                this, MainActivity::class.java
            )
        )
        Timber.d("Msg: setDefaultUncaughtExceptionHandler")


        db = AppDatabase.getInstance(this, Thread.currentThread().stackTrace)

//        val scope3 = CoroutineScope(Dispatchers.IO)
//        scope3.launch {
//            try {
//                db.EventDao().deleteAll()
//            } catch (e: Exception) {
//                Timber.d(
//                    "Exception while clearing db: %s | %s | %s",
//                    e.cause,
//                    e.stackTraceToString(),
//                    e.message
//                )
//            }
//        }

        Timber.d("Msg: database instanced in MainActivity")
        binding = ActivityMainBinding.inflate(layoutInflater)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        var isFirstBoot = prefs.getBoolean(IS_FIRST_TIME_LAUNCH, true)

        Timber.d("hello world")
        val editor = prefs.edit()
        editor.putBoolean("Connection", false)
        if (isFirstBoot) { //Set the preferences for first time app install...

            editor.putBoolean(IS_FIRST_TIME_LAUNCH, false)
            editor.putBoolean("kioskMode", false)
            editor.putString("larusIP", "192.168.0.200")
            editor.putInt("larusPort", 8005)
            editor.putString("serverIP", "")
            editor.putInt("serverPort", 80)

            editor.putInt("IFTTERM2_B0_ID", 4)
            editor.putString("IFTTERM2_DESCR", "")
            editor.putString("settingsPin", "0")
        }
        editor.apply()

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setContentView(binding.root)

        if (isAdmin() && prefs.getBoolean("kioskMode", false)) {
            setKioskPolicies(true, true)
            val editor = prefs.edit()
            editor.putBoolean("kioskMode", true)
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.scan_success);
        mediaPlayer!!.setOnCompletionListener { mediaPlayer -> // Release the MediaPlayer resources
            mediaPlayer.release()
        }
//         val manager = getSystemService(Context.USB_SERVICE) as UsbManager
//         val deviceList: HashMap = manager.deviceList
//         val deviceIterator: Iterator = deviceList.values.iterator()
//         while (deviceIterator.hasNext()) {
//         val device: UsbDevice = deviceIterator.next()
//         manager.requestPermission(device, mPermissionIntent)
//         val model: String = device.deviceName
//         val deviceId: Int = device.deviceId
//         val vendor: Int = device.vendorId
//         val product: Int = device.productId
//         val deviceClass: Int = device.deviceClass
//         val subclass: Int = device.deviceSubclass
//         }

//        rescheduleAlarms()
    }

    fun isAdmin(): Boolean {

        mDevicePolicyManager.removeActiveAdmin(mAdminComponentName)
        val b = mDevicePolicyManager.isDeviceOwnerApp(packageName)
        Timber.d("isAdmin: %b", b)
        return b
    }

    fun setKioskPolicies(enable: Boolean, isAdmin: Boolean) {
        setRestrictions(enable)
        enableStayOnWhilePluggedIn(enable)
        setUpdatePolicy(enable)
        setKeyGuardEnabled(enable)
        setAsHomeApp(enable)
        setLockTask(enable, isAdmin)
        setImmersiveMode(enable)
    }

    override fun onDestroy() {
        MyHttpClient.server.stop(0, 0)
        super.onDestroy()
    }

    private fun setRestrictions(disallow: Boolean) {
        setUserRestriction(UserManager.DISALLOW_SAFE_BOOT, disallow)
        setUserRestriction(UserManager.DISALLOW_FACTORY_RESET, disallow)
        setUserRestriction(UserManager.DISALLOW_ADD_USER, disallow)
        setUserRestriction(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, disallow)
        setUserRestriction(UserManager.DISALLOW_ADJUST_VOLUME, disallow)
        mDevicePolicyManager.setStatusBarDisabled(mAdminComponentName, disallow)
    }

    private fun setUserRestriction(restriction: String, disallow: Boolean) = if (disallow) {
        mDevicePolicyManager.addUserRestriction(mAdminComponentName, restriction)
    } else {
        mDevicePolicyManager.clearUserRestriction(mAdminComponentName, restriction)
    }
// endregion

    private fun enableStayOnWhilePluggedIn(active: Boolean) = if (active) {
        mDevicePolicyManager.setGlobalSetting(
            mAdminComponentName,
            Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
            (BatteryManager.BATTERY_PLUGGED_AC
                    or BatteryManager.BATTERY_PLUGGED_USB
                    or BatteryManager.BATTERY_PLUGGED_WIRELESS).toString()
        )
    } else {
        mDevicePolicyManager.setGlobalSetting(
            mAdminComponentName,
            Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
            "0"
        )
    }

    private fun setLockTask(start: Boolean, isAdmin: Boolean) {
        if (isAdmin) {
            mDevicePolicyManager.setLockTaskPackages(
                mAdminComponentName, if (start) arrayOf(packageName) else arrayOf()
            )
        }
        if (start) {
            startLockTask()
        } else {
            stopLockTask()
        }
    }

    private fun setUpdatePolicy(enable: Boolean) {
        if (enable) {
            mDevicePolicyManager.setSystemUpdatePolicy(
                mAdminComponentName,
                SystemUpdatePolicy.createWindowedInstallPolicy(60, 120)
            )
        } else {
            mDevicePolicyManager.setSystemUpdatePolicy(mAdminComponentName, null)
        }
    }

    private fun setAsHomeApp(enable: Boolean) {
        if (enable) {
            val intentFilter = IntentFilter(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addCategory(Intent.CATEGORY_DEFAULT)
            }
            mDevicePolicyManager.addPersistentPreferredActivity(
                mAdminComponentName,
                intentFilter,
                ComponentName(packageName, MainActivity::class.java.name)
            )
        } else {
            mDevicePolicyManager.clearPackagePersistentPreferredActivities(
                mAdminComponentName, packageName
            )
        }
    }

    private fun setKeyGuardEnabled(enable: Boolean) {
        mDevicePolicyManager.setKeyguardDisabled(mAdminComponentName, !enable)
    }

    @Suppress("DEPRECATION")
    private fun setImmersiveMode(enable: Boolean) {
        if (enable) {
            val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.decorView.systemUiVisibility = flags
        } else {
            val flags = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            window.decorView.systemUiVisibility = flags
        }
    }

    private fun startTimer() {
        val dateText = findViewById<TextView>(R.id.please_scan_card_text)
//        dateText.visibility = View.GONE


//        val editableCardText = findViewById<EditText>(R.id.cardText)
//        editableCardText.visibility = View.VISIBLE
//        editableCardText.requestFocus()

        timerHandler?.removeCallbacksAndMessages(null) // Reset the timer
        timerHandler = Handler()
        timerHandler?.postDelayed({

            val cn = getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            ).getString("usbAdapterCardCode", "")!!

            if(cn != "") {
                handleCardScan(
                    mapOf(
                        "CardCode" to cn.trimStart('0'),
                        "DateTime" to LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")).toString(),
                        "Source" to "usbAdapter"
                    )
                )
            }

            getSharedPreferences(
                PREFS_NAME,
                MODE_PRIVATE
            ).edit().putString("usbAdapterCardCode", "").commit()
            dateText.setText(R.string.please_scan_card)
//            dateText.visibility = View.VISIBLE
//            editableCardText.visibility = View.GONE

        }, delayMillis)
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        startTimer()
        return when (keyCode) {
            KeyEvent.KEYCODE_0 -> {
                parseCardCodeFromUsbAdapter("0", System.currentTimeMillis())
                true
            }

            KeyEvent.KEYCODE_1 -> {
                parseCardCodeFromUsbAdapter("1", System.currentTimeMillis())
                true
            }

            KeyEvent.KEYCODE_2 -> {
                parseCardCodeFromUsbAdapter("2", System.currentTimeMillis())
                true
            }

            KeyEvent.KEYCODE_3 -> {
                parseCardCodeFromUsbAdapter("3", System.currentTimeMillis())
                true
            }

            KeyEvent.KEYCODE_4 -> {
                parseCardCodeFromUsbAdapter("4", System.currentTimeMillis())
                true
            }

            KeyEvent.KEYCODE_5 -> {
                parseCardCodeFromUsbAdapter("5", System.currentTimeMillis())
                true
            }

            KeyEvent.KEYCODE_6 -> {
                parseCardCodeFromUsbAdapter("6", System.currentTimeMillis())
                true
            }

            KeyEvent.KEYCODE_7 -> {
                parseCardCodeFromUsbAdapter("7", System.currentTimeMillis())
                true
            }

            KeyEvent.KEYCODE_8 -> {
                parseCardCodeFromUsbAdapter("8", System.currentTimeMillis())
                true
            }

            KeyEvent.KEYCODE_9 -> {
                parseCardCodeFromUsbAdapter("9", System.currentTimeMillis())
                true
            }

            else -> {
                super.onKeyUp(keyCode, event)
            }
        }
    }

    private fun parseCardCodeFromUsbAdapter(s: String, time: Long) {
        val dateText = findViewById<TextView>(R.id.please_scan_card_text)
//        dateText.visibility = View.GONE

        val rrr = resources.getString(R.string.please_scan_card)

        if (dateText.text.equals(rrr)) {
            dateText.text = ""
        }

        dateText.setText(dateText.text.toString() + s)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()

        val currentTimestamp = time

//        if (lastReadTimestamp - currentTimestamp < 1000) {
//            var currentString = prefs.getString("usbAdapterCardCode", "")
//            currentString += s
//            editor.putString("usbAdapterCardCode", currentString)
//            editor.commit()
//        } else {
//            handleCardScan(
//                mapOf(
//                    "CardCode" to prefs.getString("usbAdapterCardCode", "")!!,
//                    "DateTime" to LocalDateTime.now()
//                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")).toString()
//                )
//            )
//            editor.putString("usbAdapterCardCode", "")
//            editor.commit()
//        }

        var currentString = prefs.getString("usbAdapterCardCode", "")

//        if(currentString.equals("") && s.equals("0")) {
//            println("gas")
//        } else {
//            currentString += s
//            editor.putString("usbAdapterCardCode", currentString)
//            editor.commit()
//        }
        currentString += s
        editor.putString("usbAdapterCardCode", currentString)
        editor.commit()
    }

    private fun rescheduleAlarms() {
        val scope3 = CoroutineScope(Dispatchers.IO)
        scope3.launch {
            try {
                MiroConverter().setRelayTimes(
                    db.OperationScheduleDao().getAll() as MutableList<OperationSchedule>
                )
            } catch (e: Exception) {
                Timber.d(
                    "Exception while rescheduling alarms: %s | %s | %s",
                    e.cause,
                    e.stackTraceToString(),
                    e.message
                )
            }
        }
    }


    private fun startLogger() {
        try {
            val logFolder = Environment.getExternalStorageDirectory().absoluteFile
            if (!logFolder.exists()) {
                logFolder.mkdir()
            }

            val t = FileLoggerTree.Builder() //RollingFileTree
                .withFileName("my_log_file.txt").withDir(logFolder)
                .withFormatter(CustomLogFormatter()).withFileLimit(1).withSizeLimit(50000000)
                .withMinPriority(Log.DEBUG).appendToFile(true).build()

            Timber.plant(t)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    override fun onResume() {
        super.onResume()

//        if (PackageManagerQuery().isCardManagerAppInstalled(this)) {
//            if (ContextCompat.checkSelfPermission(
//                    this, PERMISSION_TO_BIND_BACKEND_SERVICE
//                ) == PackageManager.PERMISSION_GRANTED
//            ) {
//                OmniCard.bindCardBackend(this, mutableCardCode, false)
//            } else {
//                ActivityCompat.requestPermissions(
//                    this,
//                    arrayOf(PERMISSION_TO_BIND_BACKEND_SERVICE),
//                    REQUEST_BIND_BACKEND_SERVICE_PERMISSION
//                )
//            }
//        } else {
//            Toast.makeText(this, "HID OMNIKEY driver is not installed", Toast.LENGTH_LONG).show()
//        }

        MyHttpClient.bindHttpClient(mutableLarusCode)

//        MyHttpClient.larusFunctions?.setDoorTime(3000, 6000, 3000, 6000)
        setObservers()
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

    fun showDialog(text: String, boolean: Boolean) {
        val dialog = CustomDialog(this, text, boolean)
        GlobalScope.launch {
            // show the progress dialog on the main thread
            withContext(Dispatchers.Main) {
                dialog.show()
            }
            // suspend the coroutine for 5 seconds
            delay(5 * 1000) // 5000 milliseconds = 5 seconds
            // dismiss the progress dialog on the main thread
            withContext(Dispatchers.Main) {
                dialog.dismiss()
            }
        }
    }

    private fun setObservers() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment

        mutableLarusCode.observe(this) {
            if (it["CardCode"] == "CONNECTION_RESTORED") {

                when (navHostFragment.navController.currentDestination?.id) {
                    R.id.MainFragment -> {
                        val dateText = findViewById<TextView>(R.id.please_scan_card_text)
                        val ddd = findViewById<ImageView>(R.id.please_scan_icon)
                        dateText.text = "Molimo očitajte karticu."
                        ddd.visibility = View.VISIBLE
                    }
                }
            } else if (it["CardCode"] != "CONNECTION_LOST" && !it["CardCode"].equals("0")) {
                when (navHostFragment.navController.currentDestination?.id) {
                    R.id.MainFragment -> {
                        handleCardScan(it)
                    }

                    R.id.CheckoutFragment -> {
                        handleCardScan(it)
                    }

                    R.id.SettingsFragment -> {
                        showDialog(
                            "skenirana kartica ${it["CardCode"]} ali nije inicijaliziran prolaz...",
                            false
                        )
                    }
                }
            } else if (it["CardCode"] == "CONNECTION_LOST") {
                when (navHostFragment.navController.currentDestination?.id) {
                    R.id.MainFragment -> {
                        val dateText = findViewById<TextView>(R.id.please_scan_card_text)
                        val ddd = findViewById<ImageView>(R.id.please_scan_icon)
                        dateText.text = "Prekinuta LAN mreža."
                        ddd.visibility = View.GONE
                    }
                }
            }
        }
    }

    private fun playSound(i: Int) {
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = MediaPlayer.create(this, i);

        }
        mediaPlayer!!.start()
    }

    fun handleCardScan(it: Map<String, String>) {
        val bundle = Bundle()
        bundle.putString("CardCode", it["CardCode"])
        bundle.putString("DateTime", it["DateTime"])
        Timber.d("skenirao se: ${it}")

        val lastScanEvent = db.EventDao().getLastScanEvent()

        if (lastScanEvent == null || !lastScanEvent.cardNumber.toString()
                .equals(it["CardCode"]) ||
            LocalDateTime.parse(lastScanEvent.dateTime).plusSeconds(10)
                .isBefore(LocalDateTime.now())
        ) {
            try {
                val card = db.CardDao().getByCardNumber(it["CardCode"]!!.toInt())
                val person =
                    card?.let { it1 -> db.PersonDao().get(it1.owner, card.classType) }

                if (person != null && card != null) {
                    bundle.putString("firstName", person.firstName)
                    bundle.putString("lastName", person.lastName)
                    bundle.putString("userId", person.uid.toString())
                    bundle.putString("classType", card.classType)

                    if (person.companyName != "") {
                        bundle.putString("companyName", person.companyName)
                    }

                    if (person.imageB64 != "") {
                        bundle.putString("imageB64", "person.imageB64")
                    }
                    if (person.imagePath != "") {
                        bundle.putString("imagePath", person.imagePath)
                    }

                    val navHostFragment =
                        supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
                    navHostFragment.navController

                    val currentTime =
                        LocalTime.parse(
                            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                        )

//                    val currentTime = LocalTime.parse("14:31:00", DateTimeFormatter.ofPattern("HH:mm:ss"))

                    val currentDateString =
                        LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                    val currentDateDate = LocalDate.parse(currentDateString)

                    val currentDayNum = LocalDateTime.now().dayOfWeek.value
                    var currentDayString = ""

                    when {
                        currentDayNum == 1 -> {
                            currentDayString = "MONDAY"
                        }

                        currentDayNum == 2 -> {
                            currentDayString = "TUESDAY"
                        }

                        currentDayNum == 3 -> {
                            currentDayString = "WEDNESDAY"
                        }

                        currentDayNum == 4 -> {
                            currentDayString = "THURSDAY"
                        }

                        currentDayNum == 5 -> {
                            currentDayString = "FRIDAY"
                        }

                        currentDayNum == 6 -> {
                            currentDayString = "SATURDAY"
                        }

                        currentDayNum == 7 -> {
                            currentDayString = "SUNDAY"
                        }
                    }

                    try {
                        val dbSchedule1 = db.OperationScheduleDao().getAll()
                        Timber.d("dohvatio raspored: ${dbSchedule1}")
                        var conforms = -1 //doesnt conform to anything before checking
                        var containsIfNotSchedule =
                            false //if contains IF_NOT_SCHEDULE param
                        var IfNotScheduleMode = 0
                        var isTodayHoliday = false

                        val d1 = db.CalendarDao().getByDate(
                            currentDateDate.dayOfWeek.value,
                            currentDateDate.monthValue,
                            currentDateDate.year
                        )
                        Timber.d("d1: ${d1?.workDay}, ${d1?.day}, ${d1?.description}")

                        val d2 = db.CalendarDao()
                            .getByDate(
                                currentDateDate.dayOfWeek.value,
                                currentDateDate.monthValue,
                                0
                            )
                        Timber.d("d2: ${d2?.workDay}, ${d2?.day}, ${d2?.description}")

                        if ((d1 != null && !d1.workDay) || (d2 != null && !d2.workDay)) {
                            isTodayHoliday = true
                            Timber.d("Danas je praznik: True")
                        }

                        if (dbSchedule1 != null) {
                            if (dbSchedule1.isEmpty()) {
                                Timber.d("Raspored prazan!!!")
                                passageControl(2, it["CardCode"]!!, bundle)
                                MyHttpClient.pushRequest("IFTTERM2_INIT0")
                            } else {
                                for (sch in dbSchedule1) {
                                    val timeConforms =
                                        currentTime.isAfter(LocalTime.parse(sch.timeFrom)) && currentTime.isBefore(
                                            LocalTime.parse(sch.timeTo)
                                        )
                                    if (sch.description.equals("IF_NOT_SCHEDULE") && timeConforms) {
                                        containsIfNotSchedule = true
                                        IfNotScheduleMode = sch.modeId
                                    } else if (sch.description.contains("SPECIFIC_DAY") && currentDateString.equals(
                                            sch.description.substring(sch.description.indexOf(":") + 1)
                                        ) && timeConforms
                                    ) {
                                        conforms =
                                            db.OperationScheduleDao().getById(sch.uid)?.uid!!
                                        Timber.d("SPECIFIC DAY")
                                        break
                                    } else if (sch.description.contains("HOLIDAY") && isTodayHoliday && timeConforms) {
                                        conforms =
                                            db.OperationScheduleDao().getById(sch.uid)?.uid!!
                                        Timber.d("HOLIDAY")
                                        break
                                    } else if (sch.description.contains("WORKING_DAY") && currentDayString != "SATURDAY" && currentDayString != "SUNDAY" && timeConforms && !isTodayHoliday) {
                                        conforms =
                                            db.OperationScheduleDao().getById(sch.uid)?.uid!!
                                        Timber.d("WORKING_DAY")
                                        break
                                    } else if (sch.description.contains(currentDayString) && timeConforms) {
                                        conforms =
                                            db.OperationScheduleDao().getById(sch.uid)?.uid!!
                                        Timber.d("currentDayString: ${currentDayString}")
                                        break
                                    }
                                }
                                if (conforms == -1 && containsIfNotSchedule) {
                                    passageControl(IfNotScheduleMode, it["CardCode"]!!, bundle)
                                    Timber.d("passageControl(IfNotScheduleMode, it[\"CardCode\"]!!, bundle)")
                                } else {
                                    passageControl(
                                        db.OperationScheduleDao().getById(conforms)?.modeId!!,
                                        it["CardCode"]!!,
                                        bundle
                                    )
                                }
                            }
                        } else {
                            Timber.d("Raspored NULL!!!")
                            passageControl(3, it["CardCode"]!!, bundle)
                            MyHttpClient.pushRequest("IFTTERM2_INIT0")
                        }
                    } catch (e: java.lang.Exception) {
                        Timber.d(
                            "Msg: Exception %s | %s | %s",
                            e.cause,
                            e.stackTraceToString(),
                            e.message
                        )
                        showDialog("Dogodila se greška! Kartica ${it["CardCode"]}", false)
                    }

                } else {
                    Timber.d("Kartica ili osoba ${it["CardCode"]} ne postoji u bazi podataka")
                    showDialog(
                        "Kartica ili osoba ${it["CardCode"]} ne postoji u bazi podataka",
                        false
                    )
                }
            } catch (e: java.lang.Exception) {
                Timber.d(
                    "Msg: Exception %s | %s | %s",
                    e.cause,
                    e.stackTraceToString(),
                    e.message
                )
                showDialog("Dogodila se greška! Kartica ${it["CardCode"]}", false)
//                passageControl(2, it["CardCode"]!!, bundle)
            }
        }
    }

    fun passageControl(free: Int, cardCode: String, bundle: Bundle) {
//        MyHttpClient.checkDoor(1)
//        MyHttpClient.checkDoor(2)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController
        bundle.putInt("eCode", 2) //TODO

        if (free == 2) {
            Timber.d("passageControl: ne treba izbor")

            bundle.putBoolean("noButtonClickNeededRegime", true)
//            val err = switchRelays(cardCode.toInt(), true)

//            if (!err) {
            Timber.d("nema err")
            when (navHostFragment.navController.currentDestination?.id) {
                R.id.MainFragment -> {
                    Timber.d("action_mainFragment_to_CheckoutFragment")

                    navController.navigate(
                        R.id.action_mainFragment_to_CheckoutFragment,
                        bundle
                    )
                }

//                    R.id.CheckoutFragment -> {
//                        Timber.d("action_CheckoutFragment_to_MainFragment")
//
//                        navController.navigate(
//                            R.id.action_CheckoutFragment_to_MainFragment,
//                            bundle
//                        )
//                    }

                R.id.SettingsFragment -> {
                    showDialog(
                        "skenirana kartica ${cardCode} ali nije inicijaliziran prolaz :)",
                        false
                    )
                }
            }
//            }
        } else if (free == 3) {
            Timber.d("passageControl: TREBA izbor")
            bundle.putBoolean("noButtonClickNeededRegime", false)
//            val err = switchRelays(cardCode.toInt(), false)
//            if (!err) {
            Timber.d("nema errora")

            when (navHostFragment.navController.currentDestination?.id) {
                //ako se tipke trebaju stisnut
                R.id.MainFragment -> {
                    Timber.d("action_mainFragment_to_FirstFragment")

                    navController.navigate(
                        R.id.action_mainFragment_to_FirstFragment,
                        bundle
                    )
                }

                R.id.CheckoutFragment -> {
                    Timber.d("action_CheckoutFragment_to_FirstFragment")
                    navController.navigate(
                        R.id.action_CheckoutFragment_to_FirstFragment,
                        bundle
                    )
                }

                R.id.SettingsFragment -> {
                    showDialog(
                        "skenirana kartica ${cardCode} ali nije inicijaliziran prolaz :)",
                        false
                    )
                }
//                }
            }
        } else {
            showDialog(
                "skenirana kartica ${cardCode} ali nije inicijaliziran prolaz",
                false
            )
        }
    }
}