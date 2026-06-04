package com.fyp.facesync;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SplashActivity extends AppCompatActivity {

    private SharedPreferences sharedPreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        setContentView(R.layout.activity_splash);

        sharedPreferences = getSharedPreferences("FaceSyncPrefs", Context.MODE_PRIVATE);

        ImageView logo = findViewById(R.id.imgLogo);
        TextView txtTitle = findViewById(R.id.txtAppName);
        Animation logoAnim = AnimationUtils.loadAnimation(this, R.anim.logo_jump);
        Animation titleAnim = AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in);
        titleAnim.setStartOffset(700);

        if (logo != null) logo.startAnimation(logoAnim);
        if (txtTitle != null) txtTitle.startAnimation(titleAnim);

        new Handler().postDelayed(this::showIpConfigurationDialog, 3000);
    }

    private void showIpConfigurationDialog() {
        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(50, 50, 50, 50);

        GradientDrawable border = new GradientDrawable();
        border.setColor(Color.parseColor("#121212"));
        border.setCornerRadius(24f);
        border.setStroke(4, Color.parseColor("#00FFFF"));
        container.setBackground(border);

        TextView title = new TextView(this);
        title.setText("NETWORK SYNC");
        title.setTextColor(Color.parseColor("#00FFFF"));
        title.setTextSize(20f);
        title.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        container.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Enter Server IP to initialize AI engine.");
        subtitle.setTextColor(Color.LTGRAY);
        subtitle.setTextSize(14f);
        subtitle.setPadding(0, 8, 0, 32);
        container.addView(subtitle);

        EditText etServerIp = new EditText(this);
        etServerIp.setHint("192.168.1.15");
        etServerIp.setTextColor(Color.WHITE);
        etServerIp.setHintTextColor(Color.DKGRAY);
        etServerIp.setGravity(Gravity.CENTER);
        etServerIp.setTextSize(20f);
        etServerIp.setPadding(20, 20, 20, 20);

        String savedIp = sharedPreferences.getString("server_ip", "192.168.1.15");
        etServerIp.setText(savedIp);

        GradientDrawable inputStyle = new GradientDrawable();
        inputStyle.setStroke(2, Color.parseColor("#00FFFF"));
        inputStyle.setCornerRadius(8f);
        etServerIp.setBackground(inputStyle);

        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 140);
        inputParams.setMargins(0, 0, 0, 40);
        container.addView(etServerIp, inputParams);

        MaterialButton btnConnect = new MaterialButton(this);
        btnConnect.setText("INITIALIZE SESSION");
        btnConnect.setBackgroundColor(Color.parseColor("#00FFFF"));
        btnConnect.setTextColor(Color.BLACK);
        btnConnect.setCornerRadius(12);
        btnConnect.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        container.addView(btnConnect, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 150));

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(container)
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        btnConnect.setOnClickListener(v -> {
            String newIp = etServerIp.getText().toString().trim();
            if (!newIp.isEmpty()) {
                // 1. Lock the Button state to prevent "vanishing" status
                btnConnect.setEnabled(false);
                btnConnect.setText("SYNCING...");
                btnConnect.setBackgroundColor(Color.parseColor("#8000FFFF"));

                sharedPreferences.edit().putString("server_ip", newIp).apply();
                verifyAndNavigate(newIp, dialog, btnConnect);
            }
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            int width;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                width = (int) (getWindowManager().getCurrentWindowMetrics().getBounds().width() * 0.90);
            } else {
                DisplayMetrics dm = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(dm);
                width = (int) (dm.widthPixels * 0.90);
            }
            dialog.getWindow().setLayout(width, WindowManager.LayoutParams.WRAP_CONTENT);
            WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.gravity = Gravity.CENTER_HORIZONTAL | Gravity.TOP;
            lp.y = 300;
            dialog.getWindow().setAttributes(lp);
        }
    }

    private void verifyAndNavigate(String ip, AlertDialog dialog, MaterialButton btn) {
        String url = "http://" + ip + ":5000/";
        ApiService apiService = RetrofitClient.getClient(url).create(ApiService.class);

        showSystemNotification("INITIALIZING");

        apiService.checkHealth().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    showSystemNotification("ONLINE");
                    dialog.dismiss();
                    startActivity(new Intent(SplashActivity.this, RecognitionActivity.class));
                    finish();
                } else {
                    showSystemNotification("OFFLINE");
                    resetButton(btn);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                showSystemNotification("REJECTED");
                resetButton(btn);
            }
        });
    }

    private void resetButton(MaterialButton btn) {
        if (btn != null) {
            btn.setEnabled(true);
            btn.setText("INITIALIZE SESSION");
            btn.setBackgroundColor(Color.parseColor("#00FFFF"));
        }
    }

    private void showSystemNotification(String message) {
        View layout = getLayoutInflater().inflate(R.layout.layout_custom_toast, null);
        TextView text = layout.findViewById(R.id.toastText);
        if (text != null) text.setText(message);

        Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.setGravity(Gravity.BOTTOM, 0, 150);
        toast.show();
    }
}