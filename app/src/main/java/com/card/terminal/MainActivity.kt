package com.card.terminal

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.*
import android.smartcardio.*
import android.smartcardio.hidglobal.Constants.PERMISSION_TO_BIND_BACKEND_SERVICE
import android.smartcardio.hidglobal.PackageManagerQuery
import android.smartcardio.ipc.CardService
import android.smartcardio.ipc.ICardService
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.card.terminal.ConvertUtils.hexStringToBinaryString
import com.card.terminal.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.NonCancellable.isCancelled
import timber.log.Timber
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity() {
    private val REQUEST_BIND_BACKEND_SERVICE_PERMISSION = 9000
    private val WILDCARD_PROTOCOL = "*"
    private var isServiceConnected = false
    private var cardService: ICardService? = null
    private var terminalFactory: TerminalFactory? = null
    private lateinit var appBarConfiguration: AppBarConfiguration
    private var availableCardTerminals = listOf<CardTerminal>()
    private lateinit var binding: ActivityMainBinding

    private var pollTerminalsTask: AsyncTask<CardTerminal, CardCommunicationProgress, Void>? = null
    private var cardResponseTask: AsyncTask<CardTerminal, CardCommunicationProgress, Void>? = null

    val USB_SLEEP_MILLIS = 200L
    val CARD_ABSENT_MILLIS = 100L
    val THREAD_SLEEP_MILLIS = 100L

    override fun onCreate(savedInstanceState: Bundle?) {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())

        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        val navController = findNavController(R.id.nav_host_fragment_content_main)
        appBarConfiguration = AppBarConfiguration(navController.graph)
        setupActionBarWithNavController(navController, appBarConfiguration)

        if (PackageManagerQuery().isCardManagerAppInstalled(this)) {
            if (ContextCompat.checkSelfPermission(this, PERMISSION_TO_BIND_BACKEND_SERVICE) ==
                PackageManager.PERMISSION_GRANTED
            ) {
                bindCardService()
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


        binding.fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show()
        }

        /*
        executor.execute {
            availableCardTerminals = getAvailableCardTerminals()
            var listaBrojeva = listOf<Int>()

            try {
                availableCardTerminals.forEach {
                    Timber.d("Waiting on terminal %s", it.name)
                    if (it.isCardPresent) {
                        Timber.d("Card present on terminal %s", it.name)
                        val card = it.connect("*")
                        listaBrojeva = parseResp(requestApdu(card))

                        card.disconnect(true)
                        var isCardAbsent = false
                        while (!isCancelled && !isCardAbsent) {
                            isCardAbsent = it.waitForCardAbsent(CARD_ABSENT_MILLIS)
                            Thread.sleep(THREAD_SLEEP_MILLIS)
                        }
                        Timber.d("Card can be removed from terminal %s", it.name)
                    }
                }
                Thread.sleep(USB_SLEEP_MILLIS)
            } catch (e: Exception) {
                Timber.e(e)
            }


            handler.post {
                Toast.makeText(this, listaBrojeva.toString(), Toast.LENGTH_SHORT)
                    .show()
            }
        }
         */
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
                   //terminal.
                    /* if (terminal.isCardPresent) {

                         val card = terminal.connect(WILDCARD_PROTOCOL)
                         getCardAtr(card)
                         val listaBrojeva = parseResp(requestApdu(card))
                         Toast.makeText(applicationContext, listaBrojeva.toString(), Toast.LENGTH_LONG).show()

                         card.disconnect(true)


                         var isCardAbsent = false
                         while (!isCardAbsent) {
                             isCardAbsent = terminal.waitForCardAbsent(100L)
                             Thread.sleep(100L)
                         }


                         pollTerminalsTask =
                             GetCardStatusTask(this@MainActivity).execute(*(availableCardTerminals.toTypedArray()))
                         //  cardResponseTask = GetApduResponseTask(this@MainActivity).execute(terminal)


                     }*/


                    //OVO RADI ali bez onpause, onstop i onresume
                     pollTerminalsTask =
                        GetCardStatusTask(this@MainActivity).execute(*(availableCardTerminals.toTypedArray()))


                }

                override fun onServiceDisconnected(componentName: ComponentName) {
                    isServiceConnected = false
                    Timber.d("No longer connected to card service")
                }
            })
        } else {
            availableCardTerminals = getAvailableCardTerminals()
        }
    }

    /*
    override fun onResume() {
        super.onResume()
        isServiceConnected = false
        if (PackageManagerQuery().isCardManagerAppInstalled(this)) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    PERMISSION_TO_BIND_BACKEND_SERVICE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                bindCardService()
            }
        } else {
            Toast.makeText(this, "card manager not installed", Toast.LENGTH_LONG).show()
        }
    }

    override fun onPause() {
        super.onPause()
        cardService?.releaseService()
        pollTerminalsTask?.cancel(true)
        cardResponseTask?.cancel(true)
    }

    public override fun onStop() {
        super.onStop()
        pollTerminalsTask?.cancel(true)
        cardResponseTask?.cancel(true)
    }
     */

    @Throws(Exception::class)
    private fun parseResp(response: String): List<Int> {
        if (response.endsWith("9000")) {
            //var listica = hexStringToHexArray(resp)
            //9Dxx 03 06 03      81 25 8c 47 d0        9000
            var resp = response

            resp = resp.dropLast(4)
            resp = resp.substring(resp.indexOf("03"))
            val tbRemovedNum = Integer.parseInt(resp.substring(4, 6))
            resp = resp.drop(6)

            resp = hexStringToBinaryString(resp)
            resp = resp.dropLast(tbRemovedNum)

            if (resp.length == 26) {
                resp = resp.dropLast(1)
                resp = resp.drop(1)
                val cardNumber =
                    Integer.parseInt((Integer.parseInt(resp, 2) and 0x00FFFF).toString(), 2)
                val facilityCode = Integer.parseInt(resp.substring(1, 8), 2)
                return listOf(cardNumber, facilityCode)
            } else if (resp.length == 34) {
                val cardNumber = Integer.parseInt(resp.substring(17, resp.length - 1), 2)
                val facilityCode = Integer.parseInt(resp.substring(0, 17), 2)
                return listOf(cardNumber, facilityCode)
            } else if (resp.length == 37) {
                resp = resp.dropLast(1)
                resp = resp.drop(1)
                val facilityCode = Integer.parseInt(resp.substring(0, 16), 2)
                val cardNumber = Integer.parseInt(resp.substring(17), 2)
                return listOf(cardNumber, facilityCode)
            }
        }

        //37bit
        //1(000 0001 0010 0101 1)(000 1100 0100 0111 1101) 0               000
        //587 - fac code
        //50301 - card number od 17.bit do 35.

        //34bit
        //card number od 17.bit do length-1.
        //fac 0-16

        //26 bit
        //makni prvi i zadnji bit
        //fac - 1. do 8.
        //logicki - cijeli string and sa 0x00FFFF

        return listOf()
    }

    @Throws(Exception::class)
    private fun getCardAtr(card: Card): String {
        var atr: String? = null
        val respATR = card.atr
        respATR?.let { atr = ConvertUtils.byteArrayToString(it.bytes) }
        return atr.orEmpty()
    }

    @Throws(Exception::class)
    private fun requestApdu(card: Card): String {
        val cmd = CommandAPDU(ConvertUtils.hexStringToByteArray("ff70076b07a005a10380010400"))
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