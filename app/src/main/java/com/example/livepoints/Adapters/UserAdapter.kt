// File: app/src/main/java/com/example/livepoints/adapters/UserAdapter.kt
package com.example.livepoints.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.livepoints.R
import com.example.livepoints.Data.User
import com.squareup.picasso.Picasso

class UserAdapter(
    private val users: List<User>,
    private val onViewPositionClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    inner class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivUserImage: ImageView = view.findViewById(R.id.ivUserImage)
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val btnViewPosition: Button = view.findViewById(R.id.btnViewPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_user, parent, false)

        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.tvUserName.text = "${user.firstName} ${user.lastName}"

        // Load user image using Picasso or Glide
        Picasso.get()
            .load(user.imageUrl)
            .placeholder(R.drawable.ic_baseline_person_24)
            .into(holder.ivUserImage)

        holder.btnViewPosition.setOnClickListener {
            onViewPositionClick(user)
        }
    }

    override fun getItemCount(): Int = users.size
}
