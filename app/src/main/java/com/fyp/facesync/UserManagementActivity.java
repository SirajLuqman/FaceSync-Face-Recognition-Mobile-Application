package com.fyp.facesync;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class UserManagementActivity extends AppCompatActivity {
    // Member variables (Use these!)
    private UserPagerAdapter adapter;
    private ViewPager2 viewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_management);

        // 1. Initialize Views (Removed "ViewPager2" prefix here to use the member variable)
        viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout = findViewById(R.id.tabLayout);
        ImageButton btnBack = findViewById(R.id.btnBack);
        EditText etSearch = findViewById(R.id.etSearch); // Added initialization

        // 2. Navigation Logic
        btnBack.setOnClickListener(v -> finish());

        // 3. Setup ViewPager and Tabs (Removed "UserPagerAdapter" prefix)
        adapter = new UserPagerAdapter(this);
        viewPager.setAdapter(adapter);

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "PERSONS" : "ADMINS");
        }).attach();

        // 4. Search logic: Listen for typing
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Now 'adapter' refers to the one initialized above
                UserListFragment currentFragment = adapter.getFragment(viewPager.getCurrentItem());
                if (currentFragment != null) {
                    currentFragment.filter(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
}