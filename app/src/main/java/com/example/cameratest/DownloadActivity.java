package com.example.cameratest;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class DownloadActivity extends AppCompatActivity {

    public static DownloadActivity getInstance;
    public RecyclerView recyclerView;

    private static String IP_ADDRESS = "13.125.254.183";
    private static String TAG = "php";

    CustomerAdapter adapter = new CustomerAdapter(DownloadActivity.this);
    private String mJsonString;
    private ArrayList<FurnitureData> furniturelist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.activity_download);
    }

    @Override
    protected void onResume() {
        super.onResume();

        setDownloadList();
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }

    public void backbuttonClick(View view){
        finish();

    }

    public void searchbuttonClick(View view){
        Toast.makeText(getApplicationContext(), "검색 기능은 준비중입니다.", Toast.LENGTH_SHORT).show();
    }

    private class GetData extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = ProgressDialog.show(DownloadActivity.this, "Please Wait", null, true, true);
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();
            Log.d(TAG, "response - " + result);

            ImageView connect = (ImageView)findViewById(R.id.connect_failed);
            ImageView search = (ImageView)findViewById(R.id.no_search);

            // 결과가 도착한 경우
            if (result != null){
                connect.setVisibility(View.INVISIBLE);

                // 내용물이 없는 경우
                if (result.equals("")) {
                    search.bringToFront();
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
                connect.setVisibility(View.VISIBLE);
                Log.d(TAG, "connect failed");
            }
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];
            String postParameters = params[1];

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                }
                else{
                    inputStream = httpURLConnection.getErrorStream();
                }

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
                Log.d(TAG, "GetData : Error ", e);
                errorString = e.toString();

                return null;
            }
        }
    }

    private void showResult(){
        String TAG_JSON = "webnautes";
        String TAG_ID = "id";
        String TAG_NAME = "name";
        String TAG_COUNTRY = "country";

        try {
            JSONObject jsonObject = new JSONObject(mJsonString);
            JSONArray jsonArray = jsonObject.getJSONArray(TAG_JSON);

            for(int i=0;i<jsonArray.length();i++){

                JSONObject item = jsonArray.getJSONObject(i);

                String id = item.getString(TAG_ID);
                String name = item.getString(TAG_NAME);
                String country = item.getString(TAG_COUNTRY);

                FurnitureData furnitureData = new FurnitureData();

                furnitureData.setMember_id(id);
                furnitureData.setMember_name(name);
                furnitureData.setMember_country(country);

                furniturelist.add(furnitureData);
            }

            for(int i = 0; i < furniturelist.size(); i++){
                adapter.addItem(furniturelist.get(i));
            }
            recyclerView.setAdapter(adapter);

        } catch (JSONException e) {
            Log.d(TAG, "showResult : ", e);
        }
    }

    public void setDownloadList(){
        recyclerView = findViewById(R.id.download_list);

        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        int spacing = 20;
        boolean includeEdge = true;
        recyclerView.addItemDecoration(new GridSpacingItemDecoration(2, spacing, includeEdge));

        furniturelist = new ArrayList<>();

        furniturelist.clear();

        GetData task = new GetData();
        task.execute("http://" + IP_ADDRESS + "/getjson.php", "");
    }

    public class GridSpacingItemDecoration extends RecyclerView.ItemDecoration {

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
        public void onBindViewHolder(@NonNull ViewHolder holder, int position){
            FurnitureData item = items.get(position);
            holder.setItem(item);
        }

        public void addItem(FurnitureData item){
            items.add(item);
        }

        class ViewHolder extends RecyclerView.ViewHolder{
            ImageButton imageButton;
            TextView name;
            TextView view_Count;
            TextView down_Count;

            public ViewHolder(@NonNull View itemView){
                super(itemView);
                name = (TextView)itemView.findViewById(R.id.name);

                // 다운로드 실시
                imageButton = (ImageButton)itemView.findViewById(R.id.download);
                imageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(DownloadActivity.this);
                        alert.setTitle("가구 모델 다운로드");
                        alert.setMessage(name.getText() + " 모델을 다운로드하시겠습니까?");
                        alert.setPositiveButton("다운", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                // 다운로드 과정 작성
                                // 가구 목록의 변화가 있으므로 갱신
                                //setFurnitureList();
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

            public void setItem(FurnitureData item){
                name = (TextView)itemView.findViewById(R.id.name);
                view_Count = (TextView)itemView.findViewById(R.id.view_Count);
                down_Count = (TextView)itemView.findViewById(R.id.down_Count);

                // 서버에서 받아온 내용 적재
                name.setText(item.getMember_name());
                view_Count.setText(item.getMember_id());
                down_Count.setText(item.getMember_id());


            }
        }
    }
}