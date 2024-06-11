package com.foodknalledge.reci_app.ui.notifications

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.foodknalledge.reci_app.R
import com.foodknalledge.reci_app.SignInActivity
import com.google.firebase.auth.FirebaseAuth

class NotificationsFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_notifications, container, false)

        sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val logoutButton = rootView.findViewById<Button>(R.id.logoutButton)

        logoutButton.setOnClickListener {
            logoutUser()
        }

        return rootView
    }

    private fun logoutUser() {
        // Log out from Firebase
        FirebaseAuth.getInstance().signOut()

        // Remove token from SharedPreferences
        val editor = sharedPreferences.edit()
        editor.remove("user_token")
        editor.apply()

        // Show a toast message
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()

        // Redirect to SignInActivity
        val intent = Intent(requireContext(), SignInActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
