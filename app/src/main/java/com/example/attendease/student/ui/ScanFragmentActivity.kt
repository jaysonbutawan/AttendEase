package com.example.attendease.student.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.provider.Settings
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.attendease.databinding.FragmentScanScreenBinding
import com.example.attendease.student.helper.LocationValidator
import com.google.android.gms.location.LocationServices
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
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().apply {
                setSurfaceProvider(binding.previewView.surfaceProvider)
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
    private fun getCurrentLocation(onLocationRetrieved: (Location) -> Unit) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        // Step 1: Check if permission is granted
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                100
            )
            Toast.makeText(requireContext(), "Location permission required", Toast.LENGTH_SHORT).show()
            return
        }

        // Step 2: Check if GPS/Location is enabled
        val locationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled =
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled) {
            Toast.makeText(requireContext(), "Please enable GPS to continue", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            return
        }

        // Step 3: Try to get cached high-accuracy location
        fusedLocationClient.getCurrentLocation(
            com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
            null
        ).addOnSuccessListener { location ->
            if (location != null) {
                Log.d("QR_SCAN", "Lat: ${location.latitude}, Lng: ${location.longitude}")
                onLocationRetrieved(location)
            } else {
                Log.w("QR_SCAN", "No cached location — requesting single GPS update...")

                // Step 4: Request a one-time fresh location update
                val request = com.google.android.gms.location.LocationRequest.Builder(
                    com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                    2000L // interval in ms
                ).setMaxUpdates(1)
                    .setWaitForAccurateLocation(true)
                    .build()

                val callback = object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        val freshLocation = result.lastLocation
                        if (freshLocation != null) {
                            Log.d(
                                "QR_SCAN",
                                "Fetched new location: ${freshLocation.latitude}, ${freshLocation.longitude}"
                            )
                            onLocationRetrieved(freshLocation)
                        } else {
                            Log.e("QR_SCAN", "Still no location — user might be indoors or GPS is off.")
                            Toast.makeText(
                                requireContext(),
                                "Unable to get location. Try moving outdoors or turning on GPS.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        fusedLocationClient.removeLocationUpdates(this)
                    }
                }

                fusedLocationClient.requestLocationUpdates(request, callback, Looper.getMainLooper())
            }
        }.addOnFailureListener { e ->
            Log.e("QR_SCAN", "Failed to get location: ${e.message}", e)
            Toast.makeText(requireContext(), "Failed to get location", Toast.LENGTH_SHORT).show()
        }
    }




    @SuppressLint("MissingPermission")
    private fun handleQrScanned(qrValue: String) {
        if (!scanningEnabled) return
        scanningEnabled = false
        Log.d("QR_FLOW", "Entered handleQrScanned with value: $qrValue")
        database.get().addOnSuccessListener { snapshot ->
            Log.d("QR_FLOW", "Fetched database snapshot: ${snapshot.childrenCount} rooms")
            var matchFound = false

            for (roomSnapshot in snapshot.children) {
                Log.d("QR_FLOW", "Checking room: ${roomSnapshot.key}")
                val sessions = roomSnapshot.child("sessions")
                Log.d("QR_FLOW", "Sessions found: ${sessions.childrenCount}")
                for (session in sessions.children) {
                    val qrValid = session.child("qrValid").getValue(Boolean::class.java) ?: false
                    val storedQR = session.child("qrCode").getValue(String::class.java)
                    Log.d("QR_FLOW", "Session QR: $storedQR")
                    val startTime = session.child("startTime").getValue(String::class.java) ?: ""
                    val allowanceTime = session.child("allowanceTime").getValue(Int::class.java) ?: 0
                    val sessionId = session.key ?: continue
                    val roomId = roomSnapshot.key ?: continue

                    if (qrValid && qrValue == storedQR) {
                        matchFound = true
                        Log.d("QR_SCAN", "Matched QR for session: $sessionId in room: $roomId")
                        Log.d("QR_SCAN", "Matched QR in Room: $roomId, Session: $sessionId")
                        Toast.makeText(requireContext(), "Detected Room: $roomId\nSession: $sessionId", Toast.LENGTH_SHORT).show()


                        // Get polygon points
                        val polygonPoints = mutableListOf<LatLng>()
                        val polygonSnapshot = roomSnapshot.child("polygon")
                        for (point in polygonSnapshot.children) {
                            val lat = point.child("lat").getValue(Double::class.java)
                            val lng = point.child("lng").getValue(Double::class.java)
                            if (lat != null && lng != null) polygonPoints.add(LatLng(lat, lng))
                        }

                        // ✅ Fetch location and mark attendance
                        getCurrentLocation { location ->
                            val studentLatLng = LatLng(location.latitude, location.longitude)
                            val isInside = LocationValidator.isInsidePolygon(studentLatLng, polygonPoints, toleranceMeters = 5f)

                            Log.d("LOCATION_VALIDATION", "Student Location: ${studentLatLng.latitude}, ${studentLatLng.longitude}")
                            Log.d("LOCATION_VALIDATION", "Polygon Points: ${polygonPoints.joinToString()}")
                            Log.d("LOCATION_VALIDATION", "Is Inside Polygon: $isInside")

                            if (isInside) {
                                Toast.makeText(requireContext(), "Inside classroom area ✅", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(requireContext(), "Outside classroom area ⚠️", Toast.LENGTH_SHORT).show()
                            }

                            markAttendance(roomId, sessionId, startTime, allowanceTime, isInside)
                        }
                        Log.d("QR_FLOW", "Match found? $matchFound")



                        break  // ✅ stop looping sessions but allow location to run
                    }
                }
                if (matchFound) break  // stop looping rooms too
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
        isInside: Boolean
    ) {
        val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val currentTime = formatter.format(Date())
        val current = formatter.parse(currentTime)
        val start = formatter.parse(startTime)
        val diffMinutes = abs((current.time - start.time) / (1000 * 60)).toInt()

        val (status, lateDuration) = when {
            diffMinutes <= allowanceTime -> "present" to 0
            else -> "late" to diffMinutes - allowanceTime
        }

        val finalStatus = if (!isInside) "partial" else status

        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e("QR_SCAN", "No logged in user found")
            Toast.makeText(requireContext(), "Not logged in", Toast.LENGTH_SHORT).show()
            scanningEnabled = true
            return
        }

        val studentId = currentUser.uid
        val studentName = currentUser.displayName ?: "Unknown Student"

        Log.d("ATTENDANCE_PATH", "roomId=$roomId, sessionId=$sessionId, studentId=$studentId")

        val attendanceRef = database.child(roomId)
            .child("sessions")
            .child(sessionId)
            .child("attendance")
            .child(studentId)
            .child(System.currentTimeMillis().toString())

        val attendanceData = mapOf(
            "name" to studentName,
            "status" to finalStatus,
            "timeScanned" to currentTime,
            "lateDuration" to lateDuration,
            "totalOutsideTime" to 0
        )

        Log.d("QR_SCAN", "Writing to: /rooms/$roomId/sessions/$sessionId/attendance/$studentId")
        Log.d("QR_SCAN", attendanceData.toString())

        attendanceRef.setValue(attendanceData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Attendance marked: $finalStatus", Toast.LENGTH_LONG).show()
                Log.d("QR_SCAN", "Successfully marked attendance for $studentName")
                scanningEnabled = true
            }
            .addOnFailureListener { e ->
                Log.e("QR_SCAN", "Failed to mark attendance: ${e.message}")
                Toast.makeText(requireContext(), "Failed to mark attendance", Toast.LENGTH_SHORT).show()
                scanningEnabled = true
            }
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
        getCurrentLocation { location ->
            Log.d("QR_SCAN", "Warm-up GPS: ${location.latitude}, ${location.longitude}")
        }
    }

}
