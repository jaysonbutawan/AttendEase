package com.example.attendease.ui.session

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import androidx.fragment.app.Fragment
import com.example.attendease.data.model.QrUtils
import com.example.attendease.data.repositories.SessionRepository
import com.example.attendease.databinding.FragmentSessionBinding

class SessionFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionId = arguments?.getString("SESSION_ID") ?: return
        startQrCodeGeneration(sessionId)
    }

    private var qrHandler: Handler? = null
    private lateinit var viewModel: SessionViewModel
    private lateinit var repo: SessionRepository
    private lateinit var qrRunnable: Runnable
    private lateinit var binding: FragmentSessionBinding

    fun startQrCodeGeneration(sessionId: String) {
        qrRunnable = object : Runnable {
            override fun run() {
                val qrCode = QrUtils.generateQrCode(sessionId)
                viewModel.updateQr(sessionId, qrCode)

                val qrBitmap = QrUtils.generateQrBitmap(qrCode)
                binding.qrImageView.setImageBitmap(qrBitmap)

                // schedule again in 30s
                qrHandler?.postDelayed(this, 30_000)
            }
        }

        qrHandler = Handler(Looper.getMainLooper())
        qrHandler?.post(qrRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        qrHandler?.removeCallbacksAndMessages(null)
    }
}
