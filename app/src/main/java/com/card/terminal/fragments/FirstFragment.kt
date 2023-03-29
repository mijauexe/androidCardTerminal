package com.card.terminal.fragments

import android.app.ProgressDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.MainActivity
import com.card.terminal.R
import com.card.terminal.databinding.FragmentFirstBinding
import com.card.terminal.http.MyHttpClient
import com.card.terminal.utils.ContextProvider
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

        act.cardScannerActive = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.firstAndLastName.text = arguments?.getString("name")

        val existingBundle = requireArguments()
        print(existingBundle)

        binding.ibWork.setOnClickListener {
            binding.ibWork.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "Poslovno")
            findNavController().navigate(R.id.action_FirstFragment_to_CheckoutFragment, existingBundle)
        }

        binding.ibPrivate.setOnClickListener {
            binding.ibPrivate.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "Privatno")
            findNavController().navigate(R.id.action_FirstFragment_to_CheckoutFragment, existingBundle)
        }

        binding.ibCoffee.setOnClickListener {
            binding.ibCoffee.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "Pauza")
            findNavController().navigate(R.id.action_FirstFragment_to_CheckoutFragment, existingBundle)
            MyHttpClient.pingy(existingBundle)
        }

        binding.ibDoctor.setOnClickListener {
            binding.ibDoctor.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "Liječnik")
            findNavController().navigate(R.id.action_FirstFragment_to_CheckoutFragment, existingBundle)
        }

        binding.ibExtra.setOnClickListener {
            binding.ibExtra.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "BE-TO")
            findNavController().navigate(R.id.action_FirstFragment_to_CheckoutFragment, existingBundle)
        }

//        binding.buttonSecond.setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_mainFragment)
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}