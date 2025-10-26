package fi.lab.rikuluumi.mobileapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ViewRecipeActivity extends AppCompatActivity {

    private int recipeId;
    private TextView title;
    private ImageView image;
    private TextView description;
    private ImageView favoriteButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recipe);

        title = findViewById(R.id.title);
        image = findViewById(R.id.recipeImagePreview);
        description = findViewById(R.id.description);
        favoriteButton = findViewById(R.id.ivFavorite);
        recipeId = getIntent().getIntExtra("recipe_id", 0);

        loadRecipe();
    }

    private void loadRecipe() {
        SharedPreferences prefs = getSharedPreferences("VisitorAppPrefs", MODE_PRIVATE);
        String token = prefs.getString("token", null);

        if (token.isEmpty()) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        new Thread(() -> {
            try {
                URL url = new URL(BuildConfig.API_BASE_URL + "/recipes/v1/recipe?id=" + recipeId);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);

                InputStream is;
                if (conn.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                    is = conn.getInputStream();
                } else {
                    is = conn.getErrorStream();
                }

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(sb.toString());
                if (jsonResponse.optBoolean("success", false)) {

                    JSONObject data = jsonResponse.getJSONObject("data");
                    Recipe recipe = new Recipe(
                            recipeId,
                            data.getString("title"),
                            data.getString("description"),
                            data.getString("image_url"),
                            data.getBoolean("favorite"));

                    runOnUiThread(()->showRecipe(recipe));
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to fetch recipe", Toast.LENGTH_SHORT).show();
                    });
                }

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private void showRecipe(Recipe recipe) {
        title.setText(recipe.getTitle());
        description.setText(recipe.getInfo());
        Glide.with(this)
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.ic_placeholder)
                .into(image);
        if (recipe.getIsFavorite()) {
            favoriteButton.setImageResource(R.drawable.ic_favorite_24_fill);
        } else {
            favoriteButton.setImageResource(R.drawable.ic_favorite_24);
        }
    }
}