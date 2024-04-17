package com.example.sem8_project

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class A5_UserProfile : AppCompatActivity() {
    lateinit var edt_uname: EditText
    lateinit var edt_age: EditText
    lateinit var edt_phoneNum: EditText
    lateinit var edt_loc: EditText
    lateinit var edt_email:EditText
    lateinit var edt_trips: EditText
    lateinit var btn_updateUser: Button

    lateinit var uID: String
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_a5_user_profile)

        edt_uname = findViewById(R.id.edt_c_uname)
        edt_age = findViewById(R.id.edt_c_age)
        edt_phoneNum = findViewById(R.id.edt_c_phoneNum)
        edt_loc = findViewById(R.id.edt_c_loc)
        btn_updateUser = findViewById(R.id.btn_updateUser)
        edt_email = findViewById(R.id.edt_c_email)
        edt_trips = findViewById(R.id.edt_c_trips)

        getCurrentUser()
        showUserData()



        btn_updateUser.setOnClickListener {
            val uName = edt_uname.text.toString()
            val uAge = edt_age.text.toString()
            val uNum = edt_phoneNum.text.toString()
            val uLoc = edt_loc.text.toString()


            // Call the updateUserData function with the provided values
            updateUserData(
                newUsername = uName,
                newUserNumber = uNum,
                newUserLoc = uLoc,
                newUserAge = uAge
            )
        }

    }

    fun showUserData() {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(uID)
        databaseReference.get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                val userData = dataSnapshot.value as? Map<String, Any>
                userData?.let { data ->
                    // Extract user data from the dataSnapshot
                    val username = data["userName"] as? String

                    val age = data["userAge"] as? String

                    val phoneNumber = data["userNumber"] as? String
                    val location = data["userLoc"] as? String
                    val email = data["userEmail"] as? String

                    val trips = data["numTrips"] as? String

                    Log.d("age", age.toString())
                    // Update EditText fields with the fetched data
                    edt_uname.setText(username)
                    edt_age.setText(age)
                    edt_email.setText(email)
                    edt_trips.setText(trips)
                    edt_phoneNum.setText(phoneNumber)
                    edt_loc.setText(location)

                }
            } else {
                Log.d(TAG, "No data found for user: $uID")
            }
        }.addOnFailureListener { e ->
            Log.e(TAG, "Error fetching user data", e)
        }
    }


    fun getCurrentUser()
    {
        val mAuth = FirebaseAuth.getInstance()
        val currentUser = mAuth.currentUser

        currentUser?.let { user ->
            uID = user.uid
            // Now you have the user ID (UID)
            Log.d(TAG, "User ID: $uID")
        } ?: run {
            // User is not signed in
            Log.d(TAG, "No user is currently signed in")
        }

    }

    fun updateUserData(
        newUsername: String,
        newUserNumber: String,
        newUserLoc: String,
        newUserAge: String
    ) {
        val databaseReference = FirebaseDatabase.getInstance().getReference("Users").child(uID)

        // Create a map to hold the updates you want to make
        val updates = HashMap<String, Any>()
        updates["userName"] = newUsername
        updates["userNumber"] = newUserNumber
        updates["userLoc"] = newUserLoc
        updates["userAge"] = newUserAge

        // Update the data in the database
        databaseReference.updateChildren(updates)
            .addOnSuccessListener {
                // Data successfully updated
                Log.d(TAG, "Data updated successfully")
                val i = Intent(this, A4_UserDashBoard::class.java)
                startActivity(i)
            }
            .addOnFailureListener { e ->
                // Failed to update data
                Log.e(TAG, "Error updating data", e)
            }
    }


}