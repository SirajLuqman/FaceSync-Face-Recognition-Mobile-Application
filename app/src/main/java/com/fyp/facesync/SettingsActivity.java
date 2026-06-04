package com.fyp.facesync;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;
    private TextView tvCurrentIp, tvStatusHeader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        sharedPreferences = getSharedPreferences("FaceSyncPrefs", Context.MODE_PRIVATE);
        tvCurrentIp = findViewById(R.id.tvCurrentIp);
        tvStatusHeader = findViewById(R.id.tvStatusHeader);

        // Load saved IP (Default to Emulator 10.0.2.2)
        String savedIp = sharedPreferences.getString("server_ip", "10.0.2.2");
        tvCurrentIp.setText("Current: " + savedIp);

        // Back button matches original logic
        findViewById(R.id.btnBackSettings).setOnClickListener(v -> finish());

        // 1. SERVER CONFIGURATION
        findViewById(R.id.rowServerConfig).setOnClickListener(v -> showIpConfigDialog());

        // 2. EXPORT LOGS
        findViewById(R.id.rowExportLogs).setOnClickListener(v -> {
            String ip = sharedPreferences.getString("server_ip", "10.0.2.2");
            String downloadUrl = "http://" + ip + ":5000/export_logs";
            downloadFile(downloadUrl);

        });

        // 3. CLEAR LOG HISTORY
        findViewById(R.id.rowClearLogs).setOnClickListener(v -> showClearLogsDialog());

        // 4. ABOUT
        findViewById(R.id.rowAbout).setOnClickListener(v -> showAboutDialog());

        // Immediate check on activity start
        checkSystemHealth(savedIp);
    }

    // --- ADD THIS METHOD INSIDE THE CLASS BRACES ---
    private void downloadFile(String url) {
        android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(android.net.Uri.parse(url));
        request.setTitle("FaceSync Report");
        request.setDescription("Downloading CSV log report...");

        // This makes the download show up in the top notification bar
        request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

        // This saves it to the standard Downloads folder
        request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, "FaceSync_Report.csv");

        android.app.DownloadManager manager = (android.app.DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (manager != null) {
            manager.enqueue(request);
            UIHelper.showSystemMsg(this, "Downloading... Check notifications");
        }
    }

    private void checkSystemHealth(String ip) {
        // Show "Checking" state immediately so user knows it's working
        tvStatusHeader.setText("LINKING TO SERVER...");
        tvStatusHeader.setTextColor(Color.LTGRAY);

        String url = "http://" + ip + ":5000/";
        ApiService apiService = RetrofitClient.getClient(url).create(ApiService.class);

        apiService.checkHealth().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                // SUCCESS: The IP is correct and server is on
                tvStatusHeader.setText("SECURE CONNECTION");
                tvStatusHeader.setTextColor(Color.parseColor("#00FFFF")); // Cyan
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // FAILURE: Wrong IP, Port blocked, or Laptop is off
                tvStatusHeader.setText("SERVER UNREACHABLE");
                tvStatusHeader.setTextColor(Color.RED);

                // Optional: Tell them exactly what to check
                UIHelper.showSystemMsg(SettingsActivity.this, "DIAGNOSTIC: CHECK IP OR WI-FI CONNECTION");
            }
        });
    }

    private void showIpConfigDialog() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_settings_tech, null);

        // 1. Initialize Views
        com.google.android.material.textfield.TextInputEditText editText = view.findViewById(R.id.diagEditText);
        com.google.android.material.button.MaterialButton btnSave = view.findViewById(R.id.diagBtnPositive);
        view.findViewById(R.id.diagInputLayout).setVisibility(android.view.View.VISIBLE);

        String currentIp = sharedPreferences.getString("server_ip", "10.0.2.2");
        editText.setText(currentIp);

        // 2. FORCE BUTTON VISIBILITY (The Fix)
        // We force a dark background and neon text so it's always visible
        btnSave.setBackgroundColor(android.graphics.Color.parseColor("#00FFFF"));
        btnSave.setTextColor(android.graphics.Color.parseColor("#000F14"));     // Neon Cyan
        btnSave.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#00FFFF")));
        btnSave.setStrokeWidth(2);

        // 3. Create the Dialog
        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(view)
                .create();

        // 4. Force Window Properties
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
        }

        btnSave.setOnClickListener(v -> {
            String newIp = editText.getText().toString().trim();
            if (!newIp.isEmpty()) {
                sharedPreferences.edit().putString("server_ip", newIp).apply();
                tvCurrentIp.setText("Current: " + newIp);
                checkSystemHealth(newIp);
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void showClearLogsDialog() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_settings_tech, null);

        TextView title = view.findViewById(R.id.diagTitle);
        TextView message = view.findViewById(R.id.diagMessage);
        com.google.android.material.button.MaterialButton btnClear = view.findViewById(R.id.diagBtnPositive);

        title.setText("Wipe Database");
        message.setText("This will permanently delete all logs. This action cannot be undone.");
        btnClear.setText("CLEAR HISTORY");
        btnClear.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FF4444"))); // Danger Red
        btnClear.setTextColor(android.graphics.Color.WHITE);

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(view)
                .setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .create();

        btnClear.setOnClickListener(v -> {
            String ip = sharedPreferences.getString("server_ip", "10.0.2.2");
            ApiService apiService = RetrofitClient.getClient("http://" + ip + ":5000/").create(ApiService.class);
            apiService.clearLogs().enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                    if (response.isSuccessful()) {
                        UIHelper.showSystemMsg(SettingsActivity.this, "SYSTEM MAINTENANCE: DATABASE CLEARED");
                    }
                    dialog.dismiss();
                }
                @Override
                public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                    UIHelper.showSystemMsg(SettingsActivity.this, "SYSTEM FAILURE: CONNECTION REFUSED");
                    dialog.dismiss();
                }
            });
        });

        dialog.show();
    }

    private void showAboutDialog() {
        android.view.View view = getLayoutInflater().inflate(R.layout.dialog_settings_tech, null);

        ((TextView)view.findViewById(R.id.diagTitle)).setText("System Info");
        ((TextView)view.findViewById(R.id.diagMessage)).setText("FaceSync v1.0\nSmart Access Control\n© 2026 FYP Project");

        com.google.android.material.button.MaterialButton btnClose = view.findViewById(R.id.diagBtnPositive);
        btnClose.setText("CLOSE");

        androidx.appcompat.app.AlertDialog dialog = new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setView(view)
                .setBackground(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                .create();

        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }
}