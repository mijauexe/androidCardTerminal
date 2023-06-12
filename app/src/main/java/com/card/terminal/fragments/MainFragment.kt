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
import com.card.terminal.http.MyHttpClient
import timber.log.Timber

class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("MainFragment onCreateView")
        _binding = FragmentMainBinding.inflate(inflater, container, false)
        val act = activity as MainActivity
        act.cardScannerActive = false
        return _binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("MainFragment onViewCreated")

        if (_binding != null) {
            var trackerSettingsIcon = IntArray(3)

            _binding?.pleaseScanCardText?.setOnClickListener {
                trackerSettingsIcon = IntArray(3)
                _binding?.setKioskPolicies?.visibility = View.GONE
                _binding?.removeKioskPolicies?.visibility = View.GONE
                _binding?.settingsButton?.visibility = View.GONE
            }

            _binding?.ervHepLogo?.setOnClickListener {
                trackerSettingsIcon[0]++
            }

            _binding?.tvClock?.setOnClickListener {
                trackerSettingsIcon[1]++
            }

            _binding?.pleaseScanIcon?.setOnClickListener {
                trackerSettingsIcon[2]++
                if (trackerSettingsIcon[0] == 1 && trackerSettingsIcon[1] == 2 && trackerSettingsIcon[2] == 3) {
                    if (_binding?.settingsButton?.visibility == View.VISIBLE) {
                        _binding?.settingsButton?.visibility = View.GONE
                    } else {
                        _binding?.settingsButton?.visibility = View.VISIBLE
                        Handler().postDelayed({
                            trackerSettingsIcon = IntArray(3)
                            _binding?.settingsButton?.visibility = View.GONE
                        }, 5000)
                    }
                    trackerSettingsIcon = IntArray(3)
                }
            }

            _binding?.settingsButton?.setOnClickListener {
                findNavController().navigate(R.id.action_mainFragment_to_SettingsFragment)
                Timber.d("Msg: Settings menu opened")
            }

            _binding?.ifsimusLogo?.setOnClickListener {
                MyHttpClient.reset()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("MainFragment onDestroyView")
        _binding = null
    }
}