package com.example.myapp;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import org.json.JSONObject;

import fi.lab.rikuluumi.mobileapp.LoginActivity;
import fi.lab.rikuluumi.mobileapp.MainActivity;
import fi.lab.rikuluumi.mobileapp.R;

public abstract class BaseActivity extends AppCompatActivity {

    protected String token;
    protected boolean isSessionValid = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!isUserLoggedIn()) {
            isSessionValid = false;
            redirectToLogin();
        }
    }

    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences("VisitorAppPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", null);
        token = prefs.getString("token", null);
        return username != null && token != null;
    }

    protected void setupToolbar(int toolbarId) {
        Toolbar toolbar = findViewById(toolbarId);
        setSupportActionBar(toolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.top_app_bar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.menu_logout) {
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

    protected boolean checkPermissionFromResponse(JSONObject jsonResponse) {
        if (jsonResponse == null) return true;

        String code = jsonResponse.optString("code", "");

        if ("rest_forbidden".equals(code)) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Session expired or permission denied. Please log in again.", Toast.LENGTH_LONG).show();
                redirectToLogin();
            });
            return false;
        }

        return true;
    }

    protected void redirectToLogin() {
        SharedPreferences prefs = getSharedPreferences("VisitorAppPrefs", MODE_PRIVATE);
        prefs.edit().remove("token").remove("username").apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }
}
