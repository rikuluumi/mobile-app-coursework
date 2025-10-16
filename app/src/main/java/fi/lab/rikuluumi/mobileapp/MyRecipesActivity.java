package fi.lab.rikuluumi.mobileapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.TaskStackBuilder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class MyRecipesActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private List<Recipe> myRecipes = new ArrayList<>();
    private SwipeRefreshLayout swipeRefreshLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_recipes);

        FloatingActionButton addRecipeButton = findViewById(R.id.addRecipeButton);
        addRecipeButton.setOnClickListener(v -> {
            Intent intent = new Intent(MyRecipesActivity.this, CreateRecipeActivity.class);
            startActivity(intent);
        });

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        recyclerView = findViewById(R.id.myRecipesRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(myRecipes);
        recyclerView.setAdapter(adapter);
        swipeRefreshLayout.setOnRefreshListener(this::fetchMyRecipes);

        swipeRefreshLayout.setRefreshing(true);
        fetchMyRecipes();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_my_recipes);

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                Intent homeIntent = new Intent(getApplicationContext(), MainActivity.class);
                Intent targetIntent;

                int id = item.getItemId();
                if (id == R.id.nav_home) {
                    TaskStackBuilder.create(getApplicationContext())
                            .addNextIntent(homeIntent)
                            .startActivities();
                    overridePendingTransition(0, 0);
                    return true;
                }
                else if (id == R.id.nav_favorites) {
                    targetIntent = new Intent(getApplicationContext(), FavoritesActivity.class);
                }
                else if (id == R.id.nav_my_recipes) {
                    return true;
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
    }

    private void fetchMyRecipes() {
        new Thread(() -> {
            try {
                URL url = new URL(BuildConfig.API_BASE_URL + "/recipes/v1/my");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");

                SharedPreferences prefs = getSharedPreferences("VisitorAppPrefs", MODE_PRIVATE);
                String token = prefs.getString("token", null);
                if (token != null) {
                    conn.setRequestProperty("Authorization", "Bearer " + token);
                }

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
                    JSONArray data = jsonResponse.getJSONArray("data");
                    myRecipes.clear();
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject recipeObj = data.getJSONObject(i);
                        String title = recipeObj.getString("title");
                        String info = recipeObj.getString("description");
                        String imageUrl = recipeObj.getString("image_url");

                        myRecipes.add(new Recipe(title, info, imageUrl));
                    }

                    runOnUiThread(() -> {
                        adapter.notifyDataSetChanged();
                        swipeRefreshLayout.setRefreshing(false);
                    });
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Failed to fetch recipes", Toast.LENGTH_SHORT).show();
                        swipeRefreshLayout.setRefreshing(false);
                    });
                }

                conn.disconnect();

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    swipeRefreshLayout.setRefreshing(false);
                });
            }
        }).start();
    }
}