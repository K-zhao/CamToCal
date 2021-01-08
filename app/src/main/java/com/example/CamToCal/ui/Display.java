package com.example.CamToCal.ui;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
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
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import com.joestelmach.natty.*;

public class Display extends AppCompatActivity {

    // Global variables
    private Bitmap bitmap;
    private Button calendarBtn;
    private String sentence = "";
    final int callbackID = 42;



    // Different models for text recognition
    private static final int DENSE_MODEL = 2;
    private static final int SPARSE_MODEL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        String[] permissions = {Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR};
        checkPermission(callbackID, permissions);
        ImageView imageView = findViewById(R.id.imageView2);
        bitmap = BitmapFactory.decodeFile(getIntent().getStringExtra("image_path"));
        calendarBtn = findViewById(R.id.calendarBtn);
        imageView.setImageBitmap(bitmap);
        runFirebaseTextRecognition(DENSE_MODEL);
        setupSwitch();
        setupCalendarButton();
//        runTextRecognition();
    }

    private void setupSwitch() {
        Switch modelSwitch = findViewById(R.id.modelSwitch);

        modelSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                // If switch is toggled, use sparse_model instead of dense_model
                if (isChecked) {
                    showToast("Successfully set to sparse text type!");
                    runFirebaseTextRecognition(SPARSE_MODEL);
                } else {
                    showToast("Successfully set to dense text type!");
                    runFirebaseTextRecognition(DENSE_MODEL);
                }
            }
        });


    }


    // Ask for permission to access user calendar
    private void checkPermission(int callbackID, String[] permissionsId) {
        boolean permissions = true;
        for (String p : permissionsId) {
            permissions = permissions && ContextCompat.checkSelfPermission(this, p) == PERMISSION_GRANTED;
        }

        if (!permissions)
            ActivityCompat.requestPermissions(this, permissionsId, callbackID);
    }

    // Firebase cloud based text recognition - slower but more accurate
    private void runFirebaseTextRecognition(int model) {
        FirebaseVisionImage firebaseImage = FirebaseVisionImage.fromBitmap(bitmap);

        FirebaseVisionCloudTextRecognizerOptions options =
                new FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setLanguageHints(Arrays.asList("en")).setModelType(model)
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
    // Firebase cloud based text recognition - slower but more accurate

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
    // Helper function to make calling toast easier
    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
    }
    // On-board text recognition - faster but at the cost of accuracy

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
    // On-board text recognition - faster but at the cost of accuracy

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

    // Add event to calendar
    private void setupCalendarButton() {

        calendarBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Calendar cal = Calendar.getInstance();
                // Get timezone of user
                TimeZone tz = cal.getTimeZone();

                // Attempts to figure out which date due to potential ambiguity of user inputted dates
                calendarBtn = findViewById(R.id.calendarBtn);
                Parser parser = new Parser(tz);
                List<DateGroup> groups = parser.parse(sentence);

                ContentResolver cr = getApplication().getContentResolver();
                ContentValues cv = new ContentValues();
                cv.put(CalendarContract.Events.TITLE, "Hello");
                cv.put(CalendarContract.Events.DTSTART, Calendar.getInstance().getTimeInMillis());
                cv.put(CalendarContract.Events.DTEND, Calendar.getInstance().getTimeInMillis() + 60*60*1000);
                cv.put(CalendarContract.Events.CALENDAR_ID, 1);
                cv.put(CalendarContract.Events.EVENT_TIMEZONE, Calendar.getInstance().getTimeZone().getID());
                Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI,cv);

                showToast("Check your Calendar");
            }
        });
    }
}