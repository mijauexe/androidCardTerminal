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
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.GlideException
import com.card.terminal.BuildConfig
import com.card.terminal.MainActivity
import com.card.terminal.R
import com.card.terminal.databinding.FragmentFirstBinding
import com.card.terminal.db.AppDatabase
import com.card.terminal.http.MyHttpClient
import com.card.terminal.utils.Constants
import com.card.terminal.utils.ContextProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URL


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
//        Timber.d("FirstFragment onCreateView")
        act.cardScannerActive = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        Timber.d("FirstFragment onViewCreated")

        timerHandler?.removeCallbacksAndMessages(null)
        val bundle = requireArguments()

        if (bundle.containsKey("imagePath")) {
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

        if(arguments?.containsKey("classType") == true && arguments?.get("classType") != "") {
            binding.workerType.visibility = View.VISIBLE
            binding.workerType.text = Constants.classType[arguments?.getString("classType")]
        }

        if (arguments?.containsKey("companyName") == true && arguments?.get("companyName") != "") {
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

        if (btnList != null) {
            val btnFirst = btnList[0]
            val btnLast = btnList[btnList.size - 1]

            val mutableBtnList = mutableListOf<com.card.terminal.db.entity.Button>()
            mutableBtnList.addAll(btnList)
            mutableBtnList.remove(mutableBtnList[0])
            mutableBtnList.remove(mutableBtnList[mutableBtnList.size - 1])

            for (i in mutableBtnList.indices) {
                val btn = layout[i] as Button
                btn.text = mutableBtnList[i].title
                btn.visibility = View.VISIBLE

                val drawable =
                    TextDrawable.builder().beginConfig().width(70).height(70).withBorder(2)
                        .textColor(Color.WHITE).endConfig()
                        .buildRoundRect(mutableBtnList[i].label, Color.parseColor("#FAA61A"), 10)
                btn.setBackgroundResource(R.drawable.card_button_background_shadow)
                btn.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)
                btn.setOnClickListener {
                    bundle.putString("selection", mutableBtnList[i].title)

                    bundle.putInt("eCode", mutableBtnList[i].eCode)
                    bundle.putInt("eCode2", mutableBtnList[i].eCode2)

                    btn.setBackgroundResource(R.drawable.card_button_background_safe_all)
                    goToCheckoutWithBundle(bundle)
                }
            }

            binding.button1.text = btnFirst.title
            binding.button7.text = btnLast.title

            binding.button1.setOnClickListener {
                binding.button1.setBackgroundResource(R.drawable.card_button_background_safe_all)
                bundle.putInt("eCode", btnFirst.eCode)
                bundle.putInt("eCode2", btnFirst.eCode2)
                bundle.putString("selection", btnFirst.title)
                goToCheckoutWithBundle(bundle)
            }
            if(bundle["noButtonClickNeededRegime"] == false) {
                //hep2 specific when the button doesnt need to be clicked, show anyway, but...
                binding.button7.setOnClickListener {
                    binding.button7.setBackgroundResource(R.drawable.card_button_background_safe_all)
                    bundle.putInt("eCode", btnLast.eCode)
                    bundle.putInt("eCode2", btnLast.eCode2)
                    bundle.putString("selection", btnLast.title)
                    goToCheckoutWithBundle(bundle)
                }
            } else {
                binding.button7.isEnabled = false
                binding.button7.alpha = 0.5F
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

                val color = Color.parseColor("#FAA61A")

//                    if (btnList[i].title.lowercase().contains("ulaz")) {
//                    Color.parseColor("#0e9910")
//                } else if (btnList[i].title.lowercase().contains("izlaz")) {
//                    Color.parseColor("#b51212")
//                } else Color.parseColor("#FAA61A")

                val drawable =
                    TextDrawable.builder().beginConfig().width(70).height(70).withBorder(2)
                        .textColor(Color.WHITE).endConfig()
                        .buildRoundRect(btnList[i].label, color, 10)

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
//        Timber.d("FirstFragment onDestroyView")
        _binding = null
    }
}