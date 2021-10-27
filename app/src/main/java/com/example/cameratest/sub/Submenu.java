package com.example.cameratest.sub;

import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cameratest.R;

import java.util.ArrayList;

import static com.example.cameratest.MainActivity.getInstance;

public class Submenu extends Fragment {
    private RecyclerView subRecyclerView;
    Handler[] handlers = new Handler[6];
    private SubAdapter subAdapter;

    private static class subMenuData{
        public int picture;
        public String name;
        public String text;

        public subMenuData(int picture, String name, String text) {
            this.picture = picture;
            this.name = name;
            this.text = text;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup)inflater.inflate(R.layout.submenu_main, container, false);

        subRecyclerView = rootView.findViewById(R.id.submenu_View);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false);
        subRecyclerView.setLayoutManager(layoutManager);

        // 데코 적용
        subRecyclerView.addItemDecoration(new DividingItemDecoration(2));

        subAdapter = new SubAdapter(getContext());

        subAdapter.addItem(new subMenuData(R.drawable.create_icon, "Model Create", "가구 모델을 만들 수 있습니다."));
        subAdapter.addItem(new subMenuData(R.drawable.download_icon, "Download", "서버에서 다양한 가구 모델을 받을 수 있습니다."));
        subAdapter.addItem(new subMenuData(R.drawable.upload_icon, "Upload", "가구 모델을 서버로 올릴 수 있습니다."));
        subAdapter.addItem(new subMenuData(R.drawable.del_icon, "Model Delete", "필요 없는 가구를 삭제할 수 있습니다."));
        subAdapter.addItem(new subMenuData(R.drawable.setting_icon, "Settings", "여러가지 설정을 조절할 수 있습니다."));
        subAdapter.addItem(new subMenuData(R.drawable.help_icon, "Help", "사용방법에 대한 설명을 확인 할 수 있습니다."));

        subRecyclerView.setAdapter(subAdapter);

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // 포커스 주기
        for (int i = 0; i < 6; i++) {
            if (subRecyclerView.findViewHolderForAdapterPosition(i) != null) {
                TextView text = subRecyclerView.findViewHolderForAdapterPosition(i).itemView.findViewById(R.id.menu_text);
                handlers[i] = new Handler();
                handlers[i].postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        text.setSelected(true);
                    }
                }, 3000);
            }
            else{
                handlers[i] = new Handler();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        // 포커스 뺏기
        for (int i = 0; i < 6; i++) {
            if (subRecyclerView.findViewHolderForAdapterPosition(i) != null) {
                TextView text = subRecyclerView.findViewHolderForAdapterPosition(i).itemView.findViewById(R.id.menu_text);
                if (handlers[i] != null) {
                    handlers[i].removeCallbacksAndMessages(null);
                }
                text.setSelected(false);
            }
        }
    }

    public class SubAdapter extends RecyclerView.Adapter<SubAdapter.ViewHolder>{
        ArrayList<subMenuData> items = new ArrayList<>();
        Context context;

        public SubAdapter(Context context){
            this.context = context;
        }

        @Override
        public int getItemCount(){
            return items.size();
        }

        @NonNull
        @Override
        public SubAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
            LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View itemView = vi.inflate(R.layout.submenu_view, parent, false);

            return new SubAdapter.ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull SubAdapter.ViewHolder holder, int position){
            subMenuData item = items.get(position);
            holder.setItem(item);
        }

        public void addItem(subMenuData item){
            items.add(item);
        }

        @Override
        public void onViewAttachedToWindow(@NonNull SubAdapter.ViewHolder holder) {
            // marquee의 시작 시점
            super.onViewAttachedToWindow(holder);
            if (handlers[holder.getAdapterPosition()] != null) {
                handlers[holder.getAdapterPosition()] = new Handler();
                handlers[holder.getAdapterPosition()].postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        holder.text.setSelected(true);
                    }
                }, 3000);
            }
        }

        @Override
        public void onViewDetachedFromWindow(@NonNull SubAdapter.ViewHolder holder) {
            // marquee의 종료 시점
            super.onViewDetachedFromWindow(holder);
            holder.text.setSelected(false);
            handlers[holder.getAdapterPosition()].removeCallbacksAndMessages(null);
        }

        class ViewHolder extends RecyclerView.ViewHolder{
            FrameLayout line;
            ImageView picture;
            TextView name;
            TextView text;

            public ViewHolder(@NonNull View itemView){
                super(itemView);

                // 메뉴의 선택
                line = (FrameLayout)itemView.findViewById(R.id.submenu_area);
                line.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String temp = name.getText().toString();
                        switch (temp){
                            case "Model Create":
                                getInstance.buttonCreateClick(null);
                                break;
                            case "Download":
                                getInstance.downloadClick(null);
                                break;
                            case "Upload":
                                getInstance.uploadClick(null);
                                break;
                            case "Model Delete":
                                getInstance.furDeleteClick(null);
                                break;
                            case "Settings":
                                getInstance.settingClick(null);
                                break;
                            case "Help":
                                getInstance.helpClick(null);
                                break;
                        }
                    }
                });
                picture = (ImageView)itemView.findViewById(R.id.menu_image);
                name = (TextView)itemView.findViewById(R.id.menu_name);
                text = (TextView)itemView.findViewById(R.id.menu_text);
            }

            public void setItem(subMenuData item){
                picture.setImageResource(item.picture);
                name.setText(item.name);
                text.setText(item.text);
            }
        }
    }

    public class DividingItemDecoration extends RecyclerView.ItemDecoration {
        // 주어진 숫자 만큼 공간을 띄워주는 데코레이터
        private final int dividing;

        public DividingItemDecoration(int dividing) {
            this.dividing = dividing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position

            if (position != 0) {
                outRect.top = dividing;
            }
        }
    }
}
