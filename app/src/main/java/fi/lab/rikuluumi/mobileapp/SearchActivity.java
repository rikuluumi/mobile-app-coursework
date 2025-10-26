package fi.lab.rikuluumi.mobileapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.core.app.TaskStackBuilder;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SearchActivity extends com.example.myapp.BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        setupToolbar(R.id.topAppBar);

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
}