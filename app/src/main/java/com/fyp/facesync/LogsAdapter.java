package com.fyp.facesync;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences; // 1. ADD THIS IMPORT
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import java.util.ArrayList;
import java.util.List;

public class LogsAdapter extends RecyclerView.Adapter<LogsAdapter.LogViewHolder> {

    private List<LogEntry> logsList;

    public LogsAdapter(List<LogEntry> logsList) {
        this.logsList = logsList;
    }

    public void updateList(List<LogEntry> newList) {
        this.logsList = new ArrayList<>(newList);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LogViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_log, parent, false);
        return new LogViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LogViewHolder holder, int position) {
        LogEntry log = logsList.get(position);
        Context context = holder.itemView.getContext(); // Helper variable

        // 1. SET TEXT DATA
        holder.tvName.setText(log.getName());
        holder.tvId.setText("ID: " + log.getPersonId());
        holder.tvStatus.setText(log.getStatus());
        holder.tvTime.setText(log.getTimestamp());

        // Get Dynamic IP for URLs
        SharedPreferences prefs = context.getSharedPreferences("FaceSyncPrefs", Context.MODE_PRIVATE);
        String ip = prefs.getString("server_ip", "10.0.2.2");
        String baseUrl = "http://" + ip + ":5000/static/unknown_faces/";

        // 2. LOGIC FOR SUCCESS VS FAILURE
        if (log.isSuccess()) {
            holder.cvImageContainer.setVisibility(View.GONE);
            int cyan = Color.parseColor("#00FFFF");
            holder.statusIndicator.setBackgroundColor(cyan);
            holder.tvStatus.setTextColor(cyan);
        } else {
            holder.cvImageContainer.setVisibility(View.VISIBLE);
            int red = Color.parseColor("#FF0000");
            holder.statusIndicator.setBackgroundColor(red);
            holder.tvStatus.setTextColor(red);

            if (log.getImagePath() != null && !log.getImagePath().isEmpty() && !log.getImagePath().equals("null")) {
                String thumbUrl = baseUrl + log.getImagePath();
                Picasso.get()
                        .load(thumbUrl)
                        .fit()
                        .centerCrop()
                        .placeholder(R.drawable.ic_no_face)
                        .error(R.drawable.ic_error_placeholder)
                        .into(holder.ivIntruderPhoto);
            } else {
                holder.ivIntruderPhoto.setImageResource(R.drawable.ic_no_face);
            }
        }

        // 3. ITEM CLICK LISTENER - Updated to use Dynamic IP
        holder.itemView.setOnClickListener(v -> {
            if (!log.isSuccess() && log.getImagePath() != null &&
                    !log.getImagePath().equals("null") && !log.getImagePath().isEmpty()) {

                // 2. FIX: Use the dynamic IP for the full image popup too
                String fullImageUrl = baseUrl + log.getImagePath();
                showFullImageDialog(context, fullImageUrl);
            } else if (log.isSuccess()) {
            // Custom Tech Style for Authorized Entry
                UIHelper.showSystemMsg(context, "AUTHORIZED ENTRY: " + log.getName().toUpperCase());
            } else {
            // Custom Tech Style for Alerts
                UIHelper.showSystemMsg(context, "SYSTEM ALERT: NO PHOTO AVAILABLE");
            }
        });
    }

    private void showFullImageDialog(Context context, String imageUrl) {
        ImageView imageView = new ImageView(context);
        imageView.setPadding(32, 32, 32, 32);
        imageView.setAdjustViewBounds(true);

        Picasso.get()
                .load(imageUrl)
                .placeholder(R.drawable.ic_no_face)
                .error(R.drawable.ic_error_placeholder)
                .into(imageView);

        new AlertDialog.Builder(context)
                .setTitle("Intruder Identification")
                .setView(imageView)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    @Override
    public int getItemCount() {
        return logsList.size();
    }

    public static class LogViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvStatus, tvTime, tvId;
        View statusIndicator;
        CardView cvImageContainer;
        ImageView ivIntruderPhoto;

        public LogViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tvLogName);
            tvId = itemView.findViewById(R.id.tvLogId);
            tvStatus = itemView.findViewById(R.id.tvLogStatus);
            tvTime = itemView.findViewById(R.id.tvLogTime);
            statusIndicator = itemView.findViewById(R.id.viewStatusIndicator);
            cvImageContainer = itemView.findViewById(R.id.cvImageContainer);
            ivIntruderPhoto = itemView.findViewById(R.id.ivIntruderPhoto);
        }
    }
}