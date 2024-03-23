package com.card.terminal.fragments

import android.annotation.SuppressLint
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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.card.terminal.BuildConfig
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
import java.net.URL
import java.util.*


class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!
    private var timerHandler: Handler? = null
    private val delayToMain: Long = 15000L

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        val act = activity as MainActivity
        Timber.d("FirstFragment onCreateView")
        act.cardScannerActive = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("FirstFragment onViewCreated")

        timerHandler?.removeCallbacksAndMessages(null)
        val bundle = requireArguments()

        if (bundle.containsKey("imageB64")) {
            try {
                //for testing with local images
//                val decodedString: ByteArray = android.util.Base64.decode(
//                    bundle.getString("imageB64"), android.util.Base64.NO_WRAP
//                )
//                val decodedBitmap =
//                    BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
//                binding.photo.setImageBitmap(decodedBitmap)
//                bundle.putParcelable("imageB64", decodedBitmap)
                binding.photo.setImageBitmap(bundle.getParcelable("imageB64"))
            } catch (e: java.lang.Exception) {
                Timber.d(
                    "Msg: Exception %s | %s | %s", e.cause, e.stackTraceToString(), e.message
                )
            }
        } else if (bundle.containsKey("imagePath")) {
            val prefs = ContextProvider.getApplicationContext()
                .getSharedPreferences("MyPrefsFile", AppCompatActivity.MODE_PRIVATE)
            try {
                val url = URL(
                    ("http://" + prefs.getString(
                        "bareIP", "?"
                    ) + bundle.get("imagePath"))
                )
                Timber.d("url je $url")

                val scope = CoroutineScope(Dispatchers.IO)
                scope.launch {
                    try {
                        val bitmap =
                            getView()?.let {
                                Glide.with(it).asBitmap().load(url.toString()).submit().get()
                            }
                        withContext(Dispatchers.Main) {
                            binding.photo.setImageBitmap(bitmap)
                            bundle.putParcelable("imageB64", bitmap)
                        }
                    } catch (e: GlideException) {
                        Timber.d(
                            "Msg: Exception while getting image in first fragment: %s | %s | %s",
                            e.cause,
                            e.stackTraceToString(),
                            e.message
                        )
                    } catch (e: java.lang.Exception) {
                        Timber.d(
                            "Msg: Exception while getting image in first fragment: %s | %s | %s",
                            e.cause,
                            e.stackTraceToString(),
                            e.message
                        )
                    } catch (e: Exception) {
                        Timber.d(
                            "Msg: Exception while getting image in first fragment: %s | %s | %s",
                            e.cause,
                            e.stackTraceToString(),
                            e.message
                        )
                    }
                }
            } catch (e: java.lang.Exception) {
                Timber.d(
                    "Msg: Exception getting photo: %s | %s | %s",
                    e.cause,
                    e.stackTraceToString(),
                    e.message
                )
            }
        } else {
            Timber.d(
                "No picture for " + bundle.getString("userId") + " " + bundle.getString("classType")
            )
        }

        binding.firstName.text = arguments?.getString("firstName")
        binding.lastName.text = arguments?.getString("lastName")

        if (arguments?.containsKey("companyName") == true) {
            binding.companyName.visibility = View.VISIBLE
            binding.companyName.text = arguments?.getString("companyName")
        }

        val ct = bundle.getString("classType")

        if (BuildConfig.FLAVOR == "HZJZ" || BuildConfig.FLAVOR == "JANAF") {
            binding.button1.visibility = View.VISIBLE
            binding.button2.visibility = View.VISIBLE
            binding.button3.visibility = View.VISIBLE
            binding.button4.visibility = View.VISIBLE
            binding.button7.visibility = View.VISIBLE

            binding.button1.setBackgroundResource(R.drawable.card_button_background_shadow)
            binding.button2.setBackgroundResource(R.drawable.card_button_background_shadow)
            binding.button3.setBackgroundResource(R.drawable.card_button_background_shadow)
            binding.button4.setBackgroundResource(R.drawable.card_button_background_shadow)
            binding.button7.setBackgroundResource(R.drawable.card_button_background_shadow)

            binding.button1.setOnClickListener {
                binding.button1.setBackgroundResource(R.drawable.card_button_background_fill)
                bundle.putInt("eCode", 1)
                bundle.putInt("eCode2", 10)
                bundle.putString("reasonValue", "Ulaz")
                bundle.putString("selection", "Ulaz")
                goToCheckoutWithBundle(bundle)
            }
            binding.button2.setOnClickListener {
                binding.button2.setBackgroundResource(R.drawable.card_button_background_fill)
                bundle.putInt("eCode", 2)
                bundle.putInt("eCode2", 11)
                bundle.putString("reasonValue", "Poslovno")
                bundle.putString("selection", "Izlaz")
                goToCheckoutWithBundle(bundle)
            }
            binding.button3.setOnClickListener {
                binding.button3.setBackgroundResource(R.drawable.card_button_background_fill)
                bundle.putInt("eCode", 2)
                bundle.putInt("eCode2", 12)
                bundle.putString("reasonValue", "Privatno")
                bundle.putString("selection", "Privatno")
                goToCheckoutWithBundle(bundle)
            }
            binding.button4.setOnClickListener {
                binding.button4.setBackgroundResource(R.drawable.card_button_background_fill)
                bundle.putInt("eCode", 2)
                bundle.putInt("eCode2", 13)
                bundle.putString("reasonValue", "Pauza")
                bundle.putString("selection", "Pauza")
                goToCheckoutWithBundle(bundle)
            }
            binding.button7.setOnClickListener {
                binding.button7.setBackgroundResource(R.drawable.card_button_background_fill)
                bundle.putInt("eCode", 2)
                bundle.putInt("eCode2", 14)
                bundle.putString("reasonValue", "Izlaz")
                bundle.putString("selection", "Izlaz")
                goToCheckoutWithBundle(bundle)
            }
        } else if (BuildConfig.FLAVOR == "HEP") {
            drawButtons(ct!!, binding.buttonsGrid, bundle)
        } else if (BuildConfig.FLAVOR == "HEP2") {
            drawButtons2(ct!!, bundle)
        } else if (BuildConfig.FLAVOR == "INA") {

        } else if (BuildConfig.FLAVOR == "DUKAT") {
            binding.button1.visibility = View.VISIBLE
            binding.button2.visibility = View.VISIBLE
            binding.button3.visibility = View.VISIBLE
            binding.button4.visibility = View.VISIBLE
            binding.button7.visibility = View.VISIBLE

            binding.button1.setBackgroundResource(R.drawable.card_button_background_shadow)
            binding.button2.setBackgroundResource(R.drawable.card_button_background_shadow)
            binding.button3.setBackgroundResource(R.drawable.card_button_background_shadow)
            binding.button4.setBackgroundResource(R.drawable.card_button_background_shadow)
            binding.button7.setBackgroundResource(R.drawable.card_button_background_shadow)

            binding.button1.setOnClickListener {
                binding.button1.setBackgroundResource(R.drawable.card_button_background_fill)
                bundle.putInt("eCode", 1)
                bundle.putInt("eCode2", 10)
                bundle.putString("reasonValue", "Ulaz")
                bundle.putString("selection", "Ulaz")
                goToCheckoutWithBundle(bundle)
            }
            binding.button2.setOnClickListener {
                binding.button2.setBackgroundResource(R.drawable.card_button_background_fill)
                bundle.putInt("eCode", 2)
                bundle.putInt("eCode2", 11)
                bundle.putString("reasonValue", "Poslovno")
                bundle.putString("selection", "Izlaz")
                goToCheckoutWithBundle(bundle)
            }
            binding.button3.setOnClickListener {
                binding.button3.setBackgroundResource(R.drawable.card_button_background_fill)
                bundle.putInt("eCode", 2)
                bundle.putInt("eCode2", 12)
                bundle.putString("reasonValue", "Privatno")
                bundle.putString("selection", "Privatno")
                goToCheckoutWithBundle(bundle)
            }
            binding.button4.setOnClickListener {
                binding.button4.setBackgroundResource(R.drawable.card_button_background_fill)
                bundle.putInt("eCode", 2)
                bundle.putInt("eCode2", 13)
                bundle.putString("reasonValue", "Pauza")
                bundle.putString("selection", "Pauza")
                goToCheckoutWithBundle(bundle)
            }

            binding.button7.setOnClickListener {
                binding.button7.setBackgroundResource(R.drawable.card_button_background_fill)
                bundle.putInt("eCode", 2)
                bundle.putInt("eCode2", 14)
                bundle.putString("reasonValue", "Izlaz")
                bundle.putString("selection", "Izlaz")
                goToCheckoutWithBundle(bundle)
            }
        }

        timerHandler?.removeCallbacksAndMessages(null) // Reset the timer
        timerHandler = Handler()
        timerHandler?.postDelayed({
            when (findNavController().currentDestination?.id) {
                R.id.FirstFragment -> {
                    bundle.putBoolean("NoOptionPressed", true)
                    MyHttpClient.publishNewEvent(bundle)
                    findNavController().navigate(
                        R.id.action_FirstFragment_to_mainFragment
                    )
                }
            }
        }, delayToMain)
    }

    private fun drawButtons2(
        str: String, bundle: Bundle
    ) {
        val db = AppDatabase.getInstance(
            ContextProvider.getApplicationContext(), Thread.currentThread().stackTrace
        )
        val btnList = db.ButtonDao().getAllByClassType(str)
        val layout = binding.buttonsGrid

        if (btnList != null && btnList.size >= 7) {
            for (i in 1 until btnList.size - 1) {
                val btn = layout[i - 1] as Button
                btn.setText(btnList[i].title)
                btn.visibility = View.VISIBLE

                val drawable =
                    TextDrawable.builder().beginConfig().width(70).height(70).withBorder(2)
                        .textColor(Color.WHITE).endConfig()
                        .buildRoundRect(btnList[i].label, Color.parseColor("#FAA61A"), 10)

                btn.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
                btn.setOnClickListener {
                    bundle.putString("selection", btnList[i].title)

                    bundle.putInt("eCode", btnList[i].eCode)
                    bundle.putInt("eCode2", btnList[i].eCode2)

                    btn.setBackgroundResource(R.drawable.card_button_background_safe_all)
                    goToCheckoutWithBundle(bundle)
                }
            }

            binding.button1.text = btnList[0].title
            binding.button7.text = btnList[6].title

            binding.button1.setOnClickListener {
                binding.button1.setBackgroundResource(R.drawable.card_button_background_safe_all)
                bundle.putInt("eCode", btnList[0].eCode)
                bundle.putInt("eCode2", btnList[0].eCode2)
                bundle.putString("selection", btnList[0].title)
                goToCheckoutWithBundle(bundle)
            }
            binding.button7.setOnClickListener {
                binding.button7.setBackgroundResource(R.drawable.card_button_background_safe_all)
                bundle.putInt("eCode", btnList[6].eCode)
                bundle.putInt("eCode2", btnList[6].eCode2)
                bundle.putString("selection", btnList[6].title)
                goToCheckoutWithBundle(bundle)
            }
        }
    }

    private fun goToCheckoutWithBundle(bundle: Bundle) {
        Handler().postDelayed({
            when (findNavController().currentDestination?.id) {
                R.id.FirstFragment -> {
                    findNavController().navigate(
                        R.id.action_FirstFragment_to_CheckoutFragment, bundle
                    )
                }
            }
        }, 500)
    }

    private fun drawButtons(
        str: String, layout: GridLayout, bundle: Bundle
    ) {
        val db = AppDatabase.getInstance(
            ContextProvider.getApplicationContext(), Thread.currentThread().stackTrace
        )
        val btnList = db.ButtonDao().getAllByClassType(str)

        if (btnList != null) {
            for (i in btnList.indices) {
                val btn = layout[i] as Button
                btn.setText("   " + btnList[i].title)
                btn.visibility = View.VISIBLE

                val drawable =
                    TextDrawable.builder().beginConfig().width(70).height(70).withBorder(2)
                        .textColor(Color.WHITE).endConfig()
                        .buildRoundRect(btnList[i].label, Color.parseColor("#FAA61A"), 10)

                btn.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
                btn.setOnClickListener {
                    bundle.putString("selection", btnList[i].title)

                    if (bundle.getBoolean("noButtonClickNeededRegime")) {
                        bundle.putInt("eCode2", 0)
                        bundle.putInt("eCode", 0)
                    } else {
                        bundle.putInt("eCode", btnList[i].eCode)
                        bundle.putInt("eCode2", btnList[i].eCode2)
                    }

                    btn.setBackgroundResource(R.drawable.card_button_background_safe)
//                    btn.setBackgroundColor(Color.parseColor("#faa61a"))
                    goToCheckoutWithBundle(bundle)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerHandler?.removeCallbacksAndMessages(null) // Reset the timer
        Timber.d("FirstFragment onDestroyView")
        _binding = null
    }
}