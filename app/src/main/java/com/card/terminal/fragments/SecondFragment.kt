package com.card.terminal.fragments

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.MainActivity
import com.card.terminal.R
import com.card.terminal.databinding.FragmentSecondBinding
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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

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
        if (tv.text.length < 8) {
            tv.text = tv.text.toString().plus(text)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


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
            binding.pinPreviewText.text = binding.pinPreviewText.text.toString().dropLast(1)
        }

        binding.enterDialButton.setOnClickListener {
            val act = activity as MainActivity

            if (act.checkPin(binding.pinPreviewText.text)) {
                findNavController().navigate(R.id.action_SecondFragment_to_mainFragment)
                //Toast.makeText(act, "correct pin!", Toast.LENGTH_SHORT).show()
                showDialog("Napomena", "Slobodan prolaz.")
            } else {
                binding.pinPreviewText.text = ""
                //Toast.makeText(act, "wrong pin!", Toast.LENGTH_SHORT).show()
                showDialog("Napomena", "PIN netočan. Molimo pokušajte ponovno.")
            }
        }
    }

    fun showDialog(title: String, message: String) {
        val builder  = AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton(android.R.string.yes,
                DialogInterface.OnClickListener { dialog, which ->
                })
            .setIcon(android.R.drawable.ic_dialog_alert)
            .create()

        builder.setOnShowListener {
            Thread.sleep(5000)
            it.dismiss()
        }
        builder.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}