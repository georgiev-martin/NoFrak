package org.apache.cordova;

import java.io.File;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;

import android.content.Context;
import android.content.ContentValues;

import android.util.Log;

/**
 * This class is used as a utility to set up the localStorage DB and insert one SecureToken per autherized domain.
 *
 * @author
 * @since 7/20/2013
 */
public class WebStorageHelperImpl extends SQLiteOpenHelper {
    
    private static WebStorageHelperImpl webStorageHelperImpl;
	private static String TAG = "WebStorageHandler";
    private static String DB_BASE_PATH;
    private static String DB_NAME;
    private SQLiteDatabase db;
    
    public WebStorageHelperImpl(Context context) {
        super(context, null, null, 1);
        DB_BASE_PATH = "/data/data/" + context.getApplicationContext().getPackageName() + "/app_database/localstorage/";
        webStorageHelperImpl = this;
    }
    
    /**
      * Get the instance of webStorageHelperImpl.
      * @return the instance of webStorageHelperImpl; null if the instance has not been created yet.
      */
    public static WebStorageHelperImpl getInstance() {
        return webStorageHelperImpl;
    }
    
	@Override
	public void onCreate(SQLiteDatabase db) { }
    
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { }
    
    /**
     * Open the database.
     */
    public void test() {
        DB_NAME = "https_www.example.com_0.localstorage";
        setItem(DB_NAME, "PhoneGap", "PhoneGap-v2.9.0");
    }
    
    /**
      * Create a database.
      * @dbName : the name of the database.
      * @return : true, if the db was created; false otherwise.
      */
    public boolean createDatabase(String dbName) {
        String dbPath = DB_BASE_PATH + dbName;
        try {
            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE);
            closeDB();
            return true;
        } catch(SQLiteException e) {
            try {
                db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.CREATE_IF_NECESSARY | SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING);
                db.execSQL("CREATE TABLE ItemTable (key TEXT UNIQUE ON CONFLICT REPLACE, value TEXT NOT NULL ON CONFLICT FAIL);");
                closeDB();
                return true;
            } catch(SQLiteException ex) {
                return false;
            }
        }
    }
    
    /**
      * Insert an item into localStorage.
      * @param dbPath : the path to the database.
      * @param key : the key of the element to be inserted.
      * @param value : the value of the element to be inserted.
      * @return true, if the element was inserted in the db; false, otherwise.
      */
    public boolean setItem(String dbName, String key, String value) {
        try {
            String dbPath = DB_BASE_PATH + dbName;
            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.OPEN_READWRITE | SQLiteDatabase.ENABLE_WRITE_AHEAD_LOGGING);
            
            ContentValues contentValues = new ContentValues(2);
            contentValues.put("key", key);
            contentValues.put("value", value);
            db.insert("ItemTable", null, contentValues);
            
            closeDB();
            return true;
        } catch(SQLiteException e) {
            return false;
        }
    }
    
    /**
     * Close the databse.
     */
    private synchronized void closeDB() {
        if(db != null) {
            db.close();
        }
        super.close();
    }    
}