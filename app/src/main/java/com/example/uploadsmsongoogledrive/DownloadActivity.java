package com.example.uploadsmsongoogledrive;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import android.widget.TextView;

public class DownloadActivity extends AppCompatActivity {

    private static final String TAG = "DownloadActivity";
    private Drive googleDriveService;
    private Button downloadButton;

    private TextView fileContentTextView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        downloadButton = findViewById(R.id.downloadButton);
        fileContentTextView = findViewById(R.id.fileContentTextView);


        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null) {
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    this, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(account.getAccount());

            googleDriveService = new Drive.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName("UploadSmsOnGoogleDrive")
                    .build();
        }

        String fileId = getIntent().getStringExtra("fileId");

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadFileFromDrive(fileId);
            }
        });
    }

    private void downloadFileFromDrive(String fileId) {
        new Thread(() -> {
            try {
                Log.d(TAG, "Starting download for fileId: " + fileId);

                // Fetch the file metadata
                File file = googleDriveService.files().get(fileId).execute();
                Log.d(TAG, "File fetched: " + file.getName());

                String fileName = file.getName();
                java.io.File filePath = new java.io.File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);
                Log.d(TAG, "File path created: " + filePath.getAbsolutePath());

                // Download the file content
                try (OutputStream outputStream = new FileOutputStream(filePath)) {
                    googleDriveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                    Log.d(TAG, "File downloaded successfully");
                }

                // Read the file content
                StringBuilder fileContent = new StringBuilder();
                try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        fileContent.append(line).append("\n");
                    }
                }

                // Update UI with file content
                runOnUiThread(() -> {
                    Toast.makeText(DownloadActivity.this, "Download completed", Toast.LENGTH_SHORT).show();
                    fileContentTextView.setText(fileContent.toString());
                });

            } catch (IOException e) {
                Log.e(TAG, "An error occurred: " + e);
                runOnUiThread(() -> Toast.makeText(DownloadActivity.this, "Download failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

}