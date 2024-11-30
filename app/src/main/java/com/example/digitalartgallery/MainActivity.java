package com.example.digitalartgallery;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int IMAGE_PICKER_REQUEST = 1000;

    private RecyclerView recyclerView;
    private GalleryAdapter galleryAdapter;
    private MaterialButton fabWithTextButton;
    private TextView userEmailTextView;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private Uri selectedImageUri;
    private Bitmap selectedImageBitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize UI components
        recyclerView = findViewById(R.id.galleryRecyclerView);
        fabWithTextButton = findViewById(R.id.fabWithTextButton);
        userEmailTextView = findViewById(R.id.userEmailTextView); // Add this to layout

        // Set up RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        galleryAdapter = new GalleryAdapter();
        recyclerView.setAdapter(galleryAdapter);

        // Display the current user's email
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();
            userEmailTextView.setText(userEmail);
        } else {
            Toast.makeText(MainActivity.this, "No user logged in", Toast.LENGTH_SHORT).show();
        }

        // Handle FAB click for uploading
        fabWithTextButton.setOnClickListener(v -> openImagePicker());

        // Load images from Firestore when the activity is created
        loadGalleryImages();
    }

    // Open the image picker to select an image from the gallery
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, IMAGE_PICKER_REQUEST);
    }

    // Handle the result of image picking
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICKER_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            try {
                selectedImageBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), selectedImageUri);
                showUploadDialog();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load image", Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Show dialog for adding a caption and uploading the image
    private void showUploadDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Upload Image");

        // Set up input for caption
        final EditText captionInput = new EditText(this);
        captionInput.setHint("Enter caption");
        builder.setView(captionInput);

        builder.setPositiveButton("Upload", (dialog, which) -> {
            String caption = captionInput.getText().toString();
            if (selectedImageBitmap != null && !caption.isEmpty()) {
                uploadImageToImgur(caption);
            } else {
                Toast.makeText(MainActivity.this, "Please select an image and enter a caption", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }

    // Upload the selected image to Imgur and save to Firestore
    private void uploadImageToImgur(String caption) {
        String encodedImage = ImgurUploader.bitmapToBase64(selectedImageBitmap);

        ImgurUploader.uploadImageToImgur(encodedImage, new ImgurUploader.UploadCallback() {
            @Override
            public void onUploadSuccess(String imageUrl) {
                // Save image URL, caption, and user email to Firestore
                saveImageToFirestore(imageUrl, caption);
            }

            @Override
            public void onUploadFailure(String errorMessage) {
                Toast.makeText(MainActivity.this, "Image upload failed: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Save the uploaded image URL and caption to Firestore
    private void saveImageToFirestore(String imageUrl, String caption) {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            String userEmail = currentUser.getEmail();

            Map<String, Object> artItem = new HashMap<>();
            artItem.put("imageUrl", imageUrl);
            artItem.put("caption", caption);
            artItem.put("email", userEmail);
            artItem.put("timestamp", FieldValue.serverTimestamp());

            db.collection("artGallery")
                    .add(artItem)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(MainActivity.this, "Image uploaded successfully!", Toast.LENGTH_SHORT).show();
                        // Reload the gallery after uploading
                        loadGalleryImages();
                    })
                    .addOnFailureListener(e -> Toast.makeText(MainActivity.this, "Failed to save image info.", Toast.LENGTH_SHORT).show());
        }
    }

    // Load images from Firestore and update the RecyclerView
    private void loadGalleryImages() {
        db.collection("artGallery")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        QuerySnapshot querySnapshot = task.getResult();
                        if (querySnapshot != null) {
                            List<DocumentSnapshot> documents = querySnapshot.getDocuments();
                            // Update the adapter with the fetched data
                            galleryAdapter.setGalleryItems(documents);
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "Error loading gallery", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
