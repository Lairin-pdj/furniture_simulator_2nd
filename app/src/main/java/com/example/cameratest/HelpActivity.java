package com.example.cameratest;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.bumptech.glide.load.resource.gif.GifDrawable;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

import me.relex.circleindicator.CircleIndicator3;

public class HelpActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private CircleIndicator3 indicator;
    private int nowPosition = 0;

    private int[] images = new int[]{
            R.drawable.plane_tutorial,
            R.drawable.plane_tutorial,
            R.drawable.plane_tutorial,
            R.drawable.plane_tutorial,
            R.drawable.plane_tutorial,
            R.drawable.plane_tutorial,
            R.drawable.plane_tutorial,
            R.drawable.plane_tutorial
    };

    private String[] headers = new String[]{
            "모델을 배치하는 방법",
            "모델을 조작하는 방법",
            "모델들을 제거하거나 바닥을 재설정하는 방법",
            "간편하게 목록 여는 방법",
            "가구를 생성하는 방법",
            "가구를 다운로드하는 방법",
            "가구를 업로드하는 방법",
            "가구를 지우는 방법"
    };

    private String[] explanes = new String[]{
            "모델을 배치하는 방법_explane",
            "모델을 조작하는 방법_explane",
            "모델들을 제거하거나 바닥을 재설정하는 방법_explane",
            "간편하게 목록 여는 방법_explane",
            "가구를 생성하는 방법_explane",
            "가구를 다운로드하는 방법_explane",
            "가구를 업로드하는 방법_explane",
            "가구를 지우는 방법_explane"
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
        viewPager.setAdapter(new ImageSliderAdapter(this, images, headers, explanes));

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                viewPager.setCurrentItem(position);

                RecyclerView recyclerView = (RecyclerView) viewPager.getChildAt(0);
                RecyclerView.ViewHolder holder = recyclerView.findViewHolderForAdapterPosition(position);
                if (holder != null) {
                    ImageView imageView = holder.itemView.findViewById(R.id.image_slider);
                    Glide.with(getApplicationContext()).load(images[position]).into(imageView);
                }
                nowPosition = position;
            }
        });

        indicator.setViewPager(viewPager);
        indicator.createIndicators(8, 0);
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
        if (nowPosition != 7){
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

    public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.MyViewHolder> {
        private Context context;
        private int[] sliderImage;
        private String[] headers;
        private String[] explanes;

        public ImageSliderAdapter(Context context, int[] sliderImage, String[] headers, String[] explanes) {
            this.context = context;
            this.sliderImage = sliderImage;
            this.headers = headers;
            this.explanes = explanes;
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
            holder.bindSliderImage(sliderImage[position], headers[position], explanes[position]);
        }

        @Override
        public int getItemCount() {
            return sliderImage.length;
        }

        public class MyViewHolder extends RecyclerView.ViewHolder {

            private ImageView imageView;
            private TextView header;
            private TextView explane;

            public MyViewHolder(@NonNull View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.image_slider);
                header = itemView.findViewById(R.id.header_title);
                explane = itemView.findViewById(R.id.explane_text);
            }

            public void bindSliderImage(int imageid, String head, String text) {
                //Glide.with(getApplicationContext()).load(imageid).into(imageView);
                header.setText(head);
                explane.setText(text);
            }
        }
    }
}
