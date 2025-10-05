package fi.lab.rikuluumi.mobileapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences("VisitorAppPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", null);
        String appPassword = prefs.getString("appPassword", null);

        if (username == null || appPassword == null) {
            // Not logged in → go to LoginActivity
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        Log.d("VisitorApp", "Saved username: " + username);
        Log.d("VisitorApp", "Saved appPassword: " + appPassword);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        Button buttonSiteA = findViewById(R.id.buttonSiteA);
        Button buttonSiteB = findViewById(R.id.buttonSiteB);

        buttonSiteA.setOnClickListener(v -> logVisit("Site A"));
        buttonSiteB.setOnClickListener(v -> logVisit("Site B"));
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
        }

        return super.onOptionsItemSelected(item);
    }

    private void logVisit(String siteName) {
        new Thread(() -> {
            try {
                URL url = new URL(BuildConfig.API_BASE_URL + "/add");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                // Basic Auth
                SharedPreferences prefs = getSharedPreferences("VisitorAppPrefs", MODE_PRIVATE);
                String username = prefs.getString("username", "");
                String appPassword = prefs.getString("appPassword", "");
                if (username.isEmpty() || appPassword.isEmpty()) {
                    runOnUiThread(() ->
                            Toast.makeText(this, "No login credentials found", Toast.LENGTH_SHORT).show()
                    );
                    return;
                }

                String credentials = username + ":" + appPassword;
                String basicAuth = "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP);
                conn.setRequestProperty("Authorization", basicAuth);

                // Send POST data
                String postData = "site_name=" + URLEncoder.encode(siteName, "UTF-8");
                OutputStream os = conn.getOutputStream();
                os.write(postData.getBytes());
                os.flush();
                os.close();

                // Read response
                InputStream is;
                if (conn.getResponseCode() < HttpURLConnection.HTTP_BAD_REQUEST) {
                    is = conn.getInputStream();
                } else {
                    is = conn.getErrorStream();
                }
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse JSON response
                String message;
                try {
                    JSONObject json = new JSONObject(response.toString());
                    if (json.has("success") && json.getBoolean("success")) {
                        message = "Visit logged!";
                    } else if (json.has("error")) {
                        message = "Error: " + json.getString("error");
                    } else {
                        message = "Unexpected response";
                    }
                } catch (JSONException e) {
                    message = "Invalid response from server";
                }

                String finalMessage = message;
                runOnUiThread(() ->
                        Toast.makeText(this, finalMessage, Toast.LENGTH_SHORT).show()
                );

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }
}