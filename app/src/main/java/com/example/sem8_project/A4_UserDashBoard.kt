package com.example.sem8_project

import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import java.lang.Math.abs


class A4_UserDashBoard : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var mDrawer: DrawerLayout? = null
    private var toolbar: Toolbar? = null
    lateinit var drawerToggle: ActionBarDrawerToggle
    lateinit var tv_User: TextView

    private lateinit var navMenuImageProfile: ImageView
    private lateinit var navMenuNameText: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var tripAdapter: A4_C_RecyclerViewAdapter



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_a4_user_dash_board)
        tripAdapter = A4_C_RecyclerViewAdapter(emptyList())

        tv_User = findViewById(R.id.tv_user)
        //Setup Hamburger Icon
        //Nav Menu Items - Create Trip Alert,Previous Trips, User Profile, Logout
        setHamburgerIcon()



        val nvDrawer: NavigationView = findViewById(R.id.nvView)
        setupDrawerContent(nvDrawer)
        val headerView = nvDrawer.getHeaderView(0)
        navMenuImageProfile = headerView.findViewById(R.id.navmenu_ImageProfile)
        navMenuNameText = headerView.findViewById(R.id.navmenu_nameview)

        loadUserProfileImage()
        loadUserName()
        setTrips()

    }
    private fun loadUserName() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val userRef = FirebaseDatabase.getInstance().getReference("Users").child(userId)

            userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val userName = dataSnapshot.child("userName").getValue(String::class.java)
                    // Set the user name to the TextView
                    navMenuNameText.text = userName
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    // Handle errors
                    Log.e(TAG, "Failed to load user name: ${databaseError.message}")
                }
            })
        }
    }
    private fun loadUserProfileImage() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val userImageRef = FirebaseStorage.getInstance().getReference("UserImages")

            userImageRef.listAll().addOnSuccessListener { result ->
                result.items.firstOrNull { imageRef ->
                    val parts = imageRef.name.split("%_%")
                    parts.size == 2 && parts[0] == userId
                }?.downloadUrl?.addOnSuccessListener { uri ->
                    // Load the image into ImageView
                    Glide.with(this@A4_UserDashBoard).load(uri).into(navMenuImageProfile)
                } ?: run {
                    // Handle no matching image found
                    Log.e(TAG, "No profile image found for user: $userId")
                    navMenuImageProfile.setImageURI(Uri.parse("R.id.imageProfile"))
                }
            }.addOnFailureListener { exception ->
                // Handle errors
                Log.e(TAG, "Failed to fetch user profile image: ${exception.message}")
            }
        }
    }



    fun setTrips() {
        val tripsList = mutableListOf<A4_C_Trip_Details>()

        // Reference to the "trips" node in Firebase
        val tripsRef = FirebaseDatabase.getInstance().getReference("trips")

        // Listener to fetch trip details from Firebase
        tripsRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Loop through each trip
                dataSnapshot.children.forEach { tripSnapshot ->
                    // Get trip ID
                    val tripId = tripSnapshot.key ?: ""

                    // Split the trip ID to get user ID and timestamp
                    val parts = tripId.split("%_%")
                    if (parts.size == 2) {
                        val userId = parts[0]

                        // Fetch user details from Firebase using user ID
                        val userRef = FirebaseDatabase.getInstance().getReference("Users/").child(userId)
                        userRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(userSnapshot: DataSnapshot) {
                                val username = userSnapshot.child("userName").value as? String ?: ""

                                // Fetch other trip details
                                val location = tripSnapshot.child("location").value as? String ?: ""
                                val imageRef = FirebaseStorage.getInstance().getReference("trip_images/").child(tripId)
                                imageRef.metadata.addOnSuccessListener {
                                    // Image file exists
                                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                                        // Add the trip details to the list
                                        val trip = A4_C_Trip_Details(location, username, uri.toString(), tripId)
                                        tripsList.add(trip)

                                        // Set up RecyclerView after fetching all trip details
                                        setupRecyclerView(tripsList)
                                    }.addOnFailureListener { exception ->
                                        // Handle errors
                                        Toast.makeText(this@A4_UserDashBoard, "Failed to fetch image: ${exception.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }.addOnFailureListener { exception ->
                                    // Handle errors
                                    Log.d("MyTest", tripId)
                                    Toast.makeText(this@A4_UserDashBoard, "Image file not found: ${exception.message}", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                // Handle errors
                                Toast.makeText(this@A4_UserDashBoard, "Failed to fetch user details: ${databaseError.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle errors
                Toast.makeText(this@A4_UserDashBoard, "Failed to fetch trips: ${databaseError.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupRecyclerView(tripsList: List<A4_C_Trip_Details>) {
        recyclerView = findViewById(R.id.rv_trips)
        recyclerView.layoutManager = LinearLayoutManager(this)
        tripAdapter = A4_C_RecyclerViewAdapter(tripsList)
        recyclerView.adapter = tripAdapter
    }


    fun setHamburgerIcon()
    {
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.ic_menu)
        mDrawer = findViewById(R.id.drawer_layout)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            android.R.id.home ->{
                mDrawer?.openDrawer(GravityCompat.START)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        selectDrawerItem(item)
        return true
    }
    fun setupDrawerContent(navigationView: NavigationView) {
        navigationView.setNavigationItemSelectedListener { menuItem ->
            selectDrawerItem(menuItem)
            true
        }
    }

    fun selectDrawerItem(menuItem: MenuItem)
    {
        when(menuItem.itemId){
            R.id.nav_createTrip -> {
                val i = Intent(this, A6_CreateTrip::class.java)
                startActivity(i)
            }
            R.id.nav_userProf ->{
                val i = Intent(this, A5_UserProfile::class.java)
                startActivity(i)

            }
            R.id.nav_logout->{
                showLogoutConfirmationDialog()
            }
            R.id.nav_steps->{
                val i = Intent(this, A8_UserSteps::class.java)
                startActivity(i)
            }
        }
        mDrawer?.closeDrawers()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { dialog, which ->
                // Logout user
                FirebaseAuth.getInstance().signOut()
                // Redirect to login or splash screen
                // For example:
                val intent = Intent(this, A3_LoginActivity::class.java)
                startActivity(intent)
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }



}