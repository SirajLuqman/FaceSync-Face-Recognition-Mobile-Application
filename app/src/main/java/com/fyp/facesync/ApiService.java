package com.fyp.facesync;

import java.util.HashMap;
import java.util.List;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Body;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.DELETE;

public interface ApiService {

    // 1. Admin Login
    @POST("admin_login")
    Call<ResponseBody> adminLogin(@Body HashMap<String, String> credentials);

    // 2. Admin Signup
    @POST("signup")
    Call<ResponseBody> adminSignup(@Body HashMap<String, String> data);

    // 3. NEW: Register User with 5 Images (Multipart)
    @Multipart
    @POST("register_user")
    Call<ResponseBody> registerUser(
            @Part("name") RequestBody name,
            @Part("user_id_code") RequestBody userId,
            @Part("role") RequestBody role,
            @Part List<MultipartBody.Part> images
    );

    //METHOD FOR RECOGNITION
    @Multipart
    @POST("/recognize")
    Call<ResponseBody> recognizeFace(
            @Part MultipartBody.Part image
    );

    @Multipart
    @POST("/recognize_live")
    Call<RecognitionResponse> recognizeLiveFace(
            @Part MultipartBody.Part image
    );

    @Multipart
    @POST("recognize_multiple") // New endpoint for gallery
    Call<ResponseBody> recognizeMultiple(@Part java.util.List<MultipartBody.Part> images);

    @POST("request-otp")
    Call<ResponseBody> requestOtp(@Body HashMap<String, String> data);

    @POST("verify-otp")
    Call<ResponseBody> verifyOtp(@Body HashMap<String, String> data);

    @POST("/update-password")
    Call<ResponseBody> updatePassword(@Body HashMap<String, String> data);

    @GET("get_users/{type}")
    Call<List<UserListFragment.UserMember>> getUsers(@Path("type") String type);

    @DELETE("delete_user/{type}/{id}")
    Call<ResponseBody> deleteUser(@Path("type") String type, @Path("id") String id);
    @GET("/")
    Call<ResponseBody> checkHealth();
    @DELETE("/clear_logs") // or @POST if you prefer
    Call<ResponseBody> clearLogs();
}