package dev.yellowpagetr.spam;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.TreeSet;

/**
 * LICENSED DAILY LIST: symbuzzer/Turkish-Spam-Numbers, GPL-3.0.
 * This is NOT an OpenSpam bulk export; OpenSpam does not permit
 * standard API keys to download a complete database.
 *
 * The result is read only from on-device storage during a call. Only
 * the JobService or explicit UI refresh contacts the public GitHub file.
 */
public final class DailyOfflineList {
    private DailyOfflineList() {}
    private static final String URL_STRING=
        "https://raw.githubusercontent.com/symbuzzer/Turkish-Spam-Numbers/main/SpamBlocker.csv";
    private static final String FILE="daily_tr_offline_list";
    private static final String DATA="list";
    private static final String TIME="updatedAt";
    private static final int MAX_BYTES=1024*1024;
    private static volatile long cachedTimestamp=-1L;
    private static volatile HashSet<String> cachedNumbers=null;

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE,Context.MODE_PRIVATE);
    }
    public static long updatedAt(Context c) {return prefs(c).getLong(TIME,0L);}
    public static int count(Context c) {return getSet(c).size();}
    public static boolean contains(Context c,String number) {
        String normalized=SpamDb.normalize(number);
        return normalized!=null && getSet(c).contains(normalized);
    }
    /** Null means no successful daily update yet: use the 201 bundled numbers. */
    private static synchronized HashSet<String> getSet(Context c) {
        SharedPreferences p=prefs(c);
        long timestamp=p.getLong(TIME,0L);
        if(timestamp>0 && cachedNumbers!=null && timestamp==cachedTimestamp)
            return cachedNumbers;
        HashSet<String> items=new HashSet<>();
        if(timestamp>0) {
            String raw=p.getString(DATA,"");
            for(String line:raw.split("\\n")) {
                String item=SpamDb.normalize(line.trim());
                if(item!=null)items.add(item);
            }
        }
        if(items.size()<50) {
            // Read the bundled CSV as a safe, durable fallback.
            try(InputStream input=c.getAssets().open("spam_tr.txt")) {
                ByteArrayOutputStream buffer=new ByteArrayOutputStream();
                byte[] chunk=new byte[2048];int n;
                while((n=input.read(chunk))!=-1 && buffer.size()<65536)
                    buffer.write(chunk,0,n);
                String raw=new String(buffer.toByteArray(),StandardCharsets.UTF_8);
                for(String line:raw.split("\\n")) {
                    String item=SpamDb.normalize(line.trim());
                    if(item!=null)items.add(item);
                }
            } catch(Exception ignored) {}
        }
        cachedNumbers=items;
        cachedTimestamp=timestamp;
        return items;
    }
    /** Network I/O MUST NEVER be invoked from the native Xiaomi caller process. */
    public static synchronized boolean refresh(Context c) {
        HttpURLConnection conn=null;
        try {
            conn=(HttpURLConnection)new URL(URL_STRING).openConnection();
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("Accept","text/plain,text/csv");
            if(conn.getResponseCode()!=200)return false;
            ByteArrayOutputStream data=new ByteArrayOutputStream();
            try(InputStream in=conn.getInputStream()) {
                byte[] block=new byte[4096];int n;
                while((n=in.read(block))!=-1) {
                    if(data.size()+n>MAX_BYTES)return false;
                    data.write(block,0,n);
                }
            }
            String body=new String(data.toByteArray(),StandardCharsets.UTF_8);
            String[] lines=body.split("\\r?\\n");
            if(lines.length<50 || !lines[0].replace("\\uFEFF","").trim().equals("number"))
                return false;
            TreeSet<String> normalized=new TreeSet<>();
            for(int i=1;i<lines.length;i++) {
                String raw=lines[i].trim();
                if(raw.isEmpty())continue;
                String number=SpamDb.normalize(raw);
                if(number==null)return false;
                normalized.add(number);
            }
            if(normalized.size()<50 || normalized.size()>50000)return false;
            StringBuilder text=new StringBuilder();
            for(String item:normalized)text.append(item).append('\\n');
            boolean ok=prefs(c).edit().putString(DATA,text.toString())
                .putLong(TIME,System.currentTimeMillis()).commit();
            if(ok) {
                cachedNumbers=new HashSet<>(normalized);
                cachedTimestamp=updatedAt(c);
            }
            return ok;
        } catch(Exception ignored) {
            return false;
        } finally {if(conn!=null)conn.disconnect();}
    }
}
