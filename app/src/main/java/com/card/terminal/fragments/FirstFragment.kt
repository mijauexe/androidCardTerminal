package com.card.terminal.fragments

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.os.Handler
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
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

        if (existingBundle.getString("CardCode") != "10037") {
            val bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.img)
            binding.photo.setImageBitmap(bitmap)
        } else {
            val bitmap: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.img2)
//            binding.photo.scaleType = ImageView.ScaleType.FIT_CENTER
//            val layoutParams = binding.photo.layoutParams as LinearLayout.LayoutParams
//            layoutParams.gravity = Gravity.CENTER
//            binding.photo.layoutParams = layoutParams
            binding.photo.setImageBitmap(bitmap)
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

        Handler().postDelayed(
            {
                when (findNavController().currentDestination?.id) {
                    R.id.FirstFragment -> {
                        existingBundle.putBoolean("NoOptionPressed", true)
                        MyHttpClient.publishNewEvent(existingBundle)
                        findNavController().navigate(
                            R.id.action_FirstFragment_to_mainFragment
                        )
                    }
                }
            }, delay
        )
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
                    .textColor(Color.BLACK)
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
                    btn.setBackgroundColor(Color.parseColor("#faa61a"))
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