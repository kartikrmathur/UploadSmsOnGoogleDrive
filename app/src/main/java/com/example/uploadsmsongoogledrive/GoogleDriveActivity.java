package com.example.uploadsmsongoogledrive;

import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;

import java.io.IOException;
import java.util.Collections;

public class GoogleDriveActivity extends AppCompatActivity {

    private AlertDialog progressDialog;

    private static final int RC_SIGN_IN = 1;
    private static final String TAG = "GoogleDriveActivity";
    private Drive googleDriveService;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE))
                .build();

        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, gso);
        startActivityForResult(googleSignInClient.getSignInIntent(), RC_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            GoogleAccountCredential credential = GoogleAccountCredential.usingOAuth2(
                    this, Collections.singleton(DriveScopes.DRIVE_FILE));
            credential.setSelectedAccount(account.getAccount());

            googleDriveService = new Drive.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance(),
                    credential)
                    .setApplicationName("UploadSmsOnGoogleDrive")
                    .build();

            uploadFileToDrive();

        } catch (ApiException e) {
            Log.w(TAG, "signInResult:failed code=" + e.getStatusCode());
            Toast.makeText(this, "Sign-in failed.", Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadFileToDrive() {
        showProgressDialog();

        new Thread(() -> {
            try {
                java.io.File filePath = new java.io.File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Kartik/sms_backup.csv");
                com.google.api.services.drive.model.File fileMetadata = new com.google.api.services.drive.model.File();
                fileMetadata.setName("sms_backup.csv");

                FileContent mediaContent = new FileContent("text/csv", filePath);
                File file = googleDriveService.files().create(fileMetadata, mediaContent)
                        .setFields("id")
                        .execute();
                Log.d(TAG, "File ID: " + file.getId());
                Intent intent = new Intent(GoogleDriveActivity.this, DownloadActivity.class);
                intent.putExtra("fileId", file.getId());
                startActivity(intent);
            } catch (IOException e) {
                Log.e(TAG, "An error occurred: " + e.getMessage(), e);
            } finally {
                runOnUiThread(this::dismissProgressDialog);
            }
        }).start();
    }

    private void showProgressDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Uploading to Drive...")
                .setCancelable(false);
        progressDialog = builder.create();
        progressDialog.show();
    }

    private void dismissProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}