package com.example.attendease.data.model

import android.graphics.Bitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.journeyapps.barcodescanner.BarcodeEncoder


object QrUtils {

    fun generateQrCode(sessionId: String): String {
        return "QR_$sessionId${System.currentTimeMillis()}"
    }

    fun generateQrBitmap(data: String, size: Int = 500): Bitmap {
        return try {
            val hints = hashMapOf<EncodeHintType, Any>(
                EncodeHintType.MARGIN to 1 // small margin around QR
            )
            val bitMatrix: BitMatrix =
                MultiFormatWriter().encode(data, BarcodeFormat.QR_CODE, size, size, hints)
            val encoder = BarcodeEncoder()
            encoder.createBitmap(bitMatrix)
        } catch (e: Exception) {
            throw RuntimeException("QR generation failed", e)
        }
    }
}
