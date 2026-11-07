package dev.yellowpagetr.spam;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;

/** Automatically refresh the GPL-3.0 Turkish spam list around once a day. */
public final class DailySpamSyncJob extends JobService {
    private static final int PERIODIC_ID=62041;
    private static final int IMMEDIATE_ID=62042;
    private static final long PERIOD=24L*60L*60L*1000L;

    public static void schedule(Context c) {
        JobScheduler scheduler=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(scheduler==null)return;
        if(scheduler.getPendingJob(PERIODIC_ID)==null) {
            JobInfo periodic=new JobInfo.Builder(PERIODIC_ID,
                new ComponentName(c,DailySpamSyncJob.class))
                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                .setPeriodic(PERIOD,2L*60L*60L*1000L)
                .setPersisted(true)
                .build();
            scheduler.schedule(periodic);
        }
        // First launch: fetch once now rather than waiting for the first periodic slot.
        if(DailyOfflineList.updatedAt(c)==0L) requestNow(c);
    }
    public static void requestNow(Context c) {
        JobScheduler scheduler=(JobScheduler)c.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if(scheduler==null)return;
        JobInfo job=new JobInfo.Builder(IMMEDIATE_ID,
            new ComponentName(c,DailySpamSyncJob.class))
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .setOverrideDeadline(1000L)
            .build();
        scheduler.schedule(job);
    }
    @Override public boolean onStartJob(JobParameters params) {
        new Thread(()->{
            DailyOfflineList.refresh(getApplicationContext());
            jobFinished(params,false);
        },"YellowPageDailyOfflineSync").start();
        return true;
    }
    @Override public boolean onStopJob(JobParameters params) {
        return true;
    }
}
