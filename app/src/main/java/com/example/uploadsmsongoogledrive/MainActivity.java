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
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.graphics.Insets;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_SMS_PERMISSION = 1;
    private Button uploadButton;

    private RecyclerView recyclerView;
    private SmsAdapter smsAdapter;
    private List<Sms> smsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        uploadButton = findViewById(R.id.button);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        smsAdapter = new SmsAdapter(smsList);
        recyclerView.setAdapter(smsAdapter);

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

        new LoadSmsTask().execute();
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
                File kartikDir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Kartik");
                if (!kartikDir.exists()) {
                    kartikDir.mkdirs();
                }
                File csvFile = new File(kartikDir, "sms_backup.csv");
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

    private class LoadSmsTask extends AsyncTask<Void, Void, List<Sms>> {
        @Override
        protected List<Sms> doInBackground(Void... voids) {
            List<Sms> smsList = new ArrayList<>();
            File csvFile = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "sms_backup.csv");
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