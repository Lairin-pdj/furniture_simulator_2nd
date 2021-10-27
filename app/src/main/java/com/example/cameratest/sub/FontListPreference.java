package com.example.cameratest.sub;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.preference.ListPreference;

import com.example.cameratest.R;

import java.util.ArrayList;
import java.util.List;

public class FontListPreference extends ListPreference {
    private class FontPreferenceAdapter extends ArrayAdapter<String> {
        private List<String> mItems;

        FontPreferenceAdapter(Context context, List<String> items) {
            super(context, android.R.layout.simple_list_item_single_choice, items);
            mItems = items;
        }

        @Override
        public @NonNull
        View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(android.R.layout.simple_list_item_single_choice, parent, false);


            TextView iconName;
            iconName = convertView.findViewById(android.R.id.text1);

            iconName.setText(mItems.get(position));
            switch (mItems.get(position)){
                case "나눔R":
                    iconName.setTypeface(getContext().getResources().getFont(R.font.nanumroundr));
                    break;
                case "나눔B":
                    iconName.setTypeface(getContext().getResources().getFont(R.font.nanumroundb));
                    break;
                case "카페":
                    iconName.setTypeface(getContext().getResources().getFont(R.font.cafe24surround));
                    break;
                case "에스코드":
                    iconName.setTypeface(getContext().getResources().getFont(R.font.scdream3));
                    break;
                default:
                    iconName.setTypeface(Typeface.DEFAULT);
                    break;
            }


            return convertView;
        }
    }

    public FontListPreference(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public FontListPreference(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FontListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FontListPreference(Context context) {
        super(context);
    }

    @Override
    protected void onClick() {
        AlertDialog.Builder alert  = new AlertDialog.Builder(getContext(), R.style.DialogSmall);
        alert.setTitle(getTitle());
        List<String> fonts = new ArrayList<>();
        for (CharSequence a : getEntries()){
            fonts.add(String.valueOf(a));
        }
        FontPreferenceAdapter fontPreferenceAdapter = new FontPreferenceAdapter(getContext(), fonts);
        alert.setSingleChoiceItems(fontPreferenceAdapter, findIndexOfValue(getValue()), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setValueIndex(which);
                dialog.cancel();
            }
        });
        alert.setPositiveButton("취소", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        alert.show();
    }
}