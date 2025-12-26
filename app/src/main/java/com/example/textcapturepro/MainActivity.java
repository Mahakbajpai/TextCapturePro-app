package com.example.textcapturepro;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.yalantis.ucrop.UCrop;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    // UI
    LinearLayout btnCamera, btnGallery;

    ProgressBar progressBar;

    // Image handling
    private Uri cameraImageUri;
    private Bitmap bitmap;

    private static final int CAMERA_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Bind UI
        btnCamera = findViewById(R.id.btnCamera);
        btnGallery = findViewById(R.id.btnGallery);

        progressBar = findViewById(R.id.progressBar);

        // Camera click
        btnCamera.setOnClickListener(v -> checkCameraPermission());

        // Gallery click
        btnGallery.setOnClickListener(v -> openGallery());
    }



    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_CODE
            );
        } else {
            openCamera();
        }
    }

    private void openCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "OCR Image");
        values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");

        cameraImageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
        cameraLauncher.launch(intent);
    }



    private void openGallery() {
        Intent intent = new Intent(
                Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }



    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK) {
                            startCrop(cameraImageUri);
                        }
                    });

    private final ActivityResultLauncher<Intent> galleryLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri galleryUri = result.getData().getData();
                            if (galleryUri != null) {
                                startCrop(galleryUri);
                            }
                        }
                    });



    private void startCrop(Uri sourceUri) {
        Uri destinationUri = Uri.fromFile(
                new java.io.File(
                        getCacheDir(),
                        "cropped_" + System.currentTimeMillis() + ".jpg"));

        UCrop.of(sourceUri, destinationUri)
                .withMaxResultSize(1080, 1080)
                .start(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK && data != null) {
            Uri croppedUri = UCrop.getOutput(data);
            if (croppedUri != null) {
                try {
                    bitmap = MediaStore.Images.Media
                            .getBitmap(this.getContentResolver(), croppedUri);



                    runTextRecognition(bitmap);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



    private void runTextRecognition(Bitmap bitmap) {

        progressBar.setVisibility(View.VISIBLE);

        InputImage image = InputImage.fromBitmap(bitmap, 0);

        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                .process(image)
                .addOnSuccessListener(result -> {

                    progressBar.setVisibility(View.GONE);

                    StringBuilder formattedText = new StringBuilder();

                    for (com.google.mlkit.vision.text.Text.TextBlock block : result.getTextBlocks()) {

                        for (com.google.mlkit.vision.text.Text.Line line : block.getLines()) {
                            formattedText.append(line.getText()).append("\n");
                        }

                        // extra space between blocks (paragraph-like)
                        formattedText.append("\n");
                    }

                    String extractedText = formattedText.toString();

                    if (extractedText.trim().isEmpty()) {
                        Toast.makeText(this, "No text detected", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Intent intent = new Intent(
                            MainActivity.this,
                            TextDisplayActivity.class);
                    intent.putExtra("recognized_text", extractedText);
                    startActivity(intent);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "OCR failed", Toast.LENGTH_SHORT).show();
                });
    }



    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(
                requestCode, permissions, grantResults);

        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(
                        this,
                        "Camera permission required",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }
}
