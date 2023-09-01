package com.card.terminal.fragments

import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.MediaController
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.BuildConfig
import com.card.terminal.MainActivity
import com.card.terminal.databinding.FragmentMainBinding
import com.card.terminal.utils.adamUtils.Adam6050D
import com.card.terminal.utils.adamUtils.DigitalOutput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber


class MainFragment : Fragment() {
    private var _binding: FragmentMainBinding? = null
    private var promoHandler: Handler? = null
    private val promoDelayMillis: Long = 5000
    private var currentVideoIndex = 0

    private val videoUriList: List<Uri> = listOf(
        Uri.parse("android.resource://" + "com.card.terminal.ina" + "/raw/ina_promo"),
        Uri.parse("android.resource://" + "com.card.terminal.ina" + "/raw/ina_promo_2"),
        Uri.parse("android.resource://" + "com.card.terminal.ina" + "/raw/ina_promo_3")
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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


        val clock = _binding?.tvClock
        clock?.isFocusable = false
        clock?.isFocusableInTouchMode = false

        val scanText = _binding?.pleaseScanCardText
        scanText?.isFocusable = false
        scanText?.isFocusableInTouchMode = false

        val scanIcon = _binding?.pleaseScanIcon
        scanIcon?.isFocusable = false
        scanIcon?.isFocusableInTouchMode = false

        val mainLogo2 = _binding?.mainLogo2
        mainLogo2?.isFocusable = false
        mainLogo2?.isFocusableInTouchMode = false

        if (_binding != null) {
            var trackerSettingsIcon = IntArray(3)

            _binding?.pleaseScanCardText?.setOnClickListener {
                trackerSettingsIcon = IntArray(3)
                _binding?.setKioskPolicies?.visibility = View.GONE
                _binding?.removeKioskPolicies?.visibility = View.GONE
                _binding?.settingsButton?.visibility = View.GONE
            }

            _binding?.mainLogo2?.setOnClickListener {
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

            val btn = _binding?.mainLogo2
            btn?.setOnClickListener {
                //make a basic demo switch
                val scope3 = CoroutineScope(Dispatchers.IO)
                scope3.launch {
                    val ip = "192.168.0.105"
                    val username = "root"
                    val password = "00000000"

                    val adam = Adam6050D(ip, username, password)
                    val doOutput = DigitalOutput()

                    try {
                        val out = adam.output()

                        val stateOfD0 = (out as DigitalOutput).asDict().get("DO1")
                        if(stateOfD0 == 1) {
                            doOutput[1] = 0
                        } else {
                            doOutput[1] = 1
                        }
                        adam.output(doOutput)
                    } catch (e: Exception) {
                        println(e)
                    }

//                    for (i in 0..50) {
//                        gas += 1
//                        doOutput[1] = gas % 2
//                        try {
//                            adam.output(doOutput)
//                        } catch (e: Exception) {
//                            println(e)
//                        }
//                    }
                }
            }

            _binding?.settingsButton?.setOnClickListener {
                findNavController().navigate(com.card.terminal.R.id.action_mainFragment_to_SettingsFragment)
                Timber.d("Msg: Settings menu opened")
            }

            _binding?.promoView?.setOnClickListener {
                deactivatePromo()
            }
            activatePromo()
        }
    }

    fun deactivatePromo() {
        if (BuildConfig.FLAVOR == "INA" && videoUriList.size != 0) {
            _binding?.promoView?.visibility = View.GONE
            _binding?.mainLogo1?.visibility = View.VISIBLE
            _binding?.mainLogo3?.visibility = View.VISIBLE
            _binding?.pleaseScanIcon?.visibility = View.VISIBLE
            _binding?.pleaseScanCardText?.visibility = View.VISIBLE
            _binding?.mainLogo2?.visibility = View.VISIBLE
            _binding?.gridLayout2?.alpha = 1F
            activatePromo()
        }
    }

    fun activatePromo() {
        if (BuildConfig.FLAVOR == "INA" && videoUriList.size != 0) {
            promoHandler?.removeCallbacksAndMessages(null)
            promoHandler = Handler()

            promoHandler?.postDelayed({

                val currentUri = videoUriList[currentVideoIndex]
                _binding?.promoView?.setVideoURI(currentUri)

                _binding?.promoView?.setOnPreparedListener { mediaPlayer ->
                    mediaPlayer.isLooping = true
                    _binding?.promoView?.start()
                }

//            _binding?.promoView?.setOnCompletionListener { mediaPlayer ->
//                Log.d("MainFragment", "setOnCompletionListener")
//                currentVideoIndex += 1
//                val currentUri = videoUriList[currentVideoIndex]
//                _binding?.promoView?.setVideoURI(currentUri)
//            }

                val mediaController1 = object : MediaController(context) {
                    override fun show() {}
                    override fun hide() {}
                }

                _binding?.promoView?.setOnTouchListener { _, event ->
                    // Hide the media controls when the VideoView is touched
                    if (event.action == MotionEvent.ACTION_DOWN) {
                        mediaController1.hide()
                        deactivatePromo()
                    }
                    true
                }

                mediaController1.setAnchorView(_binding?.promoView)
                mediaController1.setMediaPlayer(_binding?.promoView)
                _binding?.promoView?.setMediaController(mediaController1)
                _binding?.promoView?.visibility = View.VISIBLE

                // Increment the currentVideoIndex and loop back to the first video if necessary
                currentVideoIndex = (currentVideoIndex + 1) % videoUriList.size

                _binding?.mainLogo1?.visibility = View.GONE
                _binding?.mainLogo3?.visibility = View.GONE
                _binding?.pleaseScanIcon?.visibility = View.GONE
                _binding?.pleaseScanCardText?.visibility = View.GONE
                _binding?.mainLogo2?.visibility = View.GONE
                _binding?.gridLayout2?.alpha = 0.2F
                _binding?.promoView?.visibility = View.VISIBLE
            }, promoDelayMillis)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        promoHandler?.removeCallbacksAndMessages(null)
        Timber.d("MainFragment onDestroyView")
        _binding = null
    }
}