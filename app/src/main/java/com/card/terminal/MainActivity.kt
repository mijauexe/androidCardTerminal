package com.card.terminal

import android.app.AlertDialog
import android.app.PendingIntent
import android.app.ProgressDialog
import android.app.admin.DevicePolicyManager
import android.app.admin.SystemUpdatePolicy
import android.content.*
import android.content.pm.ActivityInfo
import android.content.pm.PackageInstaller
import android.graphics.Color
import android.os.*
import android.provider.Settings
import android.smartcardio.ipc.ICardService
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import com.card.terminal.components.CustomDialog
import com.card.terminal.databinding.ActivityMainBinding
import com.card.terminal.db.AppDatabase
import com.card.terminal.http.MyHttpClient
import com.card.terminal.utils.AdminUtils
import com.card.terminal.utils.ContextProvider
import com.card.terminal.utils.ShowDateTime
import com.card.terminal.utils.cardUtils.OmniCard
import com.google.android.material.snackbar.Snackbar
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
    private var extraBtnClicked = false
    private var enterBtnClicked = false
    private var exitBtnClicked = false
    var cardScannerActive = false

    private lateinit var mAdminComponentName: ComponentName
    private lateinit var mDevicePolicyManager: DevicePolicyManager
    private lateinit var adminUtils: AdminUtils

    val PREFS_NAME = "MyPrefsFile"
    val IS_FIRST_TIME_LAUNCH = "IsFirstTimeLaunch"



    companion object {
        const val LOCK_ACTIVITY_KEY = "com.card.terminal.MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ContextProvider.setApplicationContext(this)
        db = AppDatabase.getInstance((this))

        binding = ActivityMainBinding.inflate(layoutInflater)


        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val isFirstTime = prefs.getBoolean(IS_FIRST_TIME_LAUNCH, true)
        if (isFirstTime) {
            val editor = prefs.edit()
            editor.putBoolean(IS_FIRST_TIME_LAUNCH, false)
            // Set the preferences for first time app install...
            editor.putString("larusIP", "nsve.tplinkdns.com")
            editor.putInt("larusPort", 6798)
            editor.putString("serverIP", "http://sucic.info/b0pass/b0pass_iftp2.php")
            editor.putInt("serverPort", 80)
            editor.apply()
        }

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE

        setContentView(binding.root)

        mAdminComponentName = AdminReceiver.getComponentName(this)
        mDevicePolicyManager =
            getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager

        mDevicePolicyManager.removeActiveAdmin(mAdminComponentName)

        adminUtils = AdminUtils(this, mDevicePolicyManager, mAdminComponentName, LOCK_ACTIVITY_KEY)

        val isAdmin = isAdmin()

        if (isAdmin) {
            Toast.makeText(this, "you're admin!", Toast.LENGTH_LONG).show()
            val btn1 = findViewById<Button>(R.id.setKioskPolicies)
            btn1.setOnClickListener {
                setKioskPolicies(true, true)
            }
            val btn2 = findViewById<Button>(R.id.removeKioskPolicies)
            btn2.setOnClickListener {
                setKioskPolicies(false, true)
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                intent.putExtra(LOCK_ACTIVITY_KEY, false)
                startActivity(intent)
            }
        } else {
            Toast.makeText(this, "you're NOT admin!", Toast.LENGTH_LONG).show()
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

    fun switchToCheckoutFragment(selection: String) {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        val bundle = Bundle()
        bundle.putString("selection", selection)

        when (navHostFragment.navController.currentDestination?.id) {
            R.id.FirstFragment -> {
                navController.navigate(
                    R.id.action_FirstFragment_to_CheckoutFragment, bundle
                )
            }
        }
    }

    private fun resetButtons() {
        findViewById<Button>(R.id.ib_work).setBackgroundColor(Color.TRANSPARENT)
        findViewById<Button>(R.id.ib_private).setBackgroundColor(Color.TRANSPARENT)
        findViewById<Button>(R.id.ib_coffee).setBackgroundColor(Color.TRANSPARENT)
        //TODO EXTRA i doktor za INA?
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
                            bundle.putString("userId", person.uid.toString())

                            //TODO ADD PICTURE OF USER
                            bundle.putString("name", person.firstName + " " + person.lastName)
                            navController.navigate(
                                R.id.action_mainFragment_to_FirstFragment,
                                bundle
                            )
                        } catch (e: Exception) {
                            Toast.makeText(this, "KARTICA NE POSTOJI U SUSTAVU!", Toast.LENGTH_LONG)
                                .show() //TODO dodat neki dijalog il nes
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
                    dateText.text =
                        LocalDateTime.parse(it.toString(), DateTimeFormatter.ISO_DATE_TIME)
                            .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
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
        //TODO LOGGER???
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

    private fun isAdmin() = mDevicePolicyManager.isDeviceOwnerApp(packageName)

    private fun setKioskPolicies(enable: Boolean, isAdmin: Boolean) {
        if (isAdmin) {
            setRestrictions(enable)
            enableStayOnWhilePluggedIn(enable)
            setUpdatePolicy(enable)
            setAsHomeApp(enable)
            setKeyGuardEnabled(enable)
        }
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

    private fun createIntentSender(
        context: Context?,
        sessionId: Int,
        packageName: String?
    ): IntentSender {
        val intent = Intent("INSTALL_COMPLETE")
        if (packageName != null) {
            intent.putExtra("PACKAGE_NAME", packageName)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId,
            intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent.intentSender
    }

    private fun installApp() {
        if (!isAdmin()) {
            return
        }
        val raw = resources.openRawResource(R.raw.other_app)
        val packageInstaller: PackageInstaller = packageManager.packageInstaller
        val params =
            PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
        params.setAppPackageName("com.mrugas.smallapp")
        val sessionId = packageInstaller.createSession(params)
        val session = packageInstaller.openSession(sessionId)
        val out = session.openWrite("SmallApp", 0, -1)
        val buffer = ByteArray(65536)
        var c: Int
        while (raw.read(buffer).also { c = it } != -1) {
            out.write(buffer, 0, c)
        }
        session.fsync(out)
        out.close()
        createIntentSender(this, sessionId, packageName).let { intentSender ->
            session.commit(intentSender)
        }
        session.close()
    }
}