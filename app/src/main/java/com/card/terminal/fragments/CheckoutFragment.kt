package com.card.terminal.fragments

import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.card.terminal.R
import com.card.terminal.databinding.FragmentCheckoutBinding
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
 * An example full-screen fragment that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class CheckoutFragment : Fragment() {
    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        Timber.d("CheckoutFragment onCreateView")
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.firstAndLastName.text = arguments?.getString("name")
//        binding.reasonValue.text = arguments?.getString("selection")
        val prefs = ContextProvider.getApplicationContext()
            .getSharedPreferences("MyPrefsFile", AppCompatActivity.MODE_PRIVATE)

        val existingBundle = requireArguments()
        MyHttpClient.pingy(existingBundle)
        var delay = 3000L
        if (existingBundle.containsKey("noButtonClickNeededRregime")) {
            binding.reasonKey.text = ""
            delay = 2000L

            val scope = CoroutineScope(Dispatchers.IO)
            scope.launch {
                val url = URL(
                    ("http://" + prefs.getString(
                        "bareIP",
                        "?"
                    ) + existingBundle.get("imagePath"))
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

            binding.reasonValue.text = "Slobodan prolaz"
        } else {
            binding.reasonValue.text =
                ContextProvider.getApplicationContext()
                    .getSharedPreferences("MyPrefsFile", AppCompatActivity.MODE_PRIVATE)
                    .getString("selection", "?")
        }



        if (existingBundle.containsKey("imageB64")) {
            binding.photo.setImageBitmap(existingBundle.getParcelable("imageB64"))
        }

        val dt = LocalDateTime.parse(arguments?.getString("time"), DateTimeFormatter.ISO_DATE_TIME)
            .format(DateTimeFormatter.ofPattern("d. MMMM yyyy. HH:mm:ss", Locale("hr")))

        binding.readoutValue.text = binding.readoutValue.text.toString() + dt

        super.onViewCreated(view, savedInstanceState)
        Timber.d("CheckoutFragment onViewCreated")


//        binding.smile.setOnClickListener {
//            findNavController().navigate(R.id.action_CheckoutFragment_to_MainFragment)
//        }

        Handler().postDelayed({
            when (findNavController().currentDestination?.id) {
                R.id.CheckoutFragment -> {
                    findNavController().navigate(
                        R.id.action_CheckoutFragment_to_MainFragment
                    )
                }
            }
        }, delay)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("CheckoutFragment onDestroyView")
        _binding = null
    }
}