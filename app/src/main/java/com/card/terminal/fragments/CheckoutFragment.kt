package com.card.terminal.fragments

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.os.Environment
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
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.util.*


/**
 * An example full-screen fragment that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 */
class CheckoutFragment : Fragment() {
    private var _binding: FragmentCheckoutBinding? = null
    private val binding get() = _binding!!

    private var mListener: OnTakePhotoListener? = null
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCheckoutBinding.inflate(inflater, container, false)
        Timber.d("CheckoutFragment onCreateView")
        return binding.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        try {
            mListener = context as OnTakePhotoListener
        } catch (e: ClassCastException) {
            throw ClassCastException("$context must implement OnFragmentInteractionListener")
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.firstName.text = arguments?.getString("firstName")
        binding.lastName.text = arguments?.getString("lastName")

        if (arguments?.containsKey("companyName") == true) {
            binding.companyName.visibility = View.VISIBLE
            binding.companyName.text = arguments?.getString("companyName")
        }

        val existingBundle = requireArguments()

        val prefs = ContextProvider.getApplicationContext()
            .getSharedPreferences("MyPrefsFile", AppCompatActivity.MODE_PRIVATE)
        if (prefs.getBoolean("CaptureOnEvent", false)
        ) {
            if (prefs.getBoolean("pushImageToServer", false)) {
                eventImageLogic(existingBundle)
            }
        }

        MyHttpClient.publishNewEvent(existingBundle)
        val delay = 10000L

        binding.reasonValue.text = arguments?.getString("reasonValue", "")

        binding.readoutValue.text = arguments?.getString("readoutValue")

        if (arguments?.getString("reasonValue").equals("Ulaz")) {
            binding.reasonKey.text = "Razlog ulaza: "
        }

        super.onViewCreated(view, savedInstanceState)
        Timber.d("CheckoutFragment onViewCreated")

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

    private fun eventImageLogic(existingBundle: Bundle) {
        val folder =
            File(Environment.getExternalStorageDirectory().absoluteFile.toString() + "/Pictures/")

        val files = folder.listFiles()
        var mostRecentFile: File? = null
        var lastModifiedTime: Long = 0

        for (file in files!!) {
            if (file.isFile && file.lastModified() > lastModifiedTime) {
                mostRecentFile = file
                lastModifiedTime = file.lastModified()
            }
        }

        try {
            val inputStream = FileInputStream(mostRecentFile)
            val buffer = mostRecentFile?.length()?.let { ByteArray(it.toInt()) }
            inputStream.read(buffer)
            inputStream.close()
            val b64Img = android.util.Base64.encodeToString(buffer, android.util.Base64.NO_WRAP)
            existingBundle.putString(
                "EventImage", b64Img
            )
        } catch (e: Exception) {
            Timber.d(e)
            existingBundle.putString(
                "EventImage", ""
            )
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        Timber.d("CheckoutFragment onDestroyView")
        _binding = null
    }
}