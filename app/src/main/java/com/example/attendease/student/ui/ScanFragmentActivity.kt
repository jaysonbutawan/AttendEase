package com.example.attendease.student.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
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

    }
    private fun startCamera() {
        if (!scanningEnabled) {
            binding.previewView.visibility = View.GONE  // hide camera preview
            return
        }

        binding.previewView.visibility = View.VISIBLE // show preview
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                surfaceProvider = binding.previewView.surfaceProvider
            }

            val analysis = ImageAnalysis.Builder()
                .setOutputImageRotationEnabled(true)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()

            analysis.setAnalyzer(ContextCompat.getMainExecutor(requireContext())) { imageProxy ->
                if (scanningEnabled) {
                    processImageProxy(imageProxy)
                } else {
                    imageProxy.close()
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            cameraProvider?.unbindAll()

            camera = cameraProvider?.bindToLifecycle(viewLifecycleOwner, cameraSelector, preview, analysis)

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

        // 1. Try to get the last known location first (FASTEST)
        fused.lastLocation.addOnSuccessListener { lastLocation ->
            // Check if last location is recent (within 30 seconds) and reasonably accurate (under 50m)
            val thirtySecondsAgo = System.currentTimeMillis() - 30000
            if (lastLocation != null && lastLocation.time > thirtySecondsAgo && lastLocation.accuracy < 50f) {
                Log.d("USER_LOCATION_LOG", "Using fast Last Known Location.")
                onResult(lastLocation)
                return@addOnSuccessListener
            }

            // 2. If last location is not good, request a quick single update (FASTER)
            // Request a single, high-priority update with a short timeout.
            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 500L)
                .setMaxUpdates(1)
                .setMinUpdateIntervalMillis(500L)
                .setDurationMillis(5000L) // Stop trying after 5 seconds to prevent long waits
                .build()

            val callback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    fused.removeLocationUpdates(this) // Stop updates after the first result
                    val bestLocation = result.locations.minByOrNull { it.accuracy }

                    Log.d(
                        "USER_LOCATION_LOG",
                        """ 
                • Requested Update Location (Quick Fix).
                • Latitude: ${bestLocation?.latitude}
                • Longitude: ${bestLocation?.longitude}
                • Accuracy: ${bestLocation?.accuracy} meters
                • Provider: ${bestLocation?.provider}
                • Timestamp: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(bestLocation?.time ?: 0))}
                """.trimIndent()
                    )
                    onResult(bestLocation)
                }
            }
            fused.requestLocationUpdates(request, callback, Looper.getMainLooper())
        }
            .addOnFailureListener {
                // Handle cases where even getLastLocation fails (e.g., location services disabled)
                Log.e("LOCATION_ERROR", "Failed to get last location: ${it.message}")
                onResult(null)
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
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
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
                    if (location == null) {
                        resetState("Unable to get location.")
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

            val diffMinutes = ((current.time - start.time) / (1000 * 60)).toInt()

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
                    scanningEnabled = true
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
        val joinClassFragment = JoinClassFragmentActivity()
        joinClassFragment.arguments = dataToPass

        (requireActivity() as StudentDashboardActivity).loadFragment("joinClass", dataToPass)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        cameraProvider?.unbindAll()
        _binding = null
        scanningEnabled = false
    }

    override fun onPause() {
        super.onPause()
        try {
            ProcessCameraProvider.getInstance(requireContext()).get().unbindAll()
        } catch (_: Exception) {}
        cameraProvider?.unbindAll()
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
