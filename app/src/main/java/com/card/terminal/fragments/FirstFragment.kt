package com.card.terminal.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.MainActivity
import com.card.terminal.R
import com.card.terminal.databinding.FragmentFirstBinding
import com.card.terminal.http.MyHttpClient
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        val act = activity as MainActivity
        binding.tvDate.text =
            LocalDateTime.parse(act.getDateTime().toString(), DateTimeFormatter.ISO_DATE_TIME)
                .format(DateTimeFormatter.ofPattern("d. MMMM yyyy.", Locale("hr")))
        binding.tvClock.text =
            LocalDateTime.parse(act.getDateTime().toString(), DateTimeFormatter.ISO_DATE_TIME)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
        Timber.d("FirstFragment onCreateView")
        act.cardScannerActive = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("FirstFragment onViewCreated")

        val existingBundle = requireArguments()

        if (existingBundle.containsKey("imageB64")) {
            val imageBytesB64 = Base64.getDecoder().decode(existingBundle.getString("imageB64"))
            val decodedImage = BitmapFactory.decodeByteArray(imageBytesB64, 0, imageBytesB64.size)
            if (decodedImage.byteCount != 0) {
                binding.photo.setImageBitmap(decodedImage)
                existingBundle.putParcelable("imageB64", decodedImage)
//        } else if (decodedImagePath.byteCount != 0) {
//            binding.photo.setImageBitmap(decodedImagePath)
//            existingBundle.putParcelable("imageB64", decodedImagePath)
            }
//        else {
//            binding.photo.setImageResource(R.drawable.ic_unknown_person)
//        }
        }



        binding.firstAndLastName.text = arguments?.getString("name")

        val ct = existingBundle.getString("classType")
        if (ct.equals("WORKER")) {
            binding.ibWorkTrip.visibility = View.VISIBLE
            binding.ibWorkTripLocal.visibility = View.VISIBLE
            binding.ibWorkTripOther.visibility = View.VISIBLE
            binding.ibPrivateWPermission.visibility = View.VISIBLE
            binding.ibPrivateWoutPermission.visibility = View.VISIBLE
        } else if (ct.equals("CONTRACTOR")) {
            binding.ibContractor1.visibility = View.VISIBLE
            binding.ibContractor2.visibility = View.VISIBLE
            binding.ibContractor3.visibility = View.VISIBLE
        }
        binding.ibContractor1.setOnClickListener {
            binding.ibContractor1.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "Poslovni izlaz nalog HEP")
            goToCheckoutWithBundle(existingBundle)
        }

        binding.ibContractor2.setOnClickListener {
            binding.ibContractor2.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "Poslovni izlaz nalog tvrtka")
            goToCheckoutWithBundle(existingBundle)
        }

        binding.ibContractor3.setOnClickListener {
            binding.ibContractor3.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "Privatni izlaz")
            goToCheckoutWithBundle(existingBundle)
        }

        binding.ibWorkTrip.setOnClickListener {
            binding.ibWorkTrip.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "Službeno putovanje")
            goToCheckoutWithBundle(existingBundle)
        }

        binding.ibWorkTripLocal.setOnClickListener {
            binding.ibWorkTripLocal.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "Službeno BE-TO")
            goToCheckoutWithBundle(existingBundle)
        }

        binding.ibWorkTripOther.setOnClickListener {
            binding.ibWorkTripOther.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "Službeno ostalo")
            goToCheckoutWithBundle(existingBundle)
        }

        binding.ibPrivateWPermission.setOnClickListener {
            binding.ibPrivateWPermission.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "Privatno uz dozvolu")
            goToCheckoutWithBundle(existingBundle)
        }

        binding.ibPrivateWoutPermission.setOnClickListener {
            binding.ibPrivateWoutPermission.setBackgroundResource(R.drawable.card_button_background)
            existingBundle.putString("selection", "Privatno hitno bez dozvole")
            goToCheckoutWithBundle(existingBundle)
        }

        Handler().postDelayed({
            when (findNavController().currentDestination?.id) {
                R.id.FirstFragment -> {
                    findNavController().navigate(
                        R.id.action_FirstFragment_to_mainFragment
                    )
                }
            }
        }, 5000)
    }

    fun goToCheckoutWithBundle(bundle: Bundle) {
        Handler().postDelayed({
            when (findNavController().currentDestination?.id) {
                R.id.FirstFragment -> {
                    findNavController().navigate(
                        R.id.action_FirstFragment_to_CheckoutFragment,
                        bundle
                    )
                    MyHttpClient.pingy(bundle)
                }
            }
        }, 500)


    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("FirstFragment onDestroyView")
        _binding = null
    }
}