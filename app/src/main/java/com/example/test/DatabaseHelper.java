package com.example.test;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "app_database.db";
    private static final int DB_VERSION = 1;

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE user_session (id INTEGER PRIMARY KEY, username TEXT, email TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int old, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS user_session");
        onCreate(db);
    }

    // Сохранение данных пользователя
    public void saveUser(long id, String username, String email) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id", id);
        values.put("username", username);
        values.put("email", email);
        db.insertWithOnConflict("user_session", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    // Проверка, есть ли активная сессия
    public boolean isUserLoggedIn() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM user_session", null);
        boolean loggedIn = cursor.getCount() > 0;
        cursor.close();
        return loggedIn;
    }
}
