package dev.yellowpagetr.spam;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Process;
import android.os.ResultReceiver;

/**
 * Read-only local lookup IPC for Xiaomi's scoped caller UI.
 * No HTTP requests, API keys, call logs or raw-number storage.
 * The caller's UID must belong to Xiaomi InCallUI / Contacts or to us.
 */
public final class RiskCacheProvider extends ContentProvider {
    public static final Uri URI=Uri.parse("content://dev.yellowpagetr.spam.cache/risk");
    @Override public boolean onCreate() { return true; }

    private boolean authorized() {
        int uid=Binder.getCallingUid();
        if(uid==Process.myUid()) return true;
        PackageManager pm=getContext().getPackageManager();
        String[] packages=pm.getPackagesForUid(uid);
        if(packages==null) return false;
        for(String pkg:packages)
            if("com.android.incallui".equals(pkg) ||
               "com.android.contacts".equals(pkg) ||
               "com.miui.yellowpage".equals(pkg)) return true;
        return false;
    }
    @Override public Bundle call(String method,String arg,Bundle extras) {
        if(!authorized()) throw new SecurityException("Not a permitted Xiaomi caller");
        Bundle out=new Bundle();
        if(extras==null) return out;
        if("hasLocalRisk".equals(method)) {
            out.putBoolean("available",true);
            out.putBoolean("risk",DailyOfflineList.contains(getContext(),extras.getString("phone")));
            return out;
        }
        if(!"hasRisk".equals(method) && !"checkAndFetch".equals(method)) return out;
        String phone=extras.getString("phone");
        out.putBoolean("risk",OpenSpamCache.hasRisk(getContext(),phone));
        out.putBoolean("fresh",OpenSpamCache.hasFreshResult(getContext(),phone));
        if("checkAndFetch".equals(method) && !out.getBoolean("fresh",false)) {
            // This is asynchronous. Never wait for the API on an incoming call.
            ResultReceiver callback=null;
            try {callback=extras.getParcelable("callback");}
            catch(Throwable ignored) {}
            OpenSpamAutoLookup.schedule(getContext(),phone,callback);
        }
        return out;
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,
                                  String[] selectionArgs,String sortOrder) {return null;}
    @Override public String getType(Uri uri) {return null;}
    @Override public Uri insert(Uri uri,ContentValues values) {return null;}
    @Override public int delete(Uri uri,String selection,String[] selectionArgs) {return 0;}
    @Override public int update(Uri uri,ContentValues values,String selection,String[] selectionArgs) {return 0;}
}
