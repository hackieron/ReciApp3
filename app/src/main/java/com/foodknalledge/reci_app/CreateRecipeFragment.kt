package com.foodknalledge.reci_app

import android.app.Activity
import com.google.firebase.auth.FirebaseAuth
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.VideoView
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.Tasks
import com.google.firebase.storage.FirebaseStorage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage



class CreateRecipeFragment : Fragment() {

    companion object {
        private const val FILE_PICK_REQUEST_CODE = 123 // You can choose any value for the request code
    }

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private val TAG = "CreateRecipeFragment"
    private val files: MutableList<Uri> = mutableListOf()
    private lateinit var filePreviewLayout: LinearLayout

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
        val attachFilesButton = rootView.findViewById<Button>(R.id.attachFilesButton)
        filePreviewLayout = rootView.findViewById(R.id.filePreviewLayout)
        addIngredientButton.setOnClickListener {
            val ingredientEditText = EditText(requireContext())
            ingredientEditText.hint = "Ingredient"
            ingredientEditText.inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            ingredientsLayout.addView(ingredientEditText, ingredientsLayout.childCount - 1)
        }
        attachFilesButton.setOnClickListener {
            // Open a file picker dialog
            val intent = Intent(Intent.ACTION_GET_CONTENT)
            intent.type = "*/*"
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            startActivityForResult(intent, FILE_PICK_REQUEST_CODE)
            openFilePicker()
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
             // Pass an empty list for now

            if (recipeName.isNotEmpty() && ingredients.isNotEmpty() && steps.isNotEmpty()) {
                val token = sharedPreferences.getString("user_token", null)
                if (token != null) {
                    createRecipe(recipeName, ingredients, steps, files, token)
                    Log.d(TAG, "called recipe create function")
                } else {
                    Toast.makeText(requireContext(), "User token not found!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "Please fill all fields!", Toast.LENGTH_SHORT).show()
            }
        }


        return rootView

    }
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
        intent.type = "*/*"
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(intent, FILE_PICK_REQUEST_CODE)
    }
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == FILE_PICK_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val selectedFiles: MutableList<Uri> = mutableListOf()
            data?.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    val uri = clipData.getItemAt(i).uri
                    selectedFiles.add(uri)
                }
            }
            data?.data?.let { uri ->
                selectedFiles.add(uri)
            }

            // Update the files list with selected URIs
            files.clear()
            files.addAll(selectedFiles)
            displayFilePreviews()
        }
    }
    private fun displayFilePreviews() {
        filePreviewLayout.removeAllViews() // Clear previous previews

        files.forEach { uri ->
            val fileName = getFileName(requireContext(), uri)
            val previewView = createFilePreviewView(fileName, uri)
            filePreviewLayout.addView(previewView)
        }

        // Show the file preview layout
        filePreviewLayout.visibility = View.VISIBLE
    }

    private fun createFilePreviewView(fileName: String, uri: Uri): View {
        val previewView = when {
            isImageFile(uri) -> createImageView(uri)
            isVideoFile(uri) -> createVideoView(uri)
            else -> createDefaultPreviewView(fileName)
        }

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 0, resources.getDimensionPixelSize(R.dimen.file_preview_margin))
        previewView.layoutParams = layoutParams

        return previewView
    }

    private fun isImageFile(uri: Uri): Boolean {
        val mimeType = requireContext().contentResolver.getType(uri)
        return mimeType?.startsWith("image/") == true
    }

    private fun isVideoFile(uri: Uri): Boolean {
        val mimeType = requireContext().contentResolver.getType(uri)
        return mimeType?.startsWith("video/") == true
    }

    private fun createImageView(uri: Uri): View {
        val imageView = ImageView(requireContext())
        imageView.setImageURI(uri)
        return imageView
    }

    private fun createVideoView(uri: Uri): View {
        val videoView = VideoView(requireContext())
        videoView.setVideoURI(uri)
        videoView.start()
        return videoView
    }

    private fun createDefaultPreviewView(fileName: String): View {
        val textView = TextView(requireContext())
        textView.text = fileName
        return textView
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

    // Modify the createRecipe function to include the user's full name and counts
    private fun createRecipe(recipeName: String, ingredients: List<String>, steps: List<String>, files: List<Uri>, token: String) {
        val storageRef = Firebase.storage.reference.child("files/recipes/$recipeName") // Create a folder with the recipe name"recipeName) // Create a folder with the recipe name
        val url = "https://reci-app-testing.vercel.app/api/recipes"
        val client = OkHttpClient()

        val fileUploadTasks = files.map { uri ->
            val fileName = getFileName(requireContext(), uri)
            val fileRef = storageRef.child(fileName) // Upload to the folder with the original file name
            fileRef.putFile(uri)
        }

        Tasks.whenAllComplete(fileUploadTasks)
            .addOnSuccessListener { results ->
                val downloadUrls = mutableListOf<String>()
                val uploadedFileNames = mutableListOf<String>()
                results.forEachIndexed { index, taskResult ->
                    if (taskResult.isSuccessful) {
                        val fileRef = storageRef.child(getFileName(requireContext(), files[index]))
                        fileRef.downloadUrl.addOnSuccessListener { uri ->
                            val downloadUrl = uri.toString()
                            downloadUrls.add(downloadUrl)
                            uploadedFileNames.add(getFileName(requireContext(), files[index]))
                            if (downloadUrls.size == files.size) {
                                Log.d(TAG, "FILE NAMES OF UPLOADED FILES: $uploadedFileNames")
                                // Proceed with creating the recipe document
                                val currentUser = FirebaseAuth.getInstance().currentUser
                                val userId = currentUser?.uid ?: ""
                                fetchUserFullName(userId) { fullName ->
                                    val json = JSONObject().apply {
                                        put("recipeName", recipeName)
                                        put("ingredients", JSONArray(ingredients))
                                        put("steps", JSONArray(steps))
                                        put("fullName", fullName)
                                        put("files", JSONArray(uploadedFileNames)) // Update wit/h download URLs
                                        put("count", JSONObject().apply {
                                            put("likeCount", 0)
                                            put("commentCount", 0)
                                            put("shareCount", 0)
                                        })
                                    }

                                    val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
                                    val request = Request.Builder()
                                        .url(url)
                                        .header("Authorization", token)
                                        .post(body)
                                        .build()

                                    client.newCall(request).enqueue(object : Callback {
                                        override fun onFailure(call: Call, e: IOException) {
                                            Log.e(TAG, "Failed to make the request", e)
                                            requireActivity().runOnUiThread {
                                                Toast.makeText(requireContext(), "Failed to make the request", Toast.LENGTH_SHORT).show()
                                            }
                                        }

                                        override fun onResponse(call: Call, response: Response) {
                                            val responseBody = response.body?.string()
                                            if (response.isSuccessful && responseBody != null) {
                                                requireActivity().runOnUiThread {
                                                    Log.i(TAG, "Request successful: $responseBody")
                                                    Toast.makeText(requireContext(), "Request successful", Toast.LENGTH_SHORT).show()
                                                }
                                            } else {
                                                requireActivity().runOnUiThread {
                                                    Toast.makeText(requireContext(), "Request unsuccessful", Toast.LENGTH_SHORT).show()
                                                }
                                                Log.e(TAG, "Request unsuccessful: ${response.code}")
                                            }
                                        }
                                    })
                                }
                            }
                        }.addOnFailureListener { exception ->
                            Log.e(TAG, "Failed to get download URL for file", exception)
                        }
                    } else {
                        Log.e(TAG, "Failed to upload file: ${taskResult.exception?.message}")
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to upload files", exception)
                Toast.makeText(requireContext(), "Failed to upload files", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        // Get the file name from the URI
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val fileNameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (fileNameIndex != -1) {
                    return it.getString(fileNameIndex)
                }
            }
        }
        // If file name not found in the cursor, use a default name
        return "file_${System.currentTimeMillis()}"
    }


    // Function to fetch user's full name from the server
    private fun fetchUserFullName(userId: String, callback: (String) -> Unit) {
        val url = "https://reci-app-testing.vercel.app/api/user/${userId}/fullname"
        val client = OkHttpClient()
        val token = sharedPreferences.getString("user_token", null)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", token.toString()) // Add the authorization header here
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
