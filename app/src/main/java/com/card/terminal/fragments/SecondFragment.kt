package com.card.terminal.fragments

import android.annotation.SuppressLint
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
import com.card.terminal.components.CustomDialog
import com.card.terminal.databinding.FragmentSecondBinding
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class SecondFragment : Fragment() {

    private var _binding: FragmentSecondBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private var resetPin = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("SecondFragment onCreateView")
        _binding = FragmentSecondBinding.inflate(inflater, container, false)
        val act = activity as MainActivity
        binding.tvDate.text =
            LocalDateTime.parse(act.getDateTime().toString(), DateTimeFormatter.ISO_DATE_TIME)
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
        binding.tvClock.text =
            LocalDateTime.parse(act.getDateTime().toString(), DateTimeFormatter.ISO_DATE_TIME)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        act.cardScannerActive = false
        return binding.root

    }

    fun setPinText(tv: TextView, text: String) {
        if(resetPin){
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

    @SuppressLint("ResourceAsColor")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("SecondFragment onViewCreated")

        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_mainFragment)
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
            if(resetPin) {
                resetPin = false
                binding.pinPreviewText.setTextColor(Color.WHITE)
                binding.tvErrMsg.visibility = View.GONE
                binding.pinPreviewText.text = ""
            } else {
                binding.pinPreviewText.text = binding.pinPreviewText.text.toString().dropLast(1)
            }
        }

        binding.enterDialButton.setOnClickListener {
            val act = activity as MainActivity
            //TODO ????
//            if (act.checkPin(binding.pinPreviewText.text)) {
            if(binding.pinPreviewText.text == "0000"){
                findNavController().navigate(R.id.action_SecondFragment_to_mainFragment)
                showDialog()
            } else {
                resetPin = true;
                binding.pinPreviewText.setTextColor(Color.parseColor("#ff2424"))
                val shake = AnimationUtils.loadAnimation(activity?.applicationContext, R.anim.shake)
                binding.pinPreviewText.startAnimation(shake)
                binding.tvErrMsg.visibility = View.VISIBLE
            }
        }
    }

    fun showDialog() {
        val dialog = activity?.let { CustomDialog(it, "", true) }

        if (dialog != null) {
            dialog.setOnShowListener {
                Thread.sleep(3000)
                it.dismiss()
            }
            dialog.show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("SecondFragment onDestroyView")
        _binding = null
    }
}