package com.example.CamToCal.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import com.example.CamToCal.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;

import java.util.Arrays;
import java.util.List;

public class Display extends AppCompatActivity {

    private Bitmap bitmap;

    // Different models for text recognition
    private static final int DENSE_MODEL = 2;
    private static final int SPARSE_MODEL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        ImageView imageView = findViewById(R.id.imageView2);

        bitmap = BitmapFactory.decodeFile(getIntent().getStringExtra("image_path"));

        imageView.setImageBitmap(bitmap);
        runFirebaseTextRecognition();
//        runTextRecognition();
    }

    private void runFirebaseTextRecognition() {
        FirebaseVisionImage firebaseImage = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVisionCloudTextRecognizerOptions options =
                new FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setLanguageHints(Arrays.asList("en")).setModelType(DENSE_MODEL)
                .build();


        FirebaseVisionTextRecognizer textRecognizer = FirebaseVision.getInstance()
                .getCloudTextRecognizer(options);

        textRecognizer.processImage(firebaseImage)
                .addOnSuccessListener(firebaseVisionText -> {
                    showToast("Success!");
                    showFirebaseText(firebaseVisionText);

                })
                .addOnFailureListener(e -> showToast("Failure"));


    }

    private void showFirebaseText(FirebaseVisionText firebaseVisionText) {
        List<FirebaseVisionText.TextBlock> block = firebaseVisionText.getTextBlocks();
        String sentence = "";
        if (block.size() == 0) {
            showToast("No text found");
            return;
        } else {
            // Join up text found in photo into a string
            for (FirebaseVisionText.TextBlock text: block) {
                sentence += " " + text.getText();
            }
            showToast(sentence);
        }
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }

    private void runTextRecognition() {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        TextRecognizer recognizer = TextRecognition.getClient();
        recognizer.process(image)
                .addOnSuccessListener(
                        texts -> processTextRecognitionResult(texts))
                .addOnFailureListener(
                        e -> {
                            // Task failed with an exception
                            e.printStackTrace();
                        });
    }

    private void processTextRecognitionResult(Text texts) {
        String sentence = "";
        List<Text.TextBlock> blocks = texts.getTextBlocks();
        if (blocks.size() == 0) {
            showToast("No text found");
            return;
        } else {
            // Join up text found in photo into a string
            for (Text.TextBlock text: blocks) {
                sentence += " " + text.getText();
            }
            showToast(sentence);
        }
    }
}