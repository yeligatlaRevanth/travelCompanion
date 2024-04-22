package com.example.sem8_project

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class A8_UserSteps : AppCompatActivity(), SensorEventListener {
    private lateinit var tv_uname: TextView
    private lateinit var tv_date: TextView
    private lateinit var lv_user: ListView

    private var sensorManager: SensorManager? = null

    // Creating a variable which will give the running status
    // and initially given the boolean value as false
    private var running = false

    // Creating a variable which will counts total steps
    // and it has been given the value of 0 float
    private var totalSteps = 0f

    // Creating a variable  which counts previous total
    // steps and it has also been given the value of 0 float
    private var previousTotalSteps = 0f
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if step counter sensor is available
        if (isStepCounterSensorAvailable()) {
            setContentView(R.layout.activity_a8_user_steps)
            initializeVariables()
            showUserData()
            // Adding a context of SENSOR_SERVICE as Sensor Manager
            sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        } else {
            // Step counter sensor is not available, navigate back to another activity
            navigateToAnotherActivity()
        }
    }

    private fun initializeVariables()
    {
        tv_uname = findViewById(R.id.usernameTextView)
        tv_date = findViewById(R.id.dateTextView)
        lv_user = findViewById(R.id.listView)
    }
    private fun showUserData()
    {
        setUserName()
        setTodaysDate()
        loadData()
        resetSteps()

    }


    override fun onResume() {
        super.onResume()
        running = true

        // Returns the number of steps taken by the user since the last reboot while activated
        // This sensor requires permission android.permission.ACTIVITY_RECOGNITION.
        // So don't forget to add the following permission in AndroidManifest.xml present in manifest folder of the app.
        val stepSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)


        if (stepSensor == null) {
            // This will give a toast message to the user if there is no sensor in the device
            Toast.makeText(this, "No sensor detected on this device", Toast.LENGTH_SHORT).show()
        } else {
            // Rate suitable for the user interface
            sensorManager?.registerListener(this, stepSensor, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {

        // Calling the TextView that we made in activity_main.xml
        // by the id given to that TextView
        var tv_stepsTaken = findViewById<TextView>(R.id.tv_stepsTaken)

        if (running) {
            totalSteps = event!!.values[0]

            // Current steps are calculated by taking the difference of total steps
            // and previous steps
            val currentSteps = totalSteps.toInt() - previousTotalSteps.toInt()

            // It will show the current steps to the user
            tv_stepsTaken.text = ("$currentSteps")
        }
    }

    fun resetSteps() {
        var tv_stepsTaken = findViewById<TextView>(R.id.tv_stepsTaken)
        tv_stepsTaken.setOnClickListener {
            // This will give a toast message if the user want to reset the steps
            Toast.makeText(this, "Long tap to reset steps", Toast.LENGTH_SHORT).show()
        }

        tv_stepsTaken.setOnLongClickListener {

            previousTotalSteps = totalSteps

            // When the user will click long tap on the screen,
            // the steps will be reset to 0
            tv_stepsTaken.text = 0.toString()

            // This will save the data
            saveData()

            true
        }
    }

    private fun saveData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val currentDate = getCurrentDate()

            val userStepsRef = FirebaseDatabase.getInstance().getReference("UserSteps").child(userId).child(currentDate)

            userStepsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val existingSteps = dataSnapshot.getValue(Float::class.java) ?: 0f
                    val currentSteps = totalSteps - previousTotalSteps

                    // If there are already steps recorded for the current date, update them
                    // Otherwise, set the steps for the current date
                    userStepsRef.setValue(existingSteps + currentSteps)
                        .addOnSuccessListener {
                            Toast.makeText(this@A8_UserSteps, "Steps saved successfully", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(this@A8_UserSteps, "Failed to save steps: ${exception.message}", Toast.LENGTH_SHORT).show()
                        }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle errors
                    Toast.makeText(this@A8_UserSteps, "Failed to save steps: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }


    private fun loadData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val userStepsRef = FirebaseDatabase.getInstance().getReference("UserSteps").child(userId)

            userStepsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val stepsList = mutableListOf<StepsData>()
                    for (dateSnapshot in dataSnapshot.children) {
                        val date = dateSnapshot.key
                        val steps = dateSnapshot.getValue(Float::class.java) ?: 0f
                        if (date != null) {
                            stepsList.add(StepsData(date, steps))
                        }
                    }
                    displayStepsData(stepsList)
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle errors
                    Toast.makeText(this@A8_UserSteps, "Failed to load steps data: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }

    private fun displayStepsData(stepsList: List<StepsData>) {
        val adapter = StepsDataAdapter(this, stepsList)
        lv_user.adapter = adapter
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // We do not have to write anything in this function for this app
    }
    private fun setTodaysDate() {
        val currentDate = getCurrentDate()
        tv_date.text = currentDate
    }

    private fun getCurrentDate(): String {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        return dateFormat.format(Date())
    }
    private fun setUserName()
    {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val userName = dataSnapshot.child("userName").value as? String
                    tv_uname.text = userName ?: "No Username Available"
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle errors
                    Toast.makeText(this@A8_UserSteps, "Failed to load user data: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }
    }
    private fun isStepCounterSensorAvailable(): Boolean {
        // Get the sensor manager
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        // Check if the step counter sensor is available
        val stepCounterSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        return stepCounterSensor != null
    }

    private fun navigateToAnotherActivity() {
        // Navigate back to another activity
        // For example, navigate back to the previous activity
        Toast.makeText(this, "Required Sensor Not Available", Toast.LENGTH_SHORT).show()
        val i = Intent(this, A4_UserDashBoard::class.java)
        startActivity(i)
    }
}
