package com.card.terminal.fragments

import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.MainActivity
import com.card.terminal.R
import com.card.terminal.databinding.FragmentFirstBinding
import com.card.terminal.http.MyHttpClient
import com.card.terminal.utils.ContextProvider
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        var act = activity as MainActivity
        binding.tvDate.text =
            LocalDateTime.parse(act.getDateTime().toString(), DateTimeFormatter.ISO_DATE_TIME)
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        binding.tvClock.text =
            LocalDateTime.parse(act.getDateTime().toString(), DateTimeFormatter.ISO_DATE_TIME)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        Timber.d("FirstFragment onCreateView")
        act.cardScannerActive = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("FirstFragment onViewCreated")
        binding.firstAndLastName.text = arguments?.getString("name")

        val existingBundle = requireArguments()

        binding.ibWorkTrip.setOnClickListener {
            binding.ibWorkTrip.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "ibWorkTrip")
            goToCheckoutWithBundle(existingBundle)
        }

        binding.ibWorkTripLocal.setOnClickListener {
            binding.ibWorkTripLocal.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "ibWorkTripLocal")
//            throw Exception("gas")
            goToCheckoutWithBundle(existingBundle)

        }

        binding.ibWorkTripOther.setOnClickListener {
            binding.ibWorkTripOther.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "ibWorkTripOther")
            goToCheckoutWithBundle(existingBundle)
        }

        binding.ibPrivateWPermission.setOnClickListener {
            binding.ibPrivateWPermission.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "ibPrivateWPermission")
            goToCheckoutWithBundle(existingBundle)
        }

        binding.ibPrivateWoutPermission.setOnClickListener {
            binding.ibPrivateWoutPermission.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "ibPrivateWoutPermission")
            goToCheckoutWithBundle(existingBundle)
        }
    }

    fun goToCheckoutWithBundle(bundle: Bundle) {
        when (findNavController().currentDestination?.id) {
            R.id.FirstFragment -> {
                findNavController().navigate(
                    R.id.action_FirstFragment_to_CheckoutFragment,
                    bundle
                )
            }
        }
        MyHttpClient.pingy(bundle)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("FirstFragment onDestroyView")
        _binding = null
    }
}