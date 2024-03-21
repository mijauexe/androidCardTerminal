package com.card.terminal

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.hardware.usb.UsbManager
import android.media.MediaPlayer
import android.os.*
import android.provider.MediaStore
import android.provider.Settings
import android.smartcardio.hidglobal.Constants.PERMISSION_TO_BIND_BACKEND_SERVICE
import android.smartcardio.hidglobal.PackageManagerQuery
import android.smartcardio.ipc.ICardService
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.card.terminal.components.CustomDialog
import com.card.terminal.databinding.ActivityMainBinding
import com.card.terminal.db.AppDatabase
import com.card.terminal.db.entity.Person
import com.card.terminal.http.MyHttpClient
import com.card.terminal.log.CustomLogFormatter
import com.card.terminal.receivers.AdminReceiver
import com.card.terminal.receivers.USBReceiver
import com.card.terminal.utils.AlarmUtils
import com.card.terminal.utils.ContextProvider
import com.card.terminal.utils.omniCardUtils.OmniCard
import fr.bipi.tressence.context.GlobalContext.stopTimber
import fr.bipi.tressence.file.FileLoggerTree
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.IOException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private val REQUEST_BIND_BACKEND_SERVICE_PERMISSION = 9000
    private var mutableCardCode = MutableLiveData<Map<String, String>>()

    private var cardService: ICardService? = null
    private var mutableLarusCode = MutableLiveData<Map<String, String>>()

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var db: AppDatabase
//    private val usbReceiver: USBReceiver? = null

    var cardScannerActive = true
    private var mediaPlayer: MediaPlayer? = null

    private lateinit var mAdminComponentName: ComponentName

    private lateinit var mDevicePolicyManager: DevicePolicyManager
    val PREFS_NAME = "MyPrefsFile"

    private val IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch"

    private lateinit var binding: ActivityMainBinding

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService

    companion object {
        const val LOCK_ACTIVITY_KEY = "com.card.terminal.MainActivity"
        private const val TAG = "MainActivity:CameraX"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private val REQUIRED_PERMISSIONS = mutableListOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
        ).apply {
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        }.toTypedArray()
    }

    private var timerHandler: Handler? = null
    private val usbTimerDelayMilis: Long = 200

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextProvider.setApplicationContext(this)

        val croatianLocale = Locale("hr", "HR")
        Locale.setDefault(croatianLocale)

        mAdminComponentName = AdminReceiver.getComponentName(this)
        mDevicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

