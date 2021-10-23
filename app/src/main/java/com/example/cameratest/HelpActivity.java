package com.example.cameratest;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import me.relex.circleindicator.CircleIndicator;
import me.relex.circleindicator.CircleIndicator3;

public class HelpActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private CircleIndicator3 indicator;
    private int nowPosition = 0;

    private int[] images = new int[]{
            R.drawable.help_image01,
            R.drawable.help_image02,
            R.drawable.help_image03
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // 폰트 설정
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
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

        // 풀스크린
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_help);

        viewPager = findViewById(R.id.image_slider_viewpager);
        indicator = findViewById(R.id.image_slider_indicator);

        viewPager.setOffscreenPageLimit(1);
        viewPager.setAdapter(new ImageSliderAdapter(this, images));
        viewPager.setPageTransformer(new DepthPageTransformer());

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                viewPager.setCurrentItem(position);
                nowPosition = position;
            }
        });

        indicator.setViewPager(viewPager);
        indicator.createIndicators(3, 0);
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

    public void backwardClick(View view){
        if (nowPosition != 0){
            nowPosition -= 1;
            viewPager.setCurrentItem(nowPosition);
        }
    }

    public void forwardClick(View view){
        if (nowPosition != 2){
            nowPosition += 1;
            viewPager.setCurrentItem(nowPosition);
        }
    }

    public void backbuttonClick(View view){
        finish();
        overridePendingTransition(R.anim.activity_right_enter, R.anim.activity_right_exit);
    }

    @Override
    public void onBackPressed() {
        backbuttonClick(null);
        super.onBackPressed();
    }

    @RequiresApi(21)
    public class DepthPageTransformer implements ViewPager2.PageTransformer {
        private static final float MIN_SCALE = 0.75f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();

            if (position < -1) { // [-Infinity,-1)
                // This page is way off-screen to the left.
                view.setAlpha(0f);

            } else if (position <= 0) { // [-1,0]
                // Use the default slide transition when moving to the left page
                view.setAlpha(1f);
                view.setTranslationX(0f);
                view.setTranslationZ(0f);
                view.setScaleX(1f);
                view.setScaleY(1f);

            } else if (position <= 1) { // (0,1]
                // Fade the page out.
                view.setAlpha(1 - position);

                // Counteract the default slide transition
                view.setTranslationX(pageWidth * -position);
                // Move it behind the left page
                view.setTranslationZ(-1f);

                // Scale the page down (between MIN_SCALE and 1)
                float scaleFactor = MIN_SCALE
                        + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0f);
            }
        }
    }

    public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.MyViewHolder> {
        private Context context;
        private int[] sliderImage;

        public ImageSliderAdapter(Context context, int[] sliderImage) {
            this.context = context;
            this.sliderImage = sliderImage;
        }

        @NonNull
        @Override
        public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.slider_item, parent, false);
            return new MyViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
            holder.bindSliderImage(sliderImage[position]);
        }

        @Override
        public int getItemCount() {
            return sliderImage.length;
        }

        public class MyViewHolder extends RecyclerView.ViewHolder {

            private ImageView imageView;

            public MyViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.image_slider);
            }

            public void bindSliderImage(int imageid) {
                imageView.setImageResource(imageid);
            }
        }
    }
}
