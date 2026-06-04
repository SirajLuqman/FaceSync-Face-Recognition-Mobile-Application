package com.fyp.facesync;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.HashMap;
import java.util.Map;

public class UserPagerAdapter extends FragmentStateAdapter {
    // Keep track of active fragments to call their filter methods
    private final Map<Integer, UserListFragment> fragmentMap = new HashMap<>();

    public UserPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        UserListFragment fragment = UserListFragment.newInstance(position == 0 ? "user" : "admin");
        fragmentMap.put(position, fragment);
        return fragment;
    }

    @Override
    public int getItemCount() { return 2; }

    public UserListFragment getFragment(int position) {
        return fragmentMap.get(position);
    }
}