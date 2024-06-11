const express = require('express');
const admin = require('firebase-admin');
const app = express();
const multer = require('multer');
const PORT = process.env.PORT || 3000;

const path = require('path');
const serviceAccountPath = path.resolve(__dirname, 'serviceAccountKey.json');

// Initialize Firebase Admin SDK
const serviceAccount = require(serviceAccountPath);
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://console.firebase.google.com/u/0/project/reciapp-5cea0/firestore/databases/-default-/data/~2F'
});

// Firestore instance
const db = admin.firestore();

// Middleware to parse JSON bodies
app.use(express.json());

// Middleware to verify Firebase ID token
const verifyToken = async (req, res, next) => {
  const idToken = req.headers.authorization;
  if (!idToken) {
    return res.status(401).json({ error: 'Authorization header is required' });
  }

  try {
    const decodedToken = await admin.auth().verifyIdToken(idToken);
    req.uid = decodedToken.uid;
    next();
  } catch (error) {
    console.error('Error verifying ID token:', error);
    return res.status(401).json({ error: 'Invalid or expired token' });
  }
};

// POST endpoint for creating a new recipe
app.post('/api/recipes', verifyToken, async (req, res) => {
  try {
    const { recipeName, ingredients, steps, fullName, count } = req.body;
    const userId = req.uid;

    // Create new recipe document
    const recipeRef = await db.collection('recipes').add({
      userId, // Add userId to the recipe document
      recipeName,
      ingredients,
      steps,
      fullName,
      count
    });

    // Respond with success message and ID of the newly created recipe
    res.status(201).json({ message: 'Recipe created successfully', recipeId: recipeRef.id });
  } catch (error) {
    console.error('Error creating recipe:', error);
    res.status(500).json({ error: 'An unexpected error occurred' });
  }
});

// GET endpoint for fetching user's full name
app.get('/api/user/:userId/fullname', verifyToken, async (req, res) => {
  try {
    const userId = req.params.userId;
    const userSnapshot = await db.collection('users').doc(userId).get();
    const userData = userSnapshot.data();

    // Assuming 'fullName' field exists in the 'users' collection
    const fullName = userData.fullName || '';
    res.status(200).send(fullName);
  } catch (error) {
    console.error('Error fetching user full name:', error);
    res.status(500).json({ error: 'An unexpected error occurred' });
  }
});

// GET endpoint for fetching all recipes with user information
app.get('/api/recipes', verifyToken, async (req, res) => {
  try {
    const recipesSnapshot = await db.collection('recipes').get();
    const recipes = [];

    // Fetch user data for all recipes in a single batch
    const userIds = recipesSnapshot.docs.map(doc => doc.data().userId);

    // Iterate over each recipe document
    for (const doc of recipesSnapshot.docs) {
      const recipeData = doc.data();
      const userId = recipeData.userId;

      // Combine recipe data with user data
      const recipeWithUser = {
        recipeId: doc.id,
        recipeName: recipeData.recipeName,
        ingredients: recipeData.ingredients,
        steps: recipeData.steps,
        fullName: recipeData.fullName, // Check if userData exists
        count: recipeData.count // Include count field
      };

      recipes.push(recipeWithUser);
    }

    res.status(200).json(recipes);
  } catch (error) {
    console.error('Error fetching recipes:', error);
    res.status(500).json({ error: 'An unexpected error occurred' });
  }
});

// Start the server
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});
