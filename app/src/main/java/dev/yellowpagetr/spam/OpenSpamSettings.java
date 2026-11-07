package dev.yellowpagetr.spam;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.security.KeyStore;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/**
 * Per-user opt-in for querying native Xiaomi YellowPage callers online.
 * No shared app-wide credential; the owner enters their own OpenSpam API key.
 * A key is never baked into the APK and is encrypted by Android Keystore.
 */
public final class OpenSpamSettings {
    private OpenSpamSettings() {}
    private static final String STORE="openspam_config_private_v1";
    private static final String ALIAS="yellowpage_tr_openspam_aes_v1";
    private static final String KEY_CIPHER="api_cipher";
    private static final String KEY_ENABLED="native_auto_enabled";

    private static SharedPreferences prefs(Context c) {
        return c.getApplicationContext().getSharedPreferences(STORE,Context.MODE_PRIVATE);
    }
    private static SecretKey secret() throws Exception {
        KeyStore ks=KeyStore.getInstance("AndroidKeyStore");
        ks.load(null);
        if(ks.containsAlias(ALIAS))
            return ((KeyStore.SecretKeyEntry)ks.getEntry(ALIAS,null)).getSecretKey();
        KeyGenerator gen=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        gen.init(new KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256).build());
        return gen.generateKey();
    }
    public static synchronized boolean saveKey(Context c,String key) {
        if(key==null || key.trim().isEmpty()) return false;
        try {
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE,secret());
            byte[] iv=cipher.getIV();
            byte[] encrypted=cipher.doFinal(key.trim().getBytes("UTF-8"));
            String value=Base64.encodeToString(iv,Base64.NO_WRAP)+":"+
                Base64.encodeToString(encrypted,Base64.NO_WRAP);
            return prefs(c).edit().putString(KEY_CIPHER,value).commit();
        } catch(Exception e) {return false;}
    }
    public static synchronized String getKey(Context c) {
        try {
            String value=prefs(c).getString(KEY_CIPHER,null);
            if(value==null) return null;
            String[] parts=value.split(":");
            if(parts.length!=2) return null;
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,secret(),
                new GCMParameterSpec(128,Base64.decode(parts[0],Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(parts[1],Base64.NO_WRAP)),"UTF-8");
        } catch(Exception e) {return null;}
    }
    public static boolean enabled(Context c) {
        return prefs(c).getBoolean(KEY_ENABLED,false) && getKey(c)!=null;
    }
    public static boolean setEnabled(Context c,boolean enabled) {
        if(enabled && getKey(c)==null) return false;
        return prefs(c).edit().putBoolean(KEY_ENABLED,enabled).commit();
    }
    public static void clear(Context c) {
        prefs(c).edit().clear().commit();
        try {
            KeyStore ks=KeyStore.getInstance("AndroidKeyStore");
            ks.load(null);
            if(ks.containsAlias(ALIAS))ks.deleteEntry(ALIAS);
        } catch(Exception ignored) {}
    }
}
