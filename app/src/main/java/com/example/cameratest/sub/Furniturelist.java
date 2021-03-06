package com.example.cameratest.sub;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cameratest.R;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.example.cameratest.MainActivity.getInstance;

public class Furniturelist extends Fragment {
    private final String TAG = "Furniturelist";

    public static Furniturelist furniturelist;
    public RecyclerView recyclerView;
    public Parcelable recyclerViewState;
    private boolean isDeco = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        furniturelist = this;
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.furniturelist_main, container, false);

        recyclerView = rootView.findViewById(R.id.furniture_list);

        setFurnitureList();

        return rootView;
    }

    public void setFurnitureList(){

        // ????????? ????????? ??????
        boolean flag = false;
        if (recyclerView.getLayoutManager() != null) {
            recyclerViewState = recyclerView.getLayoutManager().onSaveInstanceState();
            flag = true;
        }

        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);

        // ??????????????? ??????
        if (!isDeco) {
            recyclerView.addItemDecoration(new SpacingItemDecoration(8));
            isDeco = true;
        }

        CustomerAdapter adapter = new CustomerAdapter(getContext());

        // ?????? ????????? ????????? ?????? ?????? ??????
        File path = new File(getContext().getFilesDir().getAbsolutePath() + "/models");
        File[] files = path.listFiles();
        List<String> filenames = new ArrayList<>();
        for(int i = 0; i < files.length; i++){
            String temp = files[i].getName();
            int idx = temp.lastIndexOf(".");
            if(temp.substring(idx + 1).equals("obj")){
                filenames.add(temp.substring(0, idx));
            }
        }

        // ????????? ?????? ??????????????? ??????
        for(int i = 0; i < filenames.size(); i++){
            adapter.addItem(new String(filenames.get(i)));
        }

        // ?????? ?????? ??????
        recyclerView.setAdapter(adapter);
        if (flag) {
            recyclerView.getLayoutManager().onRestoreInstanceState(recyclerViewState);
        }
    }

    public class CustomerAdapter extends RecyclerView.Adapter<CustomerAdapter.ViewHolder>{
        ArrayList<String> items = new ArrayList<>();
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
        public CustomerAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType){
            LayoutInflater vi = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View itemView = vi.inflate(R.layout.list_view, parent, false);

            return new CustomerAdapter.ViewHolder(itemView);
        }

        @Override
        public void onBindViewHolder(@NonNull CustomerAdapter.ViewHolder holder, int position){
            String item = items.get(position);
            holder.setItem(item);
        }

        @Override
        public void onViewAttachedToWindow(@NonNull CustomerAdapter.ViewHolder holder) {
            super.onViewAttachedToWindow(holder);
            if (!getInstance.isMode) {
                Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.recyclerview_zoom);
                holder.itemView.startAnimation(animation);
            }
        }

        public void addItem(String item){
            items.add(item);
        }

        class ViewHolder extends RecyclerView.ViewHolder{
            ImageButton imageButton;
            Button delButton;
            Button uploadButton;
            TextView textView;

            public ViewHolder(@NonNull View itemView){
                super(itemView);

                textView = (TextView)itemView.findViewById(R.id.textView);

                // ????????????
                delButton = (Button)itemView.findViewById((R.id.delete));
                if (getInstance.isDel){
                    delButton.setVisibility(View.VISIBLE);
                }
                else{
                    delButton.setVisibility(View.INVISIBLE);
                }
                delButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(getContext(), R.style.Dialog);
                        alert.setTitle("?????? ?????? ??????");
                        alert.setMessage(textView.getText() + " ????????? ????????????????");
                        alert.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                //?????? ??????
                                File path = new File(getContext().getFilesDir().getAbsolutePath() + "/models");
                                File[] files = path.listFiles();

                                // ????????? ?????? ?????? ??? ???????????? ????????? ?????? ??????
                                for(int i = 0; i < files.length; i++){
                                    String temp = files[i].getName();
                                    int idx = temp.lastIndexOf(".");
                                    if (temp.substring(0, idx).equals(textView.getText())) {
                                        files[i].delete();
                                    }
                                }

                                //????????? ??????
                                path = new File(getContext().getFilesDir().getAbsolutePath() + "/previews/" + textView.getText() + "preview.png");
                                path.delete();

                                // ?????? ????????? ????????? ?????? ??????
                                for (int i = 0; i < getInstance.anchors.size(); i++){
                                    if (getInstance.anchors.get(i).name.equals(textView.getText())){
                                        getInstance.anchors.remove(i);
                                        i--;
                                    }
                                }
                                getInstance.selectedAnchor = -1;

                                //????????? ????????? ?????? ??????
                                if (getInstance.selected != null && getInstance.selected.equals(textView.getText())){
                                    getInstance.selected = null;
                                    getInstance.selectPreview.setVisibility(View.INVISIBLE);
                                }

                                // ?????? ????????? ????????? ???????????? ??????
                                getInstance.isMode = true;
                                setFurnitureList();
                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        getInstance.isMode = false;
                                    }
                                }, 100);

                                BitmapDrawable bd = (BitmapDrawable) imageButton.getDrawable();
                                Bitmap temp = bd.getBitmap();

                                LayoutInflater inflater = getLayoutInflater();
                                View toastDesign = inflater.inflate(R.layout.toast, null);
                                TextView text = toastDesign.findViewById(R.id.toast_text);
                                text.setText(textView.getText() + " ????????? ??????????????? ???????????????.");
                                ImageView image = toastDesign.findViewById(R.id.toast_image);
                                image.setImageBitmap(temp);
                                Toast toast = new Toast(getContext());
                                toast.setGravity(Gravity.BOTTOM, 0, 150);
                                toast.setDuration(Toast.LENGTH_SHORT);
                                toast.setView(toastDesign);
                                toast.show();
                                dialog.cancel();
                            }
                        });
                        alert.setNegativeButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        alert.show();
                    }
                });

                // ????????? ??????
                uploadButton = (Button)itemView.findViewById((R.id.upload_check));
                if (getInstance.isUpload){
                    uploadButton.setVisibility(View.VISIBLE);
                }
                else{
                    uploadButton.setVisibility(View.INVISIBLE);
                }
                uploadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder alert = new AlertDialog.Builder(getContext(), R.style.Dialog);
                        alert.setTitle("?????? ?????? ?????????");
                        alert.setMessage(textView.getText() + " ????????? ???????????????????");
                        alert.setPositiveButton("?????????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                // ????????? ??????
                                UpData task = new UpData();
                                task.execute("http://" + getInstance.IP_ADDRESS + "/upload.php", textView.getText().toString());

                                dialog.cancel();
                            }
                        });
                        alert.setNegativeButton("??????", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                        alert.show();
                    }
                });

                // ???????????????
                imageButton = (ImageButton)itemView.findViewById(R.id.button);
                imageButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // ????????? ???????????? ?????? ?????? ?????? ??????
                        if (!getInstance.isDel && !getInstance.isUpload) {
                            BitmapDrawable bd = (BitmapDrawable) imageButton.getDrawable();
                            Bitmap temp = bd.getBitmap();

                            LayoutInflater inflater = getLayoutInflater();
                            View toastDesign = inflater.inflate(R.layout.toast, null);
                            TextView text = toastDesign.findViewById(R.id.toast_text);
                            text.setText(textView.getText() + " ????????? ??????????????????.");
                            ImageView image = toastDesign.findViewById(R.id.toast_image);
                            image.setImageBitmap(temp);
                            Toast toast = new Toast(getContext());
                            toast.setGravity(Gravity.BOTTOM, 0, 150);
                            toast.setDuration(Toast.LENGTH_SHORT);
                            toast.setView(toastDesign);
                            toast.show();

                            getInstance.setSelectedFurniture(textView.getText().toString(), temp);
                            getInstance.furnitureMenuClick(null);
                        }
                        // ????????? ????????? ??????
                        else if (getInstance.isUpload){
                            // ?????? ????????? ??????????????? ????????????
                            String [] strings = {"andy", "desk", "chair", "lamp"};

                            if (!Arrays.asList(strings).contains(textView.getText())) {
                                uploadButton.callOnClick();
                            }
                            else{
                                LayoutInflater inflater = getLayoutInflater();
                                View toastDesign = inflater.inflate(R.layout.toast, null);
                                TextView text = toastDesign.findViewById(R.id.toast_text);
                                text.setText("?????? ????????? ???????????? ??? ?????????.");
                                ImageView image = toastDesign.findViewById(R.id.toast_image);
                                image.setVisibility(View.GONE);
                                Toast toast = new Toast(getContext());
                                toast.setGravity(Gravity.BOTTOM, 0, 150);
                                toast.setDuration(Toast.LENGTH_SHORT);
                                toast.setView(toastDesign);
                                toast.show();
                            }
                        }
                        // ?????? ????????? ??????
                        else {
                            // ?????? ????????? ???????????? ????????????
                            String [] strings = {"andy", "desk", "chair", "lamp"};

                            if (!Arrays.asList(strings).contains(textView.getText())) {
                                delButton.callOnClick();
                            }
                            else{
                                LayoutInflater inflater = getLayoutInflater();
                                View toastDesign = inflater.inflate(R.layout.toast, null);
                                TextView text = toastDesign.findViewById(R.id.toast_text);
                                text.setText("?????? ????????? ????????? ??? ?????????.");
                                ImageView image = toastDesign.findViewById(R.id.toast_image);
                                image.setVisibility(View.GONE);
                                Toast toast = new Toast(getContext());
                                toast.setGravity(Gravity.BOTTOM, 0, 150);
                                toast.setDuration(Toast.LENGTH_SHORT);
                                toast.setView(toastDesign);
                                toast.show();
                            }
                        }
                    }
                });
            }

            public void setItem(String item){
                // ????????? ?????? ?????? ?????? ??? ??????
                File path = new File(getContext().getFilesDir().getAbsolutePath() + "/previews");
                File[] files = path.listFiles();
                InputStream is = null;
                Bitmap bitmap;
                try {
                    File check = new File(path + "/" + item + "preview.png");
                    if(check.exists()){
                        is = new FileInputStream(check);
                        bitmap = BitmapFactory.decodeStream(is);
                        imageButton.setImageBitmap(bitmap);
                    }else {
                        imageButton.setImageResource(R.drawable.furniture_app_icon);
                    }
                }catch (IOException e){

                }
                textView.setText(item);

                // ?????? ????????? ???????????? ???????????? ??? ?????????
                String [] strings = {"andy", "desk", "chair", "lamp"};
                delButton = (Button)itemView.findViewById((R.id.delete));
                uploadButton = (Button)itemView.findViewById((R.id.upload_check));
                if (Arrays.asList(strings).contains(textView.getText())) {
                    delButton.setVisibility(View.INVISIBLE);
                    uploadButton.setVisibility(View.INVISIBLE);
                }
            }
        }
    }

    public class SpacingItemDecoration extends RecyclerView.ItemDecoration {
        // ????????? ?????? ?????? ????????? ???????????? ???????????????
        private final int spacing;

        public SpacingItemDecoration(int spacing) {
            this.spacing = spacing;
        }

        @Override
        public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
            int position = parent.getChildAdapterPosition(view); // item position

            if (position != 0) {
                outRect.left = spacing;
            }
        }
    }

    private class UpData extends AsyncTask<String, Void, String> {
        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();

            progressDialog = new ProgressDialog(getContext(), R.style.DialogSmall);
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setMessage("?????? ???????????? ????????? ????????????.");
            progressDialog.setCancelable(false);
            progressDialog.setIndeterminate(true);
            progressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            progressDialog.dismiss();

            // ????????? ????????? ??????
            if (result != null){
                // ???????????? ?????? ??????
                if (result.equals("")) {
                    Log.d(TAG, "UpData : error");
                }
                // ???????????? ?????? ??????
                else {
                    // ???????????? ??????
                    getInstance.uploadModel(result);
                }
            }
            // ????????? ????????? ??????
            else{
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext(), R.style.Dialog);
                alert.setTitle("?????? ????????? ??????");
                alert.setMessage("????????? ????????? ??????????????????.");
                alert.setPositiveButton("??????", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                alert.show();
            }
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];
            String name = params[1];

            try {
                HttpURLConnection conn = null;
                DataOutputStream dos = null;
                String lineEnd = "\r\n";
                String twoHyphens = "--";
                String boundary = "*****";
                int bytesRead, bytesAvailable, bufferSize;
                byte[] buffer;
                int maxBufferSize = 1 * 1024 * 1024;
                File sourceFile;
                FileInputStream fileInputStream;

                URL url = new URL(serverURL);

                // Open a HTTP  connection to  the URL
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true); // Allow Inputs
                conn.setDoOutput(true); // Allow Outputs
                conn.setReadTimeout(5000);
                conn.setConnectTimeout(5000);
                conn.setUseCaches(false); // Don't use a Cached Copy
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);

                DataOutputStream wr = new DataOutputStream(conn.getOutputStream());


                // ????????? ?????????
                wr.writeBytes("\r\n--" + boundary + "\r\n");
                wr.writeBytes("Content-Disposition: form-data; name=\"name\"\r\n\r\n" + URLEncoder.encode(name, "UTF-8"));
                wr.writeBytes("\r\n--" + boundary + "\r\n");


                // obj ??????
                sourceFile = new File(getContext().getFilesDir().getAbsolutePath() + "/models/" + name + ".obj");
                fileInputStream = new FileInputStream(sourceFile);

                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"obj\";filename=\"" + name + ".obj\"" + lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


                // ????????? ??????
                sourceFile = new File(getContext().getFilesDir().getAbsolutePath() + "/models/" + name + ".png");
                fileInputStream = new FileInputStream(sourceFile);

                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"tex\";filename=\"" + name + ".png\"" + lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


                // ????????? ??????
                sourceFile = new File(getContext().getFilesDir().getAbsolutePath() + "/previews/" + name + "preview.png");
                fileInputStream = new FileInputStream(sourceFile);

                dos = new DataOutputStream(conn.getOutputStream());
                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"pre\";filename=\"" + name + "_preview.png\"" + lineEnd);
                dos.writeBytes(lineEnd);

                // create a buffer of  maximum size
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);


                // ?????? ??????
                int responseStatusCode = conn.getResponseCode();
                Log.d(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if(responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = conn.getInputStream();
                }
                else{
                    inputStream = conn.getErrorStream();
                }

                // ?????? ????????? ??????
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
                Log.d(TAG, "UpData : Error ", e);
                errorString = e.toString();

                return null;
            }
        }
    }
}