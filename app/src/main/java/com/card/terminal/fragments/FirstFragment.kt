package com.card.terminal.fragments

import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.GridLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.MainActivity
import com.card.terminal.R
import com.card.terminal.databinding.FragmentFirstBinding
import com.card.terminal.http.MyHttpClient
import com.card.terminal.utils.ContextProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.URL
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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        val act = activity as MainActivity
        binding.tvDateClock.text =
            LocalDateTime.parse(act.getDateTime().toString(), DateTimeFormatter.ISO_DATE_TIME)
                .format(DateTimeFormatter.ofPattern("d. MMMM yyyy.", Locale("hr"))) + LocalDateTime.parse(act.getDateTime().toString(), DateTimeFormatter.ISO_DATE_TIME)
                .format(DateTimeFormatter.ofPattern("HH:mm"))
//        binding.tvClock.text =
//            LocalDateTime.parse(act.getDateTime().toString(), DateTimeFormatter.ISO_DATE_TIME)
//                .format(DateTimeFormatter.ofPattern("HH:mm"))
        Timber.d("FirstFragment onCreateView")
        act.cardScannerActive = true
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("FirstFragment onViewCreated")

        val existingBundle = requireArguments()
//        if (existingBundle.containsKey("imageB64")) {
//            val imageBytesB64 = Base64.getDecoder().decode(existingBundle.getString("imageB64"))
//            val decodedImage = BitmapFactory.decodeByteArray(imageBytesB64, 0, imageBytesB64.size)
//            if (decodedImage.byteCount != 0) {
//                binding.photo.setImageBitmap(decodedImage)
//                existingBundle.putParcelable("imageB64", decodedImage)
//        }


        val prefs = ContextProvider.getApplicationContext()
            .getSharedPreferences("MyPrefsFile", AppCompatActivity.MODE_PRIVATE)


        if (existingBundle.containsKey("imagePath")) {

            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                val url = URL(
                    (prefs.getString("bareIP", "?") + existingBundle.get("imagePath"))
                )
                val connection = withContext(Dispatchers.IO) {
                    url.openConnection()
                } as HttpURLConnection
                connection.doInput = true
                withContext(Dispatchers.IO) {
                    connection.connect()
                }
                val input = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(input)
                withContext(Dispatchers.Main) {
                    binding.photo.setImageBitmap(bitmap)
                    existingBundle.putParcelable("imageB64", bitmap)
                }
            }
        }



        binding.firstName.text = arguments?.getString("firstName")
        binding.lastName.text = arguments?.getString("lastName")

        val ct = existingBundle.getString("classType")


        val layout = binding.buttonsGrid
        println(layout)

        if (ct.equals("WORKER")) {
            ubijMe("WORKER", prefs, binding.buttonsGrid, existingBundle)
        } else if (ct.equals("CONTRACTOR")) {
            ubijMe("CONTRACTOR", prefs, binding.buttonsGrid, existingBundle)
        } else if (ct.equals("GUEST")) {
            ubijMe("GUEST", prefs, binding.buttonsGrid, existingBundle)
        } else if (ct.equals("VEHICLE")) {
            ubijMe("VEHICLE", prefs, binding.buttonsGrid, existingBundle)
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
                        R.id.action_FirstFragment_to_CheckoutFragment, bundle
                    )
                    MyHttpClient.pingy(bundle)
                }
            }
        }, 500)
    }

    fun ubijMe(
        str: String,
        prefs: SharedPreferences,
        layout: GridLayout,
        bundle: Bundle
    ) {
        for (i in 0..prefs.getInt("${str}_size", 0) - 1) {
            val btn = layout.get(i) as Button
            var title = prefs.getString("WORKER_${i}", "?_0")

            val eCode2 = title!!.substring(title.indexOf("_") + 1).toInt()
            title = title.substring(0, title.indexOf("_"))
            bundle.putInt(title, eCode2)
            btn.setText("   " + title)
            btn.visibility = View.VISIBLE
            btn.setOnClickListener {
                val editor = prefs.edit()
                editor.putString("selection", title)
                editor.putInt("eCode2", eCode2)
                editor.commit()
                btn.setBackgroundResource(R.drawable.card_button_background)
                btn.setBackgroundColor(Color.parseColor("#faa61a"))
                goToCheckoutWithBundle(bundle)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("FirstFragment onDestroyView")
        _binding = null
    }
}