package com.example.nacitanigpxdatdodatabaze;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.GridView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_FILE = 1;
    private DatabaseHelper dbHelper;
    private GridView gridView;
    private ActivityResultLauncher<Intent> pickFileLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new DatabaseHelper(this);
        gridView = findViewById(R.id.gridView);

        pickFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        handleFilePick(result.getData());
                    }
                }
        );

        Button loadButton = findViewById(R.id.button_load);
        loadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_CODE_PICK_FILE);
                } else {
                    pickFile();
                }
            }
        });
    }

    private void pickFile() {
        Log.d("myLog", "pickFile");
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*"); // Povolit XML soubory
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        pickFileLauncher.launch(intent);
    }

    private String getFileName(Uri uri) {
        Log.d("myLog", "getFileName");
        String result = null;
        String[] projection = { MediaStore.Images.Media.DISPLAY_NAME };

        try (Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
                result = cursor.getString(column_index);
            }
        }

        return result;
    }


    private void handleFilePick(Intent data) {
        Log.d("myLog", "handleFilePick");
        if (data.getData() != null) {
            Uri fileUri = data.getData();
            String fileName = getFileName(fileUri);

            if (fileName != null && fileName.toLowerCase().endsWith(".gpx")) {
                try (InputStream is = getContentResolver().openInputStream(fileUri)) {
                    parseAndInsertGpx(is);
                    displayData();
                } catch (IOException | XmlPullParserException e) {
                    e.printStackTrace();
                    Toast.makeText(this, "Error processing the GPX file.", Toast.LENGTH_LONG).show();
                }
            } else {
                Toast.makeText(this, "Invalid file type. Please select a GPX file.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "File URI is null.", Toast.LENGTH_LONG).show();
        }
    }

    private void parseAndInsertGpx(InputStream inputStream) throws XmlPullParserException, IOException {
        Log.d("myLog", "ParseAndInsertGpx");
        SQLiteDatabase db = dbHelper.getWritableDatabase(); // chyba
        Log.d("myLog", "databaze nactena");
        db.execSQL("DELETE FROM " + DatabaseHelper.TABLE_NAME);
        Log.d("myLog", "table deleted");
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        XmlPullParser parser = factory.newPullParser();
        parser.setInput(inputStream, null);

        String lat = null, lon = null, ele = null, label = null;
        boolean isValidGpx = false;

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            String name = parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if ("wpt".equals(name)) {
                    isValidGpx = true;
                    lat = parser.getAttributeValue(null, "lat");
                    lon = parser.getAttributeValue(null, "lon");
                } else if ("ele".equals(name)) {
                    ele = parser.nextText();
                } else if ("name".equals(name)) {
                    label = parser.nextText();
                }
            } else if (eventType == XmlPullParser.END_TAG && "wpt".equals(name)) {
                if (lat != null && lon != null) {
                    insertWaypoint(db, lat, lon, ele, label);
                }
                lat = lon = ele = label = null; // Reset values for the next waypoint
            }
            eventType = parser.next();
        }

        if (!isValidGpx) {
            throw new XmlPullParserException("The file is not a valid GPX file.");
        }
    }


    private void insertWaypoint(SQLiteDatabase db, String lat, String lon, String ele, String label) {
        String sql = "INSERT INTO " + DatabaseHelper.TABLE_NAME + " (latitude, longitude, ele, label, symb) VALUES (?, ?, ?, ?, ?)";
        db.execSQL(sql, new String[]{lat, lon, ele, label, label == null ? "T" : "P"});
    }

    private void displayData() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Map<String, String>> data = new ArrayList<>();
        String[] columns = {"wp_id", "latitude", "longitude", "ele", "label", "symb"};

        Cursor cursor = db.query(DatabaseHelper.TABLE_NAME, columns, null, null, null, null, null);
        while (cursor.moveToNext()) {
            Map<String, String> item = new HashMap<>();
            item.put("wp_id", cursor.getString(0));
            item.put("latitude", cursor.getString(1));
            item.put("longitude", cursor.getString(2));
            item.put("ele", cursor.getString(3));
            item.put("label", cursor.getString(4));
            item.put("symb", cursor.getString(5));
            data.add(item);
        }
        cursor.close();

        String[] from = {"wp_id", "latitude"};
        int[] to = {android.R.id.text1, android.R.id.text2};

        SimpleAdapter adapter = new SimpleAdapter(this, data, android.R.layout.simple_list_item_2, from, to);
        gridView.setAdapter(adapter);
    }
}

