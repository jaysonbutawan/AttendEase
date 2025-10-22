package com.example.attendease.student.ui

import android.Manifest
import android.annotation.SuppressLint
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.attendease.databinding.FragmentScanScreenBinding
import com.example.attendease.student.helper.LocationValidator
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class ScanFragmentActivity : Fragment() {

    private var _binding: FragmentScanScreenBinding? = null
    private val binding get() = _binding!!

    private var scanningEnabled = true
    private val database = FirebaseDatabase.getInstance().getReference("rooms")

    private val scanner by lazy {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }
    private val locationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }


    private val cameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startCamera()
            else Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
        }

    private fun showLoading(isLoading: Boolean) {
        if (_binding == null) return // Safety check if fragment is destroyed

        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.previewView.visibility = if (isLoading) View.INVISIBLE else View.VISIBLE
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentScanScreenBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

    }

    private fun startCamera() {
        if (!scanningEnabled) {
            binding.previewView.visibility = View.GONE  // hide camera preview
            return
        }

        binding.previewView.visibility = View.VISIBLE // show
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                surfaceProvider = binding.previewView.surfaceProvider
            }

            val analysis = ImageAnalysis.Builder()
                .setOutputImageRotationEnabled(true)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext())) { imageProxy ->
                processImageProxy(imageProxy)
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis)

        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
