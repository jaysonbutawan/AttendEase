package com.example.attendease.teacher.ui.session.ui

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity

import androidx.lifecycle.ViewModelProvider
import com.example.attendease.teacher.data.model.QrUtils
import com.example.attendease.teacher.data.repositories.SessionRepository
import com.example.attendease.databinding.SessionScreenBinding
import com.example.attendease.teacher.ui.session.viewmodel.QrSessionViewModel
import com.example.attendease.teacher.ui.session.viewmodel.SessionViewModelFactory

class SessionActivity : AppCompatActivity() {

    private lateinit var binding: SessionScreenBinding
    private lateinit var viewModel: QrSessionViewModel
    private var qrHandler: Handler? = null
    private lateinit var qrRunnable: Runnable

    private var roomId: String? = null
    private var sessionId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SessionScreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Init ViewModel
        val repository = SessionRepository()
        val factory = SessionViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[QrSessionViewModel::class.java]

        sessionId = intent.getStringExtra("sessionId")
        roomId = intent.getStringExtra("roomId")

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

        qrHandler = Handler(mainLooper)
        qrHandler?.post(qrRunnable)
    }

    override fun onDestroy() {
        super.onDestroy()
        qrHandler?.removeCallbacksAndMessages(null)
    }
}
