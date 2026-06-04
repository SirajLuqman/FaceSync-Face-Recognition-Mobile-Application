package com.fyp.facesync;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RecognitionActivity extends AppCompatActivity {

    private MaterialCardView resultCard;
    private PreviewView previewView;
    private TextView tvResult, resName, resID, resRole, resConfidence;
    private ScrollView mainScrollView;
    private MaterialButton btnScan, btnCapture, btnUpload, btnAdmin;
    private ProgressBar loadingBar;
    private com.google.android.material.card.MaterialCardView cameraCard;
    private ImageButton btnCloseCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recognition);

        // 1. Initialize UI
        previewView = findViewById(R.id.recognitionPreview);
        loadingBar = findViewById(R.id.loadingBar);
        resultCard = findViewById(R.id.resultCard);
        tvResult = findViewById(R.id.tvResult);
        resName = findViewById(R.id.resName);
        resID = findViewById(R.id.resID);
        resRole = findViewById(R.id.resRole);
        mainScrollView = findViewById(R.id.mainScrollView);
        btnScan = findViewById(R.id.btnScanFace);
        btnCapture = findViewById(R.id.btnCaptureImage);
        btnUpload = findViewById(R.id.btnUploadImage);
        btnAdmin = findViewById(R.id.btnAdminLogin);
        cameraCard = findViewById(R.id.cameraCard);
        cameraCard = findViewById(R.id.cameraCard);
        btnCloseCamera = findViewById(R.id.btnCloseCamera); // Global variable link
        btnCloseCamera.setVisibility(View.GONE); // Hidden until camera starts
        resConfidence = findViewById(R.id.resConfidence);



        // 2. Button Listeners
        btnScan.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);

            // 1. Reset UI to original state
            resultCard.setVisibility(View.GONE);
            resName.setText("");
            resID.setText("");
            resRole.setText("");
            tvResult.setText("Align your face...");
            tvResult.setTextColor(Color.BLACK); // Reset color from Red/Green to default

            // 2. RE-ENABLE the Capture button (The one that sends data to Flask)
            btnCapture.setEnabled(true);
            btnCapture.setAlpha(1.0f); // Make it bright again

            // 3. Ensure the Camera Layout is visible again
            cameraCard.setVisibility(View.VISIBLE); // Show the container
            previewView.setVisibility(View.VISIBLE); // Show the actual camera

            // 4. Scroll back to top smoothly
            mainScrollView.smoothScrollTo(0, 0);

            // 5. Start Camera
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
            }
        });

        // Use the correct variable name
        btnCloseCamera.setOnClickListener(v -> {
            // 1. Hide the camera lens stream
            previewView.setVisibility(View.INVISIBLE);

            // 2. Hide the close button itself
            btnCloseCamera.setVisibility(View.GONE);

            // 3. Stop the hardware
            stopCamera();

            // 4. (Optional) Show a "Camera Off" message or just leave it black
            tvResult.setText("Camera Stopped");

            // Do NOT call mainScrollView.smoothScrollTo(0, 0) if you want to stay at the frame
        });

        btnCapture.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            Log.d("FaceSync", "Capture button clicked!");
            performRecognition();
        });

        btnUpload.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            openGallery();
        });

        btnAdmin.setOnClickListener(v -> {
            startActivity(new Intent(this, AdminLoginActivity.class));
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Using 200 as you defined for your GALLERY_REQUEST_CODE
        if (requestCode == 200 && resultCode == RESULT_OK && data != null) {
            java.util.List<android.net.Uri> selectedUris = new java.util.ArrayList<>();

            if (data.getClipData() != null) {
                // CASE 1: Multiple images selected (ClipData)
                int count = data.getClipData().getItemCount();
                if (count > 5) {
                    UIHelper.showSystemMsg(this, "Maximum 5 images allowed");
                    return;
                }
                for (int i = 0; i < count; i++) {
                    selectedUris.add(data.getClipData().getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                // CASE 2: Single image selected (Data)
                selectedUris.add(data.getData());
            }

            // Final check: Did we actually get any images?
            if (!selectedUris.isEmpty()) {
                performGalleryRecognition(selectedUris);
            } else {
                UIHelper.showSystemMsg(this, "No images selected");
            }
        }
    }

    private void startCamera() {
        previewView.setVisibility(View.VISIBLE);
        btnCloseCamera.setVisibility(View.VISIBLE);
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                CameraSelector cameraSelector;
                if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                    cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                } else {
                    cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                }

                cameraProvider.bindToLifecycle(this, cameraSelector, preview);
            } catch (Exception e) {
                UIHelper.showSystemMsg(this, "Camera Error: " + e.getMessage());
            }
        }, ContextCompat.getMainExecutor(this));
    }
    private void stopCamera() {
        // UI: Hide cross and lens, leave frame
        if (btnCloseCamera != null) btnCloseCamera.setVisibility(View.GONE);
        if (previewView != null) previewView.setVisibility(View.INVISIBLE);

        try {
            ProcessCameraProvider.getInstance(this).get().unbindAll();
        } catch (Exception e) {
            Log.e("FaceSync", "Error stopping camera", e);
        }
    }
    private void performRecognition() {
        Bitmap bitmap = previewView.getBitmap();
        if (bitmap == null) return;

        // 2. Start Loading State
        loadingBar.setVisibility(View.VISIBLE);
        // Keep preview visible but hide the "X" during analysis
        btnCloseCamera.setVisibility(View.GONE);
        // This stops the lens but keeps the last frame on screen as a "freeze frame"
        stopCamera();
        btnCapture.setEnabled(false);
        btnCapture.setAlpha(0.5f); // Make it look greyed out
        // 3. Smooth scroll to the loading area
        mainScrollView.post(() -> {
            mainScrollView.smoothScrollTo(0, loadingBar.getTop());
        });
        resultCard.setVisibility(View.GONE);    // Hide old results
        showResult("ANALYZING BIOMETRICS...");
        tvResult.setTextColor(Color.YELLOW);

        File file = new File(getCacheDir(), "auth_temp.jpg");
        try (FileOutputStream out = new FileOutputStream(file)) {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
        } catch (IOException e) { e.printStackTrace(); }

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", file.getName(), requestFile);

        // --- ADD THESE 3 LINES HERE ---
        android.content.SharedPreferences prefs = getSharedPreferences("FaceSyncPrefs", MODE_PRIVATE);
        String ip = prefs.getString("server_ip", "10.0.2.2");
        String dynamicBaseUrl = "http://" + ip + ":5000/";

        // UPDATE THIS LINE to pass 'dynamicBaseUrl'
        ApiService apiService = RetrofitClient.getClient(dynamicBaseUrl).create(ApiService.class);;
        apiService.recognizeFace(body).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                // 1. Always stop the spinner first
                loadingBar.setVisibility(View.GONE);

                // The capture button stays disabled.
                // The user must click 'Start Scanning' to bring the camera back.
                btnCapture.setEnabled(false);

                if (response.isSuccessful() && response.body() != null) {
                    // CASE: 200 OK - Match Found
                    try {
                        String jsonResponse = response.body().string();
                        org.json.JSONObject obj = new org.json.JSONObject(jsonResponse);

                        // Extract real data from your Flask DB query
                        String name = obj.optString("name", "Unknown Face");
                        String id = obj.optString("user_id", "N/A");
                        String role = obj.optString("role", "N/A");
                        String confidence = obj.optString("confidence", "0.0");

                        fillUserTable(name, id, role, confidence);
                    } catch (Exception e) {
                        showResult("Error parsing data");
                    }
                }
                else if (response.code() == 422) {
                    // CASE: 422 - No Face Detected (Detection Layer Failure)
                    resultCard.setVisibility(View.GONE); // Hide table since no face was found
                    tvResult.setText("NO FACE DETECTED");
                    tvResult.setTextColor(Color.parseColor("#FFCC00")); // Warning Yellow
                    UIHelper.showSystemMsg(RecognitionActivity.this, "Face not found. Please align and try again.");
                }
                else if (response.code() == 404) {
                    // CASE: 404 - Face Found but No Match (Biometric Layer Rejection)
                    resName.setText("UNKNOWN");
                    resID.setText("N/A");
                    resRole.setText("UNAUTHORIZED");
                    resConfidence.setText("0.0%");

                    resultCard.setVisibility(View.VISIBLE);
                    resultCard.setStrokeColor(Color.RED);
                    tvResult.setText("ACCESS DENIED: NO MATCH");
                    tvResult.setTextColor(Color.RED);

                    mainScrollView.postDelayed(() -> {
                        mainScrollView.smoothScrollTo(0, resultCard.getBottom());
                    }, 200);
                }
                else {
                    resultCard.setVisibility(View.GONE);
                    // CASE: 500 or 400 - System/Server Errors
                    showResult("SERVER ERROR: " + response.code());
                    tvResult.setTextColor(Color.YELLOW);
                    UIHelper.showSystemMsg(RecognitionActivity.this, "CRITICAL ERROR: SERVER-SIDE EXCEPTION");
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 1. MUST HIDE THE SPINNER HERE
                loadingBar.setVisibility(View.GONE);
                // 2. Inform the user
                showResult("CONNECTION TIMED OUT");
                tvResult.setTextColor(Color.RED);
                // 3. This will show the detailed technical reason in Logcat
                Log.e("FaceSync", "CRITICAL FAILURE: ", t);
                UIHelper.showSystemMsg(RecognitionActivity.this, "CONNECTION TIMEOUT: SERVER NOT RESPONDING");
            }
        });
    }

    private void fillUserTable(String name, String id, String role, String confidence) {
        // 1. Set the data
        resName.setText(name);
        resID.setText(id);
        resRole.setText(role);
        resConfidence.setText(confidence + "%");

        // --- FIX: Reset border to Neon Cyan (#00FFFF) ---
        resultCard.setStrokeColor(Color.parseColor("#00FFFF"));
        resultCard.setStrokeWidth(6); // Roughly 2dp; ensures border is visible
        // -----------------------------------------------

        // 2. Make the card visible ONLY now
        resultCard.setVisibility(View.VISIBLE);
        tvResult.setText("MATCH FOUND: ACCESS GRANTED");
        tvResult.setTextColor(Color.GREEN);

        // 3. Smooth Scroll
        mainScrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                mainScrollView.smoothScrollTo(0, resultCard.getBottom());
            }
        }, 200);
    }

    private void showResult(String text) {
        tvResult.setText(text);
        tvResult.setTextColor(Color.CYAN);
    }

    private void openGallery() {
        // 1. Create the Dialog
        final android.app.Dialog dialog = new android.app.Dialog(this);
        dialog.setContentView(R.layout.dialog_admin_gate);

        // 2. Make background transparent so the rounded corners and base_black show correctly
        if (dialog.getWindow() != null) {
            // Get screen width
            android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
            int width = (int) (metrics.widthPixels * 0.90); // 90% of screen width
            // Set width to 90% and height to wrap_content
            dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        // 3. Initialize Dialog UI
        TextInputEditText etID = dialog.findViewById(R.id.dialogAdminID);
        TextInputEditText etPass = dialog.findViewById(R.id.dialogAdminPassword);
        MaterialButton btnVerify = dialog.findViewById(R.id.btnVerifyAdmin);
        ImageButton btnClose = dialog.findViewById(R.id.btnCloseDialog);

        btnClose.setOnClickListener(v -> dialog.dismiss());

        btnVerify.setOnClickListener(v -> {
            String id = etID.getText().toString().trim();
            String pass = etPass.getText().toString().trim();

            if (id.isEmpty() || pass.isEmpty()) {
                UIHelper.showSystemMsg(this, "Credentials required");
                return;
            }

            // Show a "loading" state on the button
            btnVerify.setEnabled(false);
            btnVerify.setText("VERIFYING...");

            java.util.HashMap<String, String> credentials = new java.util.HashMap<>();
            credentials.put("email_or_id", id);
            credentials.put("password", pass);

            // --- ADD THESE 3 LINES HERE ---
            android.content.SharedPreferences prefs = getSharedPreferences("FaceSyncPrefs", MODE_PRIVATE);
            String ip = prefs.getString("server_ip", "10.0.2.2");
            String dynamicBaseUrl = "http://" + ip + ":5000/";

            // UPDATE THIS LINE to pass 'dynamicBaseUrl'
            ApiService apiService = RetrofitClient.getClient(dynamicBaseUrl).create(ApiService.class);
            apiService.adminLogin(credentials).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    if (response.isSuccessful()) {
                        dialog.dismiss();
                        launchGalleryPicker();
                    } else {
                        btnVerify.setEnabled(true);
                        btnVerify.setText("AUTHORIZE ACCESS");
                        UIHelper.showSystemMsg(RecognitionActivity.this, "SECURITY ALERT: ACCESS DENIED");
                    }
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    btnVerify.setEnabled(true);
                    btnVerify.setText("AUTHORIZE ACCESS");
                    UIHelper.showSystemMsg(RecognitionActivity.this, "SYSTEM CRITICAL: SERVER OFFLINE");
                }
            });
        });

        dialog.show();
    }

    private void launchGalleryPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true); // Key for multi-selection
        startActivityForResult(Intent.createChooser(intent, "Select 3-5 Images"), 200);
    }

    private boolean allPermissionsGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    private void performGalleryRecognition(java.util.List<android.net.Uri> uris) {
        loadingBar.setVisibility(View.VISIBLE);
        previewView.setVisibility(View.GONE);
        resultCard.setVisibility(View.GONE);
        showResult("ANALYZING GALLERY...");

        // This ensures the app scrolls down to the loading bar immediately
        mainScrollView.post(() -> {
            mainScrollView.smoothScrollTo(0, loadingBar.getTop());
        });

        java.util.List<MultipartBody.Part> imageParts = new java.util.ArrayList<>();
        for (int i = 0; i < uris.size(); i++) {
            try {
                File file = new File(getCacheDir(), "gallery_temp_" + System.currentTimeMillis() + "_" + i + ".jpg");
                Bitmap bitmap = android.provider.MediaStore.Images.Media.getBitmap(this.getContentResolver(), uris.get(i));
                FileOutputStream out = new FileOutputStream(file);
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out);
                out.close();

                RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), file);
                imageParts.add(MultipartBody.Part.createFormData("images", file.getName(), requestFile));
            } catch (IOException e) { e.printStackTrace(); }
        }

        // --- ADD THESE 3 LINES HERE ---
        android.content.SharedPreferences prefs = getSharedPreferences("FaceSyncPrefs", MODE_PRIVATE);
        String ip = prefs.getString("server_ip", "10.0.2.2");
        String dynamicBaseUrl = "http://" + ip + ":5000/";

        // UPDATE THIS LINE to pass 'dynamicBaseUrl'
        ApiService apiService = RetrofitClient.getClient(dynamicBaseUrl).create(ApiService.class);
        apiService.recognizeMultiple(imageParts).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                loadingBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    // CASE: 200 OK - Match Found in Gallery
                    try {
                        String jsonResponse = response.body().string();
                        org.json.JSONObject obj = new org.json.JSONObject(jsonResponse);
                        fillUserTable(
                                obj.optString("name", "Unknown"),
                                obj.optString("user_id", "N/A"),
                                obj.optString("role", "N/A"),
                                obj.optString("confidence", "0.0")
                        );
                    } catch (Exception e) {
                        showResult("Error parsing data");
                    }
                }
                else if (response.code() == 422) {
                    // CASE: 422 - No Face Found in any of the uploaded images
                    resultCard.setVisibility(View.GONE);
                    tvResult.setText("NO FACE DETECTED IN GALLERY");
                    tvResult.setTextColor(Color.parseColor("#FFCC00")); // Warning Yellow
                    UIHelper.showSystemMsg(RecognitionActivity.this, "The uploaded photos are too blurry or have no clear face.");
                }
                else {
                    // CASE: 404 (Unknown) or other errors
                    resultCard.setVisibility(View.VISIBLE);
                    resultCard.setStrokeColor(Color.RED);

                    resName.setText("UNKNOWN");
                    resID.setText("N/A");
                    resRole.setText("UNAUTHORIZED");
                    resConfidence.setText("0.0%");

                    tvResult.setText("NO MATCH IN GALLERY");
                    tvResult.setTextColor(Color.RED);

                    mainScrollView.post(() -> {
                        mainScrollView.smoothScrollTo(0, resultCard.getBottom());
                    });
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                loadingBar.setVisibility(View.GONE);
                showResult("UPLOAD FAILED");
            }
        });
    }
}