package dev.rastadev.oxyfree;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences sharedpreferences;
    private final int maxSteps = 6;
    public static final String stepPref = "step";
    private int current = 0;
    private TextView mTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedpreferences = getSharedPreferences(stepPref, Context.MODE_PRIVATE);
        sharedpreferences.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                Log.d("MOBILE_APP", "Shared pref changed");
                if (key.equals("current")) {
                    current = sharedPreferences.getInt("current", 0);
                    updateUI();
                }
            }
        });
        if (sharedpreferences.contains("current")) {
            current = sharedpreferences.getInt("current", 0);
        }
        mTextView = findViewById(R.id.main_text_current);
        updateUI();
    }

    public void onPreviousClick(View view) {
        Log.d("MOBILE_APP", "Previous clicked");
        if (current > 0) {
            current -= 1;
            updatePrefs();
        }
        updateUI();
    }

    public void onNextClick(View view) {
        Log.d("MOBILE_APP", "Next clicked");
        if (current < maxSteps) {
            current += 1;
            updatePrefs();
        }
        updateUI();
    }

    private void updatePrefs () {
        SharedPreferences.Editor editor = sharedpreferences.edit();
        editor.putInt("current", current);
        editor.apply();
    }

    private void updateUI () {
        mTextView.setText(String.valueOf(current));
    }
}
