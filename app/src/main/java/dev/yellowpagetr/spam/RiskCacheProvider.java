package dev.yellowpagetr.spam;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;

/**
 * Only same-device number membership. Native Xiaomi InCallUI and YellowPage
 * never perform Internet requests or receive/submit user credentials.
 * Incoming numbers are not stored or logged by this provider.
 */
public final class RiskCacheProvider extends ContentProvider {
    private static final String AUTHORITY="dev.yellowpagetr.spam.cache";
    @Override public boolean onCreate() {
        // Job creation is not done synchronously on the in-call thread.
        // This also initializes the sync without requiring the user to open the UI.
        new Thread(()->{
            try {DailySpamSyncJob.schedule(getContext());}
            catch(Throwable ignored) {}
        },"YellowPageTR-SyncSchedule").start();
        return true;
    }
    private boolean authorized() {
        int uid=Binder.getCallingUid();
        if(uid==Process.myUid()) return true;
        PackageManager pm=getContext().getPackageManager();
        String[] packages=pm.getPackagesForUid(uid);
        if(packages==null)return false;
        for(String pkg:packages)
            if("com.android.incallui".equals(pkg) ||
               "com.android.contacts".equals(pkg) ||
               "com.miui.yellowpage".equals(pkg)) return true;
        return false;
    }
    @Override public Bundle call(String method,String arg,Bundle extras) {
        if(!authorized())throw new SecurityException("Caller not authorized");
        Bundle out=new Bundle();
        if(!"hasLocalRisk".equals(method) || extras==null)return out;
        out.putBoolean("available",true);
        out.putBoolean("risk",DailyOfflineList.contains(getContext(),extras.getString("phone")));
        return out;
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,
                                  String[] selectionArgs,String sortOrder) {return null;}
    @Override public String getType(Uri uri) {return null;}
    @Override public Uri insert(Uri uri,ContentValues values) {return null;}
    @Override public int delete(Uri uri,String selection,String[] selectionArgs) {return 0;}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] selectionArgs) {return 0;}
}
