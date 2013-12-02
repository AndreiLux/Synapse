/**
 * Author: Andrei F.
 *
 * This file is part of the "Synapse" software and is licensed under
 * under the Microsoft Reference Source License (MS-RSL).
 *
 * Please see the attached LICENSE.txt for the full license.
 */

package com.af.synapse.utils;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.af.synapse.Synapse;

/**
 * Created by Andrei on 03/09/13.
 */
public class ActionValueDatabase extends SQLiteOpenHelper {
    private static final String DB_PATH = Synapse.getAppContext()
                                                         .getFilesDir()
                                                         .getParentFile()
                                                         .getPath() + "/databases/";
    private static String DB_NAME = "actionValueStore";

    private static final String TABLE_NAME = "action_value";
    private static final String COL_CONTEXT = "context";
    private static final String COL_KEY = "key";
    private static final String COL_VALUE = "value";

    private SQLiteDatabase db;

    public ActionValueDatabase() {
        super(Synapse.getAppContext(), DB_NAME, null, 1);
    }

    public void createDataBase() {
        if ((db = getDatabase()) == null) {
            L.d("Database not found, creating one");
            db = this.getWritableDatabase();
        }
    }

    private SQLiteDatabase getDatabase() {
        SQLiteDatabase tempDB = null;

        try {
            String path = DB_PATH + DB_NAME;
            tempDB = SQLiteDatabase.openDatabase(path, null, SQLiteDatabase.OPEN_READWRITE);
        } catch (SQLiteException e){
            L.e(e.getMessage());
        }

        return tempDB;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String sqlCreateTable =
            "CREATE TABLE " + TABLE_NAME +
                    " (" +
                    COL_CONTEXT + " TEXT NOT NULL, " +
                    COL_KEY + " TEXT NOT NULL, " +
                    COL_VALUE + " TEXT NOT NULL, " +
                        "PRIMARY KEY (" + COL_CONTEXT + ", " + COL_KEY + ")" +
                    " );";
        L.d(sqlCreateTable);
        db.execSQL(sqlCreateTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    @Override
    public synchronized void close() {
        if(db != null)
            db.close();

        super.close();
    }

    /**
     *  Actual utility methods
     */
    public String getValue(String key) {
        return getValue(ContextSwitcher.getContext(), key);
    }

    public String getValue(String context, String key) {
        Cursor c = db.query(TABLE_NAME, new String[] { COL_VALUE }, "context=? AND key=?",
                    new String[] { context, key }, null, null, null);

        if (c.getCount() == 0)
            return null;

        c.moveToFirst();
        return c.getString(c.getColumnIndex(COL_VALUE));
    }

    public void setValue(String key, String value) {
        setValue(ContextSwitcher.getContext(), key, value);
    }

    public void setValue(String context, String key, String value) {
        ContentValues cv = new ContentValues();
        cv.put(COL_VALUE, value);

        if (getValue(context, key) == null) {
            cv.put(COL_CONTEXT, context);
            cv.put(COL_KEY, key);
            db.insertOrThrow(TABLE_NAME, null, cv);
        } else {
            db.update(TABLE_NAME, cv, "context=? AND key=?",
                      new String[] { context, key});
        }
    }
}