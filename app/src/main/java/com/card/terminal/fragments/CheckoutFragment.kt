package com.card.terminal.fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.R
import com.card.terminal.databinding.FragmentCheckoutBinding
import com.card.terminal.http.MyHttpClient
import com.card.terminal.utils.ContextProvider
import timber.log.Timber
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * An example full-screen fragment that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class CheckoutFragment : Fragment() {
    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        Timber.d("CheckoutFragment onCreateView")
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.firstName.text = arguments?.getString("firstName")
        binding.lastName.text = arguments?.getString("lastName")

        if (arguments?.containsKey("companyName") == true) {
            binding.companyName.visibility = View.VISIBLE
            binding.companyName.text = arguments?.getString("companyName")
        }

        binding.tvDateClock.text =
            LocalDateTime.parse(LocalDateTime.now().toString(), DateTimeFormatter.ISO_DATE_TIME)
                .format(
                    DateTimeFormatter.ofPattern(
                        "d.M.yyyy.",
                        Locale("hr")
                    )
                ) + LocalTime.parse(
                LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
            )

        val prefs = ContextProvider.getApplicationContext()
            .getSharedPreferences("MyPrefsFile", AppCompatActivity.MODE_PRIVATE)

        val existingBundle = requireArguments()
        MyHttpClient.openDoor(1)
        MyHttpClient.publishNewEvent(existingBundle)
        val delay = 10000L

        binding.reasonValue.text =
            ContextProvider.getApplicationContext()
                .getSharedPreferences("MyPrefsFile", AppCompatActivity.MODE_PRIVATE)
                .getString("reasonValue", "?")


        if (prefs.contains("IFTTERM2_DESCR")) {
            var v = "" //TODO
            if (existingBundle.getInt("eCode") == 2) {
                v = "Izlaz"
            } else {
                v = "Ulaz"
            }

            if (prefs.getString("IFTTERM2_DESCR", "") == "") {
                binding.readoutValue.text = v
            } else {
                binding.readoutValue.text = v + ": " + prefs.getString("IFTTERM2_DESCR", "")
            }
        }

        super.onViewCreated(view, savedInstanceState)
        Timber.d("CheckoutFragment onViewCreated")

        Handler().postDelayed({
            when (findNavController().currentDestination?.id) {
                R.id.CheckoutFragment -> {
                    findNavController().navigate(
                        R.id.action_CheckoutFragment_to_MainFragment
                    )
                }
            }
        }, delay)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("CheckoutFragment onDestroyView")
        _binding = null
    }
}