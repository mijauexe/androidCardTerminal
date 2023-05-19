package com.card.terminal

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.ProgressDialog
import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.*
import android.provider.Settings
import android.smartcardio.hidglobal.Constants.PERMISSION_TO_BIND_BACKEND_SERVICE
import android.smartcardio.hidglobal.PackageManagerQuery
import android.smartcardio.ipc.ICardService
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
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
import com.card.terminal.log.CustomLogFormatter
import com.card.terminal.utils.ContextProvider
import com.card.terminal.utils.ShowDateTime
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


class MainActivity : AppCompatActivity() {

    private val REQUEST_BIND_BACKEND_SERVICE_PERMISSION = 9000
    private var mutableCardCode = MutableLiveData<Map<String, String>>()

    private var cardService: ICardService? = null
    var mutableLarusCode = MutableLiveData<Map<String, String>>()

    private var mutableDateTime = MutableLiveData<LocalDateTime>()

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    private lateinit var db: AppDatabase

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ContextProvider.setApplicationContext(this)
        mAdminComponentName = AdminReceiver.getComponentName(this)
        mDevicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        val permission = READ_EXTERNAL_STORAGE
        val requestCode = 123 // You can choose any integer value for the request code
        if (ContextCompat.checkSelfPermission(
                this,
                permission
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), requestCode)
        } else {
            startLogger()
        }

        Timber.d("Msg: MainActivity OnCreate called")
        Thread.setDefaultUncaughtExceptionHandler(
            UEHandler(
                this,
                MainActivity::class.java
            )
        )
        Timber.d("Msg: setDefaultUncaughtExceptionHandler")


        db = AppDatabase.getInstance((this))

