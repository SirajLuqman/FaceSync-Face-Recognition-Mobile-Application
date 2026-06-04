package com.fyp.facesync;

import com.google.gson.annotations.SerializedName;

public class RecognitionResponse {
    @SerializedName("name")
    private String name;

    @SerializedName("confidence")
    private double confidence;

    // Getters
    public String getName() { return name; }
    public double getConfidence() { return confidence; }
}