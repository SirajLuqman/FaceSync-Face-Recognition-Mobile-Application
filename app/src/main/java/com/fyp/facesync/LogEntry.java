package com.fyp.facesync; // Ensure this matches your package name

public class LogEntry {
    private String personId; // 1
    private String name;     // 2
    private String status;   // 3
    private String timestamp;// 4
    private boolean isSuccess; // 5
    private String imagePath; // 6

    // Update constructor to take 6 arguments
    public LogEntry(String personId, String name, String status, String timestamp, boolean isSuccess, String imagePath) {
        this.personId = personId;
        this.name = name;
        this.status = status;
        this.timestamp = timestamp;
        this.isSuccess = isSuccess;
        this.imagePath = imagePath;
    }

    // Getters
    public String getPersonId() { return personId; }
    public String getName() { return name; }
    public String getStatus() { return status; }
    public String getTimestamp() { return timestamp; }
    public boolean isSuccess() { return isSuccess; }
    public String getImagePath() { return imagePath; }
}