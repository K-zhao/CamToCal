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
import android.util.Log;
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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

import com.joestelmach.natty.*;

public class Display extends AppCompatActivity {

    // Global variables
    private Bitmap bitmap;
    private Button calendarBtn;
    private String sentence = "";
    private String LOGNAME = "calendarlog";
    final int callbackID = 42;



    // Different models for text recognition
    private static final int DENSE_MODEL = 2;
    private static final int SPARSE_MODEL = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display);
        String[] permissions = {
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR,
                Manifest.permission.READ_EXTERNAL_STORAGE};
        checkPermission(callbackID, permissions);

        // Error handling if the user attempts to access the activity without sending a photo through
        try {
            ImageView imageView = findViewById(R.id.imageView2);
            bitmap = BitmapFactory.decodeFile(getIntent().getStringExtra("image_path"));
            imageView.setImageBitmap(bitmap);
            calendarBtn = findViewById(R.id.calendarBtn);
            runFirebaseTextRecognition(DENSE_MODEL);
            setupSwitch();
            setupCalendarButton();
        } catch (NullPointerException e) {
            e.printStackTrace();
            showToast("Take a photo or pick one from the gallery first.");
        }


//        runTextRecognition();
    }

    private void setupSwitch() {
        Switch modelSwitch = findViewById(R.id.modelSwitch);

        modelSwitch.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            // If switch is toggled, use sparse_model instead of dense_model
            if (isChecked) {
                showToast("Successfully set to sparse text type!");
                runFirebaseTextRecognition(SPARSE_MODEL);
            } else {
                showToast("Successfully set to dense text type!");
                runFirebaseTextRecognition(DENSE_MODEL);
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
                    showToast("Text recognition...");
                    showFirebaseText(firebaseVisionText);

                })
                .addOnFailureListener(e -> showToast("Failure"));


    }
    // Firebase cloud based text recognition - slower but more accurate

    private void showFirebaseText(FirebaseVisionText firebaseVisionText) {

        List<FirebaseVisionText.TextBlock> block = firebaseVisionText.getTextBlocks();
        sentence = "";
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

        calendarBtn.setOnClickListener(view -> {
            Calendar cal = Calendar.getInstance();
            // Get timezone of user
            TimeZone tz = cal.getTimeZone();

            // Attempts to figure out which date due to potential ambiguity of user inputted dates
            calendarBtn = findViewById(R.id.calendarBtn);
            Parser parser = new Parser(tz);
            List<DateGroup> groups = parser.parse(sentence);
            Log.d(LOGNAME, "hello");
            for(DateGroup group:groups) {
                ArrayList<Date> dates = new ArrayList<Date> (group.getDates());
                int line = group.getLine();
                int column = group.getPosition();
                String matchingValue = group.getText();
                Log.d(LOGNAME, "line" + String.valueOf(line));
                Log.d(LOGNAME, "column"+ String.valueOf(column));
                Log.d(LOGNAME, "text" + matchingValue);
                long eventTime = 0;
                for (int x = 0; x < dates.size(); x++) {
                    Log.d(LOGNAME, "dates" + dates.get(x).toString());
                    Date date = dates.get(x);
                    eventTime = date.getTime();
                }

                // Title is everything with time/date removed
                String title =  sentence.replace(matchingValue, "");


                // Add event to calendar
                ContentResolver cr = getApplication().getContentResolver();
                ContentValues cv = new ContentValues();
                cv.put(CalendarContract.Events.TITLE, title);
                cv.put(CalendarContract.Events.DTSTART, eventTime);
                cv.put(CalendarContract.Events.DTEND, eventTime + 3600000);
                cv.put(CalendarContract.Events.CALENDAR_ID, 1);
                cv.put(CalendarContract.Events.EVENT_TIMEZONE, Calendar.getInstance().getTimeZone().getID());
                Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI,cv);

                showToast("Check your Calendar");

            }


            // Alerts user if the calendar failed to add event due to unrecognizable dates
            if (groups.size() == 0) {
                showToast("Couldn't find any dates.");
            }


        });
    }
}
