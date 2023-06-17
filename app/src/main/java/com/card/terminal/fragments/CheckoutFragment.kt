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
        val prefs = ContextProvider.getApplicationContext()
            .getSharedPreferences("MyPrefsFile", AppCompatActivity.MODE_PRIVATE)

        binding.firstName.text = arguments?.getString("firstName")
        binding.lastName.text = arguments?.getString("lastName")

        if (arguments?.containsKey("companyName") == true) {
            binding.companyName.visibility = View.VISIBLE
            binding.companyName.text = arguments?.getString("companyName")
        }

        val existingBundle = requireArguments()
        MyHttpClient.openDoor(1)

        eventImageLogic(existingBundle)

        MyHttpClient.publishNewEvent(existingBundle)
        val delay = 10000L

        binding.reasonValue.text = arguments?.getString("selection", "")

        if (existingBundle.containsKey("imageB64")) {
            binding.photo.setImageBitmap(existingBundle.getParcelable("imageB64"))
        }

        if (prefs.contains("IFTTERM2_DESCR")) {
            binding.readoutValue.text =
                binding.readoutValue.text.toString() + ": " + prefs.getString("IFTTERM2_DESCR", "")
        }

        if (arguments?.getString("reasonValue").equals("Ulaz")) {
            binding.reasonKey.text = "Razlog ulaza: "
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

    private fun eventImageLogic(existingBundle: Bundle) {
        /*
        if (existingBundle.containsKey("Source") && existingBundle.getString("Source")
                .equals("Omnikey")
         */
        val prefs = ContextProvider.getApplicationContext()
            .getSharedPreferences("MyPrefsFile", AppCompatActivity.MODE_PRIVATE)

        if (prefs.getBoolean("CaptureOnEvent", true)) {
//            CameraUtils.captureImage(ContextProvider.getApplicationContext())
//            existingBundle.putString(
//                "EventImage", prefs.getString(
//                    "EventImage",
//                    ""
//                )
//            )

            existingBundle.putString(
                "EventImage", "")

            val editor = prefs.edit()
            editor.putString("EventImage", "")
            editor.commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("CheckoutFragment onDestroyView")
        _binding = null
    }
}