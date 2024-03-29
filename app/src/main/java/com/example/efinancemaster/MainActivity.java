package com.example.efinancemaster;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText searchEditText;
    private Button searchButton;
    private RecyclerView transactionsRecyclerView;
    private TransactionAdapter transactionAdapter;
    private AppDatabase db;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        searchEditText = findViewById(R.id.searchEditText);
        searchButton = findViewById(R.id.searchButton);

        transactionsRecyclerView = findViewById(R.id.transactionsRecyclerView);
        transactionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        transactionAdapter = new TransactionAdapter();
        transactionsRecyclerView.setAdapter(transactionAdapter);

        initializeDatabase();

        searchButton.setOnClickListener(view -> searchTransactions(searchEditText.getText().toString()));

        if (getIntent().hasExtra("CALL_FUNCTION")) {
            String functionToCall = getIntent().getStringExtra("CALL_FUNCTION");
            if ("ReadFileData".equals(functionToCall)) {
                sendToSlaveApp();
            }
        }

    }

    private void initializeDatabase() {
        databaseExecutor.execute(() -> {
            db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, "EFDB")
                    .fallbackToDestructiveMigration()
                    .build();
            Log.d("DatabaseInit", "Database initialized.");
            addTransactions();
            loadTransactions();
        });
    }

    void sendToSlaveApp() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    String fileContents = readAuthNumbersFromJsonFile();
                    Intent returnIntent = new Intent();
                    returnIntent.putExtra("result", fileContents);
                    setResult(Activity.RESULT_OK, returnIntent);
                    finish();
                } catch (Exception e) {
                    Log.e("ExcelReadError", "Error reading Excel file", e);
                }

            }
        }, 2);
    }

    private void addTransactions() {
        databaseExecutor.execute(() -> {
            TransactionDao transactionDao = db.transactionDao();

            // Check if there are any transactions already in the database
            List<TransactionEntity> existingTransactions = transactionDao.getAll();
            if (existingTransactions.isEmpty()) {
                // Database is empty, so add initial transactions
                for (int i = 1; i <= 10; i++) {
                    TransactionEntity transaction = new TransactionEntity();
                    transaction.amount = 100.0 * i; // Example amount
                    transaction.referenceNumber = "Reference " + i; // Example reference number
                    transaction.authNumber = "AuthNumber " + i; // Example auth ID
                    transaction.transactionId = "TransactionId " + i; // Example transaction number
                    transactionDao.insertAll(transaction);
                }
                Log.d("MainActivity", "Initial transactions added.");
            } else {
                Log.d("MainActivity", "Database already contains transactions. No initial data added.");
            }
        });
    }

    private void loadTransactions() {
        databaseExecutor.execute(() -> {
            try {
                List<TransactionEntity> transactions = db.transactionDao().getAll();
                runOnUiThread(() -> {
                    Log.d("UIUpdate", "Attempting to update UI with transactions");
                    try {
                        transactionAdapter.setTransactions(transactions);
                    } catch (Exception e) {
                        Log.e("UIUpdateError", "Error updating UI", e);
                    }
                });
                Log.d("UIUpdate66", "Attempting 66 UI with ");

            } catch (Exception e) {
                Log.e("LoadTransactions", "Error loading transactions", e);
            }
        });
    }

    private void searchTransactions(String queryText) {
        String query = "%" + queryText + "%";
        databaseExecutor.execute(() -> {
            List<TransactionEntity> transactions = db.transactionDao().searchByQuery(query);
            if (!transactions.isEmpty()) {
                // Export transactions to Excel
                runOnUiThread(() -> transactionAdapter.setTransactions(transactions));
                exportTransactionsToJson(transactions);
            } else {
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "No transactions found with that reference number.", Toast.LENGTH_SHORT).show());
            }

        });
    }

    private void exportTransactionsToJson(List<TransactionEntity> transactions) {
        Gson gson = new Gson();// Use a List to collect auth numbers
        List<String> authNumbers = new ArrayList<>();
        for (TransactionEntity transaction : transactions) {

            String authNumber = transaction.authNumber;
            authNumbers.add(authNumber);
        }

        String jsonArray = gson.toJson(authNumbers);
        String fileName = "AuthNumbers.txt";
        File file = new File(MainActivity.this.getExternalFilesDir(null), fileName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(jsonArray);
            Log.d("SUCCESS", "Exported auth numbers to " + file.getAbsolutePath());
            runOnUiThread(() -> {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle(Html.fromHtml("<font color='#FF5722'>Export Result</font>")); // Custom color for title
                builder.setMessage("Exported auth numbers to " + file.getAbsolutePath());
                builder.setIcon(android.R.drawable.stat_sys_download_done); // Example icon
                builder.setPositiveButton("OK", null); // Button text can be styled in the dialog theme

                AlertDialog dialog = builder.create();
                dialog.setOnShowListener(dialogInterface -> {
                    Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

                    positiveButton.setTextColor(Color.parseColor("#FF5722"));
                });
                dialog.show();
            });

        } catch (IOException e) {
            Log.e("ERROR", "Failed to write auth numbers to file", e);
            e.printStackTrace();
        }
    }


    private String readAuthNumbersFromJsonFile() {
        StringBuilder stringBuilder = new StringBuilder();
        String fileName = "AuthNumbers.txt"; // Ensure this matches the file where auth numbers are stored
        File file = new File(getExternalFilesDir(null), fileName);

        try (FileInputStream fileInputStream = new FileInputStream(file);
             InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
             BufferedReader reader = new BufferedReader(inputStreamReader)) {

            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        } catch (IOException e) {
            Log.e("ERROR", "Failed to read the file", e);
            e.printStackTrace();
            return "Failed to read auth numbers";
        }


        String jsonContent = stringBuilder.toString();
        Gson gson = new Gson();
        Type type = new TypeToken<List<String>>() {
        }.getType();
        List<String> authNumbers = gson.fromJson(jsonContent, type);

        StringBuilder authNumbersStringBuilder = new StringBuilder();
        for (String authNumber : authNumbers) {
            authNumbersStringBuilder.append(authNumber).append("\n");
        }

        return authNumbersStringBuilder.toString();
    }


}
