// File: app/src/main/java/com/example/livepoints/UserTrackingActivity.kt
package com.example.livepoints

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.livepoints.Data.User
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlin.math.*

class UserTrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private lateinit var firestore: FirebaseFirestore
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var trackedUserId: String? = null
    private var trackedUserListener: ListenerRegistration? = null

    private var userMarker: Marker? = null
    private var currentUserLocation: Location? = null

    private val LOCATION_PERMISSION_REQUEST_CODE = 2

    private lateinit var tvDistance: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view to the user tracking layout
        setContentView(R.layout.activity_user_tracking)

        // Initialize Firestore
        firestore = FirebaseFirestore.getInstance()

        // Initialize Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Get tracked user ID from intent
        trackedUserId = intent.getStringExtra("userId")

        if (trackedUserId == null) {
            Toast.makeText(this, "Invalid User", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvDistance = findViewById(R.id.tvDistance)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapUserTracking) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Enable My Location layer if permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.isMyLocationEnabled = true
            getCurrentLocation()
        } else {
            // Request permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        // Listen to tracked user's location
        listenToTrackedUser()
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted
            return
        }
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentUserLocation = location
                }
            }
    }

    private fun listenToTrackedUser() {
        trackedUserId?.let { userId ->
            trackedUserListener = firestore.collection("users").document(userId)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Toast.makeText(this, "Error fetching user location: ${e.message}", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        val user = snapshot.toObject(User::class.java)
                        val userLat = user?.latitude ?: 0.0
                        val userLng = user?.longitude ?: 0.0
                        val userLocation = LatLng(userLat, userLng)

                        if (userMarker == null) {
                            userMarker = mMap.addMarker(
                                MarkerOptions()
                                    .position(userLocation)
                                    .title("${user?.firstName} ${user?.lastName}")
                                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                            )
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
                        } else {
                            userMarker?.position = userLocation
                        }

                        // Calculate distance
                        currentUserLocation?.let { currentLoc ->
                            val distance = calculateDistance(currentLoc.latitude, currentLoc.longitude, userLat, userLng)
                            tvDistance.text = "Distance: %.2f km".format(distance)
                        }
                    }
                }
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371 // km
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2.0)
        val c = 2 * asin(sqrt(a))
        return earthRadius * c
    }

    // Handle permission result
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray){
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == LOCATION_PERMISSION_REQUEST_CODE){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
                    mMap.isMyLocationEnabled = true
                    getCurrentLocation()
                }
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Location Permission Needed")
                    .setMessage("This feature requires location permission to function properly.")
                    .setPositiveButton("OK"){ _, _ -> }
                    .create()
                    .show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        trackedUserListener?.remove()
    }
}
