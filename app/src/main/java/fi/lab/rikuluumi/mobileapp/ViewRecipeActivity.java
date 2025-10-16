package fi.lab.rikuluumi.mobileapp;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class ViewRecipeActivity extends AppCompatActivity {

    private TextView title;
    private int recipeId;
    private Recipe recipe;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_recipe);

        title = findViewById(R.id.title);

        recipeId = getIntent().getIntExtra("recipe_id", 0);

        loadRecipe(recipeId);
    }

    private void loadRecipe(int id) {
        title.setText(String.valueOf(recipeId));
    }
}