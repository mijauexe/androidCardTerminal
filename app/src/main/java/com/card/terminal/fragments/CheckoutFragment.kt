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
import com.card.terminal.BuildConfig
import com.card.terminal.R
import com.card.terminal.databinding.FragmentCheckoutBinding
import com.card.terminal.http.MyHttpClient
import com.card.terminal.utils.ContextProvider
import timber.log.Timber
import java.util.*


class CheckoutFragment : Fragment() {
    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private var timerHandler: Handler? = null
    private val delayToMain: Long = 6000L

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        Timber.d("CheckoutFragment onCreateView")
        return binding.root
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("CheckoutFragment onViewCreated")

        setupUI()

        val existingBundle = requireArguments()

        if (BuildConfig.Larus) {
            MyHttpClient.openDoorLarus(1)
        } else if (BuildConfig.Adam) {
            MyHttpClient.openDoorAdam()
        }

//        Utils.updateEvent(existingBundle)
        MyHttpClient.publishNewEvent(existingBundle)

        timerHandler?.removeCallbacksAndMessages(null) // Reset the timer
        timerHandler = Handler()
        timerHandler?.postDelayed({
            when (findNavController().currentDestination?.id) {
                R.id.CheckoutFragment -> {
                    findNavController().navigate(
                        R.id.action_CheckoutFragment_to_MainFragment
                    )
                }
            }
        }, delayToMain)

        setupUiText(existingBundle)
    }

    private fun setupUiText(existingBundle: Bundle) {
        val prefs = ContextProvider.getApplicationContext()
            .getSharedPreferences("MyPrefsFile", AppCompatActivity.MODE_PRIVATE)

        binding.firstName.text = arguments?.getString("firstName")
        binding.lastName.text = arguments?.getString("lastName")

        if (arguments?.containsKey("companyName") == true) {
            binding.companyName.visibility = View.VISIBLE
            binding.companyName.text = arguments?.getString("companyName")
        }


        if (existingBundle.containsKey("imageB64")) {
            binding.photo.setImageBitmap(existingBundle.getParcelable("imageB64"))
        }

        binding.selection.text = arguments?.getString("selection", "")
    }

    private fun setupUI() {
        //this is needed if a connected device is a physical keyboard, it would type text inside views and disrupt app flow
        val readoutValueTextView = _binding?.readoutKey
        readoutValueTextView?.isFocusable = false
        readoutValueTextView?.isFocusableInTouchMode = false

        val dateClockView = _binding?.tvDateClock
        dateClockView?.isFocusable = false
        dateClockView?.isFocusableInTouchMode = false

        val reasonKeyView = _binding?.reasonKey
        reasonKeyView?.isFocusable = false
        reasonKeyView?.isFocusableInTouchMode = false

        val selectionView = _binding?.selection
        selectionView?.isFocusable = false
        selectionView?.isFocusableInTouchMode = false

        val nothingView = _binding?.nothing
        nothingView?.isFocusable = false
        nothingView?.isFocusableInTouchMode = false

        val nothing1View = _binding?.nothing1
        nothing1View?.isFocusable = false
        nothing1View?.isFocusableInTouchMode = false

        val smile = _binding?.smile
        smile?.isFocusable = false
        smile?.isFocusableInTouchMode = false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        timerHandler?.removeCallbacksAndMessages(null) // Reset the timer
        Timber.d("CheckoutFragment onDestroyView")
        _binding = null
    }
}