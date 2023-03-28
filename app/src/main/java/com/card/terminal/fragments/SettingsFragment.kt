package com.card.terminal.fragments

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.MainActivity
import com.card.terminal.R
import com.card.terminal.databinding.FragmentSettingsBinding
import com.card.terminal.utils.ContextProvider


class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private lateinit var mySharedPreferences: SharedPreferences


    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!
    private var resetPin = false
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
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
        setKeyboardButtons()

        val larusIPEditText = binding.larusIP
        larusIPEditText.setText(
            ContextProvider.getApplicationContext().getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE).getString("larusIP", "")
        )

        val serverIPEditText = binding.ServerIP
        serverIPEditText.setText(
            ContextProvider.getApplicationContext().getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE).getString("serverIP", "")
        )

        val larusPortEditText = binding.larusPort
        larusPortEditText.setText(
            Integer.toString(
                ContextProvider.getApplicationContext().getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE).getInt("larusPort", 0)
            )
        )

        val serverPortEditText = binding.ServerPort
        serverPortEditText.setText(
            Integer.toString(
                ContextProvider.getApplicationContext().getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE).getInt("serverPort", 0)
            )
        )

        binding.saveSettingsButton.setOnClickListener {
            mySharedPreferences =
                ContextProvider.getApplicationContext().getSharedPreferences("MyPrefsFile", Context.MODE_PRIVATE)
            // Set the value of a preference
            val editor = mySharedPreferences.edit()
            editor.putString("larusIP", larusIPEditText.text.toString())
            editor.putInt("larusPort", larusPortEditText.text.toString().toInt())
            editor.putString("serverIP", serverIPEditText.text.toString())
            editor.putInt("serverPort", serverPortEditText.text.toString().toInt())
            editor.apply()
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
            //TODO promjeni
//            if (act.checkPin(binding.pinPreviewText.text)) {
            if (binding.pinPreviewText.text == "46701950") {
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

//    fun setTexts() {
//        ipOfPCB = binding.larusIP.text.toString()
//        val editText1 = binding.larusIP
//        editText1.setText(ipOfPCB, TextView.BufferType.EDITABLE)
//
//        try {
//            portOfPCB = binding.larusPort.text.toString().toInt()
//            val editText2 = binding.larusPort
//            editText2.setText(binding.larusPort.text, TextView.BufferType.EDITABLE)
//        } catch (e: java.lang.NumberFormatException) {
//            Toast.makeText(this@SettingsFragment.context, "Unesi broj za port.", Toast.LENGTH_LONG)
//                .show()
//        }
//
//        ipOfServer = binding.ServerIP.text.toString()
//        val editText3 = binding.ServerIP
//        editText3.setText(ipOfServer, TextView.BufferType.EDITABLE)
//
//        try {
//            portOfServer = binding.ServerPort.text.toString().toInt()
//            val editText4 = binding.ServerPort
//            editText4.setText(binding.ServerPort.text, TextView.BufferType.EDITABLE)
//        } catch (e: java.lang.NumberFormatException) {
//            Toast.makeText(this@SettingsFragment.context, "Unesi broj za port.", Toast.LENGTH_LONG)
//                .show()
//        }
//    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}