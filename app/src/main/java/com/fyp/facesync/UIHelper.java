package com.fyp.facesync;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

public class UIHelper {
    public static void showSystemMsg(Context context, String message) {
        View layout = LayoutInflater.from(context).inflate(R.layout.layout_custom_toast, null);
        TextView text = layout.findViewById(R.id.toastText);
        text.setText(message);

        Toast toast = new Toast(context);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        toast.show(); // The .show() is already here!
    }
}