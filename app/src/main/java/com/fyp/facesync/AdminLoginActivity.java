package com.fyp.facesync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.HashMap; // Add this line
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminLoginActivity extends AppCompatActivity {

    private TextInputEditText etAdminID, etAdminPassword;
    private MaterialButton btnLogin;
    private TextView tvError, tvForgotPassword;
    private ImageButton btnBack; // Added btnBack

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        // 1. INITIALIZE REFERENCES
        etAdminID = findViewById(R.id.etAdminID);
        etAdminPassword = findViewById(R.id.etAdminPassword);
        btnLogin = findViewById(R.id.btnAdminLoginSubmit);
        tvError = findViewById(R.id.tvLoginError);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);

        // New references for navigation
        btnBack = findViewById(R.id.btnBack);

        // 2. BACK ARROW CLICK (Navigates to Recognition/Main Screen)
        btnBack.setOnClickListener(v -> {
            // Replace 'MainActivity' with whatever your Recognition screen class is named
            // Intent intent = new Intent(AdminLoginActivity.this, RecognitionActivity.class);
            // startActivity(intent);
            finish(); // Simply closes this login screen to go back to the previous one
        });

        // 4. LOGIN BUTTON CLICK
        btnLogin.setOnClickListener(v -> {
            String inputCredential = etAdminID.getText().toString().trim();
            String inputPassword = etAdminPassword.getText().toString().trim();

            if (inputCredential.isEmpty()) {
                showErrorMessage("Email or Admin ID cannot be empty");
                return;
            }

            if (inputCredential.contains("@") && !android.util.Patterns.EMAIL_ADDRESS.matcher(inputCredential).matches()) {
                showErrorMessage("Please enter a valid email address");
                return;
            }

            if (inputPassword.isEmpty()) {
                showErrorMessage("Password cannot be empty");
                return;
            }

            // NEW: Call the server instead of the "if (id.equals("admin"))" check
            performLogin(inputCredential, inputPassword);
        });

        // 5. FORGOT PASSWORD CLICK
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(AdminLoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void performLogin(String emailOrId, String password) {
        // 1. Get the Dynamic IP from Settings
        android.content.SharedPreferences prefs = getSharedPreferences("FaceSyncPrefs", MODE_PRIVATE);
        // Default to 10.0.2.2 for emulator if nothing is saved yet
        String ip = prefs.getString("server_ip", "10.0.2.2");
        String dynamicBaseUrl = "http://" + ip + ":5000/";
        HashMap<String, String> credentials = new HashMap<>();
        credentials.put("email_or_id", emailOrId);
        credentials.put("password", password);

        ApiService apiService = RetrofitClient.getClient(dynamicBaseUrl).create(ApiService.class);
        apiService.adminLogin(credentials).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    tvError.setVisibility(View.GONE);

                    // Check if it's the Master Admin to show a special message
                    if (emailOrId.equals("admin00")) {
                        // Matches your Flask MASTER_ID - Custom Tech Style
                        UIHelper.showSystemMsg(AdminLoginActivity.this, "MASTER SYSTEM ACCESS GRANTED");
                    } else {
                        // Standard Admin - Custom Tech Style
                        UIHelper.showSystemMsg(AdminLoginActivity.this, "WELCOME BACK");
                    }

                    startActivity(new Intent(AdminLoginActivity.this, DashboardActivity.class));
                    finish();
                } else {
                    showErrorMessage("Invalid Admin ID or Password");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showErrorMessage("Network Error: Is the server running?");
            }
        });
    }

    // Helper method to keep code clean
    private void showErrorMessage(String message) {
        // 1. Ensure the old on-screen error text remains hidden
        if (tvError != null) {
            tvError.setVisibility(View.GONE);
        }

        // 2. Trigger the Universal System Notification (Your Cyan design)
        showSystemNotification(message);
    }
    private void showSystemNotification(String message) {
        View layout = getLayoutInflater().inflate(R.layout.layout_custom_toast, null);
        TextView text = layout.findViewById(R.id.toastText);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
}