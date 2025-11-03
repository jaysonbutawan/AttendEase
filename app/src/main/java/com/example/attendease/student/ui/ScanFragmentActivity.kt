package com.example.attendease.student.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Context
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
import com.example.attendease.student.ui.dialogs.OutsideDialog
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanFragmentActivity : Fragment() {

    private var _binding: FragmentScanScreenBinding? = null
    private val binding get() = _binding!!

    private var scanningEnabled = true
    private val database = FirebaseDatabase.getInstance().getReference("rooms")
    private lateinit var outsideDialog: OutsideDialog


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


    private var cameraProvider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageAnalysis: ImageAnalysis? = null

    // NEW: dedicated executor for image analysis (shutdown on stop)
    private var cameraExecutor: ExecutorService? = null


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
        outsideDialog = OutsideDialog(requireContext())

        if (cameraExecutor == null || cameraExecutor!!.isShutdown) {
            cameraExecutor = Executors.newSingleThreadExecutor()
        }

    }
    private fun startCamera() {
        // Prevent reinitialization if camera already active
        if (cameraProvider != null && camera != null) {
            Log.d("CAMERA_STATE", "Camera already running, skipping reinit.")
            return
        }

        if (!scanningEnabled || _binding == null) {
            Log.d("CAMERA_STATE", "Camera start skipped: scanning disabled or binding null.")
            return
        }

        binding.previewView.visibility = View.VISIBLE

        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                cameraProvider?.unbindAll()

                val preview = Preview.Builder().build().apply {
                    surfaceProvider = binding.previewView.surfaceProvider
                }

                // CHANGED: keep reference to ImageAnalysis
                imageAnalysis = ImageAnalysis.Builder()
                    .setOutputImageRotationEnabled(true)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                // CHANGED: use background executor for analyzer (not main executor)
                val executor = cameraExecutor ?: ContextCompat.getMainExecutor(requireContext())
                imageAnalysis?.setAnalyzer(executor) { imageProxy ->
                    // Guard: close early if scanning disabled or binding gone
                    if (!scanningEnabled || _binding == null) {
                        try { imageProxy.close() } catch (_: Exception) {}
                        return@setAnalyzer
                    }
                    processImageProxy(imageProxy)
                }

                camera = cameraProvider?.bindToLifecycle(
                    viewLifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageAnalysis
                )

                Log.d("CAMERA_STATE", "Camera successfully started.")

            } catch (e: Exception) {
                Log.e("CAMERA_INIT", "Error initializing camera: ${e.message}")
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    @SuppressLint("UnsafeOptInUsageError")
    private fun processImageProxy(imageProxy: ImageProxy) {
        // Additional guard just in case
        if (!scanningEnabled || _binding == null) {
            try { imageProxy.close() } catch (_: Exception) {}
            return
        }

        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    barcode.rawValue?.let { qrValue ->
                        Log.d("QR_SCAN", "Scanned: $qrValue")
                        // Do NOT toast on every frame in production — heavy for UX; kept for debug.
                        Toast.makeText(requireContext(), "Scanned: $qrValue", Toast.LENGTH_SHORT).show()
                        handleQrScanned(qrValue)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("QR_SCAN", "Error scanning QR: ${e.message}")
            }
            .addOnCompleteListener {
                try { imageProxy.close() } catch (_: Exception) {}
            }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(onResult: (Location?) -> Unit) {
        val fused = LocationServices.getFusedLocationProviderClient(requireContext())

        // Start a timeout timer (30 seconds)
        val timeoutMillis = 30000L
        var callbackCalled = false

        val timeoutHandler = android.os.Handler(Looper.getMainLooper())
        timeoutHandler.postDelayed({
            if (!callbackCalled) {
                Log.w("LOCATION_TIMEOUT", "Location request timed out after 30 seconds.")
                callbackCalled = true
                onResult(null)
            }
        }, timeoutMillis)

        // Try last known location first
        fused.lastLocation.addOnSuccessListener { lastLocation ->
            val thirtySecondsAgo = System.currentTimeMillis() - 30000
            if (lastLocation != null && lastLocation.time > thirtySecondsAgo && lastLocation.accuracy < 50f) {
                Log.d("USER_LOCATION_LOG", "✅ Using fast last known location.")
                if (!callbackCalled) {
                    callbackCalled = true
                    timeoutHandler.removeCallbacksAndMessages(null)
                    onResult(lastLocation)
                }
                return@addOnSuccessListener
            }

            // Otherwise, request a fresh one
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
                .setMaxUpdates(1)
                .setMinUpdateIntervalMillis(500L)
                .setDurationMillis(5000L)
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fused.removeLocationUpdates(this)
                    if (callbackCalled) return

                    val bestLocation = result.locations.minByOrNull { it.accuracy }

                    // Check accuracy
                    if (bestLocation != null && bestLocation.accuracy > 50f) {
                        Log.w("LOCATION_ACCURACY", "⚠️ Location too inaccurate: ${bestLocation.accuracy}m")
                    }

                    callbackCalled = true
                    timeoutHandler.removeCallbacksAndMessages(null)
                    onResult(bestLocation)
                }
            }

            fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
        }
    }



    // --- Optimized handleQrScanned: Faster flow and exit conditions ---
    @SuppressLint("MissingPermission")
    private fun handleQrScanned(qrValue: String) {
        if (!scanningEnabled) return
        scanningEnabled = false
        showLoading(true)

        // Helper function to reset state on failure or end of flow
        fun resetState(message: String? = null) {
            if (message != null) {
                requireActivity().runOnUiThread {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Notice")
                        .setMessage(message)
                        .setCancelable(false)
                        .setPositiveButton("OK") { dialog, _ ->
                            dialog.dismiss()
                        }
                        .show()
                }
            }

            scanningEnabled = true
            binding.previewView.visibility = View.VISIBLE // Re-enable the scanner view
            showLoading(false)
        }


        // --- OPTIMIZATION POINT 1: Efficiently locate the session and break the loop ---
        database.get().addOnSuccessListener { snapshot ->
            var matchFound = false
            var sessionDetails: Pair<String, String>? = null // Pair of <roomId, sessionId>

            // Client-side scan is still the main bottleneck due to the nested DB structure.
            // We ensure we break as soon as possible.
            for (roomSnapshot in snapshot.children) {
                val sessions = roomSnapshot.child("sessions")
                for (session in sessions.children) {
                    val qrValid = session.child("qrValid").getValue(Boolean::class.java) ?: false
                    val storedQR = session.child("qrCode").getValue(String::class.java)

                    if (qrValid && qrValue == storedQR) {
                        sessionDetails = Pair(roomSnapshot.key!!, session.key!!)
                        matchFound = true
                        break
                    }
                }
                if (matchFound) break
            }

            if (!matchFound || sessionDetails == null) {
                resetState("Invalid QR code")
                return@addOnSuccessListener
            }

            // Deconstruct found details and retrieve necessary snapshots
            val (roomId, sessionId) = sessionDetails
            val roomSnapshot = snapshot.child(roomId)
            val session = roomSnapshot.child("sessions").child(sessionId)

            val sessionStatus = session.child("sessionStatus").getValue(String::class.java) ?: "ended"
            val startTime = session.child("startTime").getValue(String::class.java) ?: ""
            val allowanceTime = session.child("allowanceTime").getValue(Int::class.java) ?: 0

            // --- OPTIMIZATION POINT 2: Quick exit if session has ended ---
            if (sessionStatus != "started") {
                resetState("Session has ended. Scanning disabled.")
                return@addOnSuccessListener
            }

            val attendanceRef = database.child(roomId)
                .child("sessions")
                .child(sessionId)
                .child("attendance")
                .child(FirebaseAuth.getInstance().currentUser?.uid ?: "NO_USER")

            // --- OPTIMIZATION POINT 3: Check existing attendance (faster network call than polygon + GPS) ---
            attendanceRef.get().addOnSuccessListener { attendanceSnapshot ->
                if (attendanceSnapshot.exists()) {
                    resetState("You already scanned for this class.")
                    return@addOnSuccessListener
                }

                // Location-based checks start here (inherently slow part)
                val polygonPoints = mutableListOf<LatLng>()
                val polygonSnapshot = roomSnapshot.child("polygon")
                for (point in polygonSnapshot.children) {
                    val lat = point.child("lat").getValue(Double::class.java)
                    val lng = point.child("lng").getValue(Double::class.java)
                    if (lat != null && lng != null) polygonPoints.add(LatLng(lat, lng))
                }
                Log.d("POLYGON_LOG", buildString {
                    appendLine("════════════════════════════════════════")
                    appendLine("🏫 Room Polygon Points (${polygonPoints.size}):")
                    polygonPoints.forEachIndexed { index, point ->
                        appendLine("  • Point ${index + 1}: Lat=${point.latitude}, Lng=${point.longitude}")
                    }
                    appendLine("════════════════════════════════════════")
                })


                getLocation { location ->
                    // --- Handle missing or inaccurate location ---
                    if (location == null || location.accuracy > 50f) {
                        requireActivity().runOnUiThread {
                            AlertDialog.Builder(requireContext())
                                .setTitle("Location Issue")
                                .setMessage(
                                    if (location == null)
                                        "We couldn’t get your location accurately. Would you like to try scanning again?"
                                    else
                                        "Your GPS accuracy is too low (${location.accuracy.toInt()}m). Would you like to rescan?"
                                )
                                .setCancelable(false)
                                .setPositiveButton("Rescan") { dialog, _ ->
                                    dialog.dismiss()
                                    showLoading(false)
                                    scanningEnabled = true
                                    binding.previewView.visibility = View.VISIBLE
                                    startCamera() // Restart scanning
                                }
                                .setNegativeButton("Cancel") { dialog, _ ->
                                    dialog.dismiss()
                                    showLoading(false)
                                    scanningEnabled = true
                                    binding.previewView.visibility = View.VISIBLE
                                }
                                .show()
                        }
                        return@getLocation
                    }
                    val studentLatLng = LatLng(location.latitude, location.longitude)
                    val gpsAccuracy = location.accuracy // meters

                    val distance = LocationValidator.getDistanceFromPolygon(studentLatLng, polygonPoints)
                    val isInsideBuffer = LocationValidator.isInsidePolygon(studentLatLng, polygonPoints, toleranceMeters = 50f)

                    Log.d(
                        "USER_LOCATION_LOG",
                        """
    Validation Details:
    Distance from Polygon: $distance meters
    GPS Accuracy: $gpsAccuracy meters
    Inside 50m Buffer: $isInsideBuffer
    """.trimIndent()
                    )

                    if (distance > 100f) {
                        AlertDialog.Builder(requireContext())
                            .setTitle("Too Far Away")
                            .setMessage("You are ${distance.toInt()} meters far from the session . Please move closer and try scanning again.")
                            .setPositiveButton("OK") { dialog, _ ->
                                dialog.dismiss()
                                resetState()
                            }
                            .setCancelable(false)
                            .show()
                        return@getLocation
                    }

                    val validationResult = when {
                        gpsAccuracy > 30f -> "partial"
                        distance <= 15f -> "present"
                        else -> "partial"
                    }

                    val confidence = when (validationResult) {
                        "present" -> if (gpsAccuracy <= 15f) "High" else "Medium"
                        "partial" -> "Partial - Low GPS accuracy in attendance"
                        else -> "Low / Needs review"
                    }

                    // Final markAttendance call
                    markAttendance(
                        roomId,
                        sessionId,
                        startTime,
                        allowanceTime,
                        isInsideBuffer,
                        confidence,
                        validationResult
                    )

                    showLoading(false)
                    binding.previewView.visibility = View.GONE
                }
            }
        }
            .addOnFailureListener { e ->
                Log.e("SCAN_FLOW", "Database fetch failed: ${e.message}")
                resetState("Failed to retrieve session data from database.")
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

            val diffMinutes = ((current!!.time - start!!.time) / (1000 * 60)).toInt()

            // Determine present or late
            val (status, lateDuration) = when {
                diffMinutes < 0 -> "present" to 0
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

                    // Prevent re-scan for this session
                    scanningEnabled = false
                    saveScanStatus(roomId, sessionId, true)

                    stopCameraAndScanner()
                    binding.previewView.visibility = View.GONE

                    val dataToPass = Bundle().apply {
                        putString("status", finalStatus)
                        putString("timeScanned", currentTime)
                        putString("roomId", roomId)
                        putString("sessionId", sessionId)
                        putString("dateScanned", currentDate)
                    }

                    startTrackingOutsideTime(roomId, sessionId, studentId)
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
    private fun saveScanStatus(roomId: String, sessionId: String, scanned: Boolean) {
        val prefs = requireContext().getSharedPreferences("ScanPrefs", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("${roomId}_$sessionId", scanned).apply()
    }

    private fun hasScannedForSession(roomId: String, sessionId: String): Boolean {
        val prefs = requireContext().getSharedPreferences("ScanPrefs", Context.MODE_PRIVATE)
        return prefs.getBoolean("${roomId}_$sessionId", false)
    }

        @SuppressLint("MissingPermission")
        private fun startTrackingOutsideTime(roomId: String, sessionId: String, studentId: String) {
            val fused = LocationServices.getFusedLocationProviderClient(requireContext())
            val sessionRef = database.child(roomId).child("sessions").child(sessionId)
            val attendanceRef = sessionRef
                .child("attendance")
                .child(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()))
                .child(studentId)

            var outsideStartTime: Long? = null
            var totalOutsideTime = 0L
            var trackingActive = true
            val polygonPoints = mutableListOf<LatLng>()

            database.child(roomId).child("polygon").get().addOnSuccessListener { polygonSnapshot ->
                for (point in polygonSnapshot.children) {
                    val lat = point.child("lat").getValue(Double::class.java)
                    val lng = point.child("lng").getValue(Double::class.java)
                    if (lat != null && lng != null) polygonPoints.add(LatLng(lat, lng))
                }

                if (polygonPoints.isEmpty()) return@addOnSuccessListener

                val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                    .setMinUpdateIntervalMillis(5000L)
                    .setMaxUpdates(Int.MAX_VALUE)
                    .build()

                val callback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        if (!trackingActive) return

                        val location = result.lastLocation ?: return
                        val studentLatLng = LatLng(location.latitude, location.longitude)
                        val distance = LocationValidator.getDistanceFromPolygon(studentLatLng, polygonPoints)


                        val newConfidence = when {
                            distance > 10f -> "Partial - Left geofence area"
                            else -> "High"
                        }

                        attendanceRef.child("confidence").setValue(newConfidence)


                        if (distance > 10f) {
                            if (outsideStartTime == null) {
                                outsideStartTime = System.currentTimeMillis()
                                requireActivity().runOnUiThread {
                                    if (!outsideDialog.isShowing()) outsideDialog.show(distance)
                                }
                            }

                        } else {
                            if (outsideStartTime != null) {
                                val outsideDuration = System.currentTimeMillis() - outsideStartTime!!
                                totalOutsideTime += outsideDuration
                                outsideStartTime = null

                                val totalOutsideMinutes = (totalOutsideTime / 60000).toInt()
                                val totalOutsideDisplay = formatDuration(totalOutsideTime)

                                val updates = mapOf(
                                    "totalOutsideTime" to totalOutsideMinutes,
                                    "outsideTimeDisplay" to totalOutsideDisplay
                                )

                                attendanceRef.updateChildren(updates)
                            }
                            requireActivity().runOnUiThread {
                                if (outsideDialog.isShowing()) outsideDialog.dismiss()
                            }
                        }
                    }
                }

                fused.requestLocationUpdates(request, callback, Looper.getMainLooper())

                sessionRef.child("sessionStatus").addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val status = snapshot.getValue(String::class.java)
                        if (status.equals("ended", ignoreCase = true)) {
                            trackingActive = false
                            fused.removeLocationUpdates(callback)
                            scanningEnabled = true
                            startCamera()
                            Toast.makeText(requireContext(), "New session started. You can scan again.", Toast.LENGTH_SHORT).show()
                            requireActivity().runOnUiThread {
                                if (outsideDialog.isShowing()) outsideDialog.dismiss()
                            }


                            if (outsideStartTime != null) {
                                val outsideDuration = System.currentTimeMillis() - outsideStartTime!!
                                totalOutsideTime += outsideDuration
                                outsideStartTime = null
                            }

                            val totalOutsideMinutes = (totalOutsideTime / 60000).toInt()
                            val totalOutsideDisplay = formatDuration(totalOutsideTime)

                            val updates = mapOf(
                                "totalOutsideTime" to totalOutsideMinutes,
                                "outsideTimeDisplay" to totalOutsideDisplay
                            )

                            attendanceRef.updateChildren(updates).addOnSuccessListener {
                                Toast.makeText(
                                    requireContext(),
                                    "Session Ended. Total Time Outside: $totalOutsideDisplay",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }
        }


    private fun formatDuration(ms: Long): String {
        val minutes = ms / 60000
        val seconds = (ms % 60000) / 1000
        return when {
            minutes > 0 -> "$minutes minute${if (minutes > 1) "s" else ""} $seconds second${if (seconds != 1L) "s" else ""}"
            else -> "$seconds second${if (seconds != 1L) "s" else ""}"
        }
    }




    private fun navigateToJoinClass(dataToPass: Bundle) {
        val joinClassFragment = JoinClassBottomSheet()
        joinClassFragment.arguments = dataToPass

        (requireActivity() as StudentDashboardActivity).loadFragment("joinClass", dataToPass)

    }

    private fun stopCameraAndScanner() {
        try {
            scanningEnabled = false

            // clear analyzer so it stops receiving frames
            try {
                imageAnalysis?.clearAnalyzer()
            } catch (e: Exception) {
                Log.e("CAMERA_STOP", "Error clearing analyzer: ${e.message}")
            }

            try {
                cameraProvider?.unbindAll()
            } catch (e: Exception) {
                Log.e("CAMERA_STOP", "Error unbinding camera: ${e.message}")
            }

            camera = null
            imageAnalysis = null

            // shutdown executor
            try {
                cameraExecutor?.shutdownNow()
            } catch (e: Exception) {
                Log.e("CAMERA_STOP", "Error shutting down executor: ${e.message}")
            }
            cameraExecutor = null

            Log.d("CAMERA_STATE", "Camera and scanner stopped manually.")
        } catch (e: Exception) {
            Log.e("CAMERA_STOP_ERROR", "Error stopping camera: ${e.message}")
        }
    }

    override fun onPause() {
        super.onPause()
        stopCameraAndScanner()
        Log.d("CAMERA_STATE", "Camera paused and released.")
    }

    override fun onResume() {
        super.onResume()

        if (isVisible && _binding != null) {
            scanningEnabled = true

            if (cameraExecutor == null || cameraExecutor!!.isShutdown) {
                cameraExecutor = Executors.newSingleThreadExecutor()
            }
            ProcessCameraProvider.getInstance(requireContext()).addListener({
                try {
                    val provider = ProcessCameraProvider.getInstance(requireContext()).get()
                    if (provider != null && scanningEnabled) {
                        cameraProvider = provider
                        startCamera()
                        Log.d("CAMERA_STATE", "Camera reinitialized on resume.")
                    }
                } catch (e: Exception) {
                    Log.e("CAMERA_INIT", "Error restarting camera: ${e.message}")
                }
            }, ContextCompat.getMainExecutor(requireContext()))
        }
        getLocation { location ->
            Log.d("QR_SCAN", "Warm-up GPS: ${location?.latitude}, ${location?.longitude}")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // ensure camera cleaned
        stopCameraAndScanner()

        _binding = null
        Log.d("CAMERA_STATE", "Camera and binding fully destroyed.")
    }
}