package com.card.terminal

import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.AlertDialog
import android.app.ProgressDialog
import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.*
import android.provider.Settings
import android.smartcardio.ipc.ICardService
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
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
import com.card.terminal.utils.cardUtils.OmniCard
import fr.bipi.tressence.context.GlobalContext.stopTimber
import fr.bipi.tressence.file.FileLoggerTree
import kotlinx.coroutines.*
import timber.log.Timber
import java.io.IOException
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*
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
    private var extraBtnClicked = false
    private var enterBtnClicked = false
    private var exitBtnClicked = false
    var cardScannerActive = false

    private lateinit var mAdminComponentName: ComponentName
    private lateinit var mDevicePolicyManager: DevicePolicyManager

    val PREFS_NAME = "MyPrefsFile"
    val IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch"

    companion object {
        const val LOCK_ACTIVITY_KEY = "com.card.terminal.MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
        ContextProvider.setApplicationContext(this)

        db = AppDatabase.getInstance((this))
//        db.clearAllTables()

        Timber.d("Msg: database instanced in MainActivity")
        binding = ActivityMainBinding.inflate(layoutInflater)

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isFirstTime = prefs.getBoolean(IS_FIRST_TIME_LAUNCH, true)

        Timber.d("hello world")

        if (isFirstTime) {
            val editor = prefs.edit()
            editor.putBoolean(IS_FIRST_TIME_LAUNCH, false)
            // Set the preferences for first time app install...
            editor.putBoolean("kioskMode", false)
            editor.putString("larusIP", "192.168.0.200")
            editor.putInt("larusPort", 8005)
            editor.putString("serverIP", "http://sucic.info/b0pass/b0pass_iftp2.php")
            editor.putInt("serverPort", 80)
//            editor.putString("lastScannedCardNumber", "")
//            editor.putString("lastScannedCardTime", "")
            editor.apply()
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setContentView(binding.root)

        mAdminComponentName = AdminReceiver.getComponentName(this)
        mDevicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        mDevicePolicyManager.removeActiveAdmin(mAdminComponentName)

        val isAdmin = isAdmin()

        val btn1 = findViewById<Button>(R.id.setKioskPolicies)
        btn1.setOnClickListener {
            Timber.d("setKioskPolicies button clicked")
            if (isAdmin()) {
                setKioskPolicies(true, true)
                val editor = prefs.edit()
                editor.putBoolean("kioskMode", true)
                editor.apply()
            }
        }

        val btn2 = findViewById<Button>(R.id.removeKioskPolicies)
        btn2.setOnClickListener {
            Timber.d("removeKioskPolicies button clicked")
            if (isAdmin()) {
                setKioskPolicies(false, true)
                val editor = prefs.edit()
                editor.putBoolean("kioskMode", false)
                editor.apply()
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                intent.putExtra(LOCK_ACTIVITY_KEY, false)
                startActivity(intent)
            }
        }

        if (isAdmin && prefs.getBoolean("kioskMode", false)) {
            setKioskPolicies(true, true)
        }
    }

    private fun startLogger() {
        try {
            val logFolder = Environment.getExternalStorageDirectory().absoluteFile
            if (!logFolder.exists()) {
                logFolder.mkdir()
            }

            val t = FileLoggerTree.Builder()
                .withFileName("my_log_file.txt")
                .withDir(logFolder)
                .withFormatter(CustomLogFormatter())
                .withFileLimit(1)
                .withSizeLimit(5000000)
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

        //TODO INA
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
            if (!it["CardCode"].equals("0")) {

                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
                val navController = navHostFragment.navController

                when (navHostFragment.navController.currentDestination?.id) {
                    R.id.MainFragment -> {
                        handleCardScan(it)
                    }

                    R.id.CheckoutFragment -> {
                        handleCardScan(it)
                    }

                    R.id.SettingsFragment -> {
                        showDialog("skenirana kartica ali nije inicijaliziran prolaz...", false)
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
                    dateText.text =
                        LocalDateTime.parse(it.toString(), DateTimeFormatter.ISO_DATE_TIME)
                            .format(DateTimeFormatter.ofPattern("d. MMMM yyyy.", Locale("hr")))
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

    fun handleCardScan(it: Map<String, String>) {
        val bundle = Bundle()
        bundle.putString("CardCode", it["CardCode"])
        bundle.putString("DateTime", it["DateTime"])

        val lastScanEvent = db.EventDao().getLastScanEvent()

        if (lastScanEvent == null || !lastScanEvent.cardNumber.toString().equals(it["CardCode"]) ||
            LocalDateTime.parse(lastScanEvent.dateTime).plusSeconds(15)
                .isBefore(LocalDateTime.now())
        ) {
            try {
                val cardOwner = db.CardDao().get(it["CardCode"]!!.toInt()).owner
                val person = db.PersonDao().get(cardOwner)

                //TODO ADD PICTURE OF USER
                bundle.putString("name", person.firstName + " " + person.lastName)
                bundle.putString("userId", person.uid.toString())
                bundle.putString("classType", person.classType)

                val navHostFragment =
                    supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
                val navController = navHostFragment.navController

                when (navHostFragment.navController.currentDestination?.id) {
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
                            "skenirana kartica ${it["CardCode"]} ali nije inicijaliziran prolaz :)",
                            false
                        )
                    }
                }
            } catch (e: NullPointerException) {
                showDialog("Kartica nevažeća!", false)
            } catch (e: Exception) {
                showDialog("Dogodila se greška! Molimo pokušajte ponovno.", false)
            }
        }


    }

    fun getDateTime(): LocalDateTime? {
        return mutableDateTime.value
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

    private fun isAdmin(): Boolean {
        val b = mDevicePolicyManager.isDeviceOwnerApp(packageName)
        Timber.d("isAdmin: %b", b)
        return b
    }

    private fun setKioskPolicies(enable: Boolean, isAdmin: Boolean) {
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