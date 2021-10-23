package com.example.cameratest.sub;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;

import com.example.cameratest.sub.Fragment_2d;
import com.example.cameratest.sub.Fragment_3d;

import java.util.ArrayList;
import java.util.List;

public class PagerAdapter extends FragmentStatePagerAdapter {

    List<Fragment> fragments = new ArrayList<>();

    public PagerAdapter(@NonNull FragmentManager fm) {
        super(fm);
        fragments.add(new Fragment_2d());
        fragments.add(new Fragment_3d());
    }

    @NonNull
    @Override
    public Fragment getItem(int position) {
        return fragments.get(position);
    }

    @Override
    public int getCount() {
        return fragments.size();
    }
}
