package com.card.terminal

import android.content.pm.PackageManager
import android.os.Bundle
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
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.room.Room
import com.card.terminal.cardUtils.OmniCard
import com.card.terminal.databinding.ActivityMainBinding
import com.card.terminal.db.AppDatabase
import com.card.terminal.http.MyHttpClient
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {
    private val REQUEST_BIND_BACKEND_SERVICE_PERMISSION = 9000
    private var cardService: ICardService? = null

    private var mutableCardCode = MutableLiveData<Map<String, String>>()
    private var mutableServerCode = MutableLiveData<Map<String, String>>()

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }
    }

    override fun onResume() {
        super.onResume()



        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "AppDatabase"
        ).build()

        /*
        thread {
            db.clearAllTables()
        }
         */

        if (PackageManagerQuery().isCardManagerAppInstalled(this)) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    PERMISSION_TO_BIND_BACKEND_SERVICE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                OmniCard.bindCardBackend(this, mutableCardCode, false)

                MyHttpClient.bindHttpClient(mutableServerCode)

                mutableCardCode.observe(this) {
                    val textic = findViewById<TextView>(R.id.textview_output)
                    textic.text = it.toString()


                    if(it["CardNumber"] != null) {

                        //provjeri jel taj cardNumber allowed


                    }

                    /*
                    thread {
                        val readInfoDao = db.ReadInfoDao()
                        readInfoDao.insertAll(ReadInfo(0, it["CardNumber"], LocalDateTime.now()))
                        val dataList = readInfoDao.getAll().toString()
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(this, dataList, Toast.LENGTH_LONG)
                                .show()
                        }

                    }
                     */

                    /*
                    lifecycleScope.launch() {
                        MyHttpClient.greeting(it)
                    }
                     */


                }

                mutableServerCode.observe(this) {
                    val textic = findViewById<TextView>(R.id.textview_output2)
                    textic.text = it.toString()
                }

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

        //MyHttpClient.bindHttpClient(mutableCardCode)

        /*
        thread {
            embeddedServer(Netty, port=5005) {
                routing {
                    get("/") {
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(this@MainActivity, "pingao te netko", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }.start(wait=true)
        }
         */


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