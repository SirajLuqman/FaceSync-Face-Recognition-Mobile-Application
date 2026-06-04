package com.fyp.facesync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.HashMap;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ForgotPasswordActivity extends AppCompatActivity {

    private TextInputEditText etID, etEmail;
    private MaterialButton btnVerify;
    private TextView tvError;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        etID = findViewById(R.id.etResetID);
        etEmail = findViewById(R.id.etResetEmail);
        btnVerify = findViewById(R.id.btnVerifyAndReset);
        tvError = findViewById(R.id.tvResetError);
        btnBack = findViewById(R.id.btnBackToLogin);

        btnBack.setOnClickListener(v -> finish());

        btnVerify.setOnClickListener(v -> {
            String id = etID.getText().toString().trim();
            String email = etEmail.getText().toString().trim();

            // 1. Validation (Button stays active if this fails)
            if (id.isEmpty() || email.isEmpty()) {
                UIHelper.showSystemMsg(this, "Please fill all fields");
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Invalid email format");
                return;
            }

            // 1. Launch the OTP Dialog IMMEDIATELY for better UX
            showOtpDialog(email);

            // 2. Disable button to prevent multiple clicks while waiting for Flask
            btnVerify.setEnabled(false);
            btnVerify.setText("REQUESTING...");
            btnVerify.setAlpha(0.5f);

            // Start a 60-second countdown
            new android.os.CountDownTimer(60000, 1000) {
                public void onTick(long millisUntilFinished) {
                    btnVerify.setText("RETRY IN (" + millisUntilFinished / 1000 + "s)");
                }

                public void onFinish() {
                    btnVerify.setEnabled(true);
                    btnVerify.setAlpha(1.0f);
                    btnVerify.setText("VERIFY DETAILS");
                }
            }.start();

            // 3. Send request
            requestOtpFromServer(id, email);
        });
    }

    private void requestOtpFromServer(String id, String email) {
        HashMap<String, String> map = new HashMap<>();
        map.put("admin_id", id);
        map.put("email", email);

        // --- ADD THESE 3 LINES ---
        android.content.SharedPreferences prefs = getSharedPreferences("FaceSyncPrefs", MODE_PRIVATE);
        String ip = prefs.getString("server_ip", "10.0.2.2");
        String dynamicBaseUrl = "http://" + ip + ":5000/";

        // UPDATE THIS LINE
        ApiService apiService = RetrofitClient.getClient(dynamicBaseUrl).create(ApiService.class);
        apiService.requestOtp(map).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    UIHelper.showSystemMsg(ForgotPasswordActivity.this, "SYSTEM NOTIFICATION: OTP SENT TO EMAIL");
                } else {
                    showError("Admin ID or Email not found.");
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showError("Network Error: Is Flask running?");
            }
        });
    }

    private void showOtpDialog(String email) {
        // 🚀 USE BUILDER TO MATCH THE PASSWORD DIALOG STYLE
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_otp_input, null);
        builder.setView(view);

        AlertDialog dialog = builder.create();

        TextInputEditText etOtp = view.findViewById(R.id.etOtpCode);
        MaterialButton btnVerifyOtp = view.findViewById(R.id.btnVerifyOtp);

        btnVerifyOtp.setOnClickListener(v -> {
            String enteredOtp = etOtp.getText().toString().trim();
            if (!enteredOtp.isEmpty()) {
                verifyOtpOnServer(email, enteredOtp, dialog); // Pass the AlertDialog
            } else {
                etOtp.setError("Enter OTP");
            }
        });

        dialog.show();

        // 🚀 FORCE THE WINDOW TO BE TRANSPARENT SO CORNERS SHOW
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

            // This adds the "margin" around the border by not being full screen
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.90);
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }

    private void verifyOtpOnServer(String email, String otpCode, android.app.Dialog otpDialog) {
        HashMap<String, String> map = new HashMap<>();
        map.put("email", email);
        map.put("otp", otpCode);

        // --- ADD THESE 3 LINES ---
        android.content.SharedPreferences prefs = getSharedPreferences("FaceSyncPrefs", MODE_PRIVATE);
        String ip = prefs.getString("server_ip", "10.0.2.2");
        String dynamicBaseUrl = "http://" + ip + ":5000/";

        // UPDATE THIS LINE
        ApiService apiService = RetrofitClient.getClient(dynamicBaseUrl).create(ApiService.class);
        apiService.verifyOtp(map).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    otpDialog.dismiss();
                    showNewPasswordDialog(email);
                } else {
                    UIHelper.showSystemMsg(ForgotPasswordActivity.this, "ERROR: INVALID OR EXPIRED OTP");
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                UIHelper.showSystemMsg(ForgotPasswordActivity.this, "SYSTEM ERROR: CONNECTION FAILED");
            }
        });
    }

    private void showNewPasswordDialog(String email) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        // 🚀 Inflates the new XML with the FrameLayout wrapper
        View view = getLayoutInflater().inflate(R.layout.dialog_new_password, null);
        builder.setView(view);

        TextInputEditText etNewPassword = view.findViewById(R.id.etNewPassword);
        TextInputEditText etConfirmPassword = view.findViewById(R.id.etConfirmNewPassword);
        MaterialButton btnSave = view.findViewById(R.id.btnSaveNewPassword);
        ImageButton btnClose = view.findViewById(R.id.btnCloseDialog);

        final String PASSWORD_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";

        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().matches(PASSWORD_PATTERN)) {
                    etNewPassword.setError("Need: Capital, Small, Number (Min 8)");
                } else {
                    etNewPassword.setError(null);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        AlertDialog dialog = builder.create();

        btnSave.setOnClickListener(v -> {
            String p1 = etNewPassword.getText().toString().trim();
            String p2 = etConfirmPassword.getText().toString().trim();

            if (!p1.matches(PASSWORD_PATTERN)) {
                etNewPassword.setError("Password too weak!");
                return;
            }
            if (!p1.equals(p2)) {
                etConfirmPassword.setError("Passwords do not match");
                return;
            }
            updatePasswordOnServer(email, p1, dialog);
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        // 🚀 SHOW THE DIALOG FIRST
        dialog.show();

        // 🚀 APPLY THE FIX: Removes the default white background box
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    private void updatePasswordOnServer(String email, String newPassword, AlertDialog dialog) {
        HashMap<String, String> map = new HashMap<>();
        map.put("email", email);
        map.put("new_password", newPassword);

        // --- ADD THESE 3 LINES ---
        android.content.SharedPreferences prefs = getSharedPreferences("FaceSyncPrefs", MODE_PRIVATE);
        String ip = prefs.getString("server_ip", "10.0.2.2");
        String dynamicBaseUrl = "http://" + ip + ":5000/";

        // UPDATE THIS LINE
        ApiService apiService = RetrofitClient.getClient(dynamicBaseUrl).create(ApiService.class);
        apiService.updatePassword(map).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    UIHelper.showSystemMsg(ForgotPasswordActivity.this, "SECURITY UPDATED: ACCESS SECURED");
                    dialog.dismiss();

                    // 🚀 REDIRECT TO LOGIN
                    // CUSTOMIZATION: Bring Login to the front without killing the Recognition page
                    Intent intent = new Intent(ForgotPasswordActivity.this, AdminLoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    startActivity(intent);
                    finish();
                } else {
                    UIHelper.showSystemMsg(ForgotPasswordActivity.this, "SERVER ERROR: RETRY INITIATED");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                UIHelper.showSystemMsg(ForgotPasswordActivity.this, "CRITICAL: NETWORK FAILURE DETECTED");
            }
        });
    }

    private void showError(String message) {
        if (tvError != null) {
            tvError.setText(message);
            tvError.setVisibility(View.VISIBLE);
            tvError.postDelayed(() -> tvError.setVisibility(View.GONE), 3000);
        }
    }
}