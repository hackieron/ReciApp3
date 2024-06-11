package com.foodknalledge.reci_app

import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

class CreateRecipeFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private val TAG = "CreateRecipeFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.fragment_create_recipe, container, false)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = requireActivity().getSharedPreferences("user_prefs", Context.MODE_PRIVATE)

        val recipeNameEditText = rootView.findViewById<EditText>(R.id.recipeNameEditText)
        val ingredientsLayout = rootView.findViewById<LinearLayout>(R.id.ingredientsLayout)
        val stepsLayout = rootView.findViewById<LinearLayout>(R.id.stepsLayout)
        val addIngredientButton = rootView.findViewById<Button>(R.id.addIngredientButton)
        val addStepButton = rootView.findViewById<Button>(R.id.addStepButton)
        val createButton = rootView.findViewById<Button>(R.id.createButton)

        addIngredientButton.setOnClickListener {
            val ingredientEditText = EditText(requireContext())
            ingredientEditText.hint = "Ingredient"
            ingredientEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            ingredientsLayout.addView(ingredientEditText, ingredientsLayout.childCount - 1)
        }

        addStepButton.setOnClickListener {
            val stepEditText = EditText(requireContext())
            stepEditText.hint = "Step"
            stepEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            stepsLayout.addView(stepEditText, stepsLayout.childCount - 1)
        }

        createButton.setOnClickListener {
            val recipeName = recipeNameEditText.text.toString().trim()
            val ingredients = getEditTextValues(ingredientsLayout)
            val steps = getEditTextValues(stepsLayout)

            if (recipeName.isNotEmpty() && ingredients.isNotEmpty() && steps.isNotEmpty()) {
                val token = sharedPreferences.getString("user_token", null)
                if (token != null) {
                    createRecipe(recipeName, ingredients, steps, token)
                } else {
                    Toast.makeText(requireContext(), "User token not found!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please fill all fields!", Toast.LENGTH_SHORT).show()
            }
        }

        return rootView
    }

    private fun getEditTextValues(layout: LinearLayout): List<String> {
        val values = mutableListOf<String>()
        for (i in 0 until layout.childCount - 1) {
            val child = layout.getChildAt(i)
            if (child is EditText) {
                values.add(child.text.toString().trim())
            }
        }
        return values
    }
    // Modify the createRecipe function to include the user's full name
    private fun createRecipe(recipeName: String, ingredients: List<String>, steps: List<String>, token: String) {
        val url = "https://reci-app-testing.vercel.app/api/recipes"
        val client = OkHttpClient()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid ?: ""

        // Fetch the user's full name from the server
        fetchUserFullName(userId) { fullName ->
            val json = JSONObject().apply {
                put("recipeName", recipeName)
                put("ingredients", JSONArray(ingredients))
                put("steps", JSONArray(steps))
                put("fullName", fullName)
            }

            val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
            val request = Request.Builder()
                .url(url)
                .header("Authorization", token)
                .post(body)
                .build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    Log.e(TAG, "Failed to create recipe", e)
                    requireActivity().runOnUiThread {
                        Toast.makeText(requireContext(), "Failed to create recipe", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        Log.i(TAG, "Recipe created successfully")
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Recipe created successfully", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Log.e(TAG, "Failed to create recipe: ${response.code}")
                        requireActivity().runOnUiThread {
                            Toast.makeText(requireContext(), "Failed to create recipe", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            })
        }
    }

    // Function to fetch user's full name from the server
    private fun fetchUserFullName(userId: String, callback: (String) -> Unit) {
        val url = "https://reci-app-testing.vercel.app/api/user/${userId}/fullname" // Modify endpoint to fetch full name
        val client = OkHttpClient()

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to fetch user full name", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val fullName = response.body?.string() ?: ""
                callback(fullName)
            }
        })
    }


}