package com.example.attendease.ui.session

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.attendease.data.model.QrUtils
import com.example.attendease.data.repositories.SessionRepository
import com.example.attendease.databinding.FragmentSessionBinding

class SessionFragment : Fragment() {

    private var qrHandler: Handler? = null
    private lateinit var viewModel: SessionViewModel
    private var _binding: FragmentSessionBinding? = null
    private val binding get() = _binding!!

    private lateinit var qrRunnable: Runnable
    private var roomId: String? = null
    private var sessionId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Init ViewModel
        val repository = SessionRepository()
        val factory = SessionViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[SessionViewModel::class.java]

        // ✅ Get arguments safely
        sessionId = arguments?.getString("sessionId")
        roomId = arguments?.getString("roomId")

        if (sessionId.isNullOrEmpty() || roomId.isNullOrEmpty()) {
            Log.e("SessionFragment", "❌ sessionId or roomId is missing in arguments")
            return
        }

        Log.d("SessionFragment", "✅ Opened with sessionId=$sessionId, roomId=$roomId")

        startQrCodeGeneration()
    }

    private fun startQrCodeGeneration() {
        qrRunnable = object : Runnable {
            override fun run() {
                val sessionIdValue = sessionId ?: return
                val roomIdValue = roomId ?: return

                val qrCode = QrUtils.generateQrCode(sessionIdValue)

                // ✅ update in Firebase
                viewModel.updateQr(roomIdValue, sessionIdValue, qrCode)

                // ✅ update ImageView
                val qrBitmap: Bitmap = QrUtils.generateQrBitmap(qrCode)
                binding.qrImageView.setImageBitmap(qrBitmap)

                // ✅ regenerate every 30 seconds
                qrHandler?.postDelayed(this, 30_000)
            }
        }

        qrHandler = Handler(requireActivity().mainLooper)
        qrHandler?.post(qrRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        qrHandler?.removeCallbacksAndMessages(null)
        _binding = null
    }
}
