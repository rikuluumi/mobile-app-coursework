package fi.lab.rikuluumi.mobileapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.TaskStackBuilder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("VisitorAppPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", null);
        String token = prefs.getString("token", null);

        if (username == null || token == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_main);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                Intent homeIntent = new Intent(getApplicationContext(), MainActivity.class);
                Intent targetIntent;
                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    return true;
                }
                else if (id == R.id.nav_search) {
                    targetIntent = new Intent(getApplicationContext(), SearchActivity.class);
                }
                else if (id == R.id.nav_my_recipes) {
                    targetIntent = new Intent(getApplicationContext(), MyRecipesActivity.class);
                }
                else if (id == R.id.nav_profile) {
                    targetIntent = new Intent(getApplicationContext(), ProfileActivity.class);
                }
                else {
                    return false;
                }

                TaskStackBuilder.create(getApplicationContext())
                        .addNextIntent(homeIntent)
                        .addNextIntent(targetIntent)
                        .startActivities();
                overridePendingTransition(0, 0);
                return true;
            }
        });

        RecyclerView popularRecyclerView = findViewById(R.id.popularRecyclerView);
        popularRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        List<Recipe> popularRecipes = new ArrayList<>();
        RecipeAdapter adapter = new RecipeAdapter(popularRecipes);
        popularRecyclerView.setAdapter(adapter);

        new Thread(() -> {
            try {
                URL url = new URL(BuildConfig.API_BASE_URL + "/recipes/v1/popular");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Authorization", "Bearer " + token);

                InputStream is = conn.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                reader.close();

                JSONObject jsonResponse = new JSONObject(sb.toString());
                if (jsonResponse.getBoolean("success")) {
                    JSONArray data = jsonResponse.getJSONArray("data");
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject recipeObj = data.getJSONObject(i);
                        int id = recipeObj.getInt("id");
                        String title = recipeObj.getString("title");
                        String info = recipeObj.getString("description");
                        String imageUrl = recipeObj.getString("image_url");

                        popularRecipes.add(new Recipe(id, title, info, imageUrl, false));
                    }

                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        adapter.setOnItemClickListener(recipe -> {
                            Intent intent = new Intent(getApplicationContext(), ViewRecipeActivity.class);
                            intent.putExtra("recipe_id", recipe.getId());
                            intent.putExtra("recipe_title", recipe.getTitle());
                            startActivity(intent);
                        });
                    });
                }

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error fetching recipes: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finishAffinity();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_first) {
            // Already on first page or navigate to it
            Toast.makeText(this, "First Page clicked", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.menu_second) {
            Intent intent = new Intent(this, SecondActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.menu_logout) {
            logoutUser();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void logoutUser() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences("VisitorAppPrefs", MODE_PRIVATE);
                    prefs.edit().clear().apply();
                    Intent intent = new Intent(this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}