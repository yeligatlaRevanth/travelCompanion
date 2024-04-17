package com.example.sem8_project

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.util.EventListener

class A7_TripPage : AppCompatActivity() {
    lateinit var tripId:String
    private lateinit var locationTextView: TextView
    private lateinit var createdByTextView: TextView
    private lateinit var descriptionTextView: TextView
    private lateinit var tripImageView: ImageView
    private lateinit var nDaysTextView:TextView
    private lateinit var dateTextView:TextView
    private lateinit var btnJoin: Button
    private lateinit var mUserId:String
    private lateinit var btnOpenMap: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_a7_trip_page)

        backBtn()
        locationTextView = findViewById(R.id.locationTextView)
        createdByTextView = findViewById(R.id.createdByTextView)
        descriptionTextView = findViewById(R.id.descriptionTextView)
        tripImageView = findViewById(R.id.tripImageView)
        nDaysTextView = findViewById(R.id.numberOfDaysTextView)
        dateTextView = findViewById(R.id.dateTextView)
        btnJoin = findViewById(R.id.btnJoin)
        btnOpenMap = findViewById(R.id.btnOpenMap)


        tripId = mytripId()
        loadTripDetails()
        btnOpenMap.setOnClickListener {
            openMap()
        }

        btnJoin.setOnClickListener {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                // Request the permission
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.SEND_SMS), PERMISSION_REQUEST_CODE)
            } else {
                // Permission already granted, proceed with sending SMS
                sendSmsToCreatedBy()
            }
        }
    }

    private fun openMap() {
        val tripRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId)
        tripRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val location = snapshot.child("location").value as? String ?: ""
                val geocoder = Geocoder(applicationContext)
                val lst = geocoder.getFromLocationName(location, 1)

                // Check if the location was found
                if (lst!!.isNotEmpty()) {
                    val lat = lst[0].latitude
                    val lng = lst[0].longitude

                    // Construct the URI with the query parameters for pinning the location
                    val uri = Uri.parse("geo:$lat,$lng?q=$lat,$lng($location)")

                    // Create an intent to view the map
                    val mapIntent = Intent(Intent.ACTION_VIEW, uri)
                    mapIntent.setPackage("com.google.android.apps.maps")
                    startActivity(mapIntent)

                } else {
                    // Handle case where location was not found
                    Toast.makeText(applicationContext, "Location not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle onCancelled
                Log.e("Firebase", "onCancelled", error.toException())
            }
        })
    }


    private fun sendSmsToCreatedBy() {
        val userRef = FirebaseDatabase.getInstance().getReference("Users").child(mUserId)
        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                val phoneNumber = userSnapshot.child("userNumber").value as? String ?: ""
                val message = "Hello, I'd like to join your trip. Can you provide me with more details?"

                // Open messaging app with pre-filled message
                val intent = Intent(Intent.ACTION_VIEW, Uri.fromParts("sms", "+91$phoneNumber", null))
                intent.putExtra("sms_body", message)
                startActivity(intent)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database error
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, proceed with sending SMS
                sendSmsToCreatedBy()
            } else {
                // Permission denied, inform the user
                Toast.makeText(this, "SMS permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 101
    }

    private fun mytripId() : String
    {
        val tId = intent.getStringExtra("tripId")
        return tId!!

    }
    fun backBtn()
    {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
    private fun loadTripDetails() {
        val tripRef = FirebaseDatabase.getInstance().getReference("trips").child(tripId)
        tripRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val location = dataSnapshot.child("location").value as? String ?: ""
                val userId = tripId.split("%_%").firstOrNull() ?: ""
                mUserId = userId
                val description = dataSnapshot.child("description").value as? String ?: ""
                val numberOfDays = dataSnapshot.child("days").value as? String ?: ""
                val date = dataSnapshot.child("date").value as? String ?: ""

                // Fetch username from Firebase using userId
                val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)
                userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(userSnapshot: DataSnapshot) {
                        val createdBy = userSnapshot.child("userName").value as? String ?: ""

                        // Set trip details to TextViews
                        locationTextView.text = "Location: $location"
                        createdByTextView.text = "Created by: $createdBy"
                        descriptionTextView.text = "Description: $description"
                        nDaysTextView.text = "Number of days: $numberOfDays"
                        dateTextView.text = "Date: $date"

                        // Load trip image from Firebase Storage using Glide
                        val imageRef = FirebaseStorage.getInstance().getReference("trip_images").child(tripId)
                        imageRef.downloadUrl.addOnSuccessListener { uri ->
                            Glide.with(this@A7_TripPage)
                                .load(uri)
                                .into(tripImageView)
                        }.addOnFailureListener { exception ->
                            // Handle image loading failure
                            // For example, you can set a placeholder image or display an error message
                            tripImageView.setImageResource(R.drawable.city_vector)
                        }
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Handle database error
                    }
                })
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle database error
            }
        })
    }
}