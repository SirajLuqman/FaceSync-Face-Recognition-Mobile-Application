package com.fyp.facesync;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import android.view.View; // NEW: Required for View.VISIBLE/GONE
import android.widget.ImageView; // NEW: Required for thumbnails
import android.widget.LinearLayout; // NEW: Required for the gallery layout
import android.widget.ProgressBar; // NEW: Required for the loading spinner

public class RegistrationActivity extends AppCompatActivity {

    private PreviewView previewView;
    private TextView tvCaptureCount;
    private MaterialButton btnCapture, btnSave;

    private android.widget.ImageButton btnBack;
    private com.google.android.material.textfield.TextInputEditText etName, etUserID, etRole;

    // This list will hold the 5 images until you click SAVE
    private java.util.List<android.graphics.Bitmap> capturedImages = new java.util.ArrayList<>();
    private int imageCount = 0; // Tracks the 5-image requirement
    private LinearLayout llImagePreviewContainer;
    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registration);

        // 1. Initialize UI
        previewView = findViewById(R.id.registrationPreview);
        tvCaptureCount = findViewById(R.id.tvCaptureCount);
        btnCapture = findViewById(R.id.btnCapture);
        btnSave = findViewById(R.id.btnSave);

        // New References
        btnBack = findViewById(R.id.btnBack);
        etName = findViewById(R.id.etName);
        etUserID = findViewById(R.id.etID);
        etRole = findViewById(R.id.etRole);
        // NEW: Linking the new XML elements to Java
        llImagePreviewContainer = findViewById(R.id.llImagePreviewContainer);
        progressBar = findViewById(R.id.registrationProgressBar);

        // 2. BACK ARROW CLICK
        btnBack.setOnClickListener(v -> {
            finish(); // Returns to the Dashboard
        });

        // UPDATED: Replaced manual button disabling with a central UI helper method
        updateCaptureUI();

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA)
                == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.CAMERA}, 101);
        }

        // 2. Start Camera Preview
        startCamera();

        // 3. UPDATED: Capture Logic with FrameLayout and Delete Icon
        btnCapture.setOnClickListener(v -> {
            if (imageCount < 5) {
                android.graphics.Bitmap bitmap = previewView.getBitmap();
                if (bitmap != null) {
                    capturedImages.add(bitmap);
                    imageCount++;

                    // 1. Create a Container (FrameLayout) to hold Image + Cross Icon
                    android.widget.FrameLayout container = new android.widget.FrameLayout(this);
                    LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(200, 200);
                    containerParams.setMargins(15, 10, 15, 10);
                    container.setLayoutParams(containerParams);

                    // 2. Create the Thumbnail Image
                    ImageView thumbnail = new ImageView(this);
                    android.widget.FrameLayout.LayoutParams imgParams = new android.widget.FrameLayout.LayoutParams(170, 170);
                    imgParams.gravity = android.view.Gravity.BOTTOM | android.view.Gravity.START;
                    thumbnail.setLayoutParams(imgParams);
                    thumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    thumbnail.setImageBitmap(bitmap);
                    thumbnail.setBackgroundResource(R.drawable.camera_border);
                    thumbnail.setPadding(4, 4, 4, 4);

                    // 3. Create the "Close/Cross" Icon
                    // UPDATED Step 3 with Full Path to prevent "Cannot Resolve"
                    ImageView crossIcon = new ImageView(this);
                    android.widget.FrameLayout.LayoutParams crossParams = new android.widget.FrameLayout.LayoutParams(55, 55);
                    crossParams.gravity = android.view.Gravity.TOP | android.view.Gravity.END;
                    crossIcon.setLayoutParams(crossParams);

                    crossIcon.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
                    // Using full path here:
                    crossIcon.setColorFilter(android.graphics.Color.parseColor("#00FFFF"));

                    android.graphics.drawable.GradientDrawable circle = new android.graphics.drawable.GradientDrawable();
                    circle.setShape(android.graphics.drawable.GradientDrawable.OVAL);
                    // Using full path here:
                    circle.setColor(android.graphics.Color.BLACK);
                    circle.setStroke(2, android.graphics.Color.parseColor("#00FFFF"));

                    crossIcon.setBackground(circle);
                    crossIcon.setPadding(8, 8, 8, 8);

                    // 4. DELETE LOGIC: Only happens when clicking the Cross Icon
                    crossIcon.setOnClickListener(view -> {
                        container.animate().scaleX(0f).scaleY(0f).alpha(0f).setDuration(200).withEndAction(() -> {
                            llImagePreviewContainer.removeView(container);
                            capturedImages.remove(bitmap);
                            imageCount--;
                            updateCaptureUI();
                        }).start();
                    });

                    // 5. Build the view hierarchy
                    container.addView(thumbnail);
                    container.addView(crossIcon);

                    // Entrance Animation
                    container.setScaleX(0.5f);
                    container.setAlpha(0f);
                    container.animate().scaleX(1f).alpha(1f).setDuration(300).start();

                    llImagePreviewContainer.addView(container);

                    // Auto-scroll to the right
                    findViewById(R.id.hsvGallery).post(() ->
                            ((android.widget.HorizontalScrollView) findViewById(R.id.hsvGallery)).fullScroll(View.FOCUS_RIGHT)
                    );

                    updateCaptureUI();
                }
            }
        });

        // 4. UPDATED: Save Logic now handles Loading states and Smart Backend Errors
        btnSave.setOnClickListener(v -> {
            String name = etName.getText().toString().trim();
            String userId = etUserID.getText().toString().trim();
            String role = etRole.getText().toString().trim();

            if (name.isEmpty() || userId.isEmpty() || role.isEmpty()) {
                UIHelper.showSystemMsg(this, "Please fill all fields");
                return;
            }

            // NEW: Show Loading UI so user knows processing is happening
            progressBar.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);
            btnSave.setText("ENROLLING...");

            // --- NEW DYNAMIC IP LOGIC ---
            android.content.SharedPreferences prefs = getSharedPreferences("FaceSyncPrefs", MODE_PRIVATE);
            String ip = prefs.getString("server_ip", "10.0.2.2"); // Default for emulator
            String dynamicBaseUrl = "http://" + ip + ":5000/";

            // Prepare API parts
            okhttp3.RequestBody namePart = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, name);
            okhttp3.RequestBody idPart = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, userId);
            okhttp3.RequestBody rolePart = okhttp3.RequestBody.create(okhttp3.MultipartBody.FORM, role);

            java.util.List<okhttp3.MultipartBody.Part> imageParts = new java.util.ArrayList<>();
            for (int i = 0; i < capturedImages.size(); i++) {
                okhttp3.MultipartBody.Part part = prepareImagePart("images", capturedImages.get(i), i);
                if (part != null) imageParts.add(part);
            }

            ApiService apiService = RetrofitClient.getClient(dynamicBaseUrl).create(ApiService.class);
            apiService.registerUser(namePart, idPart, rolePart, imageParts).enqueue(new retrofit2.Callback<okhttp3.ResponseBody>() {
                @Override
                public void onResponse(retrofit2.Call<okhttp3.ResponseBody> call, retrofit2.Response<okhttp3.ResponseBody> response) {
                    // NEW: Hide progress bar once server responds
                    progressBar.setVisibility(View.GONE);

                    if (response.isSuccessful()) {
                        UIHelper.showSystemMsg(RegistrationActivity.this, "USER ENROLLED SUCCESSFULLY");
                        finish();
                    }
                    // NEW: Handle the "Duplicate/Conflict" error (409) from our Smart Backend
                    else if (response.code() == 409) {
                        btnSave.setEnabled(true);
                        btnSave.setText("SAVE USER TO DATABASE");
                        UIHelper.showSystemMsg(RegistrationActivity.this, "ALREADY REGISTERED: CHECK DETAILS!");
                    } else {
                        btnSave.setEnabled(true);
                        btnSave.setText("SAVE USER TO DATABASE");
                        UIHelper.showSystemMsg(RegistrationActivity.this, "SYSTEM ERROR: " + response.code());
                    }
                }

                @Override
                public void onFailure(retrofit2.Call<okhttp3.ResponseBody> call, Throwable t) {
                    progressBar.setVisibility(View.GONE);
                    btnSave.setEnabled(true);
                    btnSave.setText("SAVE USER TO DATABASE");
                    UIHelper.showSystemMsg(RegistrationActivity.this, "CRITICAL: NETWORK CONNECTION FAILED");
                }
            });
        });
    }

    // NEW: Central helper method to manage button states and text
    // This keeps the logic organized in one place
    private void updateCaptureUI() {
        tvCaptureCount.setText("Images Captured: " + imageCount + "/5");

        if (imageCount < 5) {
            btnCapture.setEnabled(true);
            btnCapture.setText("CAPTURE IMAGE");
            // Disable save button if images < 5
            btnSave.setEnabled(false);
            btnSave.setAlpha(0.5f);
        } else {
            // Once 5 images are reached, lock capture and unlock save
            btnCapture.setEnabled(false);
            btnCapture.setText("LIMIT REACHED");
            btnSave.setEnabled(true);
            btnSave.setAlpha(1.0f);
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                // --- MINIMAL FRONT-FIRST SWITCH ---
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

    private okhttp3.MultipartBody.Part prepareImagePart(String partName, android.graphics.Bitmap bitmap, int index) {
        try {
            java.io.File file = new java.io.File(getCacheDir(), "user_image_" + index + ".jpg");
            java.io.FileOutputStream fos = new java.io.FileOutputStream(file);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
            okhttp3.RequestBody requestFile = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/jpeg"), file);
            return okhttp3.MultipartBody.Part.createFormData(partName, file.getName(), requestFile);
        } catch (java.io.IOException e) {
            return null;
        }
    }
}