//                if (barcodes.isEmpty()) {
//                    Log.d("QR_SCAN", "No QR detected")
//                } else {
                    for (barcode in barcodes) {
                        barcode.rawValue?.let { qrValue ->
                            Log.d("QR_SCAN", "Scanned: $qrValue")
                            Toast.makeText(requireContext(), "Scanned: $qrValue", Toast.LENGTH_SHORT).show()
                            handleQrScanned(qrValue)
                            Toast.makeText(requireContext(), "on the way to handleqrScanned", Toast.LENGTH_SHORT).show()
//                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("QR_SCAN", "Error scanning QR: ${e.message}")
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(onResult: (Location?) -> Unit) {
        val fused = LocationServices.getFusedLocationProviderClient(requireContext())
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 2000L)
            .setMaxUpdates(5)
            .setMinUpdateIntervalMillis(1000L)
            .setWaitForAccurateLocation(true)
            .build()

        val callback = object : LocationCallback() {
            private val collected = mutableListOf<Location>()
            override fun onLocationResult(result: LocationResult) {
                collected += result.locations
                val best = collected.minByOrNull { it.accuracy }
                if (best != null && best.accuracy <= 10f || collected.size >= 5) {
                    fused.removeLocationUpdates(this)
                    Log.d(
                        "USER_LOCATION_LOG",
                        """ 
                    • Latitude: ${best?.latitude}
                    • Longitude: ${best?.longitude}
                    • Accuracy: ${best?.accuracy} meters
                    • Provider: ${best?.provider}
                    • Timestamp: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(best?.time ?: 0))}
                    """.trimIndent()
                    )
                    onResult(best)
                }
            }
        }
        fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
    }

    @SuppressLint("MissingPermission")
    private fun handleQrScanned(qrValue: String) {
        if (!scanningEnabled) return
        scanningEnabled = false

        showLoading(true)

        database.get().addOnSuccessListener { snapshot ->
            var matchFound = false
            for (roomSnapshot in snapshot.children) {
                val sessions = roomSnapshot.child("sessions")
                for (session in sessions.children) {
                    val qrValid = session.child("qrValid").getValue(Boolean::class.java) ?: false
                    val storedQR = session.child("qrCode").getValue(String::class.java)
                    val startTime = session.child("startTime").getValue(String::class.java) ?: ""
                    val allowanceTime = session.child("allowanceTime").getValue(Int::class.java) ?: 0
                    val sessionId = session.key ?: continue
                    val roomId = roomSnapshot.key ?: continue

                    if (qrValid && qrValue == storedQR) {
                        matchFound = true
                        val attendanceRef = database.child(roomId)
                            .child("sessions")
                            .child(sessionId)
                            .child("attendance")
                            .child(FirebaseAuth.getInstance().currentUser?.uid ?: "NO_USER")

                        attendanceRef.get().addOnSuccessListener { attendanceSnapshot ->
                            val sessionStatus = session.child("sessionStatus").getValue(String::class.java) ?: "ended"

                            // Check if session is ongoing
                            if (sessionStatus != "started") {
                                Log.d("SCAN_FLOW", "Session not ongoing — skipping markAttendance")
                                Toast.makeText(requireContext(), "Session has ended. Scanning disabled.", Toast.LENGTH_SHORT).show()
                                scanningEnabled = false
                                binding.previewView.visibility = View.GONE
                                return@addOnSuccessListener
                            }

                            // Check if already scanned
                            if (attendanceSnapshot.exists()) {
                                Log.d("SCAN_FLOW", "Attendance already exists — skipping markAttendance")
                                Toast.makeText(requireContext(), "You already scanned for this class.", Toast.LENGTH_LONG).show()
                                scanningEnabled = false
                                binding.previewView.visibility = View.GONE
                                showLoading(false)
                                return@addOnSuccessListener
                            }

                            val polygonPoints = mutableListOf<LatLng>()
                            val polygonSnapshot = roomSnapshot.child("polygon")
                            for (point in polygonSnapshot.children) {
                                val lat = point.child("lat").getValue(Double::class.java)
                                val lng = point.child("lng").getValue(Double::class.java)
                                if (lat != null && lng != null) polygonPoints.add(LatLng(lat, lng))
                            }

                            getLocation { location ->
                                if (location == null) {
                                    Toast.makeText(requireContext(), "Unable to get location.", Toast.LENGTH_SHORT).show()
                                    scanningEnabled = true
                                    return@getLocation
                                }

                                val studentLatLng = LatLng(location.latitude, location.longitude)
                                val gpsAccuracy = location.accuracy // meters

                                // --- Compute distance from the polygon (approximate classroom area) ---
                                val distance = LocationValidator.getDistanceFromPolygon(studentLatLng, polygonPoints)

                                // --- Buffered Polygon Check (for safety margin) ---
                                val isInsideBuffer = studentLatLng.let {
                                    LocationValidator.isInsidePolygon(it, polygonPoints, toleranceMeters = 50f)
                                }

                                Log.d(
                                    "USER_LOCATION_LOG",
                                    """
        Distance: $distance meters
        Accuracy: $gpsAccuracy meters
        QR Valid: $qrValid
        Inside Buffer: $isInsideBuffer
        """.trimIndent()
                                )

                                // --- Determine validation result ---
                                val validationResult = when {
                                    qrValid && gpsAccuracy > 30 -> "present"
                                    distance <= 15 && gpsAccuracy <= 30 -> "present"
                                    else -> "partial"
                                }

                                // --- Set confidence label ---
                                val confidence = when (validationResult) {
                                    "present" -> if (gpsAccuracy <= 30) "High" else "Confirmed by QR"
                                    "partial" -> "Partial — low GPS accuracy"
                                    else -> "Low / Needs review"
                                }

                                // --- Handle result ---
                                when (validationResult) {
                                    "present" -> markAttendance(
                                        roomId,
                                        sessionId,
                                        startTime,
                                        allowanceTime,
                                        isInsideBuffer,
                                        confidence,
                                        validationResult
                                    )



                                    else -> {
                                        markAttendance(
                                            roomId,
                                            sessionId,
                                            startTime,
                                            allowanceTime,
                                            isInsideBuffer,
                                            confidence,
                                            validationResult
                                        )


                                    }
                                }
                            }

                        }
                        break
                    }
                }
                if (matchFound) break
            }

            if (!matchFound) {
                Toast.makeText(requireContext(), "Invalid QR code", Toast.LENGTH_SHORT).show()
                scanningEnabled = true
            }
        }
    }

    private fun markAttendance(
        roomId: String,
        sessionId: String,
        startTime: String,
        allowanceTime: Int,
        isInside: Boolean,
        confidence: String,
        validationResult: String
    ) {
        try {
            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
            val currentTime = formatter.format(Date())
            val current = formatter.parse(currentTime)
            val start = formatter.parse(startTime)
            val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormatter.format(Date())

            val diffMinutes = abs((current.time - start.time) / (1000 * 60)).toInt()

            // Determine present or late
            val (status, lateDuration) = when {
                diffMinutes <= 0 -> "present" to 0
                diffMinutes <= allowanceTime -> "present" to 0
                else -> "late" to diffMinutes - allowanceTime
            }

            // Apply the final logic based on validation result
            val finalStatus = when (validationResult) {
                "present" -> status // can be "present" or "late"
                "partial" -> "partial"
                else -> "absent"
            }

            // Get user info
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser ?: run {
                Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
                scanningEnabled = true
                return
            }

            val studentId = currentUser.uid
            val studentName = currentUser.displayName ?: "Unknown Student"

            // Firebase reference
            val attendanceRef = database.child(roomId)
                .child("sessions")
                .child(sessionId)
                .child("attendance")
                .child(currentDate)
                .child(studentId)

            // Data to store
            val attendanceData = mapOf(
                "name" to studentName,
                "status" to finalStatus,
                "timeScanned" to currentTime,
                "lateDuration" to lateDuration,
                "totalOutsideTime" to 0,
                "confidence" to confidence,
                "qrValid" to (confidence == "Confirmed by QR")
            )

            Log.d(
                "USER_LOCATION_LOG",
                """
            roomId: $roomId
            sessionId: $sessionId
            studentId: $studentId
            finalStatus: $finalStatus
            confidence: $confidence
            validationResult: $validationResult
            time: $currentTime
            """.trimIndent()
            )

            // Save to Firebase
            attendanceRef.setValue(attendanceData)
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "Attendance marked: $finalStatus",
                        Toast.LENGTH_LONG
                    ).show()
                    showLoading(false)
                    scanningEnabled = true
                    binding.previewView.visibility = View.GONE
                    val dataToPass = Bundle().apply {
                        putString("status", finalStatus)
                        putString("timeScanned", currentTime)
                        // Add other necessary session info to the Bundle
                        putString("roomId", roomId)
                        putString("sessionId", sessionId)
                    }

                    // 3. Navigate to the confirmation fragment (JoinClassFragmentActivity)
                    // Assuming you are using SafeArgs or standard Fragment transactions
                    navigateToJoinClass(dataToPass)
                }
                .addOnFailureListener { e ->
                    Log.e("ATTENDANCE_ERROR", "Failed to write attendance", e)
                    Toast.makeText(
                        requireContext(),
                        "Failed to mark attendance",
                        Toast.LENGTH_SHORT
                    ).show()
                    showLoading(false)
                    scanningEnabled = true
                }

        } catch (e: Exception) {
            Log.e("ATTENDANCE_EXCEPTION", "Error marking attendance", e)
            Toast.makeText(requireContext(), "Unexpected error occurred.", Toast.LENGTH_SHORT).show()
            showLoading(false)
            scanningEnabled = true
        }
    }


    private fun navigateToJoinClass(dataToPass: Bundle) {
        val joinClassFragment = JoinClassFragmentActivity()
        joinClassFragment.arguments = dataToPass
        (requireActivity() as StudentDashboardActivity).loadFragment("joinClass")

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onPause() {
        super.onPause()
        try {
            ProcessCameraProvider.getInstance(requireContext()).get().unbindAll()
        } catch (_: Exception) {}
    }
    override fun onResume() {
        super.onResume()
        database.get().addOnSuccessListener { snapshot ->
            if (scanningEnabled && binding.progressBar.visibility == View.GONE) {
                startCamera() // Re-bind the camera
            }
            for (roomSnapshot in snapshot.children) {
                val sessions = roomSnapshot.child("sessions")
                for (session in sessions.children) {
                    val sessionStatus = session.child("status").getValue(String::class.java) ?: "ended"
                    val attendanceSnapshot = session.child("attendance")
                        .child(FirebaseAuth.getInstance().currentUser?.uid ?: "NO_USER")

                    scanningEnabled = sessionStatus == "ended" || !attendanceSnapshot.exists()
                    binding.previewView.visibility = if (scanningEnabled) View.VISIBLE else View.GONE
                }
            }
        }

        getLocation { location ->
            Log.d("QR_SCAN", "Warm-up GPS: ${location?.latitude}, ${location?.longitude}")
        }
    }


}
