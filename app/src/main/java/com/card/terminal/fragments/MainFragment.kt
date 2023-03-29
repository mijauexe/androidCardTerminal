package com.card.terminal.fragments

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.MainActivity
import com.card.terminal.R
import com.card.terminal.databinding.FragmentMainBinding
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

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

    override fun onStart() {
        super.onStart()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        var trackerSettingsIcon = IntArray(3)

        Handler().postDelayed({
            trackerSettingsIcon = IntArray(3)
            binding.settingsButton.visibility = View.GONE
        }, 10000)

        var trackerKioskIcons = IntArray(3)

        Handler().postDelayed({
            trackerKioskIcons = IntArray(3)
            binding.setKioskPolicies.visibility = View.GONE
            binding.removeKioskPolicies.visibility = View.GONE
        }, 10000)

        binding.tvDate.setOnClickListener {
            trackerKioskIcons[0]++
        }

        binding.pleaseScanCardText.setOnClickListener {
            trackerKioskIcons = IntArray(3)
            trackerSettingsIcon = IntArray(3)
            binding.setKioskPolicies.visibility = View.GONE
            binding.removeKioskPolicies.visibility = View.GONE
            binding.settingsButton.visibility = View.GONE
        }

        binding.ifsimusLogo.setOnClickListener {
            trackerKioskIcons[2]++
            if (trackerKioskIcons[0] == 2 && trackerKioskIcons[1] == 3 && trackerKioskIcons[2] == 5) {
                if(binding.setKioskPolicies.visibility == View.VISIBLE) {
                    binding.setKioskPolicies.visibility = View.GONE
                    binding.removeKioskPolicies.visibility = View.GONE
                } else {
                    binding.setKioskPolicies.visibility = View.VISIBLE
                    binding.removeKioskPolicies.visibility = View.VISIBLE
                }
                trackerKioskIcons = IntArray(3)
            }
        }

        binding.ervHepLogo.setOnClickListener {
            trackerSettingsIcon[0]++
        }

        binding.tvClock.setOnClickListener {
            trackerSettingsIcon[1]++
        }

        binding.pleaseScanIcon.setOnClickListener {
            trackerSettingsIcon[2]++
            trackerKioskIcons[1]++
            if (trackerSettingsIcon[0] == 1 && trackerSettingsIcon[1] == 2 && trackerSettingsIcon[2] == 3) {
                if(binding.settingsButton.visibility == View.VISIBLE) {
                    binding.settingsButton.visibility = View.GONE
                } else {
                    binding.settingsButton.visibility = View.VISIBLE
                }
                trackerSettingsIcon = IntArray(3)
            }
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