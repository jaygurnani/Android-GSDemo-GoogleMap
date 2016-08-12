package com.dji.GSDemo.GoogleMap;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.media.MediaScannerConnection;
import android.os.Environment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.channels.FileChannel;

/**
 * Created by mac on 10/08/2016.
 */
public class DbHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 3;

    private static final String DB_NAME = "SensorData.db";

    public static final String TABLE_NAME = "SensorData";

    public static final String COL_LONG = "Longitude";
    public static final String COL_LAT = "Latitude";
    public static final String COL_ALT = "Altitude";
    public static final String COL_BATTERY = "Battery";
    public static final String COL_VOLT = "Voltage";
    public static final String COL_CURR = "Current";
    public static final String COL_DATE = "Date";
    public static final String COL_METHOD = "Method";



    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //Create the DB
        final String STRING_CREATE = "CREATE TABLE " + TABLE_NAME + " (_id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_LONG + " INTEGER, "
                + COL_LAT + " INTEGER, "
                + COL_ALT + " INTEGER, "
                + COL_BATTERY + " INTEGER, "
                + COL_VOLT + " INTEGER, "
                + COL_CURR + " INTEGER, "
                + COL_DATE + " DATE, "
                + COL_METHOD + " TEXT);";

        db.execSQL(STRING_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void exportDB(Context context){
        try {
            File sd = Environment.getExternalStorageDirectory();

            if (sd.canWrite()) {
                String currentDBPath = "/data/data/" + context.getPackageName()  + "/databases/" + DB_NAME;

                String backupDBPath = "ExportedBattery"+ (System.currentTimeMillis()/1000) +".db";
                File currentDB = new File(currentDBPath);
                File backupDB = new File(sd, backupDBPath);

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                }
                MediaScannerConnection.scanFile(context, new String[]{backupDB.getAbsolutePath()}, null, null);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
