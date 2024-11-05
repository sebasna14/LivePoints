// File: app/src/main/java/com/example/livepoints/UsersListActivity.kt
package com.example.livepoints

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.livepoints.Adapters.UserAdapter
import com.example.livepoints.Data.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class UsersListActivity : AppCompatActivity() {

    private lateinit var firestore: FirebaseFirestore
    private val availableUsers = mutableListOf<User>()
    private var listenerRegistration: ListenerRegistration? = null

    private lateinit var rvUsers: androidx.recyclerview.widget.RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Set the content view to the users list layout
        setContentView(R.layout.activity_user_list)

        firestore = FirebaseFirestore.getInstance()

        rvUsers = findViewById(R.id.rvUsers)
        rvUsers.layoutManager = LinearLayoutManager(this)
        rvUsers.adapter = UserAdapter(availableUsers) { user ->
            // Handle view position click
            val intent = Intent(this, UserTrackingActivity::class.java)
            intent.putExtra("userId", user.uid)
            startActivity(intent)
        }

        fetchAvailableUsers()
    }

    private fun fetchAvailableUsers(){
        listenerRegistration = firestore.collection("users")
            .whereEqualTo("isAvailable", true)
            .addSnapshotListener { snapshots, e ->
                if(e != null){
                    Toast.makeText(this, "Error fetching users: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                availableUsers.clear()
                for(doc in snapshots!!){
                    val user = doc.toObject(User::class.java)
                    availableUsers.add(user)
                }
                rvUsers.adapter?.notifyDataSetChanged()
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        listenerRegistration?.remove()
    }
}
