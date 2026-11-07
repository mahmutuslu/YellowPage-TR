package dev.yellowpagetr.spam;

import android.content.Context;
import org.json.JSONObject;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;

/** Opt-in, foreground-user-initiated HTTPS lookup. Never called from incoming calls. */
public final class OpenSpamClient {
    private OpenSpamClient() {}
    public static final class Result {
        public final int reports;
        public final String category;
        public final String danger;
        public final String message;
        public final boolean success;
        public Result(boolean ok,int count,String type,String level,String msg) {
            success=ok; reports=count; category=type; danger=level; message=msg;
        }
    }
    private static String safeLabel(String s) {
        if(s==null) return "";
        String clean=s.replaceAll("[^\\p{L}\\p{N} _-]","").trim();
        return clean.length()>40?clean.substring(0,40):clean;
    }
    private static String readLimited(InputStream in) throws Exception {
        if(in==null) return "";
        try(InputStream stream=in;ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[2048]; int n;
            while((n=stream.read(buffer))!=-1) {
                if(bytes.size()+n>32768) throw new Exception("Yanıt çok uzun");
                bytes.write(buffer,0,n);
            }
            return new String(bytes.toByteArray(),StandardCharsets.UTF_8);
        }
    }
    public static Result lookup(Context context,String raw,String apiKey) {
        String number=SpamDb.normalize(raw);
        if(number==null) return new Result(false,0,"","","Türkiye numarası geçersiz");
        if(apiKey==null || apiKey.trim().isEmpty())
            return new Result(false,0,"","","OpenSpam API anahtarı gerekli");
        HttpURLConnection conn=null;
        try {
            // Phone numbers are sent ONLY when the user presses the online lookup button.
            URL url=new URL("https://api.openspam.es/api/number/"+number);
            conn=(HttpURLConnection)url.openConnection();
            conn.setConnectTimeout(4500);
            conn.setReadTimeout(4500);
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("X-API-Key",apiKey.trim());
            conn.setRequestProperty("Accept","application/json");
            int status=conn.getResponseCode();
            if(status==404) {
                OpenSpamCache.put(context,number,0);
                return new Result(true,0,"","","OpenSpam'de kayıt bulunamadı");
            }
            if(status==401 || status==403)
                return new Result(false,0,"","","API anahtarı reddedildi veya yetki yok");
            if(status==429)
                return new Result(false,0,"","","OpenSpam sorgu sınırına ulaşıldı; daha sonra dene");
            if(status!=200)
                return new Result(false,0,"","","OpenSpam HTTP "+status);
            JSONObject response=new JSONObject(readLimited(conn.getInputStream()));
            if(!response.optBoolean("success",false))
                return new Result(false,0,"","","OpenSpam sorgusu başarısız");
            JSONObject data=response.optJSONObject("data");
            if(data==null) {
                OpenSpamCache.put(context,number,0);
                return new Result(true,0,"","","OpenSpam'de kayıt bulunamadı");
            }
            String returned=SpamDb.normalize(data.optString("telefono",""));
            if(returned==null || !number.equals(returned))
                return new Result(false,0,"","","OpenSpam yanıtındaki numara uyuşmuyor");
            int reports=Math.max(0,data.optInt("reportes",0));
            String category=safeLabel(data.optString("tipo",""));
            String danger=safeLabel(data.optString("nivel_peligro",""));
            OpenSpamCache.put(context,number,reports);
            return new Result(true,reports,category,danger,"Sorgu tamamlandı");
        } catch(Exception error) {
            // Never expose URLs containing numbers, API keys, or exception messages.
            return new Result(false,0,"","","OpenSpam bağlantı veya yanıt hatası");
        } finally {
            if(conn!=null) conn.disconnect();
        }
    }
}
