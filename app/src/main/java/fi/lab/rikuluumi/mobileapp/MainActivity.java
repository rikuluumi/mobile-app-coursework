package fi.lab.rikuluumi.mobileapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

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

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        //Button buttonSiteA = findViewById(R.id.buttonSiteA);
        //Button buttonSiteB = findViewById(R.id.buttonSiteB);

        //buttonSiteA.setOnClickListener(v -> logVisit("Site A"));
        //buttonSiteB.setOnClickListener(v -> logVisit("Site B"));

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int id = item.getItemId();

                if (id == R.id.nav_home) {
                    return true; // already on Home
                }
                else if (id == R.id.nav_favorites) {
                    startActivity(new Intent(getApplicationContext(), FavoritesActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                }
                else if (id == R.id.nav_my_recipes) {
                    startActivity(new Intent(getApplicationContext(), MyRecipesActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                }
                else if (id == R.id.nav_profile) {
                    startActivity(new Intent(getApplicationContext(), ProfileActivity.class));
                    overridePendingTransition(0, 0);
                    return true;
                }

                return false;
            }
        });

        RecyclerView popularRecyclerView = findViewById(R.id.popularRecyclerView);
        popularRecyclerView.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        List<Recipe> popularRecipes = new ArrayList<>();
        popularRecipes.add(new Recipe("Spaghetti Carbonara", "20 min | Medium", R.drawable.ic_placeholder));
        popularRecipes.add(new Recipe("Avocado Toast", "10 min | Easy", R.drawable.ic_placeholder));
        popularRecipes.add(new Recipe("Chicken Curry", "30 min | Medium", R.drawable.ic_placeholder));

        RecipeAdapter adapter = new RecipeAdapter(popularRecipes);
        popularRecyclerView.setAdapter(adapter);
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

    /*private void logVisit(String siteName) {
        new Thread(() -> {
            HttpURLConnection conn = null;
            try {
                URL url = new URL(BuildConfig.API_BASE_URL + "/visitorlog/v1/add");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setDoOutput(true);

                SharedPreferences prefs = getSharedPreferences("VisitorAppPrefs", MODE_PRIVATE);
                String token = prefs.getString("token", "");

                if (token.isEmpty()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "No login token found, please log in again", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                conn.setRequestProperty("Authorization", "Bearer " + token);

                JSONObject body = new JSONObject();
                body.put("site_name", siteName);

                OutputStream os = conn.getOutputStream();
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                os.close();

                int responseCode = conn.getResponseCode();
                InputStream is = (responseCode < HttpURLConnection.HTTP_BAD_REQUEST)
                        ? conn.getInputStream()
                        : conn.getErrorStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder responseBuilder = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    responseBuilder.append(line);
                }
                reader.close();

                String responseText = responseBuilder.toString();

                String message;
                try {
                    JSONObject json = new JSONObject(responseText);
                    if (json.optBoolean("success", false)) {
                        message = "Visit logged successfully!";
                    } else if (json.has("message")) {
                        message = "Error: " + json.getString("message");
                    } else {
                        message = "Unexpected response: " + responseText;
                    }
                } catch (JSONException e) {
                    message = "Invalid JSON response: " + responseText;
                }

                String finalMessage = message;
                runOnUiThread(() -> Toast.makeText(this, finalMessage, Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            } finally {
                if (conn != null) conn.disconnect();
            }
        }).start();
    }*/
}