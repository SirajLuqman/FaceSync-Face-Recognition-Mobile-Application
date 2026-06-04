package com.fyp.facesync;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DashboardActivity extends AppCompatActivity {

    private MaterialCardView cardLiveMonitor, cardRegistration, cardUserManagement, cardLogs, cardSettings, cardAdminSignup;
    private ImageButton btnLogout;
    private TextView tvStatusHeader;
    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // 1. Initialize View Components
        initializeViews();

        // 2. Set Click Listeners for Dashboard Features
        setupNavigation();

        // 3. Logout Logic
        btnLogout.setOnClickListener(v -> {
            Intent intent = new Intent(DashboardActivity.this, RecognitionActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            UIHelper.showSystemMsg(this, "Logged out successfully");
        });
    }

    // This ensures status refreshes whenever you return to this screen
    @Override
    protected void onResume() {
        super.onResume();
        String savedIp = sharedPreferences.getString("server_ip", "10.0.2.2");
        checkSystemHealth(savedIp);
    }

    private void initializeViews() {
        cardLiveMonitor = findViewById(R.id.cardLiveMonitor);
        cardRegistration = findViewById(R.id.cardRegistration);
        cardUserManagement = findViewById(R.id.cardUserManagement);
        cardLogs = findViewById(R.id.cardLogs);
        cardSettings = findViewById(R.id.cardSettings);
        btnLogout = findViewById(R.id.btnLogout);
        cardAdminSignup = findViewById(R.id.cardAdminSignup);
        tvStatusHeader = findViewById(R.id.tvStatusHeader);
        sharedPreferences = getSharedPreferences("FaceSyncPrefs", Context.MODE_PRIVATE);
    }

    private void checkSystemHealth(String ip) {
        // Initial state
        tvStatusHeader.setText("● LINKING SYSTEM...");
        tvStatusHeader.setTextColor(Color.parseColor("#55FFFFFF")); // Grayish

        String url = "http://" + ip + ":5000/";
        ApiService apiService = RetrofitClient.getClient(url).create(ApiService.class);

        apiService.checkHealth().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                // SUCCESS: Replaces "System Online" with the Secure Connection look
                tvStatusHeader.setText("● SECURE CONNECTION");
                tvStatusHeader.setTextColor(Color.parseColor("#00FFFF")); // Cyan
                tvStatusHeader.setAlpha(1.0f);
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // FAILURE: Alert the admin
                tvStatusHeader.setText("● SERVER UNREACHABLE");
                tvStatusHeader.setTextColor(Color.RED);
                tvStatusHeader.setAlpha(1.0f);
            }
        });
    }

    private void setupNavigation() {
        cardLiveMonitor.setOnClickListener(v ->
                startActivity(new Intent(this, LiveMonitorActivity.class)));

        cardRegistration.setOnClickListener(v ->
                startActivity(new Intent(this, RegistrationActivity.class)));

        cardUserManagement.setOnClickListener(v ->
                startActivity(new Intent(this, UserManagementActivity.class)));

        cardLogs.setOnClickListener(v ->
                startActivity(new Intent(this, LogsActivity.class)));

        cardAdminSignup.setOnClickListener(v ->
                startActivity(new Intent(this, AdminSignupActivity.class)));

        cardSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
    }
}