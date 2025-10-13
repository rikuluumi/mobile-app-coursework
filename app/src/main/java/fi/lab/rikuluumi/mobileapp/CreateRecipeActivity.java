package fi.lab.rikuluumi.mobileapp;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.Nullable;

public class CreateRecipeActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1001;
    private ImageView recipeImagePreview;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_recipe);

        recipeImagePreview = findViewById(R.id.recipeImagePreview);
        Button selectImageButton = findViewById(R.id.selectImageButton);
        Button saveRecipeButton = findViewById(R.id.saveRecipeButton);
        EditText editTitle = findViewById(R.id.editRecipeTitle);
        EditText editDescription = findViewById(R.id.editRecipeDescription);
        Button cancelButton = findViewById(R.id.cancelButton);

        selectImageButton.setOnClickListener(v -> openImagePicker());
        cancelButton.setOnClickListener(v -> finish());

        saveRecipeButton.setOnClickListener(v -> {
            String title = editTitle.getText().toString().trim();
            String description = editDescription.getText().toString().trim();

            if (title.isEmpty() || description.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Recipe saved (placeholder)", Toast.LENGTH_SHORT).show();
            finish();
        });
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            recipeImagePreview.setImageURI(selectedImageUri);
        }
    }
}