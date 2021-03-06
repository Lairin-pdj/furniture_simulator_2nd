package com.example.cameratest.sub;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.example.cameratest.R;

import java.io.File;
import java.util.Arrays;

public class PreferenceFragment extends PreferenceFragmentCompat {

    SharedPreferences pref;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
        ListPreference font = (ListPreference)findPreference("font");
        pref = PreferenceManager.getDefaultSharedPreferences(getContext());
        font.setSummary(pref.getString("font", "기본"));

        Preference planeImage = findPreference("planeimage");
        planeImage.setEnabled(false);
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        String key = preference.getKey();
        switch (key) {
            case "cache":
                AlertDialog.Builder alert = new AlertDialog.Builder(getContext(), R.style.Dialog);
                alert.setTitle("캐시 및 임시 데이터 삭제");
                alert.setMessage("앱의 캐시와 임시 데이터를 전부 삭제할까요?");
                alert.setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //캐시 삭제
                        File path = new File(getContext().getCacheDir().getAbsolutePath());
                        if (path.exists()) {
                            setDirEmpty(path);
                        }

                        //임시데이터 삭제
                        path = new File(getContext().getFilesDir().getAbsolutePath() + "/TEMP");
                        if (path.exists()) {
                            File[] files = path.listFiles();

                            for (File file : files) {
                                file.delete();
                            }
                        }

                        dialog.cancel();

                        LayoutInflater inflater = getLayoutInflater();
                        View toastDesign = inflater.inflate(R.layout.toast, null);
                        TextView text = toastDesign.findViewById(R.id.toast_text);
                        text.setText("캐시 삭제 완료");
                        ImageView image = toastDesign.findViewById(R.id.toast_image);
                        image.setVisibility(View.GONE);
                        Toast toast = new Toast(getContext());
                        toast.setGravity(Gravity.BOTTOM, 0, 150);
                        toast.setDuration(Toast.LENGTH_SHORT);
                        toast.setView(toastDesign);
                        toast.show();
                    }
                });
                alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                alert.show();
                break;
            case "data":
                alert = new AlertDialog.Builder(getContext(), R.style.Dialog);
                alert.setTitle("가구 데이터 삭제");
                alert.setMessage("기본 가구를 제외한 가구 데이터를 전부 삭제할까요?");
                alert.setPositiveButton("삭제", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //가구 데이터 삭제
                        File path = new File(getContext().getFilesDir().getAbsolutePath() + "/models");
                        File[] files = path.listFiles();

                        // 기본 가구가 아닌 가구 삭제
                        String [] strings = {"andy", "desk", "chair", "lamp"};
                        for(int i = 0; i < files.length; i++){
                            String temp = files[i].getName();
                            int idx = temp.lastIndexOf(".");
                            if (temp.substring(idx + 1, temp.length()).equals("obj")) {
                                if (!Arrays.asList(strings).contains(temp.substring(0, idx))){
                                    files[i].delete();
                                    File png = new File(path.getAbsolutePath() + "/" + temp.substring(0, idx) + ".png");
                                    png.delete();
                                }
                            }
                        }

                        //프리뷰 제거
                        path = new File(getContext().getFilesDir().getAbsolutePath() + "/previews/");
                        files = path.listFiles();
                        for(int i = 0; i < files.length; i++){
                            String temp = files[i].getName();
                            int idx = temp.lastIndexOf(".");
                            if (!Arrays.asList(strings).contains(temp.substring(0, idx - 7))) {
                                files[i].delete();
                            }
                        }

                        dialog.cancel();

                        LayoutInflater inflater = getLayoutInflater();
                        View toastDesign = inflater.inflate(R.layout.toast, null);
                        TextView text = toastDesign.findViewById(R.id.toast_text);
                        text.setText("가구 데이터 삭제 완료");
                        ImageView image = toastDesign.findViewById(R.id.toast_image);
                        image.setVisibility(View.GONE);
                        Toast toast = new Toast(getContext());
                        toast.setGravity(Gravity.BOTTOM, 0, 150);
                        toast.setDuration(Toast.LENGTH_SHORT);
                        toast.setView(toastDesign);
                        toast.show();
                    }
                });
                alert.setNegativeButton("취소", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });
                alert.show();
                break;
        }
        return super.onPreferenceTreeClick(preference);
    }

    public void setDirEmpty(File path){
        if (path.exists()) {
            File[] files = path.listFiles();

            for (File child : files) {
                if (child.isDirectory()) {
                    setDirEmpty(new File(child.getAbsolutePath()));
                } else {
                    child.delete();
                }
            }
        }
    }
}
