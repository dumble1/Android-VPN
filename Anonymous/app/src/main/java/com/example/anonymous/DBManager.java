package com.example.anonymous;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by 신승수 on 2016-07-11.
 */
public class DBManager extends SQLiteOpenHelper {
    public DBManager(Context context,String name,CursorFactory factory,int version) {
        super(context,name,factory,version);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE PACKET_LIST( _id INTEGER PRIMARY KEY AUTOINCREMENT, packet TEXT);");
    }           //build PACKET_LIST table.
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
    }

    public void insert(String _query){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(_query);
        db.close();
    }

    public void update(String _query){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(_query);
        db.close();
    }
    public void delete(String _query){
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(_query);
        db.close();
    }
    public String get(String _query){
        SQLiteDatabase db = getReadableDatabase();
        String str = "";

        Cursor cursor = db.rawQuery("select * from PACKET_LIST", null);
        while(cursor.moveToNext()){
            str += cursor.getString(1) + "\n";
        }

        return str;
    }
}
