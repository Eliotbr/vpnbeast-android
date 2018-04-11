package com.b.android.openvpn60.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.widget.TabHost;

import com.b.android.openvpn60.constant.AppConstants;
import com.b.android.openvpn60.model.VpnProfile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;

public class DbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "VpnBeast.db";
    public static final String TABLE_NAME = "vpn_profiles";
    public static final String TABLE_ID = "id";
    public static final String SERVER_UUID = "server_uuid";
    public static final String SERVER_NAME = "server_name";
    public static final String SERVER_IP = "server_ip";
    public static final String SERVER_PORT = "server_port";
    public static final String SERVER_CERT = "server_cert";
    private LogHelper logHelper;



    public DbHelper(Context context) {
        super(context, DATABASE_NAME , null, 1);
        logHelper = LogHelper.getLogHelper(DbHelper.class.getName());
        //deleteAllProfiles();
    }


    @Override
    public void onCreate(SQLiteDatabase db) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        db.execSQL("create table vpn_profiles" + "(id integer primary key, server_uuid text, server_name text, " +
                "server_ip text, server_port text, server_cert text)");
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }


    public boolean insertProfile (String serverUuid, String serverName, String serverIp, String serverPort, String serverCert) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(SERVER_UUID, serverUuid);
        contentValues.put(SERVER_NAME, serverName);
        contentValues.put(SERVER_IP, serverIp);
        contentValues.put(SERVER_PORT, serverPort);
        contentValues.put(SERVER_CERT, serverCert);
        db.insert(TABLE_NAME, null, contentValues);
        return true;
    }


    public Cursor getData(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from contacts where id=" + id + "", null);
        return res;
    }


    public int numberOfRows() {
        SQLiteDatabase db = this.getReadableDatabase();
        int numRows = (int) DatabaseUtils.queryNumEntries(db, TABLE_NAME);
        return numRows;
    }


    public boolean updateProfile (Integer id, String serverUuid, String serverName, String serverIp, String serverPort,
                                  String serverCert) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(SERVER_UUID, serverUuid);
        contentValues.put(SERVER_NAME, serverName);
        contentValues.put(SERVER_IP, serverIp);
        contentValues.put(SERVER_PORT, serverPort);
        contentValues.put(SERVER_CERT, serverCert);
        db.update(TABLE_NAME, contentValues, "id = ? ", new String[] { Integer.toString(id) } );
        return true;
    }


    public Integer deleteProfile (Integer id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, "id = ? ", new String[] { Integer.toString(id) });
    }


    public Integer deleteAllProfiles() {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, null, null);
    }


    public ArrayList<VpnProfile> getAllProfiles() {
        ArrayList<VpnProfile> profileList = new ArrayList<>();
        //hp = new HashMap();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res =  db.rawQuery( "select * from " + TABLE_NAME, null );
        res.moveToFirst();
        try {
            while(!res.isAfterLast()) {
                String serverUuid = res.getString(res.getColumnIndex(SERVER_UUID));
                String serverName = res.getString(res.getColumnIndex(SERVER_NAME));
                String serverIp = res.getString(res.getColumnIndex(SERVER_IP));
                String serverPort = res.getString(res.getColumnIndex(SERVER_PORT));
                String serverCert = res.getString(res.getColumnIndex(SERVER_CERT));
                VpnProfile tempProfile = new VpnProfile(serverUuid, serverName, serverIp, serverPort, serverCert);
                profileList.add(tempProfile);
                res.moveToNext();
            }
        } catch (Exception exception) {
            logHelper.logException(exception);
        } finally {
            res.close();
        }
        return profileList;
    }


    public boolean insertProfileList(ArrayList<VpnProfile> profileList) {
        if (profileList.size() > numberOfRows()) {
            for (VpnProfile profile : profileList) {
                if (!checkIfDataExist(profile.getName())) {
                    insertProfile(profile.getUUIDString(), profile.getName(), profile.connections[0].serverName,
                            profile.connections[0].serverPort, profile.getServerCert());
                    logHelper.logInfo("INSERTED PROFILE INFOS = ");
                    logHelper.logInfo("uuid = " + profile.getUUIDString());
                    logHelper.logInfo("serverName = " + profile.getName());
                    logHelper.logInfo("serverIp = " + profile.connections[0].serverName);
                    logHelper.logInfo("serverPort = " + profile.connections[0].serverPort);
                } else
                    logHelper.logInfo("PROFILE ALREADY EXISTS, SKIPPING...");
            }
            logHelper.logInfo("ALL PROFILE INFOS INSERTED TO SQLITE!");
            logHelper.logInfo("numberOfRows = " + String.valueOf(numberOfRows()));
            return true;
        } else {
            logHelper.logInfo("NOTHING TO INSERT!");
            logHelper.logInfo("numberOfRows = " + String.valueOf(numberOfRows()));
        }
        return false;
    }


    private boolean checkIfDataExist(String fieldValue) {
        SQLiteDatabase db = this.getReadableDatabase();
        String selectString = "select * from " + TABLE_NAME + " where " + SERVER_NAME + " = ?";
        Cursor cursor = db.rawQuery(selectString, new String[] {fieldValue});
        if(cursor.getCount() <= 0){
            cursor.close();
            return false;
        }
        cursor.close();
        return true;
    }

}
