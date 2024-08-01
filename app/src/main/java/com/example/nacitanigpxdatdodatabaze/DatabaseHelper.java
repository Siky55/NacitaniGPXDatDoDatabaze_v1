package com.example.nacitanigpxdatdodatabaze;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.io.File;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "waypoints.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "waypoints";

    public DatabaseHelper(Context context) {
        super(context, "/sdcard/myapp/" + DATABASE_NAME, null, DATABASE_VERSION);

        File databaseDir = new File("/sdcard/myapp/");
        if (!databaseDir.exists()) {
            databaseDir.mkdirs();
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_NAME + " (" +
                "wp_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "latitude FLOAT, " +
                "longitude FLOAT, " +
                "ele FLOAT, " +
                "label VARCHAR(60), " +
                "symb CHAR(1) NOT NULL)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}

