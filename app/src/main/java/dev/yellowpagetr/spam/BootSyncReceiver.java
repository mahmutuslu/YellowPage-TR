package dev.yellowpagetr.spam;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Restore/check the periodic list sync after reboot or APK replacement.
 * No UI and no API credentials; the OS controls exact execution time.
 */
public final class BootSyncReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context,Intent intent) {
        if(context==null || intent==null)return;
        String action=intent.getAction();
        if(Intent.ACTION_BOOT_COMPLETED.equals(action) ||
           Intent.ACTION_MY_PACKAGE_REPLACED.equals(action)) {
            DailySpamSyncJob.schedule(context.getApplicationContext());
        }
    }
}
