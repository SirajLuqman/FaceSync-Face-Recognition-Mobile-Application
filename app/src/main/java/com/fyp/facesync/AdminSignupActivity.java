// Option - Admin Signup Logic with Detailed Comments
package com.fyp.facesync;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import java.util.HashMap;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class AdminSignupActivity extends AppCompatActivity {

    // Member variables for UI components
    private TextInputEditText etName, etEmail, etID, etPassword, etConfirmPassword;
    private MaterialButton btnSignup;
    private TextView tvError, tvLoginLink;
    private ImageButton btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_signup);

        initializeViews();

        // 1. BACK ARROW: Matches RegistrationActivity behavior
        btnBack.setOnClickListener(v -> {
            // We finish this activity to slide back to the Dashboard
            finish();
        });

        // 2. LOGIN LINK: Takes user to Login page (Standard transition)
        tvLoginLink.setOnClickListener(v -> {
            startActivity(new Intent(AdminSignupActivity.this, AdminLoginActivity.class));
            finish();
        });

        // 3. SIGNUP SUBMIT
        btnSignup.setOnClickListener(v -> {
            if (validateInputs()) {
                processRegistration();
            }
        });
    }

    /**
     * Finds and assigns UI elements from the layout.
     */
    private void initializeViews() {
        btnBack = findViewById(R.id.btnBackToLogin);
        etName = findViewById(R.id.etSignupName);
        etID = findViewById(R.id.etSignupID);
        etEmail = findViewById(R.id.etSignupEmail);
        etPassword = findViewById(R.id.etSignupPassword);
        etConfirmPassword = findViewById(R.id.etSignupConfirmPassword);
        btnSignup = findViewById(R.id.btnAdminSignupSubmit);
        tvError = findViewById(R.id.tvSignupError);
        tvLoginLink = findViewById(R.id.tvLoginLink);
    }

    /**
     * Validates user inputs against system requirements.
     * @return true if inputs are valid, false otherwise.
     */
    private boolean validateInputs() {
        String name = etName.getText().toString().trim();
        String adminId = etID.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();
        String confirmPass = etConfirmPassword.getText().toString().trim();

        if (name.isEmpty() || adminId.isEmpty() || email.isEmpty() || pass.isEmpty()) {
            showError("All fields are required");
            return false;
        }

        // 2. Email Check (Proper Format)
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            showError("Invalid email format (e.g., name@email.com)");
            return false;
        }

        // 4. Advanced Password Security
        // Minimum 8 characters, at least 1 Uppercase, 1 Lowercase, 1 Number, and 1 Special Character
        String passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$";

        if (pass.isEmpty()) {
            showError("Password is required");
            return false;
        } else if (pass.length() < 8) {
            showError("Password must be at least 8 characters");
            return false;
        } else if (!pass.matches(passwordPattern)) {
            showError("Use: Upper, Lower, Number & Special Char (@#$%^&+=!)");
            return false;
        }

        if (!pass.equals(confirmPass)) {
            showError("Passwords do not match");
            return false;
        }
        return true;
    }

    /**
     * Placeholder for Backend Integration (Flask API).
     */
    private void processRegistration() {
        HashMap<String, String> params = new HashMap<>();
        params.put("username", etName.getText().toString().trim());
        params.put("email", etEmail.getText().toString().trim());
        params.put("password", etPassword.getText().toString().trim());

        // FIX: You must add this line so the server receives the ID!
        params.put("admin_id_code", etID.getText().toString().trim());

        // --- ADD THESE 3 LINES FOR DYNAMIC IP ---
        android.content.SharedPreferences prefs = getSharedPreferences("FaceSyncPrefs", MODE_PRIVATE);
        String ip = prefs.getString("server_ip", "10.0.2.2");
        String dynamicBaseUrl = "http://" + ip + ":5000/";

        // UPDATE THIS LINE to use the Dynamic URL
        ApiService apiService = RetrofitClient.getClient(dynamicBaseUrl).create(ApiService.class);
        apiService.adminSignup(params).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    UIHelper.showSystemMsg(AdminSignupActivity.this, "DATABASE UPDATED: NEW ADMIN SAVED");
                    startActivity(new Intent(AdminSignupActivity.this, AdminLoginActivity.class));
                    finish();
                } else {
                    showError("Registration failed. Check server terminal.");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showError("Network Error: Is the Python server running?");
            }
        });
    }

    /**
     * Helper to display error messages using the Universal Theme.
     */
    private void showError(String message) {
        // 1. Keep the on-screen error hidden to maintain a clean UI
        if (tvError != null) {
            tvError.setVisibility(View.GONE);
        }

        // 2. Trigger the branded Universal System Notification
        showSystemNotification(message);
    }

    /**
     * Inflates the custom XML layout for the notification.
     */
    private void showSystemNotification(String message) {
        View layout = getLayoutInflater().inflate(R.layout.layout_custom_toast, null);
        TextView text = layout.findViewById(R.id.toastText);
        text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show();
    }
} // Final closing brace of the class