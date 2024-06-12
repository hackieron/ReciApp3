package com.foodknalledge.reci_app

import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.CheckBox
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

class RecipeListFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var recipeRecyclerView: RecyclerView
    private val TAG = "RecipeListFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recipe_list, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        recipeRecyclerView = rootView.findViewById(R.id.recipeRecyclerView)
        recipeRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val token = sharedPreferences.getString("user_token", null)
        if (token != null) {
            fetchRecipes(token)
        } else {
            Toast.makeText(requireContext(), "User token not found!", Toast.LENGTH_SHORT).show()
        }

        return rootView
    }

    private fun fetchRecipes(token: String) {
        val url = "https://reci-app-testing.vercel.app/api/recipes"
        val client = OkHttpClient()
        Log.i(TAG, "user kier TOKEN: $token")
        val request = Request.Builder()
            .url(url)
            .header("Authorization", token)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to fetch recipes", e)
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to fetch recipes", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    Log.i(TAG, "Recipes data: $responseData")
                    requireActivity().runOnUiThread {
                        try {
                            val recipes = JSONArray(responseData)
                            val recipeList = mutableListOf<Recipe>()
                            for (i in 0 until recipes.length()) {
                                val recipe = recipes.getJSONObject(i)
                                val recipeName = recipe.getString("recipeName")
                                val ingredients = jsonArrayToList(recipe.getJSONArray("ingredients"))
                                val steps = jsonArrayToList(recipe.getJSONArray("steps"))
                                val fullName = recipe.getString("fullName")
                                val recipeId = recipe.getString("recipeId")
                                // Access count object
                                val countObject = recipe.getJSONObject("count")
                                val likeCount = countObject.getInt("likeCount")
                                val commentCount = countObject.getInt("commentCount")
                                val shareCount = countObject.getInt("shareCount")
                                recipeList.add(Recipe(recipeId, recipeName, ingredients, steps, fullName, likeCount, commentCount, shareCount))
                            }
                            recipeRecyclerView.adapter = RecipeAdapter(recipeList, token)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to parse recipes", e)
                            Toast.makeText(requireContext(), "Failed to parse recipes", Toast.LENGTH_SHORT).show()
                        }
                    }

                } else {
                    Log.e(TAG, "Failed to fetch recipes: ${response.code}")
                    requireActivity().runOnUiThread {

                        Toast.makeText(requireContext(), "Failed to fetch recipes", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun jsonArrayToList(jsonArray: JSONArray): List<String> {
        val list = mutableListOf<String>()
        for (i in 0 until jsonArray.length()) {
            list.add(jsonArray.getString(i))
        }
        return list
    }
    data class Recipe(
        val id: String,
        val name: String,
        val ingredients: List<String>,
        val steps: List<String>,
        val fullName: String,
        val likeCount: Int = 0,
        val commentCount: Int = 0,
        val shareCount: Int = 0
    )

    class RecipeAdapter(
        private val recipeList: List<Recipe>,
        private val token: String
    ) : RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

        class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val recipeNameTextView: TextView = itemView.findViewById(R.id.recipeNameTextView)
            val fullNameTextView: TextView = itemView.findViewById(R.id.fullNameTextView)
            val ingredientsTextView: TextView = itemView.findViewById(R.id.ingredientsTextView)
            val stepsTextView: TextView = itemView.findViewById(R.id.stepsTextView)
            val likeButton: Button = itemView.findViewById(R.id.likeButton)
            val shareButton: Button = itemView.findViewById(R.id.shareButton)
            val commentButton: Button = itemView.findViewById(R.id.commentButton)
            val likeCountTextView: TextView = itemView.findViewById(R.id.likeCountTextView)
            val commentCountTextView: TextView = itemView.findViewById(R.id.commentCountTextView)
            val shareCountTextView: TextView = itemView.findViewById(R.id.shareCountTextView)
            val commentEditText: TextView = itemView.findViewById(R.id.commentEditText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.recipe_item, parent, false)
            return RecipeViewHolder(itemView)
        }
        val db = FirebaseFirestore.getInstance()
        override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
            val recipe = recipeList[position]
            holder.recipeNameTextView.text = recipe.name
            holder.fullNameTextView.text = recipe.fullName
            holder.ingredientsTextView.text = recipe.ingredients.joinToString(", ")
            holder.stepsTextView.text = recipe.steps.joinToString(", ")
            holder.likeCountTextView.text = "Likes: ${recipe.likeCount}"
            holder.commentCountTextView.text = "Comments: ${recipe.commentCount}"
            holder.shareCountTextView.text = "Shares: ${recipe.shareCount}"
            holder.likeButton.setOnClickListener {
                updateCount(recipe, "likeCount", 1, token) // Increment like count by 1
            }
            holder.commentButton.setOnClickListener {
                val commentText = holder.commentEditText.text.toString()
                if (commentText.isNotEmpty()) {
                    addComment(recipe.id, commentText, token)
                    Log.i(TAG, "Comment added successfully")
                    updateCount(recipe, "commentCount", 1, token)

                } else {
                    Toast.makeText(
                        holder.itemView.context,
                        "Comment cannot be empty",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            holder.shareButton.setOnClickListener {
                updateCount(recipe, "shareCount", 1, token) // Increment like count by 1
            }
            holder.itemView.setOnClickListener {
                // Open RecipeView activity when the CardView is clicked
                val intent = Intent(holder.itemView.context, RecipeView::class.java).apply {
                    putExtra("recipeId", recipe.id)
                    putExtra("fullName", recipe.fullName)
                    putExtra("recipeName", recipe.name)
                    putStringArrayListExtra("ingredients", ArrayList(recipe.ingredients))
                    putStringArrayListExtra("steps", ArrayList(recipe.steps))
                }
                holder.itemView.context.startActivity(intent)
            }
            db.collection("recipes").document(recipe.id)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.w(TAG, "Listen failed", e)
                        return@addSnapshotListener
                    }

                    if (snapshot != null && snapshot.exists()) {
                        // Update like count
                        val likeCount = snapshot.getLong("count.likeCount") ?: 0
                        holder.likeCountTextView.text = "Likes: $likeCount"

                        // Update comment count
                        val commentCount = snapshot.getLong("count.commentCount") ?: 0
                        holder.commentCountTextView.text = "Comments: $commentCount"

                        // Update share count
                        val shareCount = snapshot.getLong("count.shareCount") ?: 0
                        holder.shareCountTextView.text = "Shares: $shareCount"
                    } else {
                        Log.d(TAG, "Current data: null")
                    }
                }

        }

        override fun getItemCount(): Int {
            return recipeList.size
        }

        private fun updateCount(recipe: Recipe, countType: String, increment: Int, token: String) {
            val url = "https://reci-app-testing.vercel.app/api/recipes/${recipe.id}/$countType"
            val client = OkHttpClient()

            // Create a JSON object for the request body
            val requestBody = JSONObject().apply {
                put(countType, 1) // Increment count by the provided value
            }

            val request = Request.Builder()
                .url(url)
                .header("Authorization", token)
                .put(
                    requestBody.toString()
                        .toRequestBody("application/json".toMediaTypeOrNull())
                )
                .build()


            // Log the URL and request body
            Log.i(TAG, "PUT URL: $url")
            Log.i(TAG, "Request Body for $countType count: $requestBody")

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Failed to update $countType", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (!response.isSuccessful) {
                        Log.e(TAG, "Failed to update $countType: ${response.code}")
                        return
                    }

                    // Update was successful, log the updated count
                    try {
                        val responseData = response.body?.string()
                        val updatedCount = JSONObject(responseData).getInt(countType)
                        Log.i(TAG, "COUNT OF $countType for ${recipe.id} | after $countType: $updatedCount")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse $countType count after update", e)
                    }
                }
            })
        }



        private fun addComment(recipeId: String, commentText: String, token: String) {
            val url = "https://reci-app-testing.vercel.app/api/recipes/$recipeId/comments"
            val client = OkHttpClient()
            val requestBody = JSONObject().apply {
                put("commentText", commentText)
            }.toString().toRequestBody("application/json".toMediaTypeOrNull())

            val request = Request.Builder()
                .url(url)
                .header("Authorization", token)
                .post(requestBody)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Failed to add comment", e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        Log.i(TAG, "Comment added successfully")
                        // Update UI or fetch comments again
                    } else {
                        Log.e(TAG, "Failed to add comment: ${response.code}")
                    }
                }
            })
        }
    }

}
