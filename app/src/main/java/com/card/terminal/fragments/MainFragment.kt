package com.card.terminal.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.MainActivity
import com.card.terminal.R
import com.card.terminal.databinding.FragmentMainBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [MainFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        val act = activity as MainActivity
        if (act.getDateTime() != null) {
            binding.tvDate.text =
                LocalDateTime.parse(act.getDateTime().toString(), DateTimeFormatter.ISO_DATE_TIME)
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
            binding.tvClock.text =
                LocalDateTime.parse(act.getDateTime().toString(), DateTimeFormatter.ISO_DATE_TIME)
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
        }
        act.cardScannerActive = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cardButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_FirstFragment)
        }

        binding.pinButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_SecondFragment)
        }

        binding.settingsButton.setOnClickListener {
            findNavController().navigate(R.id.action_mainFragment_to_SettingsFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}