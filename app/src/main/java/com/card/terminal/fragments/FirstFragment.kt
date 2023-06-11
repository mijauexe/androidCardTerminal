package com.card.terminal.fragments

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.MainActivity
import com.card.terminal.R
import com.card.terminal.databinding.FragmentFirstBinding
import com.card.terminal.http.MyHttpClient
import com.card.terminal.utils.ContextProvider
import timber.log.Timber
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null
    private val binding get() = _binding!!

    private var button1Pressed = false
    private var button2Pressed = false
    private var button3Pressed = false
    private var buttonExitPressed = false
    private var buttonEnterPressed = false

    @SuppressLint("SetTextI18n")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        val act = activity as MainActivity


        _binding?.tvDateClock?.format24Hour = "d. MMMM yyyy., HH:mm"

        Timber.d("FirstFragment onCreateView")
        act.cardScannerActive = true
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("FirstFragment onViewCreated")

        val delay = 15000L

        val existingBundle = requireArguments()

        binding.tvDateClock.text =
            LocalDateTime.parse(LocalDateTime.now().toString(), DateTimeFormatter.ISO_DATE_TIME)
                .format(
                    DateTimeFormatter.ofPattern(
                        "d.M.yyyy.",
                        Locale("hr")
                    )
                ) + " " + LocalTime.parse(
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            )

        binding.cardNumber.text =
            binding.cardNumber.text.toString() + " " + existingBundle.getString("CardCode")
        binding.firstName.text = arguments?.getString("firstName")
        binding.lastName.text = arguments?.getString("lastName")

        binding.button1.setOnClickListener {
            binding.button1.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putInt("eCode", 2) //TODO
            existingBundle.putInt("eCode2", 3) //TODO
            existingBundle.putString("reasonValue", "Poslovno")
            existingBundle.putString("readoutValue", "Izlaz")
            goToCheckoutWithBundle(existingBundle)
        }

        binding.button2.setOnClickListener {
            binding.button2.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putInt("eCode", 2) //TODO
            existingBundle.putInt("eCode2", 4) //TODO
            existingBundle.putString("reasonValue", "Privatno")
            existingBundle.putString("readoutValue", "Izlaz")
            goToCheckoutWithBundle(existingBundle)
        }

        binding.button3.setOnClickListener {
            binding.button3.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putInt("eCode", 2) //TODO
            existingBundle.putInt("eCode2", 2) //TODO
            existingBundle.putString("reasonValue", "Pauza")
            existingBundle.putString("readoutValue", "Izlaz")
            goToCheckoutWithBundle(existingBundle)
        }

        binding.buttonEnter.setOnClickListener {
            binding.buttonEnter.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putInt("eCode", 1) //TODO
            existingBundle.putInt("eCode2", 1) //TODO
            existingBundle.putString("reasonValue", "Ulaz")
            existingBundle.putString("readoutValue", "Ulaz")
            goToCheckoutWithBundle(existingBundle)
        }

        binding.buttonExit.setOnClickListener {
            binding.buttonExit.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putInt("eCode", 2) //TODO
            existingBundle.putInt("eCode2", 0) //TODO nema pomocnog koda pa je 0
            existingBundle.putString("reasonValue", "Izlaz")
            existingBundle.putString("readoutValue", "Izlaz")
            goToCheckoutWithBundle(existingBundle)
        }

        Handler().postDelayed({
            when (findNavController().currentDestination?.id) {
                R.id.FirstFragment -> {
//                    existingBundle.putBoolean("NoOptionPressed", true)
//                    MyHttpClient.publishNewEvent(existingBundle)
                    findNavController().navigate(
                        R.id.action_FirstFragment_to_mainFragment
                    )
                }
            }
        }, delay)
    }

    fun handleButtonClickV1(existingBundle: Bundle) {
        if (buttonEnterPressed) {
            buttonExitPressed = false
            binding.buttonExit.setBackgroundColor(Color.TRANSPARENT)
            binding.buttonEnter.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("readoutValue", "Ulaz")
        }

        if (buttonExitPressed) {
            buttonEnterPressed = false
            binding.buttonEnter.setBackgroundColor(Color.TRANSPARENT)
            binding.buttonExit.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("readoutValue", "Izlaz")
        }

        if (button1Pressed) {
            button2Pressed = false
            binding.button2.setBackgroundColor(Color.TRANSPARENT)
            button3Pressed = false
            binding.button3.setBackgroundColor(Color.TRANSPARENT)
            binding.button1.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("reasonValue", "Poslovno")
        }

        if (button2Pressed) {
            button3Pressed = false
            binding.button3.setBackgroundColor(Color.TRANSPARENT)
            button1Pressed = false
            binding.button1.setBackgroundColor(Color.TRANSPARENT)
            binding.button2.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("reasonValue", "Privatno")
        }

        if (button3Pressed) {
            button2Pressed = false
            binding.button2.setBackgroundColor(Color.TRANSPARENT)
            button1Pressed = false
            binding.button1.setBackgroundColor(Color.TRANSPARENT)
            binding.button3.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("reasonValue", "Pauza")
        }

        if ((button1Pressed || button2Pressed || button3Pressed) && (buttonEnterPressed || buttonExitPressed)) {
            goToCheckoutWithBundle(existingBundle)
        }
    }

    fun goToCheckoutWithBundle(bundle: Bundle) {
        Handler().postDelayed({
            when (findNavController().currentDestination?.id) {
                R.id.FirstFragment -> {
                    findNavController().navigate(
                        R.id.action_FirstFragment_to_CheckoutFragment, bundle
                    )
//                    MyHttpClient.openDoor(1)
                }
            }
        }, 500)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("FirstFragment onDestroyView")
        _binding = null
    }
}