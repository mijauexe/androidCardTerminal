package com.card.terminal.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.MainActivity
import com.card.terminal.R
import com.card.terminal.databinding.FragmentSettingsBinding
import com.card.terminal.http.MyHttpClient
import com.card.terminal.utils.ContextProvider
import timber.log.Timber


class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private lateinit var mySharedPreferences: SharedPreferences

    private val binding get() = _binding!!
    private var resetPin = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
//        Timber.d("SettingsFragment onCreateView")
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    fun setPinText(tv: TextView, text: String) {
        if (resetPin) {
            resetPin = false
            binding.pinPreviewText.setTextColor(Color.WHITE)
            binding.tvErrMsg.visibility = View.GONE
            tv.text = text
        } else {
            if (tv.text.length < 8) {
                tv.text = tv.text.toString().plus(text)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
//        Timber.d("SettingsFragment onViewCreated")
        setKeyboardButtons()

        val larusIPEditText = binding.larusIP
        larusIPEditText.setText(
            ContextProvider.getApplicationContext()
                .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE).getString("larusIP", "")
        )

        val serverIPEditText = binding.ServerIP
        serverIPEditText.setText(
            ContextProvider.getApplicationContext()
                .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE).getString("serverIP", "")
        )

        val larusPortEditText = binding.larusPort
        larusPortEditText.setText(
            Integer.toString(
                ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
                    .getInt("larusPort", 0)
            )
        )

        val serverPortEditText = binding.ServerPort
        serverPortEditText.setText(
            Integer.toString(
                ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
                    .getInt("serverPort", 0)
            )
        )

        val iftTermIdEditText = binding.ifttermId
        iftTermIdEditText.setText(
            Integer.toString(
                ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
                    .getInt("IFTTERM2_B0_ID", 0)
            )
        )

        val adamUsernameEditText = binding.adamUsername
        adamUsernameEditText.setText(
            ContextProvider.getApplicationContext()
                .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE).getString("adamUsername", ""))

        val adamPasswordEditText = binding.adamPassword
        adamPasswordEditText.setText(
            ContextProvider.getApplicationContext()
                .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE).getString("adamPassword", ""))

        val adamIPEditText = binding.adamIP
        adamIPEditText.setText(
            ContextProvider.getApplicationContext()
                .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE).getString("adamIP", ""))

        val adamRelayNumEditText = binding.adamRelayNum
        adamRelayNumEditText.setText(
            Integer.toString(
                ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
                    .getInt("adamRelayNum", 0)
            )
        )

        val settingsPinEditText = binding.settingsPin
        settingsPinEditText.setText(
            ContextProvider.getApplicationContext()
                .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
                .getString("settingsPin", "4670")
        )

        binding.kioskToggle.setOnClickListener {
            val mainActivity = activity as MainActivity?
            val prefs = ContextProvider.getApplicationContext()
                .getSharedPreferences(MainActivity().PREFS_NAME, AppCompatActivity.MODE_PRIVATE)
            val sett = prefs.getBoolean("kioskMode", false)

            if (mainActivity!!.isAdmin()) {
                if (!sett) {

                    mainActivity.setKioskPolicies(true, true)
                    val editor = prefs.edit()
                    editor.putBoolean("kioskMode", true)
                    Timber.d("kiosk set to: " + true)
                    editor.apply()

                } else {
                    mainActivity.setKioskPolicies(false, true)
                    val editor = prefs.edit()
                    editor.putBoolean("kioskMode", false)
                    Timber.d("kiosk set to: " + false)
                    editor.apply()
                    val intent = Intent(
                        ContextProvider.getApplicationContext(),
                        MainActivity::class.java
                    ).apply {
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    }
                    intent.putExtra(MainActivity.LOCK_ACTIVITY_KEY, false)
                    startActivity(intent)
                }
            }
        }


        binding.hcal.setOnClickListener {
            MyHttpClient.pushRequest("ADD_HCAL")
        }

        binding.init1.setOnClickListener {
            MyHttpClient.pushRequest("ADD_INIT1")
        }

        binding.init0.setOnClickListener {
            MyHttpClient.pushRequest("IFTTERM2_INIT0")
        }

        binding.calendar.setOnClickListener {
            MyHttpClient.pushRequest("ADD_HOLIDAYS")
        }

        binding.photos.setOnClickListener {
            MyHttpClient.pushRequest("PHOTOS_ALL")
        }

        binding.saveSettingsButton.setOnClickListener {
            mySharedPreferences =
                ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
            val editor = mySharedPreferences.edit()
            editor.putString("larusIP", larusIPEditText.text.toString())
            editor.putInt("larusPort", larusPortEditText.text.toString().toInt())

            if (!serverIPEditText.text.contains("http")) {
                editor.putString("bareIP", serverIPEditText.text.toString())
                editor.putString(
                    "serverIP",
                    "http://" + serverIPEditText.text.toString() + "/b0pass/b0pass_iftp2.php"
                )
                editor.putString(
                    "serverIP_s",
                    "https://" + serverIPEditText.text.toString() + "/b0pass/b0pass_iftp2.php"
                )
            }

            editor.putString("adamUsername", adamUsernameEditText.text.toString())
            editor.putString("adamPassword", adamPasswordEditText.text.toString())
            editor.putString("adamIP", adamIPEditText.text.toString())
            editor.putInt("adamRelayNum", adamRelayNumEditText.text.toString().toInt())

            editor.putInt("serverPort", serverPortEditText.text.toString().toInt())
            editor.putInt("IFTTERM2_B0_ID", iftTermIdEditText.text.toString().toInt())
            editor.putString("settingsPin", settingsPinEditText.text.toString())
            editor.apply()

            Timber.d("larusIP is now %s", larusIPEditText.text.toString())
            Timber.d("larusPort is now %s", larusPortEditText.text.toString())
            Timber.d("serverIP is now %s", serverIPEditText.text.toString())
            Timber.d("serverPort is now %s", serverPortEditText.text.toString())
            Timber.d("adamUsername is now %s", adamUsernameEditText.text.toString())
            Timber.d("adamPassword is now %s", adamPasswordEditText.text.toString())
            Timber.d("adamIP is now %s", adamIPEditText.text.toString())
            Timber.d("adamRelayNum is now %s", adamRelayNumEditText.text.toString())
            Timber.d("IFTTERM2_B0_ID is now %s", iftTermIdEditText.text.toString())
        }
    }

    private fun setKeyboardButtons() {
        binding.buttonBack.setOnClickListener {
            findNavController().navigate(R.id.action_SettingsFragment_to_mainFragment)
        }

        binding.zeroDialButton.setOnClickListener {
            setPinText(binding.pinPreviewText, "0")
        }

        binding.oneDialButton.setOnClickListener {
            setPinText(binding.pinPreviewText, "1")
        }

        binding.twoDialButton.setOnClickListener {
            setPinText(binding.pinPreviewText, "2")
        }

        binding.threeDialButton.setOnClickListener {
            setPinText(binding.pinPreviewText, "3")
        }

        binding.fourDialButton.setOnClickListener {
            setPinText(binding.pinPreviewText, "4")
        }

        binding.fiveDialButton.setOnClickListener {
            setPinText(binding.pinPreviewText, "5")
        }

        binding.sixDialButton.setOnClickListener {
            setPinText(binding.pinPreviewText, "6")
        }

        binding.sevenDialButton.setOnClickListener {
            setPinText(binding.pinPreviewText, "7")
        }

        binding.eightDialButton.setOnClickListener {
            setPinText(binding.pinPreviewText, "8")
        }

        binding.nineDialButton.setOnClickListener {
            setPinText(binding.pinPreviewText, "9")
        }

        binding.delDialButton.setOnClickListener {
            if (resetPin) {
                resetPin = false
                binding.pinPreviewText.setTextColor(Color.WHITE)
                binding.tvErrMsg.visibility = View.GONE
                binding.pinPreviewText.text = ""
            } else {
                binding.pinPreviewText.text = binding.pinPreviewText.text.toString().dropLast(1)
            }
        }

        binding.enterDialButton.setOnClickListener {
            mySharedPreferences =
                ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)


            if (binding.pinPreviewText.text == mySharedPreferences.getString("settingsPin", "0")) {
                binding.pinLayout.visibility = View.GONE
                binding.glMain.visibility = View.VISIBLE
            } else {
                resetPin = true;
                binding.pinPreviewText.setTextColor(Color.parseColor("#ff2424"))
                val shake = AnimationUtils.loadAnimation(activity?.applicationContext, R.anim.shake)
                binding.pinPreviewText.startAnimation(shake)
                binding.tvErrMsg.visibility = View.VISIBLE
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
//        Timber.d("SettingsFragment onDestroyView")
        _binding = null
    }
}