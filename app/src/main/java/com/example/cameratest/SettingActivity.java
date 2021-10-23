package com.example.cameratest;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.cameratest.sub.PreferenceFragment;

public class SettingActivity extends AppCompatActivity {

    SharedPreferences pref;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // 폰트 설정
        pref = PreferenceManager.getDefaultSharedPreferences(this);
        switch (pref.getString("font", "기본")){
            case "나눔R":
                setTheme(R.style.AppTheme_NanumR);
                break;
            case "나눔B":
                setTheme(R.style.AppTheme_NanumB);
                break;
            case "카페":
                setTheme(R.style.AppTheme_Cafe);
                break;
            case "에스코드":
                setTheme(R.style.AppTheme_Sc);
                break;
            default:
                setTheme(R.style.AppTheme);
                break;
        }

        super.onCreate(savedInstanceState);

        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_setting);
        getSupportFragmentManager().beginTransaction().replace(R.id.setting, new PreferenceFragment()).commit();


        pref.registerOnSharedPreferenceChangeListener(listener);
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

    android.content.SharedPreferences.OnSharedPreferenceChangeListener listener = new SharedPreferences.OnSharedPreferenceChangeListener(){
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
                    // 폰트 설정
                    String whatFont = pref.getString("font", "기본");
                    switch (whatFont){
                        case "나눔R":
                            setTheme(R.style.AppTheme_NanumR);
                            break;
                        case "나눔B":
                            setTheme(R.style.AppTheme_NanumB);
                            break;
                        case "카페":
                            setTheme(R.style.AppTheme_Cafe);
                            break;
                        case "에스코드":
                            setTheme(R.style.AppTheme_Sc);
                            break;
                        default:
                            setTheme(R.style.AppTheme);
                            break;
                    }
                    setContentView(R.layout.activity_setting);
                    getFragmentManager().isDestroyed();
                    getSupportFragmentManager().beginTransaction().replace(R.id.setting, new PreferenceFragment()).commit();
                    pref.registerOnSharedPreferenceChangeListener(listener);
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
    };

    @Override
    public void onBackPressed() {
        backClick(null);
        super.onBackPressed();
    }

    public void backClick(View view){
        Intent data = new Intent();
        setResult(RESULT_OK, data);
        finish();
        overridePendingTransition(R.anim.activity_right_enter, R.anim.activity_right_exit);
    }
}
