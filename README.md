# FaceSync: Real-Time AI-Powered Biometric Authentication System Using Deep Learning Face Recognition
### Semester 8 Final Year Project Submission

---

### 1. PROJECT OVERVIEW
FaceSync is an integrated biometric system consisting of an Android frontend for real-time capture, a Python/Flask backend for embedding generation and recognition, and a MySQL database for persistent storage of subject profiles and access logs.

---

### 2. REPOSITORY STRUCTURE
* **Backend_Flask/**: Python Flask API and database utility modules.
* **Database/**: SQL dump for schema and table initialization.
* **Executable_APK/**
* **Frontend_Android/**: Android Studio project source code (Cleaned).

---

### 3. INSTALLATION & SETUP

#### A. Database Configuration (MySQL)
1. Open **MySQL Workbench**.
2. Navigate to **Server > Data Import**.
3. Select **"Import from Self-Contained File"** and browse to `/Database/facesync_db.sql`.
4. Select **"Start Import"**. The system will automatically create the `facesync_db` schema and tables.

#### B. Backend Configuration (VS Code)
1. Open the `/Backend_Flask/` folder in VS Code.
2. Open a terminal and install the required dependencies:
   ```bash
   pip install -r requirements.txt
   ```
3. Verify your database credentials in `db/db_config.py`.
4. Start the server:
   ```bash
   python app.py
   ```

#### C. Frontend Configuration (Android Studio)
1. Open the `/Frontend_Android/` folder in Android Studio.
2. Allow Gradle to sync and build the project.
3. **Important:** Update the `BASE_URL` in your network constants file to match your machine's local IPv4 address (e.g., `http://192.168.x.x:5000`).
4. Build and run the app on an Android device or emulator.

---

### 4. SYSTEM REQUIREMENTS
* **Python:** 3.10+
* **Android SDK:** Level 21+ (Android 5.0+)
* **Database:** MySQL 8.0
* **Network:** Ensure the Android device and Backend server are on the same Wi-Fi network.

---

### 5. DEVELOPER NOTES
* The backend utilizes Flask for API routing and OpenCV/TensorFlow for face processing.
* The `requirements.txt` file contains all necessary libraries including `flask-cors` and `mysql-connector-python`.

Note: For quick evaluation, a pre-compiled executable is provided in the 05_Executable_APK folder