//        mDevicePolicyManager.clearDeviceOwnerApp(this.packageName)

        val permission = READ_EXTERNAL_STORAGE
        val requestCode = 123
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

        db = AppDatabase.getInstance((this), Thread.currentThread().stackTrace)

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

        binding = ActivityMainBinding.inflate(layoutInflater)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isFirstBoot = prefs.getBoolean(IS_FIRST_TIME_LAUNCH, true)

        val editor = prefs.edit()
        editor.putBoolean("Connection", false)

        if (isAdmin() && prefs.getBoolean("kioskMode", false)) {
            setKioskPolicies(enable = true, isAdmin = true)
            editor.putBoolean("kioskMode", true)
        }

        if (isFirstBoot) {
            editor.putBoolean(IS_FIRST_TIME_LAUNCH, false)
            editor.putBoolean("kioskMode", false)
            editor.putString("larusIP", "192.168.0.200")
            editor.putInt("larusPort", 8005)
            editor.putString("serverIP", "")
            editor.putInt("serverPort", 80)
            editor.putInt("IFTTERM2_B0_ID", 4)
            editor.putString("IFTTERM2_DESCR", "")
            editor.putString("settingsPin", "0")
            editor.putString("adamUsername", "root")
            editor.putString("adamPassword", "00000000")
            editor.putString("adamIP", "192.168.0.105")
            editor.putInt("adamRelayNum", 0)
        }

        editor.putString("usbAdapterCardCode", "")
        editor.apply()
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setContentView(binding.root)

        if(BuildConfig.RelayAlarm) {
            AlarmUtils().rescheduleAlarms()
        }

        mediaPlayer = MediaPlayer.create(this, R.raw.scan_success)
        mediaPlayer!!.setOnCompletionListener { mediaPlayer ->
            mediaPlayer.release()
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestCameraPermissions()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    private fun takePhoto(
        cardCode: Map<String, String>, bundle: Bundle
    ) {
        val imageCapture = imageCapture ?: return

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, bundle.getString("imageUUID"))
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/")
            }
        }

        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues
        ).build()

        CoroutineScope(Dispatchers.IO).launch {
            imageCapture.takePicture(outputOptions,
                ContextCompat.getMainExecutor(ContextProvider.getApplicationContext()),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onError(exc: ImageCaptureException) {
                        Timber.d("Photo capture failed: ${exc.message}")
//                        continueHandleCardScan(cardCode, bundle, "")
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
//                        Timber.d("Photo capture succeeded: ${output.savedUri}")
//                        continueHandleCardScan(cardCode, bundle, output.savedUri.toString())
                    }
                })
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            imageCapture = ImageCapture.Builder().setTargetResolution(Size(1000, 500)).build()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, imageCapture
                )
            } catch (exc: Exception) {
                Timber.tag(TAG).e(exc, "Use case binding failed")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun requestCameraPermissions() {
        activityResultLauncher.launch(REQUIRED_PERMISSIONS)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    private val activityResultLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        var permissionGranted = true
        permissions.entries.forEach {
            if (it.key in REQUIRED_PERMISSIONS && !it.value) permissionGranted = false
        }
        if (!permissionGranted) {
            Toast.makeText(
                baseContext, "Permission request denied", Toast.LENGTH_SHORT
            ).show()
        } else {
            startCamera()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        startTimer()
        return when (keyCode) {
            KeyEvent.KEYCODE_0 -> {
                parseCardCodeFromUsbAdapter("0")
                true
            }

            KeyEvent.KEYCODE_1 -> {
                parseCardCodeFromUsbAdapter("1")
                true
            }

            KeyEvent.KEYCODE_2 -> {
                parseCardCodeFromUsbAdapter("2")
                true
            }

            KeyEvent.KEYCODE_3 -> {
                parseCardCodeFromUsbAdapter("3")
                true
            }

            KeyEvent.KEYCODE_4 -> {
                parseCardCodeFromUsbAdapter("4")
                true
            }

            KeyEvent.KEYCODE_5 -> {
                parseCardCodeFromUsbAdapter("5")
                true
            }

            KeyEvent.KEYCODE_6 -> {
                parseCardCodeFromUsbAdapter("6")
                true
            }

            KeyEvent.KEYCODE_7 -> {
                parseCardCodeFromUsbAdapter("7")
                true
            }

            KeyEvent.KEYCODE_8 -> {
                parseCardCodeFromUsbAdapter("8")
                true
            }

            KeyEvent.KEYCODE_9 -> {
                parseCardCodeFromUsbAdapter("9")
                true
            }

            else -> {
                super.onKeyUp(keyCode, event)
            }
        }
    }

    private fun startTimer() {
        timerHandler?.removeCallbacksAndMessages(null)
        timerHandler = Handler()
        timerHandler?.postDelayed({

            val cn = getSharedPreferences(
                PREFS_NAME, MODE_PRIVATE
            ).getString("usbAdapterCardCode", "")!!

            if (cn != "") {
                handleCardScan(
                    mapOf(
                        "CardCode" to cn.trimStart('0'),
                        "DateTime" to LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
                            .toString(),
                        "Source" to "usbAdapter"
                    )
                )
            }

            getSharedPreferences(
                PREFS_NAME, MODE_PRIVATE
            ).edit().putString("usbAdapterCardCode", "").commit()
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment

            when (navHostFragment.navController.currentDestination?.id) {
                R.id.MainFragment -> {
                    findViewById<TextView>(R.id.please_scan_card_text)?.setText(R.string.please_scan_card)
                }
            }
        }, usbTimerDelayMilis)
    }

    private fun parseCardCodeFromUsbAdapter(s: String) {
        val rrr = resources.getString(R.string.please_scan_card)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment

        when (navHostFragment.navController.currentDestination?.id) {
            R.id.MainFragment -> {
                val dateText = findViewById<TextView>(R.id.please_scan_card_text)
                if (dateText.text.equals(rrr)) {
                    dateText.text = ""
                }
                dateText.text = dateText.text.toString() + s
            }
        }


        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()

        var currentString = prefs.getString("usbAdapterCardCode", "")

        currentString += s
        editor.putString("usbAdapterCardCode", currentString)
        editor.commit()
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

        if (BuildConfig.Omnikey) {
            Toast.makeText(this, "Omnikey", Toast.LENGTH_LONG)
                .show()
            if (PackageManagerQuery().isCardManagerAppInstalled(this)) {
                if (ContextCompat.checkSelfPermission(
                        this, PERMISSION_TO_BIND_BACKEND_SERVICE
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    OmniCard.bindCardBackend(this, mutableCardCode, false)
                } else {
                    ActivityCompat.requestPermissions(
                        this,
                        arrayOf(PERMISSION_TO_BIND_BACKEND_SERVICE),
                        REQUEST_BIND_BACKEND_SERVICE_PERMISSION
                    )
                }
            } else {
                Toast.makeText(this, "HID OMNIKEY driver is not installed", Toast.LENGTH_LONG)
                    .show()
            }
        }

        MyHttpClient.bindHttpClient(mutableLarusCode)
        setObservers()
    }

    private fun showDialog(text: String, boolean: Boolean) {
        val dialog = CustomDialog(this, text, boolean)
        GlobalScope.launch {
            withContext(Dispatchers.Main) {
                dialog.show()
            }
            delay(5 * 1000)
            withContext(Dispatchers.Main) {
                dialog.dismiss()
            }
        }
    }

    private fun setObservers() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment

        if (BuildConfig.Larus) {
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

        if (BuildConfig.Omnikey) {
            mutableCardCode.observe(this) {
                if (!cardScannerActive) {
                    return@observe
                }

                if (it["CURRENTLY_SCANNING"].equals("TRUE")) {
                    when (navHostFragment.navController.currentDestination?.id) {
                        R.id.MainFragment -> {
                            val scanCardText = findViewById<TextView>(R.id.please_scan_card_text)
                            scanCardText.textSize = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                50.0f,
                                resources.displayMetrics
                            )
                            scanCardText.setTextColor(Color.parseColor("#FAA61A"))
                            scanCardText.text =
                                "Molimo držite karticu na čitaču\n do otvaranja slijedećeg ekrana."
                            val ddd = findViewById<ImageView>(R.id.please_scan_icon)
                            ddd.visibility = View.GONE
                            val dddd = findViewById<ProgressBar>(R.id.progressBar)
                            dddd.visibility = View.VISIBLE
                        }
                    }
                }

                if (it["CURRENTLY_SCANNING"].equals("FALSE")) {
                    when (navHostFragment.navController.currentDestination?.id) {
                        R.id.MainFragment -> {
                            val scanCardText = findViewById<TextView>(R.id.please_scan_card_text)
                            scanCardText.text = "Molimo očitajte karticu."
                            scanCardText.setTextColor(Color.BLACK)
                            scanCardText.textSize = TypedValue.applyDimension(
                                TypedValue.COMPLEX_UNIT_DIP,
                                65.0f,
                                resources.displayMetrics
                            )
                            val ddd = findViewById<ImageView>(R.id.please_scan_icon)
                            ddd.visibility = View.VISIBLE
                            val dddd = findViewById<ProgressBar>(R.id.progressBar)
                            dddd.visibility = View.GONE
                        }
                    }
                }

                if (it["CardCode"] != null) {
                    when (navHostFragment.navController.currentDestination?.id) {
                        R.id.MainFragment -> {
                            playSound(R.raw.scan_success)
                            handleCardScan(it)
                        }

                        R.id.CheckoutFragment -> {
                            playSound(R.raw.scan_success)
                            handleCardScan(it)
                        }

                        R.id.SettingsFragment -> {
                            showDialog(
                                "skenirana kartica ${it["CardCode"]} ali nije inicijaliziran prolaz...",
                                false
                            )
                        }
                    }
                }
            }
        }
    }

    private fun playSound(i: Int) {
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = MediaPlayer.create(this, i)
        }
        mediaPlayer!!.start()
    }

    fun handleCardScan(cardCode: Map<String, String>) {
        val bundle = Bundle()
        bundle.putString("CardCode", cardCode["CardCode"])
        bundle.putString("DateTime", cardCode["DateTime"])
        Timber.d("skenirao se: $cardCode")

        val prefs = getSharedPreferences("MyPrefsFile", MODE_PRIVATE)

        if (prefs.getBoolean("CaptureOnEvent", false)) {
            bundle.putString("imageUUID", UUID.randomUUID().toString()) //used to find captured image later
            takePhoto(cardCode, bundle)
        }
        continueHandleCardScan(prefs, cardCode, bundle, "")
    }

    private fun continueHandleCardScan(
        prefs: SharedPreferences, it: Map<String, String>, bundle: Bundle, imageUri: String
    ) {
        try {
            val card = db.CardDao().getByCardNumber(it["CardCode"]!!.toInt())
            val person = card?.let { it1 -> db.PersonDao().get(it1.owner, card.classType) }

            if (person != null && card != null) {
                bundle.putString("firstName", person.firstName)
                bundle.putString("lastName", person.lastName)
                bundle.putString("userId", person.uid.toString())
                bundle.putString("classType", card.classType)

                if (person.companyName != "") {
                    bundle.putString("companyName", person.companyName)
                }

                if (person.imageB64 != "") {
                    bundle.putString("imageB64", person.imageB64)
                }
                if (person.imagePath != "") {
                    bundle.putString("imagePath", person.imagePath)
                    try {
                        val scope = CoroutineScope(Dispatchers.IO)
                        scope.launch {
                            try {
                                val url = "http://" + prefs.getString(
                                    "bareIP", "?"
                                ) + person.imagePath
                                val bitmap =
                                    Glide.with(this@MainActivity).asBitmap().load(url).submit().get()
                                withContext(Dispatchers.Main) {
                                    bundle.putParcelable("imageB64", bitmap)
                                }
                            } catch (e : GlideException) {
                                Timber.d("Msg: Exception while getting: %s | %s | %s", e.cause, e.stackTraceToString(), e.message)
                            } catch (e : java.lang.Exception) {
                                Timber.d("Msg: Exception while getting: %s | %s | %s", e.cause, e.stackTraceToString(), e.message)
                            }catch (e : Exception) {
                                Timber.d("Msg: Exception while getting: %s | %s | %s", e.cause, e.stackTraceToString(), e.message)
                            }
                        }
                    } catch (e : Exception) {
                        Timber.d("Msg: Exception while getting image: %s | %s | %s", e.cause, e.stackTraceToString(), e.message)
                    }
                }

                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
                navHostFragment.navController

                val currentTime = LocalTime.parse(
                    LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                )

                val currentDateString =
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                val currentDateDate = LocalDate.parse(currentDateString)

                val currentDayNum = LocalDateTime.now().dayOfWeek.value
                var currentDayString = ""

                when (currentDayNum) {
                    1 -> {
                        currentDayString = "MONDAY"
                    }

                    2 -> {
                        currentDayString = "TUESDAY"
                    }

                    3 -> {
                        currentDayString = "WEDNESDAY"
                    }

                    4 -> {
                        currentDayString = "THURSDAY"
                    }

                    5 -> {
                        currentDayString = "FRIDAY"
                    }

                    6 -> {
                        currentDayString = "SATURDAY"
                    }

                    7 -> {
                        currentDayString = "SUNDAY"
                    }
                }

                try {
                    val dbSchedule1 = db.OperationScheduleDao().getAll()
                    var conforms = -1 //doesnt conform to anything before checking
                    var containsIfNotSchedule = false //if contains IF_NOT_SCHEDULE param
                    var ifNotScheduleMode = 0
                    var isTodayHoliday = false

                    val d1 = db.CalendarDao().getByDate(
                        currentDateDate.dayOfWeek.value,
                        currentDateDate.monthValue,
                        currentDateDate.year
                    )

                    val d2 = db.CalendarDao().getByDate(
                        currentDateDate.dayOfWeek.value, currentDateDate.monthValue, 0
                    )

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
                                if (sch.description == "IF_NOT_SCHEDULE" && timeConforms) {
                                    containsIfNotSchedule = true
                                    ifNotScheduleMode = sch.modeId
                                } else if (sch.description.contains("SPECIFIC_DAY") && currentDateString.equals(
                                        sch.description.substring(sch.description.indexOf(":") + 1)
                                    ) && timeConforms
                                ) {
                                    conforms = db.OperationScheduleDao().getById(sch.uid)?.uid!!
                                    break
                                } else if (sch.description.contains("HOLIDAY") && isTodayHoliday && timeConforms) {
                                    conforms = db.OperationScheduleDao().getById(sch.uid)?.uid!!
                                    break
                                } else if (sch.description.contains("WORKING_DAY") && currentDayString != "SATURDAY" && currentDayString != "SUNDAY" && timeConforms && !isTodayHoliday) {
                                    conforms = db.OperationScheduleDao().getById(sch.uid)?.uid!!
                                    break
                                } else if (sch.description.contains(currentDayString) && timeConforms) {
                                    conforms = db.OperationScheduleDao().getById(sch.uid)?.uid!!
                                    break
                                }
                            }
                            if (conforms == -1 && containsIfNotSchedule) {
                                passageControl(ifNotScheduleMode, it["CardCode"]!!, bundle)
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
                        "Msg: Exception %s | %s | %s", e.cause, e.stackTraceToString(), e.message
                    )
                    showDialog("Dogodila se greška! Kartica ${it["CardCode"]}", false)
                }

            } else {
                Timber.d("Kartica ili osoba ${it["CardCode"]} ne postoji u bazi podataka")
                showDialog(
                    "Kartica ili osoba ${it["CardCode"]} ne postoji u bazi podataka", false
                )
            }
        } catch (e: NumberFormatException) {
            Timber.d(
                "Msg: NumberFormatException %s | %s | %s",
                e.cause,
                e.stackTraceToString(),
                e.message
            )
            showDialog("QR kod ${it["CardCode"]} je neispravan!", false)
        } catch (e: java.lang.Exception) {
            Timber.d(
                "Msg: Exception %s | %s | %s", e.cause, e.stackTraceToString(), e.message
            )
            showDialog("Dogodila se greška! Kartica ${it["CardCode"]}", false)
        }
    }


    private fun passageControl(free: Int, cardCode: String, bundle: Bundle) {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        //sto ako nisu definirane tipke za neku klasu -> sibaj na checkout fragment
        var passageType = free
        if(prefs.getBoolean("${bundle.getString("classType")}_noButtons", false)) {
            passageType = 2
        }

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        when (passageType) {
            2 -> {
                bundle.putBoolean("noButtonClickNeededRegime", true)
                when (navHostFragment.navController.currentDestination?.id) {
                    R.id.MainFragment -> {
                        navController.navigate(
                            R.id.action_mainFragment_to_CheckoutFragment, bundle
                        )
                    }

                    R.id.CheckoutFragment -> {
                        navController.navigate(
                            R.id.action_CheckoutFragment_to_CheckoutFragment, bundle
                        )
                    }

                    R.id.SettingsFragment -> {
                        showDialog(
                            "skenirana kartica $cardCode ali nije inicijaliziran prolaz :)", false
                        )
                    }
                }
            }

            3 -> {
                bundle.putBoolean("noButtonClickNeededRegime", false)
                when (navHostFragment.navController.currentDestination?.id) {
                    R.id.MainFragment -> {
                        navController.navigate(
                            R.id.action_mainFragment_to_FirstFragment, bundle
                        )
                    }

                    R.id.CheckoutFragment -> {
                        navController.navigate(
                            R.id.action_CheckoutFragment_to_FirstFragment, bundle
                        )
                    }

                    R.id.SettingsFragment -> {
                        showDialog(
                            "skenirana kartica $cardCode ali nije inicijaliziran prolaz :)", false
                        )
                    }

                    R.id.FirstFragment -> {
                        navController.navigate(
                            R.id.action_FirstFragment_to_FirstFragment, bundle
                        )
                    }
                }
            }

            else -> {
                showDialog(
                    "skenirana kartica $cardCode ali nije inicijaliziran prolaz", false
                )
            }
        }
    }

    override fun onPause() {
        Timber.d("MainActivity onPause")
        MyHttpClient.stop()
        cardService?.releaseService()
        super.onPause()
    }

    public override fun onStop() {
        Timber.d("MainActivity onStop")
        MyHttpClient.stop()
        OmniCard.release()
        stopTimber()
        super.onStop()
    }

    override fun onDestroy() {
        Timber.d("MainActivity onDestroy")
        super.onDestroy()
        MyHttpClient.server.stop(0, 0)
//        unregisterReceiver(usbReceiver)
        cameraExecutor.shutdown()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    fun isAdmin(): Boolean {
        mDevicePolicyManager.removeActiveAdmin(mAdminComponentName)
        return mDevicePolicyManager.isDeviceOwnerApp(packageName)
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

    private fun enableStayOnWhilePluggedIn(active: Boolean) = if (active) {
        mDevicePolicyManager.setGlobalSetting(
            mAdminComponentName,
            Settings.Global.STAY_ON_WHILE_PLUGGED_IN,
            (BatteryManager.BATTERY_PLUGGED_AC or BatteryManager.BATTERY_PLUGGED_USB or BatteryManager.BATTERY_PLUGGED_WIRELESS).toString()
        )
    } else {
        mDevicePolicyManager.setGlobalSetting(
            mAdminComponentName, Settings.Global.STAY_ON_WHILE_PLUGGED_IN, "0"
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
                mAdminComponentName, SystemUpdatePolicy.createWindowedInstallPolicy(60, 120)
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
            val flags =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
            window.decorView.systemUiVisibility = flags
        } else {
            val flags =
                (View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
            window.decorView.systemUiVisibility = flags
        }
    }
}