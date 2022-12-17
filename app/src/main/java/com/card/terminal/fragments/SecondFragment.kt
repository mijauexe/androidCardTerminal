package com.card.terminal.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.MainActivity
import com.card.terminal.R
import com.card.terminal.databinding.FragmentSecondBinding


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
        act.cardScannerActive = false
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.buttonSecond.setOnClickListener {
            findNavController().navigate(R.id.action_SecondFragment_to_mainFragment)
        }

        binding.zeroDialButton.setOnClickListener {
            binding.pinPreviewText.text = binding.pinPreviewText.text.toString().plus("0")
        }

        binding.oneDialButton.setOnClickListener {
            binding.pinPreviewText.text = binding.pinPreviewText.text.toString().plus("1")
        }

        binding.twoDialButton.setOnClickListener {
            binding.pinPreviewText.text = binding.pinPreviewText.text.toString().plus("2")
        }

        binding.threeDialButton.setOnClickListener {
            binding.pinPreviewText.text = binding.pinPreviewText.text.toString().plus("3")
        }

        binding.fourDialButton.setOnClickListener {
            binding.pinPreviewText.text = binding.pinPreviewText.text.toString().plus("4")
        }

        binding.fiveDialButton.setOnClickListener {
            binding.pinPreviewText.text = binding.pinPreviewText.text.toString().plus("5")
        }

        binding.sixDialButton.setOnClickListener {
            binding.pinPreviewText.text = binding.pinPreviewText.text.toString().plus("6")
        }

        binding.sevenDialButton.setOnClickListener {
            binding.pinPreviewText.text = binding.pinPreviewText.text.toString().plus("7")
        }

        binding.eightDialButton.setOnClickListener {
            binding.pinPreviewText.text = binding.pinPreviewText.text.toString().plus("8")
        }

        binding.nineDialButton.setOnClickListener {
            binding.pinPreviewText.text = binding.pinPreviewText.text.toString().plus("9")
        }

        binding.delDialButton.setOnClickListener {
            binding.pinPreviewText.text = binding.pinPreviewText.text.toString().dropLast(1)
        }

        binding.enterDialButton.setOnClickListener {
            val act = activity as MainActivity

            if (act.checkPin(binding.pinPreviewText.text)) {
                findNavController().navigate(R.id.action_SecondFragment_to_mainFragment)
                Toast.makeText(act, "correct pin!", Toast.LENGTH_SHORT).show()
            } else {
                binding.pinPreviewText.text = ""
                Toast.makeText(act, "wrong pin!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}