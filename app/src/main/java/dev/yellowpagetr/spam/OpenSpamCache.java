package dev.yellowpagetr.spam;

import android.content.Context;
import android.content.SharedPreferences;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;

/**
 * Private, limited 24h cache for explicitly requested OpenSpam queries.
 * A SHA-256 of the normalized phone is the key; raw numbers and API keys
 * are NOT stored. A cached positive must have >=3 reports.
 */
public final class OpenSpamCache {
    private OpenSpamCache() {}
    private static final long TTL=24L*60L*60L*1000L;
    private static final String FILE="openspam_manual_cache_v1";

    private static String hash(String raw) {
        String normalized=SpamDb.normalize(raw);
        if(normalized==null) return null;
        try {
            MessageDigest md=MessageDigest.getInstance("SHA-256");
            byte[] bytes=md.digest(normalized.getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder(bytes.length*2);
            for(byte b:bytes) out.append(String.format(Locale.ROOT,"%02x",b & 255));
            return out.toString();
        } catch(Exception error) {return null;}
    }
    public static void put(Context context,String raw,int reports) {
        String key=hash(raw);
        if(key==null || reports<0) return;
        long now=System.currentTimeMillis();
        context.getSharedPreferences(FILE,Context.MODE_PRIVATE).edit()
            .putString(key,now+"|"+Math.min(reports,999999)).apply();
    }
    public static boolean hasRisk(Context context,String raw) {
        String key=hash(raw);
        if(key==null) return false;
        SharedPreferences p=context.getSharedPreferences(FILE,Context.MODE_PRIVATE);
        String entry=p.getString(key,"");
        String[] parts=entry.split("\\|");
        if(parts.length!=2) return false;
        try {
            long timestamp=Long.parseLong(parts[0]);
            int reports=Integer.parseInt(parts[1]);
            long elapsed=System.currentTimeMillis()-timestamp;
            if(elapsed<0 || elapsed>=TTL) return false;
            return reports>=3;
        } catch(Exception ignored) {return false;}
    }
}
