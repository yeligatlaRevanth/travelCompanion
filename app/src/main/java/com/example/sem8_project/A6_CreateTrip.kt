package com.example.sem8_project

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class A6_CreateTrip : AppCompatActivity() {
    lateinit var edtLocation: EditText
    lateinit var edtDate: EditText
    lateinit var edtDays: EditText
    lateinit var edtDescription: EditText
    lateinit var iv_tripImage: ImageView
    lateinit var btnCreateTrip: Button
    lateinit var tv_iv: TextView

    // Firebase Storage reference
    private lateinit var storageReference: StorageReference
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_a6_create_trip)

        // Initialize views and storage reference
        edtLocation = findViewById(R.id.edt_location)
        edtDate = findViewById(R.id.edt_date)
        edtDays = findViewById(R.id.edt_nDays)
        edtDescription = findViewById(R.id.edt_desc)
        btnCreateTrip = findViewById(R.id.btn_create)
        iv_tripImage = findViewById(R.id.iv_tripImage)
        tv_iv = findViewById(R.id.tv_iv)
        storageReference = FirebaseStorage.getInstance().reference

        // Set click listener for ImageView
        iv_tripImage.setOnClickListener {
            setImageView()
        }

        // Set click listener for Create Trip button
        btnCreateTrip.setOnClickListener {
            createTrip()
        }
    }

    private fun setImageView() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        resultLauncher.launch(intent)
    }

    private var resultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                selectedImageUri = data?.data
                // Display the selected image in the ImageView
                selectedImageUri?.let { uri ->
                    iv_tripImage.setImageURI(uri)
                    iv_tripImage.alpha = 1f // Set opacity to 50%
                    tv_iv.visibility = View.INVISIBLE
                }
            }
        }

    private fun createTrip() {
        // Generate a unique trip ID
        val tripId = generateUniqueTripId()

        // Get trip details from EditTexts
        val location = edtLocation.text.toString()
        val date = edtDate.text.toString()
        val days = edtDays.text.toString()
        val description = edtDescription.text.toString()

        // Check if an image is selected
        if (selectedImageUri != null) {
            // Define the path where the image will be stored in Firebase Storage
            val imageRef = storageReference.child("trip_images/$tripId")

            // Upload the image to Firebase Storage
            imageRef.putFile(selectedImageUri!!)
                .addOnSuccessListener { taskSnapshot ->
                    // Image uploaded successfully
                    // Get the download URL of the uploaded image
                    imageRef.downloadUrl.addOnSuccessListener { imageUrl ->
                        // Store trip details along with the image URL in Firebase Database under the tripId
                        val databaseReference = FirebaseDatabase.getInstance().getReference("trips")
                        val tripData = hashMapOf(
                            "location" to location,
                            "date" to date,
                            "days" to days,
                            "description" to description,
                            "imageUrl" to imageUrl.toString() // Store image URL in the database
                            // Add more fields as needed
                        )
                        databaseReference.child(tripId).setValue(tripData)
                            .addOnSuccessListener {
                                // Trip details stored successfully
                                Toast.makeText(this, "Trip details stored successfully", Toast.LENGTH_SHORT).show()
                                val i = Intent(this, A4_UserDashBoard::class.java)
                                startActivity(i)
                            }
                            .addOnFailureListener { exception ->
                                // Handle any errors
                                Toast.makeText(this, "Failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    // Handle errors that occurred during the upload process
                    Toast.makeText(this, "Failed to upload image: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // No image selected, only store trip details in Firebase Database
            val databaseReference = FirebaseDatabase.getInstance().getReference("trips")
            val tripData = hashMapOf(
                "location" to location,
                "date" to date,
                "days" to days,
                "description" to description
                // Add more fields as needed
            )
            databaseReference.child(tripId).setValue(tripData)
                .addOnSuccessListener {
                    // Trip details stored successfully
                    Toast.makeText(this, "Trip details stored successfully", Toast.LENGTH_SHORT).show()
                    val i = Intent(this, A4_UserDashBoard::class.java)
                    startActivity(i)
                }
                .addOnFailureListener { exception ->
                    // Handle any errors
                    Toast.makeText(this, "Failed: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }


    // Method to generate a unique trip ID
    private fun generateUniqueTripId(): String {
        // Concatenate the currently signed-in user ID with a timestamp or any other unique identifier
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserId = currentUser?.uid ?: "unknown_user"
        val timestamp = System.currentTimeMillis()
        return "$currentUserId%_%$timestamp"
    }
}
