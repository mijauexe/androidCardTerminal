package com.card.terminal

import android.Manifest
import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.smartcardio.*
import android.smartcardio.hidglobal.Constants.PERMISSION_TO_BIND_BACKEND_SERVICE
import android.smartcardio.hidglobal.PackageManagerQuery
import android.smartcardio.ipc.CardService
import android.smartcardio.ipc.ICardService
import android.support.design.widget.Snackbar
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.card.terminal.R
import com.card.terminal.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private var cardService: ICardService? = null
    private var terminalFactory: TerminalFactory? = null
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var availableCardTerminals = listOf<CardTerminal>()
    private lateinit var binding: ActivityMainBinding
    private var isServiceConnected = false

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

        // if CardReaderManager is installed bind card service from CardReaderManager app
        if (PackageManagerQuery().isCardManagerAppInstalled(this)) {
// check if app has permission to bind card service
            if (ContextCompat.checkSelfPermission(this, PERMISSION_TO_BIND_BACKEND_SERVICE) ==
                PackageManager.PERMISSION_GRANTED
            ) {
// bind service if permission granted - see code below
                bindCardService()

            } else {
// request permission to bind service and expect result in onRequestPermissionsResult
                //ActivityCompat.requestPermissions(this, arrayOf(PERMISSION_TO_BIND_BACKEND_SERVICE), REQUEST_BIND_BACKEND_SERVICE_PERMISSION)
            }
        } else {
// show dialog that CardReaderManager app is not installed
            //showReaderAppNotInstalledDialog()
        }

    }

    private fun bindCardService() {
        if (!isServiceConnected) {
            isServiceConnected = true
            cardService = CardService.getInstance(this, object : ServiceConnection {
                override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
                    isServiceConnected = true
                    Timber.d("OmniKeyDemo connected to card service")
                    Timber.d("Reader connected")

                    availableCardTerminals = getAvailableCardTerminals()

                    val terminal = availableCardTerminals.get(0)
                    if (terminal.isCardPresent) {
                        val card = terminal.connect("*")
                        getCardAtr(card)
                        parseResp(requestApdu(card))
                        card.disconnect(true)
                        var isCardAbsent = false
                        while (!isCardAbsent) {
                            isCardAbsent = terminal.waitForCardAbsent(100L)
                            Thread.sleep(100L)
                        }
                    }
// Interpret response, do further work.
// ...
// At the end, release card connection.
                }

                override fun onServiceDisconnected(componentName: ComponentName) {
                    isServiceConnected = false
                    Timber.d("OmniKeyDemo disconnected to card service")
                }
            })
        } else {
            availableCardTerminals = getAvailableCardTerminals()
        }
    }

    //@Throws(Exception::class)
    private fun parseResp(resp: String) {
        if (resp.endsWith("9000")) {
            val bin = ConvertUtils.hexStringToAscii(resp.dropLast(4)).toLong(16).toString(2)
            if (bin.length == 26) {
                val pacs = ConvertUtils.convertBinaryToDecimal(bin.drop(9).dropLast(1).toLong())
                println(pacs)
            }
            TODO("34 i 37 bit")
        }
    }

    //@Throws(Exception::class)
    private fun getCardAtr(card: Card): String {
        var atr: String? = null
        val respATR = card.atr
        respATR?.let { atr = ConvertUtils.byteArrayToString(it.bytes) }
        return atr.orEmpty()
    }

    @Throws(Exception::class)
    private fun requestApdu(card: Card): String {
        val cmd = CommandAPDU(ConvertUtils.hexStringToByteArray("ff680d0000"))
        val bytes = card.basicChannel.transmit(cmd).bytes
        return bytes?.let { ConvertUtils.byteArrayToString(it) }.toString()
    }

    private fun getAvailableCardTerminals(): List<CardTerminal> {
        if (terminalFactory == null) {
            try {
                terminalFactory = cardService?.terminalFactory
            } catch (e: Exception) {
                Timber.e(e, "Unable to get terminal factory")
                return emptyList()
            }
        }

        try {
            terminalFactory?.terminals()?.list()?.let {
                return it.filterNotNull()
            }
        } catch (e: CardException) {
            Timber.e(e)
        }
        return emptyList()
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
        return navController.navigateUp(appBarConfiguration)
                || super.onSupportNavigateUp()
    }
}