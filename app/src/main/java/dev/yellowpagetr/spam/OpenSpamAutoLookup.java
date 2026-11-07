package dev.yellowpagetr.spam;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.ResultReceiver;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Background-only fetch started by Xiaomi's own native YellowPage / call card.
 * No network activity unless the owner enabled automatic lookup.
 * One queue, number-level de-duplication, provider-specific rate limits,
 * and at most 180 requests/hour (below OpenSpam's published 200 quota).
 */
public final class OpenSpamAutoLookup {
    private OpenSpamAutoLookup() {}
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor(r->{
        Thread t=new Thread(r,"OpenSpamNativeYellowPage");
        t.setDaemon(true);return t;
    });
    private static final ConcurrentHashMap<String,CopyOnWriteArrayList<ResultReceiver>>
        WAITING=new ConcurrentHashMap<>();
    private static final Object RATE_LOCK=new Object();
    private static final String RATES="openspam_auto_rate_v1";

    public static void schedule(Context app,String raw,ResultReceiver receiver) {
        String normalized=SpamDb.normalize(raw);
        if(normalized==null) return;
        Context context=app.getApplicationContext();
        if(!OpenSpamSettings.enabled(context)) return;
        if(OpenSpamCache.hasFreshResult(context,normalized)) {
            send(receiver,OpenSpamCache.hasRisk(context,normalized),true);
            return;
        }
        CopyOnWriteArrayList<ResultReceiver> list=new CopyOnWriteArrayList<>();
        if(receiver!=null) list.add(receiver);
        CopyOnWriteArrayList<ResultReceiver> already=WAITING.putIfAbsent(normalized,list);
        if(already!=null) {
            if(receiver!=null && already.size()<10) already.add(receiver);
            return;
        }
        WORK.execute(()->{
            boolean success=false;
            try {
                if(!OpenSpamSettings.enabled(context)) return;
                if(!reserveRate(context)) return;
                String key=OpenSpamSettings.getKey(context);
                if(key==null) return;
                OpenSpamClient.Result result=OpenSpamClient.lookup(context,normalized,key);
                success=result.success;
            } finally {
                CopyOnWriteArrayList<ResultReceiver> recipients=WAITING.remove(normalized);
                if(recipients!=null)
                    for(ResultReceiver target:recipients)
                        send(target,OpenSpamCache.hasRisk(context,normalized),success);
            }
        });
    }
    private static void send(ResultReceiver receiver,boolean risk,boolean finished) {
        if(receiver==null)return;
        try {
            Bundle data=new Bundle();
            data.putBoolean("risk",risk);
            data.putBoolean("checked",finished);
            receiver.send(0,data);
        } catch(Throwable ignored) {}
    }
    private static boolean reserveRate(Context c) {
        synchronized(RATE_LOCK) {
            SharedPreferences p=c.getSharedPreferences(RATES,Context.MODE_PRIVATE);
            long now=System.currentTimeMillis();
            long start=p.getLong("hourStart",0L);
            int count=p.getInt("hourCount",0);
            if(start<=0 || now<start || now-start>=3600000L) {start=now;count=0;}
            long last=p.getLong("lastRequest",0L);
            if(count>=180) return false;
            long remaining=2500L-(now-last);
            if(remaining>0 && remaining<2500L) {
                try {Thread.sleep(remaining);} catch(InterruptedException e) {
                    Thread.currentThread().interrupt();return false;
                }
                now=System.currentTimeMillis();
            }
            return p.edit().putLong("lastRequest",now)
                .putLong("hourStart",start).putInt("hourCount",count+1).commit();
        }
    }
}
