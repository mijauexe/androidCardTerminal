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
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.firstAndLastName.text = arguments?.getString("name")
        binding.reasonValue.text = arguments?.getString("selection")

        Handler().postDelayed({
            findNavController().navigate(R.id.action_CheckoutFragment_to_MainFragment)
        }, 10000)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}