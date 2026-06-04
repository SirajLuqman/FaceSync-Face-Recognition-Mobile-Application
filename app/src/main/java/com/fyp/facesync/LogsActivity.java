package com.fyp.facesync;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class LogsActivity extends AppCompatActivity {

    private RecyclerView rvLogs;
    private LogsAdapter adapter;
    private List<LogEntry> logList;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private Button btnAll, btnAlerts;

    // REMOVED: Static LOGS_URL. We will build it dynamically now.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_logs);

        swipeRefreshLayout = findViewById(R.id.swipeRefreshLayout);
        swipeRefreshLayout.setOnRefreshListener(this::fetchLogsFromServer);

        btnAll = findViewById(R.id.btnFilterAll);
        btnAlerts = findViewById(R.id.btnFilterAlerts);
        rvLogs = findViewById(R.id.rvLogs);

        findViewById(R.id.btnBackLogs).setOnClickListener(v -> finish());

        rvLogs.setLayoutManager(new LinearLayoutManager(this));
        logList = new ArrayList<>();
        adapter = new LogsAdapter(logList);
        rvLogs.setAdapter(adapter);

        btnAll.setOnClickListener(v -> {
            adapter.updateList(logList);
            highlightButton(btnAll, btnAlerts);
        });

        btnAlerts.setOnClickListener(v -> {
            List<LogEntry> alerts = new ArrayList<>();
            for (LogEntry log : logList) {
                if (!log.isSuccess()) {
                    alerts.add(log);
                }
            }
            adapter.updateList(alerts);
            highlightButton(btnAlerts, btnAll);
        });

        fetchLogsFromServer();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // This triggers the refresh automatically when you come back from Settings
        fetchLogsFromServer();
    }

    private void highlightButton(Button active, Button inactive) {
        active.setAlpha(1.0f);
        inactive.setAlpha(0.5f);
    }

    private void fetchLogsFromServer() {
        swipeRefreshLayout.setRefreshing(true);

        // --- NEW DYNAMIC URL LOGIC ---
        SharedPreferences prefs = getSharedPreferences("FaceSyncPrefs", Context.MODE_PRIVATE);
        // Default to 10.0.2.2 if you are using an Emulator, or 192.168.1.4 if using a real phone
        String ip = prefs.getString("server_ip", "10.0.2.2");
        String dynamicLogsUrl = "http://" + ip + ":5000/get_logs";

        Log.d("NETWORK_CHECK", "Requesting logs from: " + dynamicLogsUrl);

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.GET, dynamicLogsUrl, null,
                response -> {
                    swipeRefreshLayout.setRefreshing(false);
                    logList.clear();
                    try {
                        // 1. Check if the server returned zero results
                        if (response.length() == 0) {
                            UIHelper.showSystemMsg(this, "No activity logs found");
                        }

                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);

                            String displayId = obj.isNull("user_id_code") ? "N/A" : obj.getString("user_id_code");
                            String imgPath = obj.isNull("image_path") ? null : obj.getString("image_path");

                            logList.add(new LogEntry(
                                    displayId,
                                    obj.optString("name", "Unknown"),
                                    obj.optString("status", ""),
                                    obj.optString("timestamp", ""),
                                    obj.optInt("is_success") == 1,
                                    imgPath
                            ));
                        }
                        // 2. Refresh the UI (this will clear the list if response was 0)
                        adapter.notifyDataSetChanged();

                    } catch (Exception e) {
                        Log.e("LogsActivity", "JSON Error: " + e.getMessage());
                    }
                },
                error -> {
                    swipeRefreshLayout.setRefreshing(false);
                    Log.e("NETWORK_ERROR", "Error: " + error.toString());
                    UIHelper.showSystemMsg(this, "Connection Error: Check IP in Settings");
                });

        queue.add(jsonArrayRequest);
    }
}