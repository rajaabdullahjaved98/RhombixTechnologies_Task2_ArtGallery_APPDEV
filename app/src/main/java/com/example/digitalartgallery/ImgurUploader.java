package com.example.digitalartgallery;

import android.graphics.Bitmap;
import android.util.Base64;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayOutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

public class ImgurUploader {

    private static final String CLIENT_ID = "2d353f85eb97c2f"; // Replace with your Imgur client ID
    private static final String UPLOAD_URL = "https://api.imgur.com/3/image";

    public interface UploadCallback {
        void onUploadSuccess(String imageUrl);
        void onUploadFailure(String errorMessage);
    }

    // Convert Bitmap to Base64 string
    public static String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    // Upload image to Imgur
    public static void uploadImageToImgur(String base64Image, final UploadCallback callback) {
        OkHttpClient client = new OkHttpClient();

        // Create JSON body for the request
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("image", base64Image);
        } catch (JSONException e) {
            e.printStackTrace();
            callback.onUploadFailure("Failed to create JSON request body.");
            return;
        }

        RequestBody requestBody = RequestBody.create(
                jsonBody.toString(),
                MediaType.parse("application/json; charset=utf-8")
        );

        // Create request
        Request request = new Request.Builder()
                .url(UPLOAD_URL)
                .addHeader("Authorization", "Client-ID " + CLIENT_ID)
                .post(requestBody)
                .build();

        // Execute request
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e("ImgurUploader", "Image upload failed: " + e.getMessage());
                callback.onUploadFailure("Image upload failed: " + e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful()) {
                    try {
                        JSONObject jsonResponse = new JSONObject(response.body().string());
                        String imageUrl = jsonResponse.getJSONObject("data").getString("link");
                        callback.onUploadSuccess(imageUrl);
                    } catch (JSONException e) {
                        Log.e("ImgurUploader", "Failed to parse Imgur response: " + e.getMessage());
                        callback.onUploadFailure("Failed to parse Imgur response.");
                    }
                } else {
                    callback.onUploadFailure("Image upload failed: " + response.message());
                }
            }
        });
    }
}
