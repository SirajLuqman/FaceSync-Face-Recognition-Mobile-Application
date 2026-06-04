package com.fyp.facesync;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.graphics.Color;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LiveMonitorActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView tvAdminLog, tvFaceResult;
    private MaterialButton btnStart;
    private android.widget.ImageButton btnBack;
    private long lastAnalysisTime = 0;
    private ApiService apiService;
    private static final long ANALYSIS_INTERVAL = 500;
    private int unknownCounter = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_monitor);

        // 1. Initialize all views from your new XML
        previewView = findViewById(R.id.previewView);
        tvAdminLog = findViewById(R.id.tvAdminLog);
        tvFaceResult = findViewById(R.id.tvFaceResult);
        btnStart = findViewById(R.id.btnStartCamera);
        btnBack = findViewById(R.id.btnBack);
        // 2. Navigation logic
        btnBack.setOnClickListener(v -> finish());

        // 3. Trigger Camera on button click
        btnStart.setOnClickListener(v -> {
            tvAdminLog.setText("> Initializing System Feed...");
            startLiveAnalysis();

            startScanAnimation();

            // Optional: Hide button once camera starts to clear the UI
            btnStart.setVisibility(View.GONE);
        });
    }

    private void startLiveAnalysis() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                // 1. Preview Use Case
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // 2. Image Analysis Use Case (Frame grabbing)
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), image -> {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastAnalysisTime >= ANALYSIS_INTERVAL) {
                        lastAnalysisTime = currentTime;
                        android.graphics.Bitmap bitmap = previewView.getBitmap();
                        if (bitmap != null) {
                            runOnUiThread(() -> tvAdminLog.setText("> Analyzing face..."));
                            sendFrameToServer(bitmap);
                        }
                    }
                    image.close();
                });

                // --- MINIMAL FRONT-FIRST FALLBACK LOGIC ---
                CameraSelector cameraSelector;
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                } else {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                }

                // 3. Bind to Lifecycle
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (Exception e) {
                Log.e("FaceSync", "Camera Error: " + e.getMessage());
                tvAdminLog.setText("> Error: Could not open camera.");
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void startScanAnimation() {
        // 1. You must declare cameraCard locally so Java knows what it is
        View cameraCard = findViewById(R.id.cameraCard);
        View scanLine = findViewById(R.id.scanLine);

        // 2. Now the 'if' statement will work because cameraCard is defined
        if (scanLine != null && cameraCard != null) {

            // Use .post to wait for the UI to draw so height isn't 0
            cameraCard.post(() -> {
                scanLine.setVisibility(View.VISIBLE);

                // Get the actual height of the card
                float travelDistance = cameraCard.getHeight();

                android.view.animation.Animation animation = new android.view.animation.TranslateAnimation(
                        0, 0, 0, travelDistance);
                animation.setDuration(2500);
                animation.setRepeatCount(android.view.animation.Animation.INFINITE);
                animation.setRepeatMode(android.view.animation.Animation.REVERSE);
                scanLine.startAnimation(animation);
            });
        }
    }
    private void sendFrameToServer(android.graphics.Bitmap bitmap) {
        // --- ADD THESE 3 LINES HERE ---
        android.content.SharedPreferences prefs = getSharedPreferences("FaceSyncPrefs", MODE_PRIVATE);
        String ip = prefs.getString("server_ip", "10.0.2.2");
        String dynamicBaseUrl = "http://" + ip + ":5000/";

        // Re-initialize apiService to use the Dynamic IP
        apiService = RetrofitClient.getClient(dynamicBaseUrl).create(ApiService.class);
        // 1. Prepare the image
        java.io.ByteArrayOutputStream stream = new java.io.ByteArrayOutputStream();
        bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 40, stream);
        byte[] byteArray = stream.toByteArray();
        okhttp3.RequestBody requestBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), byteArray);
        okhttp3.MultipartBody.Part body = okhttp3.MultipartBody.Part.createFormData("image", "live_frame.jpg", requestBody);

        // 2. Call the API
        apiService.recognizeLiveFace(body).enqueue(new retrofit2.Callback<RecognitionResponse>() {
            @Override
            public void onResponse(retrofit2.Call<RecognitionResponse> call, retrofit2.Response<RecognitionResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String name = response.body().getName();
                    double confidence = response.body().getConfidence();

                    // 1. RECOGNITION STATE (Positive Match)
                    if (!name.equalsIgnoreCase("Unknown")) {
                        unknownCounter = 0; // Reset immediately on any match
                        tvFaceResult.setText(String.format("VERIFIED: %s [%.1f%%]", name.toUpperCase(), confidence));
                        tvFaceResult.setTextColor(android.graphics.Color.GREEN);
                        tvAdminLog.setText("> ACCESS GRANTED");
                    }
                    // 2. UNKNOWN STATE (Unauthorized)
                    else {
                        unknownCounter++;
                        if (unknownCounter >= 3) {
                            tvFaceResult.setText("STATUS: UNAUTHORIZED");
                            tvFaceResult.setTextColor(android.graphics.Color.RED);
                            tvAdminLog.setText("> ALERT: UNKNOWN SUBJECT");
                        }
                        // Note: If counter is < 3, we simply keep the last state to avoid flickering
                    }
                }
                // 3. SEARCHING STATE (No Face)
                else if (response.code() == 422) {
                    unknownCounter = 0;
                    tvFaceResult.setText("LOOKING FOR FACE...");
                    tvFaceResult.setTextColor(android.graphics.Color.YELLOW); // Pure White for "Looking"
                    tvAdminLog.setText("> SEARCHING");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<RecognitionResponse> call, Throwable t) {
                // Log the error so you can debug if the server goes down
                Log.e("FaceSync", "Network skip: " + t.getMessage());
                tvAdminLog.setText("> NETWORK LAG DETECTED...");
            }
        });
    }

}