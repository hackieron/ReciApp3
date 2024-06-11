package com.foodknalledge.reci_app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import java.io.IOException

class RecipeListFragment : Fragment() {

    private lateinit var sharedPreferences: SharedPreferences
    private val TAG = "RecipeListFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_recipe_list, container, false)
        sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val recipeListLayout = rootView.findViewById<LinearLayout>(R.id.recipeListLayout)
        val token = sharedPreferences.getString("user_token", null)
        if (token != null) {
            fetchRecipes(token, recipeListLayout)
        } else {
            Toast.makeText(requireContext(), "User token not found!", Toast.LENGTH_SHORT).show()
        }

        return rootView
    }

    private fun fetchRecipes(token: String, recipeListLayout: LinearLayout) {
        val url = "https://reci-app-testing.vercel.app/api/recipes"
        val client = OkHttpClient()

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
                    requireActivity().runOnUiThread {
                        try {
                            val recipes = JSONArray(responseData)
                            for (i in 0 until recipes.length()) {
                                val recipe = recipes.getJSONObject(i)
                                val recipeName = recipe.getString("recipeName")
                                val ingredients = recipe.getJSONArray("ingredients").join(", ")
                                val steps = recipe.getJSONArray("steps").join(", ")

                                val recipeView = LayoutInflater.from(requireContext()).inflate(R.layout.recipe_item, recipeListLayout, false)
                                recipeView.findViewById<TextView>(R.id.recipeNameTextView).text = recipeName
                                recipeView.findViewById<TextView>(R.id.ingredientsTextView).text = ingredients
                                recipeView.findViewById<TextView>(R.id.stepsTextView).text = steps

                                recipeListLayout.addView(recipeView)
                            }
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
}
