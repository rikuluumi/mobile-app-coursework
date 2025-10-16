package fi.lab.rikuluumi.mobileapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CreateRecipeActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1001;
    private ImageView recipeImagePreview;
    private Uri selectedImageUri;
    private EditText editTitle, editDescription;
    private String uploadedImageUrl = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_recipe);

        recipeImagePreview = findViewById(R.id.recipeImagePreview);
        Button selectImageButton = findViewById(R.id.selectImageButton);
        Button saveRecipeButton = findViewById(R.id.saveRecipeButton);
        editTitle = findViewById(R.id.editRecipeTitle);
        editDescription = findViewById(R.id.editRecipeDescription);
        uploadedImageUrl = "https://picsum.photos/300/200?random=4";
        Button cancelButton = findViewById(R.id.cancelButton);

        selectImageButton.setOnClickListener(v -> openImagePicker());
        saveRecipeButton.setOnClickListener(v -> saveRecipe());
        cancelButton.setOnClickListener(v -> finish());
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(Intent.createChooser(intent, "Select Recipe Image"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            recipeImagePreview.setImageURI(selectedImageUri);
        }
    }

    private void saveRecipe() {
        String title = editTitle.getText().toString().trim();
        String description = editDescription.getText().toString().trim();

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences prefs = getSharedPreferences("VisitorAppPrefs", MODE_PRIVATE);
        String token = prefs.getString("token", null);

        if (token.isEmpty()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(BuildConfig.API_BASE_URL + "/recipes/v1/add");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String postData = "title=" + title +
                        "&description=" + description +
                        "&image_url=" + uploadedImageUrl;

                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        responseCode < HttpURLConnection.HTTP_BAD_REQUEST ? conn.getInputStream() : conn.getErrorStream()
                ));

                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JSONObject json = new JSONObject(response.toString());
                runOnUiThread(() -> {
                    if (json.optBoolean("success")) {
                        Toast.makeText(this, "Recipe added!", Toast.LENGTH_SHORT).show();
                        Intent resultIntent = new Intent();
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        Toast.makeText(this, "Error: " + json.optString("message"), Toast.LENGTH_LONG).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }
}