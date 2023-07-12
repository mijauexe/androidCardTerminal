package com.card.terminal.fragments

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.amulyakhare.textdrawable.TextDrawable
import com.card.terminal.MainActivity
import com.card.terminal.R
import com.card.terminal.databinding.FragmentFirstBinding
import com.card.terminal.db.AppDatabase
import com.card.terminal.http.MyHttpClient
import com.card.terminal.utils.ContextProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.NoRouteToHostException
import java.net.URL
import java.net.UnknownHostException
import java.util.*


class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        val act = activity as MainActivity

        Timber.d("FirstFragment onCreateView")
        act.cardScannerActive = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("FirstFragment onViewCreated")

        val delay = 15000L

        val existingBundle = requireArguments()

        val prefs = ContextProvider.getApplicationContext()
            .getSharedPreferences("MyPrefsFile", AppCompatActivity.MODE_PRIVATE)

        if (existingBundle.containsKey("imageB64")) {
            try {
                val decodedString: ByteArray =
                    android.util.Base64.decode(
                        existingBundle.getString("imageB64"),
                        android.util.Base64.NO_WRAP
                    )
                val decodedBitmap =
                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                binding.photo.setImageBitmap(decodedBitmap)
            } catch (e: java.lang.Exception) {
                Timber.d(
                    "Msg: Exception %s | %s | %s",
                    e.cause,
                    e.stackTraceToString(),
                    e.message
                )
            }
        } else if (existingBundle.containsKey("imagePath")) {
            try {
                val scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
                    try {
                        val url = URL(
                            ("http://" + prefs.getString(
                                "bareIP",
                                "?"
                            ) + existingBundle.get("imagePath"))
                        )
                        Timber.d("url je ${url}")
                        val connection = withContext(Dispatchers.IO) {
                            url.openConnection()
                        } as HttpURLConnection
                        connection.doInput = true
                        withContext(Dispatchers.IO) {
                            connection.connect()
                        }
                        val input = connection.inputStream
                        val bitmap = BitmapFactory.decodeStream(input)
                        withContext(Dispatchers.Main) {
                            binding.photo.setImageBitmap(bitmap)
                            existingBundle.putParcelable("imageB64", bitmap)
                        }
                        connection.disconnect()
                    } catch (e: java.lang.Exception) {
                        Timber.d(
                            "Msg: Exception %s | %s | %s",
                            e.cause,
                            e.stackTraceToString(),
                            e.message
                        )
                    }
                }
            } catch (e: NoRouteToHostException) {
                Timber.d(
                    "Msg: No route to host while getting photo: %s | %s | %s",
                    e.cause,
                    e.stackTraceToString(),
                    e.message
                )
            } catch (e: UnknownHostException) {
                Timber.d(
                    "Msg: Unknown host while getting photo: %s | %s | %s",
                    e.cause,
                    e.stackTraceToString(),
                    e.message
                )
            }
        }

        binding.firstName.text = arguments?.getString("firstName")
        binding.lastName.text = arguments?.getString("lastName")

        if (arguments?.containsKey("companyName") == true) {
            binding.companyName.visibility = View.VISIBLE
            binding.companyName.text = arguments?.getString("companyName")
        }

        val ct = existingBundle.getString("classType")

        if (ct.equals("WORKER")) {
            ubijMe("WORKER", binding.buttonsGrid, existingBundle)
        } else if (ct.equals("CONTRACTOR")) {
            ubijMe("CONTRACTOR", binding.buttonsGrid, existingBundle)
        } else if (ct.equals("GUEST")) {
            ubijMe("GUEST", binding.buttonsGrid, existingBundle)
        } else if (ct.equals("VEHICLE")) {
            ubijMe("VEHICLE", binding.buttonsGrid, existingBundle)
        }

        Handler().postDelayed({
            when (findNavController().currentDestination?.id) {
                R.id.FirstFragment -> {
                    existingBundle.putBoolean("NoOptionPressed", true)
                    MyHttpClient.publishNewEvent(existingBundle)
                    findNavController().navigate(
                        R.id.action_FirstFragment_to_mainFragment
                    )
                }
            }
        }, delay)
    }

    fun goToCheckoutWithBundle(bundle: Bundle) {
        Handler().postDelayed({
            when (findNavController().currentDestination?.id) {
                R.id.FirstFragment -> {
                    findNavController().navigate(
                        R.id.action_FirstFragment_to_CheckoutFragment, bundle
                    )
                    MyHttpClient.openDoor(1)
                }
            }
        }, 500)
    }

    fun ubijMe(
        str: String,
        layout: GridLayout,
        bundle: Bundle
    ) {

        val db = AppDatabase.getInstance(
            ContextProvider.getApplicationContext(),
            Thread.currentThread().stackTrace
        )
        val btnList = db.ButtonDao().getAllByClassType(str)

        if (btnList != null) {
            for (i in btnList.indices) {
                val btn = layout[i] as Button
                bundle.putInt(btnList[i].title, btnList[i].eCode2)
                btn.setText("   " + btnList[i].title)
                btn.visibility = View.VISIBLE

                val drawable = TextDrawable.builder()
                    .beginConfig()
                    .width(70).height(70)
                    .withBorder(2)
                    .textColor(Color.WHITE)
                    .endConfig()
                    .buildRoundRect(btnList[i].label, Color.parseColor("#FAA61A"), 10)

                btn.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
                btn.setOnClickListener {
                    bundle.putString("selection", btnList[i].title)
                    if (bundle.getBoolean("noButtonClickNeededRegime")) {
                        bundle.putInt("eCode2", 0)
                    } else {
                        bundle.putInt("eCode2", btnList[i].eCode2)
                    }

                    btn.setBackgroundResource(R.drawable.card_button_background)
//                    btn.setBackgroundColor(Color.parseColor("#faa61a"))
                    goToCheckoutWithBundle(bundle.deepCopy())
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("FirstFragment onDestroyView")
        _binding = null
    }
}