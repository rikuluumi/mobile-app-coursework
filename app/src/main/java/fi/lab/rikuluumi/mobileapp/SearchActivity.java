package fi.lab.rikuluumi.mobileapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.app.TaskStackBuilder;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class SearchActivity extends com.example.myapp.BaseActivity {

    private EditText searchInput;
    private Button searchButton;
    private RecyclerView recyclerView;
    private RecipeAdapter adapter;
    private List<Recipe> searchResults = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isSessionValid) return;

        setContentView(R.layout.activity_search);
        setupToolbar(R.id.topAppBar);

        searchInput = findViewById(R.id.searchInput);
        searchButton = findViewById(R.id.searchButton);
        recyclerView = findViewById(R.id.searchResultsRecycler);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(searchResults);
        recyclerView.setAdapter(adapter);

        searchButton.setOnClickListener(v -> {
            String query = searchInput.getText().toString().trim();
            if (query.isEmpty()) {
                Toast.makeText(this, "Enter a search term", Toast.LENGTH_SHORT).show();
                return;
            }
            performSearch(query);
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_search);

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
                else if (id == R.id.nav_search) {
                    return true;
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
    }

    private void performSearch(String query) {
        new Thread(() -> {
            try {
                URL url = new URL(BuildConfig.API_BASE_URL + "/recipes/v1/search");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");

                JSONObject postData = new JSONObject();
                postData.put("query", query);

                OutputStream os = conn.getOutputStream();
                os.write(postData.toString().getBytes("UTF-8"));
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

                JSONObject jsonResponse = new JSONObject(response.toString());
                if (!checkPermissionFromResponse(jsonResponse)) return;

                if (jsonResponse.optBoolean("success")) {
                    searchResults.clear();
                    JSONArray data = jsonResponse.getJSONArray("data");
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject recipeObj = data.getJSONObject(i);
                        int id = recipeObj.getInt("id");
                        String title = recipeObj.getString("title");
                        String description = recipeObj.getString("description");
                        String imageUrl = recipeObj.getString("image_url");

                        searchResults.add(new Recipe(id, title, description, imageUrl, false));
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
                } else {
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Error: " + jsonResponse.optString("message"), Toast.LENGTH_LONG).show();
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error fetching results", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}