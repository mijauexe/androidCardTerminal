package com.card.terminal.fragments

import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.MainActivity
import com.card.terminal.R
import com.card.terminal.databinding.FragmentMainBinding
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
//    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("MainFragment onCreateView")
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        val act = activity as MainActivity
        if (act.getDateTime() != null) {
            _binding?.tvDate?.text =
                LocalDateTime.parse(act.getDateTime().toString(), DateTimeFormatter.ISO_DATE_TIME)
                    .format(DateTimeFormatter.ofPattern("d. MMMM yyyy.", Locale("hr")))
            _binding?.tvClock?.text =
                LocalDateTime.parse(act.getDateTime().toString(), DateTimeFormatter.ISO_DATE_TIME)
                    .format(DateTimeFormatter.ofPattern("HH:mm"))
        }
        act.cardScannerActive = false
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("MainFragment onViewCreated")

        if (_binding != null) {
            var trackerSettingsIcon = IntArray(3)

            Handler().postDelayed({
                trackerSettingsIcon = IntArray(3)
                _binding?.settingsButton?.visibility = View.GONE
            }, 10000)

            var trackerKioskIcons = IntArray(3)

            Handler().postDelayed({
                trackerKioskIcons = IntArray(3)
                _binding?.setKioskPolicies?.visibility = View.GONE
                _binding?.removeKioskPolicies?.visibility = View.GONE
            }, 10000)

            _binding?.tvDate?.setOnClickListener {
                trackerKioskIcons[0]++
            }

            _binding?.pleaseScanCardText?.setOnClickListener {
                trackerKioskIcons = IntArray(3)
                trackerSettingsIcon = IntArray(3)
                _binding?.setKioskPolicies?.visibility = View.GONE
                _binding?.removeKioskPolicies?.visibility = View.GONE
                _binding?.settingsButton?.visibility = View.GONE
            }

            _binding?.ifsimusLogo?.setOnClickListener {
                trackerKioskIcons[2]++
                if (trackerKioskIcons[0] == 2 && trackerKioskIcons[1] == 3 && trackerKioskIcons[2] == 5) {
                    if (_binding?.setKioskPolicies?.visibility == View.VISIBLE) {
                        _binding?.setKioskPolicies?.visibility = View.GONE
                        _binding?.removeKioskPolicies?.visibility = View.GONE
                    } else {
                        _binding?.setKioskPolicies?.visibility = View.VISIBLE
                        _binding?.removeKioskPolicies?.visibility = View.VISIBLE
                    }
                    trackerKioskIcons = IntArray(3)
                }
            }

            _binding?.ervHepLogo?.setOnClickListener {
                trackerSettingsIcon[0]++
            }

            _binding?.tvClock?.setOnClickListener {
                trackerSettingsIcon[1]++
            }

            _binding?.pleaseScanIcon?.setOnClickListener {
                trackerSettingsIcon[2]++
                trackerKioskIcons[1]++
                if (trackerSettingsIcon[0] == 1 && trackerSettingsIcon[1] == 2 && trackerSettingsIcon[2] == 3) {
                    if (_binding?.settingsButton?.visibility == View.VISIBLE) {
                        _binding?.settingsButton?.visibility = View.GONE
                    } else {
                        _binding?.settingsButton?.visibility = View.VISIBLE
                    }
                    trackerSettingsIcon = IntArray(3)
                }
            }

            _binding?.settingsButton?.setOnClickListener {
                findNavController().navigate(R.id.action_mainFragment_to_SettingsFragment)
                Timber.d("Msg: Settings menu opened")
            }

        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("MainFragment onDestroyView")
        _binding = null
    }
}