package com.example.cameratest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LayoutAnimationController;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.cameratest.sub.FurnitureData;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import de.javagl.obj.Obj;
import de.javagl.obj.ObjReader;
import de.javagl.obj.ObjWriter;


public class DownloadActivity extends AppCompatActivity {
    // ip주소
    // 캡슐화 필요
    private final static String IP_ADDRESS = "13.125.254.183";
    private final static String TAG = "php";

    // 저장을 위한 변수들
    private String mJsonString;
    private ArrayList<FurnitureData> furniturelist;
    private static class Data{
        Bitmap preview;
        Obj obj;
        Bitmap texture;

        public Data(Bitmap preview, Obj obj, Bitmap texture) {
            this.preview = preview;
            this.obj = obj;
            this.texture = texture;
        }
    }
    private Data downloadData = new Data(null, null, null);

    // UI용 변수들
    public static DownloadActivity getInstance;
    public RecyclerView recyclerView;
    CustomerAdapter adapter = new CustomerAdapter(DownloadActivity.this);
    private boolean isSearch;

    // 애니메이션
    private Animation search_up;
    private Animation search_down;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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
        getInstance = this;

        setContentView(R.layout.activity_download);

        // 데코레이터
        recyclerView = findViewById(R.id.download_list);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        int spacing = 30;
        boolean includeEdge = true;
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, spacing, includeEdge));

        // search의 애니메이션
        search_up = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.search_up);
        search_down = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.search_down);

        // search의 엔터키 체크
        EditText editText = (EditText)findViewById(R.id.search);
        editText.setOnKeyListener(new View.OnKeyListener(){
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event){
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)){
                    searchbuttonClick(v);
                    return true;
                }
                else {
                    return false;
                }
            }
        });

        // 다운로드 목록 세팅
        setDownloadList(null, false);
    }

    @Override
    protected void onResume() {
        super.onResume();

        isSearch = false;
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
    public boolean dispatchTouchEvent(MotionEvent ev){
        // 검색창 외 다른 터치 감시
        Rect viewRect = new Rect();
        EditText editText = (EditText)findViewById(R.id.search);
        editText.getGlobalVisibleRect(viewRect);
        if (isSearch && !viewRect.contains((int)ev.getX(), (int)ev.getY())){
            editText.setText("");
            searchbuttonClick(editText);
        }

        return super.dispatchTouchEvent(ev);
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

    public void searchbuttonClick(View view){
        InputMethodManager inputMethodManager = (InputMethodManager)getInstance.getSystemService(INPUT_METHOD_SERVICE);
        EditText editText = (EditText)findViewById(R.id.search);
        ImageButton imageButton = (ImageButton)findViewById(R.id.searchButton);
        editText.bringToFront();
        imageButton.bringToFront();

        // 검색창을 여는 경우
        if (!isSearch) {
            isSearch = true;
            editText.setVisibility(View.VISIBLE);
            editText.startAnimation(search_up);
        }
        // 검색창을 닫는 경우
        else{
            isSearch = false;
            editText.setVisibility(View.INVISIBLE);
            editText.startAnimation(search_down);

            // 검색 쿼리
            if (!editText.getText().toString().equals("")) {
                setDownloadList(editText.getText(), true);
            }

            // 키보드 및 포커싱 처리
            editText.clearFocus();
            inputMethodManager.hideSoftInputFromWindow(editText.getWindowToken(), 0);
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    editText.setText("");
                }
            }, 700);
        }
    }

    private class GetData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(DownloadActivity.this, R.style.DialogTransparent);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();

            ImageView connect = (ImageView)findViewById(R.id.connect_failed);
            ImageView search = (ImageView)findViewById(R.id.no_search);

            // 결과가 도착한 경우
            if (result != null){
                connect.setVisibility(View.INVISIBLE);

                // 내용물이 없는 경우
                if (result.equals("")) {
                    search.bringToFront();
                    adapter.resetItems();
                    adapter.notifyDataSetChanged();
                    recyclerView.setAdapter(adapter);
                    search.setVisibility(View.VISIBLE);
                }
                // 내용물이 있는 경우
                else {
                    search.setVisibility(View.INVISIBLE);
                    mJsonString = result;
                    showResult();
                }
            }
            // 연결에 실패한 경우
            else{
                connect.bringToFront();
                adapter.resetItems();
                adapter.notifyDataSetChanged();
                recyclerView.setAdapter(adapter);
                connect.setVisibility(View.VISIBLE);
            }
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];
            String postParameters = params[1];

            try {
                // 주어진 주소로 연결 시도
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                // 문자열이 주어진 경우 post로 전송
                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }

                // 통신 정보를 저장
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                StringBuilder sb = new StringBuilder();
                String line;

                while((line = bufferedReader.readLine()) != null){
                    sb.append(line);
                }

                bufferedReader.close();

                return sb.toString().trim();

            } catch (Exception e) {
                Log.e(TAG, "GetData : Error ", e);
                errorString = e.toString();

                return null;
            }
        }
    }

    private void showResult(){
        String TAG_JSON = "furnitures";
        String TAG_ID = "id";
        String TAG_NAME = "name";
        String TAG_EXTENSION = "extension";
        String TAG_PREVIEW_LINK = "preview_link";
        String TAG_FILE_LINK = "file_link";
        String TAG_TEXTURE_LINK = "texture_link";
        String TAG_VIEWER_COUNT = "viewer_count";
        String TAG_DOWNLOAD_COUNT = "download_count";

        try {
            JSONObject jsonObject = new JSONObject(mJsonString);
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

            for(int i=0;i<jsonArray.length();i++){
                // JSON 파싱
                JSONObject item = jsonArray.getJSONObject(i);

                String id = item.getString(TAG_ID);
                String name = item.getString(TAG_NAME);
                String extension = item.getString(TAG_EXTENSION);
                String preview_link = item.getString(TAG_PREVIEW_LINK);
                String file_link = item.getString(TAG_FILE_LINK);
                String texture_link = item.getString(TAG_TEXTURE_LINK);
                String viewer_count = item.getString(TAG_VIEWER_COUNT);
                String download_count = item.getString(TAG_DOWNLOAD_COUNT);

                // 가구 데이터 생성
                FurnitureData furnitureData = new FurnitureData();

                furnitureData.setMember_id(id);
                furnitureData.setMember_name(name);
                furnitureData.setMember_extension(extension);
                furnitureData.setMember_preview_link(preview_link);
                furnitureData.setMember_file_link(file_link);
                furnitureData.setMember_texture_link(texture_link);
                furnitureData.setMember_viewer_count(viewer_count);
                furnitureData.setMember_download_count(download_count);

                furniturelist.add(furnitureData);
            }

            adapter.resetItems();
            adapter.notifyDataSetChanged();

            for(int i = 0; i < furniturelist.size(); i++){
                adapter.addItem(furniturelist.get(i));
            }

            recyclerView.setAdapter(adapter);
            LayoutAnimationController animationController = AnimationUtils.loadLayoutAnimation(DownloadActivity.this, R.anim.layout_animation_recyclerview);
            recyclerView.setLayoutAnimation(animationController);

        } catch (JSONException e) {
            Log.e(TAG, "showResult : ", e);
        }
    }

    public void setDownloadList(Editable search_item, Boolean check){
        // 새로운 배열 준비
        furniturelist = new ArrayList<>();
        furniturelist.clear();

        // 검색의 경우
        if (check){
            GetData task = new GetData();
            task.execute("http://" + IP_ADDRESS + "/query.php", "&text=" + search_item.toString());
        }
        // 기본 시작 전체 데이터 받아오는 경우
        else{
            GetData task = new GetData();
            task.execute("http://" + IP_ADDRESS + "/getjson.php", "");
        }
    }

    private class DownData extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(DownloadActivity.this, R.style.DialogSmall);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("가구 데이터를 받는 중이에요.");
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();

            // 결과가 도착한 경우
            if (result != null){

                // 내용물이 없는 경우
                if (result.equals("")) {
                    Log.w(TAG, "DownData : savename error");
                }
                // 내용물이 있는 경우
                else {
                    createModel(result);
                }
            }
            // 연결에 실패한 경우
            else{
                Log.e(TAG, "connect failed");
            }
        }

        @Override
        protected String doInBackground(String... params) {

            String URLpreview = params[0];
            String URLobj = params[1];
            String URLtexture = params[2];
            String DATAid = params[3];
            String name = params[4];

            // preview 파일 수신
            try {
                URL url = new URL(URLpreview);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                int responseStatusCode = httpURLConnection.getResponseCode();

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }

                downloadData.preview = BitmapFactory.decodeStream(inputStream);

            } catch (Exception e) {
                Log.e(TAG, "DownData : preview : Error ", e);
                errorString = e.toString();

                return null;
            }

            // obj 파일 수신
            try {
                URL url = new URL(URLobj);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                int responseStatusCode = httpURLConnection.getResponseCode();

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }

                downloadData.obj = ObjReader.read(inputStream);

            } catch (Exception e) {
                Log.e(TAG, "DownData : obj : Error ", e);
                errorString = e.toString();

                return null;
            }

            // texture 파일 수신
            try {
                URL url = new URL(URLtexture);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                int responseStatusCode = httpURLConnection.getResponseCode();

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }

                downloadData.texture = BitmapFactory.decodeStream(inputStream);

            } catch (Exception e) {
                Log.e(TAG, "DownData : texture : Error ", e);
                errorString = e.toString();

                return null;
            }

            // 다운로드 수 쿼리 진행
            try {
                URL url = new URL("http://" + IP_ADDRESS + "/downcount.php");
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(("&id=" + DATAid).getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();

            } catch (Exception e) {
                Log.e(TAG, "DownData : counter : Error ", e);
                errorString = e.toString();

                return null;
            }

            // 수신 받은 파일 내부저장소로 저장
            File path;
            OutputStream outputStream;

            // 이름 중복 방지
            while (true) {
                path = new File(getFilesDir().getAbsolutePath() + "/models/" + name + ".obj");

                if (path.exists()) {
                    int front = name.lastIndexOf("(");
                    int back = name.lastIndexOf(")");

                    if (front != -1 && back != -1 && front < back){
                        String number = name.substring(front + 1, back);

                        boolean flag = true;
                        for (int i = 0; i < number.length(); i++){
                            char temp = number.charAt(i);
                            if (!Character.isDigit(temp)) {
                                name = name + "(1)";
                                flag = false;
                                break;
                            }
                        }
                        if (flag && number.length() > 0){
                            name = name.substring(0, front) + "(" + (Integer.valueOf(number) + 1) + ")";
                        }
                        else {
                            name = name + "(1)";
                        }
                    }
                    else {
                        name = name + "(1)";
                    }

                } else {
                    break;
                }
            }

            // 저장 진행
            try {
                path = new File(getFilesDir().getAbsolutePath() + "/previews");

                outputStream = new FileOutputStream(path + "/" + name + "preview.png");
                downloadData.preview.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

                path = new File(getFilesDir().getAbsolutePath() + "/models");

                outputStream = new FileOutputStream(path + "/" + name + ".obj");
                ObjWriter.write(downloadData.obj, outputStream);

                outputStream = new FileOutputStream(path + "/" + name + ".png");
                downloadData.texture.compress(Bitmap.CompressFormat.PNG, 100, outputStream);

                return name;
            } catch(Exception e){
                Log.e(TAG, "download error : ", e);

                return null;
            }
        }
    }

    public void createModel(String name){
        // 다운로드 진행 후 모델 렌더링 추가 및 가구 목록 리로드를 위하여 진행
        // 모델생성 완료 표시
        AlertDialog.Builder alert  = new AlertDialog.Builder(this, R.style.Dialog);
        alert.setTitle("모델 다운");
        alert.setMessage(name + " 모델 다운 완료!");
        alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        alert.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                Intent data = new Intent();
                data.putExtra("modelname", name);
                setResult(RESULT_OK, data);
                finish();
                overridePendingTransition(R.anim.activity_right_enter, R.anim.activity_right_exit);
            }
        });

        alert.show();
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {
        // 다운로드 목록 데코레이터
        // 그리드레이아웃에 적용 가능한 형태
        private int spanCount;
        private int spacing;
        private boolean includeEdge;

        public GridSpacingItemDecoration(int spanCount, int spacing, boolean includeEdge) {
            this.spanCount = spanCount;
            this.spacing = spacing;
            this.includeEdge = includeEdge;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position
            int column = position % spanCount; // item column

            if (includeEdge) {
                outRect.left = spacing - column * spacing / spanCount; // spacing - column * ((1f / spanCount) * spacing)
                outRect.right = (column + 1) * spacing / spanCount; // (column + 1) * ((1f / spanCount) * spacing)

                if (position < spanCount) { // top edge
                    outRect.top = spacing;
                }
                outRect.bottom = spacing; // item bottom
            } else {
                outRect.left = column * spacing / spanCount; // column * ((1f / spanCount) * spacing)
                outRect.right = spacing - (column + 1) * spacing / spanCount; // spacing - (column + 1) * ((1f /    spanCount) * spacing)
                if (position >= spanCount) {
                    outRect.top = spacing; // item top
                }
            }
        }
    }

    public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.ViewHolder>{
        ArrayList<FurnitureData> items = new ArrayList<>();
        Context context;

        public CustomerAdapter(Context context){
            this.context = context;
        }

        @Override
        public int getItemCount(){
            return items.size();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
            LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View itemView = vi.inflate(R.layout.download_list_view, parent, false);

            return new ViewHolder(itemView);
        }

        @Override
        public void onViewAttachedToWindow(@NonNull ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            Animation animation = AnimationUtils.loadAnimation(DownloadActivity.this, R.anim.recyclerview_zoom);
            holder.itemView.startAnimation(animation);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position){
            FurnitureData item = items.get(position);
            holder.setItem(item, position);
        }

        public void addItem(FurnitureData item){
            items.add(item);
        }

        public void resetItems(){
            items = new ArrayList<>();
            items.clear();
        }

        class ViewHolder extends RecyclerView.ViewHolder{
            ImageButton imageButton;
            TextView position_view;
            TextView name;
            TextView down_Count;

            public ViewHolder(@NonNull View itemView){
                super(itemView);
                position_view = (TextView)itemView.findViewById(R.id.position);
                name = (TextView)itemView.findViewById(R.id.name);

                // 다운로드 실시
                imageButton = (ImageButton)itemView.findViewById(R.id.download);
                imageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(DownloadActivity.this, R.style.Dialog);
                        alert.setTitle("가구 모델 다운로드");
                        alert.setMessage(name.getText() + " 모델을 다운로드하시겠습니까?");
                        alert.setPositiveButton("다운", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int temp = Integer.parseInt(position_view.getText().toString());
                                DownData task = new DownData();
                                String address = "http://" + IP_ADDRESS;
                                task.execute(address + furniturelist.get(temp).getMember_preview_link(),
                                        address + furniturelist.get(temp).getMember_file_link(),
                                        address + furniturelist.get(temp).getMember_texture_link(),
                                        furniturelist.get(temp).getMember_id(),
                                        furniturelist.get(temp).getMember_name());
                                dialog.cancel();
                            }
                        });
                        alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        alert.show();
                    }
                });
            }

            public void setItem(FurnitureData item, int position){
                position_view = (TextView)itemView.findViewById(R.id.position);
                imageButton = (ImageButton)itemView.findViewById(R.id.download);
                name = (TextView)itemView.findViewById(R.id.name);
                down_Count = (TextView)itemView.findViewById(R.id.down_Count);

                // 서버에서 받아온 내용 적재
                position_view.setText(String.valueOf(position));
                if (position >= furniturelist.size()){
                    Glide.with(itemView)
                            .load("")
                            .error(R.drawable.image_load_fail)
                            .fallback(R.drawable.download_back)
                            .into(imageButton);
                }
                else {
                    Glide.with(itemView)
                            .load("http://" + IP_ADDRESS + furniturelist.get(position).getMember_preview_link())
                            .error(R.drawable.image_load_fail)
                            .fallback(R.drawable.download_back)
                            .into(imageButton);
                }
                name.setText(item.getMember_name() + "." + item.getMember_extension());
                down_Count.setText(item.getMember_download_count());
            }
        }
    }
}