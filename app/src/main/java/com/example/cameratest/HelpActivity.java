package com.example.cameratest;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.RequestOptions;
import com.bumptech.glide.request.target.Target;

import me.relex.circleindicator.CircleIndicator3;

public class HelpActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private CircleIndicator3 indicator;
    private int nowPosition = 0;

    private int[] images = new int[]{
            R.drawable.help_media01,
            R.drawable.help_media02,
            R.drawable.help_media03,
            R.drawable.help_media04,
            R.drawable.help_media05,
            R.drawable.help_media06,
            R.drawable.help_media07
    };

    private String[] headers = new String[]{
            "모델을 배치하는 방법",
            "모델을 조작하는 방법",
            "모델들 제거 및 바닥을 재설정 방법",
            "간편하게 목록 여는 방법",
            "가구를 생성하는 방법",
            "가구를 다운로드하는 방법",
            "가구를 업로드하거나 지우는 방법"
    };

    private String[] explanes = new String[]{
            "'+' 버튼으로 가구 목록을 열어주세요.\n원하시는 가구를 선택하시면 왼쪽 위에 표시가 됩니다.\n인식된 바닥을 터치하면 그 곳에 모델이 배치돼요.",
            "터치를 통해 모델을 선택하면 파란 원 표식이 발생해요.\n선택한 모델에 다음과 같은 조작을 할 수 있어요.\nDouble Click : 자리 이동\nHorizontal Slide : 회전\nPinch Zoom : 확대 / 축소",
            "모델들을 전부 제거하고 싶은 경우에는 '모델만 제거'\n바닥 인식이 제대로 되지 않은 경우는 '바닥까지 초기화'",
            "슬라이드를 통해 여닫을 수 있어요.\n다만 메뉴는 가구 목록이 열려있을 때만 열 수 있어요.",
            "메뉴에서 Model Create를 클릭하여 진입해주세요.\n가구로 만들고 싶은 사물을 촬영한 뒤,\n이름을 설정해주면 가구가 생성돼요.\n참고로, 이름은 최소 2글자 이상 이여야해요.",
            "메뉴에서 Download를 클릭하여 진입해주세요.\n원하시는 가구를 클릭하여 다운로드할 수 있어요.\n오른쪽 위의 돋보기 버튼을 통해 검색도 가능해요.",
            "메뉴에서 (Upload or Delete)를 클릭해주세요.\n(업로드 or 삭제) 모드에 진입하게 되고,\n원하는 가구를 선택하여 (업로드 or 삭제)가 가능해요."
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
                    Log.d("webp", position + " load image check");
                    ImageView imageView = holder.itemView.findViewById(R.id.image_slider);
                    Glide.with(getApplicationContext())
                            .asDrawable()
                            .load(images[position])
                            .transition(DrawableTransitionOptions.withCrossFade(400))
                            .into(imageView);
                    //.skipmemory
                    // 메모리 폭주로 인한 갱신효과에 의해 일단 처리
                }
                nowPosition = position;
            }
        });

        indicator.setViewPager(viewPager);
        indicator.createIndicators(7, 0);
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
        if (nowPosition != 6){
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
