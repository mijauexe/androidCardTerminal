package com.card.terminal.fragments

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.R
import com.card.terminal.databinding.FragmentCheckoutBinding
import timber.log.Timber
import java.time.LocalDateTime
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.firstAndLastName.text = arguments?.getString("name")
        binding.reasonValue.text = arguments?.getString("selection")

        val existingBundle = requireArguments()

        if(existingBundle.containsKey("imageB64")) {
            binding.photo.setImageBitmap(existingBundle.getParcelable("imageB64"))
        }

        val dt = LocalDateTime.parse(arguments?.getString("time"), DateTimeFormatter.ISO_DATE_TIME)
            .format(DateTimeFormatter.ofPattern("d. MMMM yyyy. HH:mm:ss", Locale("hr")))

        binding.readoutValue.text = binding.readoutValue.text.toString() + dt

        super.onViewCreated(view, savedInstanceState)
        Timber.d("CheckoutFragment onViewCreated")



//        binding.smile.setOnClickListener {
//            findNavController().navigate(R.id.action_CheckoutFragment_to_MainFragment)
//        }

        Handler().postDelayed({
            when (findNavController().currentDestination?.id) {
                R.id.CheckoutFragment -> {
                    findNavController().navigate(
                        R.id.action_CheckoutFragment_to_MainFragment
                    )
                }
            }
        }, 5000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("CheckoutFragment onDestroyView")
        _binding = null
    }
}