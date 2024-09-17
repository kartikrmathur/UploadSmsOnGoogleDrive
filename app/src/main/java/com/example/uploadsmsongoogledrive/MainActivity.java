package com.example.uploadsmsongoogledrive;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Telephony;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_SMS_PERMISSION = 1;
    private static final int REQUEST_SIGN_IN = 2;
    private RecyclerView recyclerView;
    private SmsAdapter smsAdapter;
    private List<Sms> smsList = new ArrayList<>();
    private Drive googleDriveService;
    private Button uploadButton, downloadButton,loginButton;
    private String fileId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        smsAdapter = new SmsAdapter(smsList);
        recyclerView.setAdapter(smsAdapter);

        uploadButton = findViewById(R.id.uploadButton);
        downloadButton = findViewById(R.id.downloadButton);
        loginButton = findViewById(R.id.loginButton);

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_SMS}, REQUEST_SMS_PERMISSION);
                } else {
                    convertSmsToCsv();
                }
            }
        });

        downloadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                downloadFileFromDrive(fileId);
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInToGoogleDrive();
            }
        });

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

        fileId = getIntent().getStringExtra("fileId");

        new LoadSmsTask().execute();
    }
    private void signInToGoogleDrive() {
        GoogleSignInOptions signInOptions = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new com.google.android.gms.common.api.Scope(DriveScopes.DRIVE_FILE))
                .build();
        GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(this, signInOptions);
        startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }
    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
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

                Toast.makeText(this, "Signed in to Google Drive", Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            Toast.makeText(this, "Sign-in failed", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_SMS_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                convertSmsToCsv();
            } else {
                Toast.makeText(this, "Permission denied to read SMS", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void convertSmsToCsv() {
        Uri smsUri = Telephony.Sms.CONTENT_URI;
        Cursor cursor = getContentResolver().query(smsUri, null, null, null, null);
        if (cursor != null) {
            try {
                java.io.File kartikDir = new java.io.File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Kartik");
                if (!kartikDir.exists()) {
                    kartikDir.mkdirs();
                }
                java.io.File csvFile = new java.io.File(kartikDir, "sms_backup.csv");
                if (!csvFile.exists()) {
                    csvFile.createNewFile();
                }
                try (FileWriter writer = new FileWriter(csvFile)) {
                    writer.append("Address,Date,Body\n");
                    int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
                    int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
                    int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);

                    if (addressIndex != -1 && dateIndex != -1 && bodyIndex != -1) {
                        while (cursor.moveToNext()) {
                            String address = cursor.getString(addressIndex);
                            String date = cursor.getString(dateIndex);
                            String body = cursor.getString(bodyIndex);
                            writer.append(address).append(",").append(date).append(",").append(body.replace("\n", " ")).append("\n");
                        }
                    }
                    writer.flush();
                }
                Toast.makeText(this, "SMS backup completed", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(this, GoogleDriveActivity.class);
                startActivity(intent);
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to write CSV file", Toast.LENGTH_SHORT).show();
            } finally {
                cursor.close();
            }
        }
    }

    private void downloadFileFromDrive(String fileId) {
        if (googleDriveService == null) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, "Google Drive service is not initialized", Toast.LENGTH_SHORT).show());
            return;
        }

        new Thread(() -> {
            String finalFileId = fileId;
            try {
                if (finalFileId == null || finalFileId.isEmpty()) {
                    // Fetch the list of files and get the last uploaded file
                    FileList result = googleDriveService.files().list()
                            .setPageSize(1)
                            .setOrderBy("createdTime desc")
                            .setFields("files(id, name)")
                            .execute();
                    List<File> files = result.getFiles();
                    if (files == null || files.isEmpty()) {
                        runOnUiThread(() -> Toast.makeText(MainActivity.this, "No files found on Google Drive", Toast.LENGTH_SHORT).show());
                        return;
                    }
                    finalFileId = files.get(0).getId();
                }

                // Fetch the file metadata
                File file = googleDriveService.files().get(finalFileId).execute();
                String fileName = file.getName();
                java.io.File filePath = new java.io.File(getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName);

                // Download the file content
                try (OutputStream outputStream = new FileOutputStream(filePath)) {
                    googleDriveService.files().get(finalFileId).executeMediaAndDownloadTo(outputStream);
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
                    Toast.makeText(MainActivity.this, "Download completed", Toast.LENGTH_SHORT).show();
                    // Assuming you have a TextView to display the file content
                    TextView fileContentTextView = findViewById(R.id.fileContentTextView);
                    fileContentTextView.setText(fileContent.toString());
                });

            } catch (IOException e) {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Download failed", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }    private class LoadSmsTask extends AsyncTask<Void, Void, List<Sms>> {
        @Override
        protected List<Sms> doInBackground(Void... voids) {
            List<Sms> smsList = new ArrayList<>();
            java.io.File csvFile = new java.io.File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "sms_backup.csv");
            if (csvFile.exists()) {
                try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
                    String line;
                    reader.readLine(); // Skip header
                    while ((line = reader.readLine()) != null) {
                        String[] tokens = line.split(",");
                        if (tokens.length == 3) {
                            String address = tokens[0];
                            String date = tokens[1];
                            String body = tokens[2];
                            smsList.add(new Sms(address, date, body));
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return smsList;
        }

        @Override
        protected void onPostExecute(List<Sms> smsList) {
            MainActivity.this.smsList.clear();
            MainActivity.this.smsList.addAll(smsList);
            smsAdapter.notifyDataSetChanged();
        }
    }
}