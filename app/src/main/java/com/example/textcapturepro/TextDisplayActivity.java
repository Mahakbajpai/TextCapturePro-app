package com.example.textcapturepro;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;
import android.graphics.pdf.PdfDocument;
import android.graphics.Paint;
import android.graphics.Canvas;
import android.os.Environment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;

public class TextDisplayActivity extends AppCompatActivity {

    private TextInputEditText editTextRecognized;
    private Button btnSavePdf, btnSaveTxt, btnEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_text_display2);


        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setTitle("Scanned Text");   
        toolbar.setNavigationOnClickListener(v -> finish());


        editTextRecognized = findViewById(R.id.editTextRecognized);
        btnSavePdf = findViewById(R.id.btnSavePdf);
        btnSaveTxt = findViewById(R.id.btnSavetxt);
        btnEditText = findViewById(R.id.btnEditText);


        String extractedText = getIntent().getStringExtra("recognized_text");
        if (extractedText != null && !extractedText.trim().isEmpty()) {
            editTextRecognized.setText(extractedText);
            saveToHistory(extractedText);
        }


        btnEditText.setOnClickListener(v -> {
            editTextRecognized.requestFocus();
            Toast.makeText(this, "You can edit the text now", Toast.LENGTH_SHORT).show();
        });


        btnSavePdf.setOnClickListener(v -> {
            String text = editTextRecognized.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "No text to save", Toast.LENGTH_SHORT).show();
            } else {
                saveAsPdf(text);
            }
        });


        btnSaveTxt.setOnClickListener(v -> {
            String text = editTextRecognized.getText().toString().trim();
            if (text.isEmpty()) {
                Toast.makeText(this, "No text to save", Toast.LENGTH_SHORT).show();
            } else {
                saveAsTextFile(text);
            }
        });
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.text_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        String text = editTextRecognized.getText().toString();

        if (item.getItemId() == R.id.action_copy) {
            ClipboardManager clipboard =
                    (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("OCR Text", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Text copied", Toast.LENGTH_SHORT).show();
            return true;
        }

        if (item.getItemId() == R.id.action_share) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(shareIntent, "Share Text"));
            return true;
        }

        if (item.getItemId() == R.id.menu_history) {
            startActivity(new Intent(this, HistoryActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private void saveAsPdf(String text) {

        PdfDocument pdfDocument = new PdfDocument();
        Paint paint = new Paint();

        PdfDocument.PageInfo pageInfo =
                new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();

        paint.setTextSize(16);
        paint.setFakeBoldText(true);
        canvas.drawText("TextCapturePro - OCR Document", 40, 30, paint);

        paint.setTextSize(12);
        paint.setFakeBoldText(false);

        int x = 40;
        int y = 60;

        for (String line : text.split("\n")) {
            canvas.drawText(line, x, y, paint);
            y += 20;

            if (y > 800) {
                pdfDocument.finishPage(page);
                pageInfo = new PdfDocument.PageInfo.Builder(
                        595, 842, pdfDocument.getPages().size() + 1).create();
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 40;
            }
        }

        pdfDocument.finishPage(page);

        File file = new File(
                getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "OCR_Text_" + System.currentTimeMillis() + ".pdf");

        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF saved", Toast.LENGTH_SHORT).show();
            openFile(file, "application/pdf");
        } catch (IOException e) {
            e.printStackTrace();
        }

        pdfDocument.close();
    }



    private void saveAsTextFile(String text) {

        File file = new File(
                getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS),
                "OCR_Text_" + System.currentTimeMillis() + ".txt");

        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(text.getBytes());
            fos.close();

            Toast.makeText(this, "Text file saved", Toast.LENGTH_SHORT).show();
            openFile(file, "text/plain");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void openFile(File file, String mimeType) {

        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                file
        );

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mimeType);
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this,
                    "No app found to open this file",
                    Toast.LENGTH_SHORT).show();
        }
    }



    private void saveToHistory(String text) {

        SharedPreferences prefs =
                getSharedPreferences("OCR_HISTORY", MODE_PRIVATE);

        String oldHistory = prefs.getString("history_list", "");
        String newHistory = text + "###" + oldHistory;

        String[] entries = newHistory.split("###");
        StringBuilder limitedHistory = new StringBuilder();

        for (int i = 0; i < Math.min(entries.length, 10); i++) {
            limitedHistory.append(entries[i]).append("###");
        }

        prefs.edit()
                .putString("history_list", limitedHistory.toString())
                .apply();
    }
}