//        val scope3 = CoroutineScope(Dispatchers.IO)
//        scope3.launch {
//            try {
//                db.clearAllTables()
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
//        isFirstBoot = true
        if (isFirstBoot) { //Set the preferences for first time app install...

            val editor = prefs.edit()
            editor.putBoolean(IS_FIRST_TIME_LAUNCH, false)
            editor.putBoolean("kioskMode", false)
            editor.putString("larusIP", "192.168.0.200")
            editor.putInt("larusPort", 8005)
            editor.putString("serverIP", "")
            editor.putInt("serverPort", 80)
            editor.putBoolean("Connection", false)
            editor.putInt("IFTTERM2_B0_ID", 4)
            editor.putString("IFTTERM2_DESCR", "")
            editor.putString("settingsPin", "0")
            editor.apply()
        }

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
    }

    private fun startLogger() {
        try {
            val logFolder = Environment.getExternalStorageDirectory().absoluteFile
            if (!logFolder.exists()) {
                logFolder.mkdir()
            }

            val t = FileLoggerTree.Builder() //RollingFileTree
                .withFileName("my_log_file.txt")
                .withDir(logFolder)
                .withFormatter(CustomLogFormatter())
                .withFileLimit(1)
                .withSizeLimit(50000000)
                .withMinPriority(Log.DEBUG)
                .appendToFile(true)
                .build()

            Timber.plant(t)
        } catch (e: IOException) {
            e.printStackTrace()
        }
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
        setObservers()
    }

    private fun resetButtons() {
//        findViewById<Button>(R.id.ib_work).setBackgroundColor(Color.TRANSPARENT)
//        findViewById<Button>(R.id.ib_private).setBackgroundColor(Color.TRANSPARENT)
//        findViewById<Button>(R.id.ib_coffee).setBackgroundColor(Color.TRANSPARENT)
        //TODO EXTRAbtn i doktor za INA?
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
                Thread.sleep(3000) //TODO OVO JE JAKO LOSE
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
        mutableLarusCode.observe(this) {
//            Toast.makeText(this, it.toString(), Toast.LENGTH_LONG).show()
            val navHostFragment =
                supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
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

        mutableCardCode.observe(this) {
            if (!cardScannerActive) {
                return@observe
            }

            playSound()

            Toast.makeText(this, it.toString(), Toast.LENGTH_LONG).show()
//            var accessGranted = false
//            if ((workBtnClicked or privateBtnClicked or coffeeBtnClicked) /*and (enterBtnClicked or exitBtnClicked)?????*/) {
//                if (it["ErrorCode"].equals("1")) {
//                    thread {
//                        cardText(
//                            it["CardResponse"].toString(),
//                            accessGranted
//                        ) //TODO INA TREBA FIXAT OVO
//                    }
//                } else if (it["CardNumber"] != null) {
//                    thread {
//                        val allowedAccessDao = db.CardDao()
//
//                        val dataList = allowedAccessDao.getAll()
//
//                        if (dataList != null) {
//                            for (r in dataList) {
//                                if (r.cardNumber.equals(it["CardNumber"])) {
//                                    accessGranted = true
//                                }
//                            }
//                        }
//
//                        cardText(it["CardNumber"].toString(), accessGranted)
//                        //resetButtons() //!!!!!
////                        TODO rijesi to kad ce trebat
////                        if (MyHttpClient.isClientReady() and accessGranted) {
////                            if (exitBtnClicked) {
////                                MyHttpClient.postData(mapOf("status" to "exit"))
////                            } else {
////                                MyHttpClient.postData(mapOf("status" to "enter"))
////                            }
////                        }
//                    }
//                }
//            } else {
//                val alertDialog: AlertDialog = AlertDialog.Builder(this@MainActivity).create()
//                alertDialog.setTitle("Napomena")
//                alertDialog.setMessage("Odaberite razlog otvaranja vrata te ulaz ili izlaz")
//                alertDialog.setButton(
//                    AlertDialog.BUTTON_NEUTRAL, "OK",
//                    { dialog, which -> dialog.dismiss() })
//                alertDialog.show()
//            }
        }

        mutableDateTime.observe(this) {
            if (it != null) {
                val dateText = findViewById<TextView>(R.id.tv_date)
                if (dateText != null) {
                    dateText.text =
                        LocalDateTime.parse(it.toString(), DateTimeFormatter.ISO_DATE_TIME)
                            .format(
                                DateTimeFormatter.ofPattern(
                                    "d. MMMM yyyy.",
                                    Locale("hr")
                                )
                            )
                }

                val clockText = findViewById<TextView>(R.id.tv_clock)
                if (clockText != null) {
                    clockText.text =
                        LocalDateTime.parse(it.toString(), DateTimeFormatter.ISO_DATE_TIME)
                            .format(DateTimeFormatter.ofPattern("HH:mm"))
                }
            }
        }
    }

    private fun playSound() {
        if (mediaPlayer != null) {
            mediaPlayer!!.stop()
            mediaPlayer!!.release()
            mediaPlayer = MediaPlayer.create(this, R.raw.scan_success);

        }
        mediaPlayer!!.start()
    }

    fun handleCardScan(it: Map<String, String>) {
        val bundle = Bundle()
        bundle.putString("CardCode", it["CardCode"])
        bundle.putString("DateTime", it["DateTime"])

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

                    getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

                    val currentTime =
                        LocalTime.parse(
                            LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
                        )

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

                        val d2 = db.CalendarDao()
                            .getByDate(
                                currentDateDate.dayOfWeek.value,
                                currentDateDate.monthValue,
                                0
                            )

                        if ((d1 != null && !d1.workDay) || (d2 != null && !d2.workDay)) {
                            isTodayHoliday = true
                        }

                        if (dbSchedule1 != null) {
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
                                    break
                                } else if (sch.description.contains("HOLIDAY") && isTodayHoliday && timeConforms) {
                                    conforms =
                                        db.OperationScheduleDao().getById(sch.uid)?.uid!!
                                    break
                                } else if (sch.description.contains("WORKING_DAY") && currentDayString != "SATURDAY" && currentDayString != "SUNDAY" && timeConforms && !isTodayHoliday) {
                                    conforms =
                                        db.OperationScheduleDao().getById(sch.uid)?.uid!!
                                    break
                                } else if (sch.description.contains(currentDayString) && timeConforms) {
                                    conforms =
                                        db.OperationScheduleDao().getById(sch.uid)?.uid!!
                                    break
                                }
                            }

                            if (conforms == -1 && containsIfNotSchedule) {
                                passageControl(IfNotScheduleMode, it["CardCode"]!!, bundle)
                            } else {
                                passageControl(
                                    db.OperationScheduleDao().getById(conforms)?.modeId!!,
                                    it["CardCode"]!!,
                                    bundle
                                )
                            }
                        } else {
                            showDialog(
                                "Nemam raspored kontrole! Kartica ${it["CardCode"]}",
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
                    }

                } else {
                    showDialog(
                        "Greška u bazi podataka! Kartica ${it["CardCode"]}",
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

    fun getDateTime(): LocalDateTime? {
        return mutableDateTime.value
    }

    fun passageControl(free: Int, cardCode: String, bundle: Bundle) {
//        MyHttpClient.checkDoor(1)
//        MyHttpClient.checkDoor(2)
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        if (free == 2) {
            bundle.putBoolean("noButtonClickNeededRegime", true)
            val err = switchRelays(cardCode.toInt(), true)

            if (!err) {
                when (navHostFragment.navController.currentDestination?.id) {
                    R.id.MainFragment -> {
                        navController.navigate(
                            R.id.action_mainFragment_to_CheckoutFragment,
                            bundle
                        )
                    }
                    R.id.CheckoutFragment -> {
                        navController.navigate(
                            R.id.action_CheckoutFragment_to_MainFragment,
                            bundle
                        )
                    }
                    R.id.SettingsFragment -> {
                        showDialog(
                            "skenirana kartica ${cardCode} ali nije inicijaliziran prolaz :)",
                            false
                        )
                    }
                }
            }
        } else if (free == 3) {
            bundle.putBoolean("noButtonClickNeededRegime", false)
            val err = switchRelays(cardCode.toInt(), false)
            if (!err) {
                when (navHostFragment.navController.currentDestination?.id) {
                    //ako se tipke trebaju stisnut
                    R.id.MainFragment -> {
                        navController.navigate(
                            R.id.action_mainFragment_to_FirstFragment,
                            bundle
                        )
                    }
                    R.id.CheckoutFragment -> {
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
                }
            }
        } else {
            showDialog(
                "skenirana kartica ${cardCode} ali nije inicijaliziran prolaz",
                false
            )
        }
    }

    fun switchRelays(cardCode: Int, noButtonClickNeededRegime: Boolean): Boolean {
        var err = false
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        if (prefs.getInt("IFTTERM2_B0_ID", 0) == 212) {
            //recepcija Hep sisak
            //NE RADI NISTA; TEO NE KUPI OCITANJA KAD JE RELEJ UPALJEN PA JE SVE SJEBANO
//            MyHttpClient.hepReceptionRelayToggle(noButtonClickNeededRegime)
        } else if (prefs.getInt("IFTTERM2_B0_ID", 0) == 214) {
            //porta1, vani sisak hep
            MyHttpClient.hepPort1RelaysToggle(noButtonClickNeededRegime)
        } else {
            err = true
            showDialog(
                "GREŠKA u id-> ${prefs.getInt("IFTTERM2_B0_ID", 0)} ${cardCode}",
                false
            )
        }
        return err
    }

    override fun onPause() {
        MyHttpClient.stop()
        cardService?.releaseService()
        super.onPause()
    }

    public override fun onStop() {
        MyHttpClient.stop()
        OmniCard.release()
        stopTimber()
        super.onStop()
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
}