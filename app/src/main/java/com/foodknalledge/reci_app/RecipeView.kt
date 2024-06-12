package com.foodknalledge.reci_app

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class RecipeView : AppCompatActivity() {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recipe_view)

        // Enable edge-to-edge display
        enableEdgeToEdge()

        // Populate recipe details
        val recipeId = intent.getStringExtra("recipeId")
        val fullName = intent.getStringExtra("fullName")
        val recipeName = intent.getStringExtra("recipeName")
        val ingredients = intent.getStringArrayListExtra("ingredients")

        val steps = intent.getStringArrayListExtra("steps")

        val fullNameTextView = findViewById<TextView>(R.id.fullNameTextView)
        val recipeNameTextView = findViewById<TextView>(R.id.recipeNameTextView)
        val ingredientsTextView = findViewById<TextView>(R.id.ingredientsTextView)
        val stepsTextView = findViewById<TextView>(R.id.stepsTextView)
        val commentsRecyclerView = findViewById<RecyclerView>(R.id.commentsRecyclerView)


        fullNameTextView.text = "Full Name: $fullName"
        recipeNameTextView.text = "Recipe Name: $recipeName"
        ingredientsTextView.text = "Ingredients: ${ingredients?.joinToString(", ")}"
        stepsTextView.text = "Steps: ${steps?.joinToString(", ")}"

        // Initialize and set up RecyclerView for comments
        val layoutManager = LinearLayoutManager(this)
        commentsRecyclerView.layoutManager = layoutManager

        // Query Firestore for comments with the same recipeId
        firestore.collection("comments")
            .whereEqualTo("recipeId", recipeId)
            .get()
            .addOnSuccessListener { documents ->
                val commentsList = mutableListOf<Comment>()
                for (document in documents) {
                    val fullName = document.getString("fullName") ?: ""
                    val commentText = document.getString("commentText") ?: ""
                    val timestamp = document.getTimestamp("timestamp")
                    val timestampString = timestamp?.toDate()?.toString() ?: ""
                    commentsList.add(Comment(fullName, commentText, timestampString))
                }
                // Set up adapter for comments RecyclerView
                val commentsAdapter = CommentsAdapter(commentsList)
                commentsRecyclerView.adapter = commentsAdapter
            }
            .addOnFailureListener { exception ->
                // Handle errors here
                // For example, you can display a toast message
                // Toast.makeText(this, "Failed to fetch comments: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }
    data class Comment(
        val fullName: String,
        val commentText: String,
        val timestampString: String // Assuming timestamp is a string for simplicity
    )

}
