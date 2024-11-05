// File: app/src/main/java/com/example/livepoints/MainActivity.kt
package com.example.livepoints

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.livepoints.Data.PointOfInterest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.json.JSONArray
import java.io.IOException

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private val LOCATION_PERMISSION_REQUEST_CODE = 1

    private val pointsOfInterest = mutableListOf<PointOfInterest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view to the main layout
        setContentView(R.layout.activity_main)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Load Points of Interest from JSON
        loadPointsOfInterest()
    }

    private fun loadPointsOfInterest(){
        try {
            val inputStream = assets.open("points_of_interest.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonString = String(buffer, Charsets.UTF_8)
            val jsonArray = JSONArray(jsonString)
            for(i in 0 until jsonArray.length()){
                val obj = jsonArray.getJSONObject(i)
                val poi = PointOfInterest(
                    name = obj.getString("name"),
                    latitude = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude")
                )
                pointsOfInterest.add(poi)
            }
        } catch (e: IOException){
            e.printStackTrace()
            Toast.makeText(this, "Failed to load points of interest", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Enable My Location layer if permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            mMap.isMyLocationEnabled = true
            getCurrentLocation()
        } else {
            // Request permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
        }

        // Add Points of Interest markers
        for(poi in pointsOfInterest){
            val location = LatLng(poi.latitude, poi.longitude)
            mMap.addMarker(
                MarkerOptions()
                    .position(location)
                    .title(poi.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            )
        }
    }

    private fun getCurrentLocation(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Permission not granted, request permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if(location != null){
                    val userLatLng = LatLng(location.latitude, location.longitude)
                    mMap.addMarker(
                        MarkerOptions()
                            .position(userLatLng)
                            .title("You are here")
                            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                    )
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 12f))

                    // Update user's location in Firestore
                    updateUserLocation(location.latitude, location.longitude)
                } else {
                    Toast.makeText(this, "Unable to fetch location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateUserLocation(latitude: Double, longitude: Double){
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .update("latitude", latitude, "longitude", longitude)
            .addOnSuccessListener {
                // Location updated successfully
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update location: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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
                    .setMessage("This app requires location permission to function properly.")
                    .setPositiveButton("OK"){ _, _ -> }
                    .create()
                    .show()
            }
        }
    }

    // Inflate menu
    override fun onCreateOptionsMenu(menu: android.view.Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // Handle menu item clicks
    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        when(item.itemId){
            R.id.menu_logout -> {
                auth.signOut()
                // Stop the UserStatusService
                stopService(Intent(this, UserStatusService::class.java))
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return true
            }
            R.id.menu_toggle_status -> {
                toggleAvailabilityStatus()
                return true
            }
            R.id.menu_view_users -> {
                startActivity(Intent(this, UsersListActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun toggleAvailabilityStatus(){
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if(document != null && document.exists()){
                    val currentStatus = document.getBoolean("isAvailable") ?: false
                    firestore.collection("users").document(userId)
                        .update("isAvailable", !currentStatus)
                        .addOnSuccessListener {
                            Toast.makeText(this, if(!currentStatus) "You are now Available" else "You are now Offline", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to update status: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch current status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
