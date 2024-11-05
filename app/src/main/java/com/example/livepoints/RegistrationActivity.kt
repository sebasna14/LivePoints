// File: app/src/main/java/com/example/userauthmapapp/RegistrationActivity.kt
package com.example.livepoints

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.livepoints.Data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class RegistrationActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 71
    private var imageUri: Uri? = null

    // Firebase instances
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    // UI elements
    private lateinit var etFirstName: EditText
    private lateinit var etLastName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etIdentificationNumber: EditText
    private lateinit var etLatitude: EditText
    private lateinit var etLongitude: EditText
    private lateinit var ivProfileImage: ImageView
    private lateinit var btnSelectImage: Button
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view to the registration layout
        setContentView(R.layout.activity_registration)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Initialize UI elements
        etFirstName = findViewById(R.id.etFirstName)
        etLastName = findViewById(R.id.etLastName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etIdentificationNumber = findViewById(R.id.etIdentificationNumber)
        etLatitude = findViewById(R.id.etLatitude)
        etLongitude = findViewById(R.id.etLongitude)
        ivProfileImage = findViewById(R.id.ivProfileImage)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)

        // Set up listeners
        btnSelectImage.setOnClickListener {
            chooseImage()
        }

        btnRegister.setOnClickListener {
            registerUser()
        }

        tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // Function to open the image selector
    private fun chooseImage() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST)
    }

    // Handle the result of image selection
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK){
            data?.data?.let { uri ->
                imageUri = uri
                ivProfileImage.setImageURI(uri)
            }
        }
    }

    // Function to register the user
    private fun registerUser() {
        val firstName = etFirstName.text.toString().trim()
        val lastName = etLastName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val identificationNumber = etIdentificationNumber.text.toString().trim()
        val latitudeStr = etLatitude.text.toString().trim()
        val longitudeStr = etLongitude.text.toString().trim()

        // Validate inputs
        if(firstName.isEmpty() || lastName.isEmpty() || email.isEmpty() || password.isEmpty() ||
            identificationNumber.isEmpty() || latitudeStr.isEmpty() || longitudeStr.isEmpty()){
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Parse latitude and longitude
        val latitude: Double
        val longitude: Double
        try {
            latitude = latitudeStr.toDouble()
            longitude = longitudeStr.toDouble()
        } catch (e: NumberFormatException) {
            Toast.makeText(this, "Invalid latitude or longitude", Toast.LENGTH_SHORT).show()
            return
        }

        if(imageUri == null){
            Toast.makeText(this, "Please select a profile image", Toast.LENGTH_SHORT).show()
            return
        }

        // Register with Firebase Auth
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if(task.isSuccessful){
                    val userId = auth.currentUser?.uid ?: ""
                    uploadImageAndSaveUser(userId, firstName, lastName, email, identificationNumber, latitude, longitude)
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // Upload image and save user data to Firestore
    private fun uploadImageAndSaveUser(userId: String, firstName: String, lastName: String, email: String,
                                       identificationNumber: String, latitude: Double, longitude: Double){
        val ref = storage.reference.child("profile_images/$userId.jpg")
        ref.putFile(imageUri!!)
            .addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { uri ->
                    val user = User(
                        uid = userId,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        imageUrl = uri.toString(),
                        identificationNumber = identificationNumber,
                        latitude = latitude,
                        longitude = longitude,
                        isAvailable = false
                    )
                    firestore.collection("users").document(userId)
                        .set(user)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                            // Start the UserStatusService
                            startService(Intent(this, UserStatusService::class.java))
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to save user: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Image Upload Failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}