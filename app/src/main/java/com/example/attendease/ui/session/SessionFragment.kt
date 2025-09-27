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
    private lateinit var binding: FragmentSessionBinding
    private lateinit var qrRunnable: Runnable

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSessionBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = SessionRepository()
        val factory = SessionViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[SessionViewModel::class.java]

        val sessionId = arguments?.getString("sessionId") ?: run {
            Log.e("SessionFragment", "sessionId is null")
            return
        }
        Log.d("SessionFragment", "SessionFragment opened with sessionId: $sessionId")

        startQrCodeGeneration(sessionId)
    }

    private fun startQrCodeGeneration(sessionId: String) {
        qrRunnable = object : Runnable {
            override fun run() {
                val qrCode = QrUtils.generateQrCode(sessionId)

                viewModel.updateQr(sessionId, qrCode)

                val qrBitmap: Bitmap = QrUtils.generateQrBitmap(qrCode)
                binding.qrImageView.setImageBitmap(qrBitmap)

                qrHandler?.postDelayed(this, 30_000)
            }
        }

        qrHandler = Handler(requireActivity().mainLooper)
        qrHandler?.post(qrRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        qrHandler?.removeCallbacksAndMessages(null)
    }
}

