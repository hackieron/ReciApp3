package com.foodknalledge.reci_app

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import okhttp3.*
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
                    requireActivity().runOnUiThread {
                        try {
                            val recipes = JSONArray(responseData)
                            val recipeList = mutableListOf<Recipe>()
                            for (i in 0 until recipes.length()) {
                                val recipe = recipes.getJSONObject(i)
                                val name = recipe.getString("fullName")
                                val recipeName = recipe.getString("recipeName")
                                val ingredients = jsonArrayToList(recipe.getJSONArray("ingredients"))
                                val steps = jsonArrayToList(recipe.getJSONArray("steps"))

                                recipeList.add(Recipe(recipeName, ingredients, steps, name))
                            }
                            recipeRecyclerView.adapter = RecipeAdapter(recipeList)
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
    data class Recipe(val name: String, val ingredients: List<String>, val steps: List<String>, val fullName: String)


    class RecipeAdapter(private val recipeList: List<Recipe>) :
        RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

        class RecipeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val fullNameTextView: TextView = itemView.findViewById(R.id.fullNameTextView)
            val recipeNameTextView: TextView = itemView.findViewById(R.id.recipeNameTextView)
            val ingredientsTextView: TextView = itemView.findViewById(R.id.ingredientsTextView)
            val stepsTextView: TextView = itemView.findViewById(R.id.stepsTextView)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
            val itemView = LayoutInflater.from(parent.context)
                .inflate(R.layout.recipe_item, parent, false)
            return RecipeViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
            val recipe = recipeList[position]
            holder.fullNameTextView.text = recipe.fullName
            holder.recipeNameTextView.text = recipe.name
            holder.ingredientsTextView.text = recipe.ingredients.joinToString(", ")
            holder.stepsTextView.text = recipe.steps.joinToString(", ")
        }

        override fun getItemCount(): Int {
            return recipeList.size
        }
    }
}
