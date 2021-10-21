package com.example.cameratest;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

public class SettingActivity extends AppCompatActivity {

    SharedPreferences pref;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_setting);
        getSupportFragmentManager().beginTransaction().replace(R.id.setting, new PreferenceFragment()).commit();

        pref = PreferenceManager.getDefaultSharedPreferences(this);
        pref.registerOnSharedPreferenceChangeListener(new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                switch (key){
                    case "battery":
                        if (pref.getBoolean("battery", true)){
                            Toast.makeText(getApplicationContext(), "배터리 켜기", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(getApplicationContext(), "배터리 끄기", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "timer":
                        if (pref.getBoolean("timer", true)){
                            Toast.makeText(getApplicationContext(), "시계 켜기", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(getApplicationContext(), "시계 끄기", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    case "font":
                        Toast.makeText(getApplicationContext(), pref.getString("font", "나눔"), Toast.LENGTH_SHORT).show();
                        break;
                    case "plane":
                        if (pref.getBoolean("plane", true)){
                            Toast.makeText(getApplicationContext(), "바닥 켜기", Toast.LENGTH_SHORT).show();
                        }
                        else{
                            Toast.makeText(getApplicationContext(), "바닥 끄기", Toast.LENGTH_SHORT).show();
                        }
                        break;
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        backClick(null);
        super.onBackPressed();
    }

    public void backClick(View view){
        Intent data = new Intent();
        setResult(RESULT_OK, data);
        finish();
    }
}
