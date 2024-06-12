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

const storage = multer.memoryStorage(); // Use memory storage for Firebase

const fileFilter = (req, file, cb) => {
  // Accept only image and video files
  if (file.mimetype.startsWith('image/') || file.mimetype.startsWith('video/')) {
    cb(null, true);
  } else {
    cb(new Error('Only image and video files are allowed!'), false);
  }
};



const upload = multer({ storage: storage, fileFilter: fileFilter });

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


app.post('/api/recipes', verifyToken, upload.array('files', 5), async (req, res) => {
  try {
    const { recipeName, ingredients, steps, fullName } = req.body;
    const userId = req.uid;

    const fileUploadPromises = req.files && req.files.length > 0
      ? req.files.map((file, index) => {
          const fileRef = admin.storage().bucket().file(`files/${recipeName}_${file.originalname}`);
          const options = {
            gzip: true,
            metadata: {
              contentType: file.mimetype
            }
          };

          return fileRef.save(file.buffer, options);
        })
      : [];

    // Wait for all file uploads to complete
    const uploadedFiles = await Promise.all(fileUploadPromises);

    // Get download URLs of uploaded files

    const recipeRef = await db.collection('recipes').add({
      userId,
      recipeName,
      ingredients,
      steps,
      fullName,
      files, // Save download URLs in a new field
      count: {
        likeCount: 0,
        commentCount: 0,
        shareCount: 0
      }
    });

    // Respond with success message and ID of the newly created recipe
    res.status(201).json({
      recipeId: recipeRef.id,
      recipeName,
      ingredients,
      steps,
      fullName,
      files, // Return download URLs in the response
      count: {
        likeCount: 0,
        commentCount: 0,
        shareCount: 0
      }
    });
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

// PUT endpoint for updating like count
app.put('/api/recipes/:recipeId/likeCount', verifyToken, async (req, res) => {
  try {
    const recipeId = req.params.recipeId;
    const { likeCount } = req.body;

    const recipeRef = db.collection('recipes').doc(recipeId);
    const recipeSnapshot = await recipeRef.get();

    if (!recipeSnapshot.exists) {
      return res.status(404).json({ error: 'Recipe not found' });
    }

    // Increment the likeCount field
    await recipeRef.update({
      'count.likeCount': admin.firestore.FieldValue.increment(likeCount)
    });

    res.status(200).json({ message: 'Like count updated successfully' });
  } catch (error) {
    console.error('Error updating like count:', error);
    res.status(500).json({ error: 'An unexpected error occurred' });
  }
});

// PUT endpoint for updating comment count
app.put('/api/recipes/:recipeId/commentCount', verifyToken, async (req, res) => {
  try {
    const recipeId = req.params.recipeId;
    const { commentCount } = req.body;

    const recipeRef = db.collection('recipes').doc(recipeId);
    const recipeSnapshot = await recipeRef.get();

    if (!recipeSnapshot.exists) {
      return res.status(404).json({ error: 'Recipe not found' });
    }

    // Increment the commentCount field
    await recipeRef.update({
      'count.commentCount': admin.firestore.FieldValue.increment(commentCount)
    });

    res.status(200).json({ message: 'Comment count updated successfully' });
  } catch (error) {
    console.error('Error updating comment count:', error);
    res.status(500).json({ error: 'An unexpected error occurred' });
  }
});

// PUT endpoint for updating share count
app.put('/api/recipes/:recipeId/shareCount', verifyToken, async (req, res) => {
  try {
    const recipeId = req.params.recipeId;
    const { shareCount } = req.body;

    const recipeRef = db.collection('recipes').doc(recipeId);
    const recipeSnapshot = await recipeRef.get();

    if (!recipeSnapshot.exists) {
      return res.status(404).json({ error: 'Recipe not found' });
    }

    // Increment the shareCount field
    await recipeRef.update({
      'count.shareCount': admin.firestore.FieldValue.increment(shareCount)
    });

    res.status(200).json({ message: 'Share count updated successfully' });
  } catch (error) {
    console.error('Error updating share count:', error);
    res.status(500).json({ error: 'An unexpected error occurred' });
  }
});
//POST COMMENTS
// POST endpoint for adding a comment to a recipe
// POST endpoint for adding a comment to a recipe
app.post('/api/recipes/:recipeId/comments', verifyToken, async (req, res) => {
  try {
    const recipeId = req.params.recipeId;
    const { commentText } = req.body;
    const userId = req.uid;

    // Fetch the user's fullName from the users collection
    const userSnapshot = await db.collection('users').doc(userId).get();
    const userData = userSnapshot.data();
    const fullName = userData.fullName || ''; // Assuming fullName is a field in the user document

    // Create new comment document
    const commentRef = await db.collection('comments').add({
      recipeId,
      userId,
      fullName,
      commentText,
      timestamp: admin.firestore.FieldValue.serverTimestamp()
    });

    res.status(201).json({
      commentId: commentRef.id,
      recipeId,
      userId,
      fullName,
      commentText,
      timestamp: new Date()
    });
  } catch (error) {
    console.error('Error adding comment:', error);
    res.status(500).json({ error: 'An unexpected error occurred' });
  }
});

// GET endpoint for fetching comments for a specific recipe
app.get('/api/recipes/:recipeId/comments', verifyToken, async (req, res) => {
  try {
    const recipeId = req.params.recipeId;
    const commentsSnapshot = await db.collection('comments')
      .where('recipeId', '==', recipeId)
      .orderBy('timestamp', 'desc')
      .get();

    const comments = commentsSnapshot.docs.map(doc => ({
      commentId: doc.id,
      ...doc.data()
    }));

    res.status(200).json(comments);
  } catch (error) {
    console.error('Error fetching comments:', error);
    res.status(500).json({ error: 'An unexpected error occurred' });
  }
});


// Start the server
app.listen(PORT, () => {
  console.log(`Server is running on port ${PORT}`);
});
