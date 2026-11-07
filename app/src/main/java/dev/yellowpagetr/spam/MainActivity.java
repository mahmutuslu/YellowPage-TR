package dev.yellowpagetr.spam;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.graphics.Color;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

/** Local-list test + explicit, private OpenSpam API lookups. */
public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int p=(int)(18*getResources().getDisplayMetrics().density);
        ScrollView scroll=new ScrollView(this);
        LinearLayout parent=new LinearLayout(this);
        parent.setPadding(p,p,p,p);
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setGravity(Gravity.TOP);
        parent.setBackgroundColor(Color.WHITE);
        scroll.addView(parent);

        TextView title=new TextView(this);
        title.setText("YellowPage TR · v1.1 — Xiaomi ayarları");
        title.setTextColor(Color.BLACK);
        title.setTextSize(23);
        parent.addView(title);

        TextView explanation=new TextView(this);
        explanation.setText("Xiaomi'nin telefona yerleşik YellowPage ve gelen arama ekranına ek veri kaynağı. "
            +"Yerel listede "+SpamDb.size()+" şüpheli numara var. "
            +"OpenSpam otomatik sorgu varsayılan olarak kapalıdır. Sadece siz etkinleştirirseniz gelen çağrı numaraları internete gönderilir.");
        explanation.setTextColor(Color.DKGRAY);
        explanation.setTextSize(15);
        explanation.setPadding(0,p,0,p);
        parent.addView(explanation);

        EditText number=new EditText(this);
        number.setSingleLine(true);
        number.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        number.setHint("Türkiye numarası: +90 ...");
        parent.addView(number);

        TextView answer=new TextView(this);
        answer.setTextColor(Color.BLACK);
        answer.setTextSize(16);
        answer.setPadding(0,p/2,0,p/2);
        parent.addView(answer);

        Button test=new Button(this);
        test.setText("Yerel listede kontrol et (çevrimdışı)");
        test.setOnClickListener(v->{
            String raw=number.getText().toString();
            if(SpamDb.normalize(raw)==null) answer.setText("Geçerli Türkiye numarası girin.");
            else if(SpamDb.contains(raw))
                answer.setText("Yerel listede var: olası spam. Yanlış pozitif olabilir; otomatik engelleme yok.");
            else if(OpenSpamCache.hasRisk(this,raw))
                answer.setText("OpenSpam önbelleğinde en az 3 rapor var (son 24 saat).");
            else answer.setText("Yerel listede yok. Güvenilir olduğu anlamına gelmez.");
        });
        parent.addView(test);

        TextView section=new TextView(this);
        section.setText("OpenSpam — isteğe bağlı çevrimiçi kontrol");
        section.setTextColor(Color.BLACK);
        section.setTextSize(19);
        section.setPadding(0,p,0,p/2);
        parent.addView(section);

        TextView privacy=new TextView(this);
        privacy.setText("OpenSpam API anahtarı gerekir. Yalnızca aşağıdaki düğmeye basarsanız "
            +"girdiğiniz telefon numarası ve API anahtarı HTTPS ile OpenSpam'e gönderilir. "
            +"Servis bu sorguları kaydedebilir. API anahtarı cihazda saklanmaz. "
            +"Ücretsiz API sadece ticari olmayan kullanımlar içindir.");
        privacy.setTextColor(Color.DKGRAY);
        privacy.setTextSize(14);
        parent.addView(privacy);

        Button panel=new Button(this);
        panel.setText("OpenSpam API anahtarı edin");
        panel.setOnClickListener(v->{
            Intent browse=new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://api.openspam.es/panel"));
            startActivity(browse);
        });
        parent.addView(panel);

        EditText apiKey=new EditText(this);
        apiKey.setSingleLine(true);
        apiKey.setInputType(android.text.InputType.TYPE_CLASS_TEXT |
            android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        apiKey.setHint("OpenSpam API anahtarı");
        parent.addView(apiKey);

        TextView nativeSection=new TextView(this);
        nativeSection.setText("Yerleşik Xiaomi YellowPage — otomatik OpenSpam");
        nativeSection.setTextColor(Color.BLACK);
        nativeSection.setTextSize(19);
        nativeSection.setPadding(0,p,0,p/2);
        parent.addView(nativeSection);

        TextView nativeInfo=new TextView(this);
        nativeInfo.setText("AÇARSANIZ: Xiaomi YellowPage gelen çağrı sorgularında Türkiye "
            +"numaraları OpenSpam API'sine gönderilebilir. Servis numara ve IP "
            +"bilgilerini kaydedebilir. Telefon aramalarınız engellenmez; "
            +"orijinal Xiaomi uygulaması ve kişi adları korunur. "
            +"Numara sorgusu ağda yapılırken arama ekranı bekletilmez. "
            +"Sorgu sonucu arama bitmeden yetişmezse sonraki aramada kullanılır. "
            +"API anahtarı cihazda Android Keystore ile şifreli saklanır.");
        nativeInfo.setTextColor(Color.DKGRAY);
        nativeInfo.setTextSize(14);
        parent.addView(nativeInfo);

        CheckBox nativeConsent=new CheckBox(this);
        nativeConsent.setText("Gelen çağrı numaralarını OpenSpam'e otomatik göndermeyi kabul ediyorum.");
        nativeConsent.setTextColor(Color.BLACK);
        nativeConsent.setChecked(OpenSpamSettings.enabled(this));
        parent.addView(nativeConsent);

        TextView nativeStatus=new TextView(this);
        nativeStatus.setTextColor(Color.DKGRAY);
        nativeStatus.setText(OpenSpamSettings.enabled(this) ?
            "Otomatik Xiaomi YellowPage sorgusu açık" :
            "Otomatik Xiaomi YellowPage sorgusu kapalı (varsayılan)");
        parent.addView(nativeStatus);

        Button saveNative=new Button(this);
        saveNative.setText("Xiaomi YellowPage entegrasyon ayarını kaydet");
        saveNative.setOnClickListener(v->{
            if(!nativeConsent.isChecked()) {
                OpenSpamSettings.setEnabled(this,false);
                nativeStatus.setText("Otomatik sorgu kapatıldı; gelen çağrılarda internet kullanılmaz.");
                return;
            }
            String typed=apiKey.getText().toString().trim();
            if(!typed.isEmpty() && !OpenSpamSettings.saveKey(this,typed)) {
                nativeStatus.setText("API anahtarı şifrelenemedi, otomatik sorgu açılmadı.");
                return;
            }
            if(OpenSpamSettings.setEnabled(this,true))
                nativeStatus.setText("Otomatik sorgu açıldı. Xiaomi YellowPage artık istenirse "
                    +"arka planda OpenSpam kontrolü başlatabilir.");
            else
                nativeStatus.setText("Önce yukarıya geçerli OpenSpam API anahtarı gir.");
        });
        parent.addView(saveNative);

        Button removeNative=new Button(this);
        removeNative.setText("Otomatik entegrasyonu kapat ve kaydedilen API anahtarını sil");
        removeNative.setOnClickListener(v->{
            OpenSpamSettings.clear(this);
            nativeConsent.setChecked(false);
            apiKey.setText("");
            nativeStatus.setText("Otomatik sorgu kapalı; şifreli anahtar silindi.");
        });
        parent.addView(removeNative);

        CheckBox consent=new CheckBox(this);
        consent.setText("Bu numarayı OpenSpam'e göndermeyi kabul ediyorum.");
        consent.setTextColor(Color.BLACK);
        parent.addView(consent);

        Button online=new Button(this);
        online.setText("OpenSpam'de numarayı sorgula");
        parent.addView(online);

        TextView onlineResult=new TextView(this);
        onlineResult.setTextColor(Color.BLACK);
        onlineResult.setTextSize(15);
        onlineResult.setPadding(0,p/2,0,p);
        parent.addView(onlineResult);

        online.setOnClickListener(v->{
            String raw=number.getText().toString();
            String normalized=SpamDb.normalize(raw);
            String key=apiKey.getText().toString().trim();
            if(normalized==null) {
                onlineResult.setText("Önce geçerli Türkiye numarası gir.");
                return;
            }
            if(key.isEmpty()) {
                onlineResult.setText("Önce OpenSpam API anahtarı gir.");
                return;
            }
            if(!consent.isChecked()) {
                onlineResult.setText("Sorgulamak için veri paylaşımını onayla.");
                return;
            }
            long now=System.currentTimeMillis();
            android.content.SharedPreferences rate=getSharedPreferences("openspam_rate_v1",MODE_PRIVATE);
            long previous=rate.getLong("previous",0L);
            if(now-previous<2500L) {
                onlineResult.setText("Lütfen sorgular arasında en az 2,5 saniye bırak.");
                return;
            }
            rate.edit().putLong("previous",now).apply();
            online.setEnabled(false);
            onlineResult.setText("OpenSpam sorgulanıyor...");
            new Thread(()->{
                final OpenSpamClient.Result result=OpenSpamClient.lookup(this,normalized,key);
                runOnUiThread(()->{
                    online.setEnabled(true);
                    if(!result.success) {
                        onlineResult.setText(result.message);
                    } else if(result.reports>=3) {
                        onlineResult.setText("OpenSpam: "+result.reports+" bildirim. "
                            +"Tür: "+result.category+". Düzey: "+result.danger+". "
                            +"Bu kayıt 24 saat boyunca yerel arama etiketi için kullanılabilir. "
                            +"Yanlış pozitif olabilir.");
                    } else if(result.reports>0) {
                        onlineResult.setText("OpenSpam: "+result.reports+
                            " bildirim. Uyarı eşiği 3 olduğundan gelen arama etiketi eklenmez.");
                    } else {
                        onlineResult.setText("OpenSpam'de bu numaraya ilişkin bildirim bulunamadı.");
                    }
                });
            },"OpenSpamManualLookup").start();
        });

        TextView footer=new TextView(this);
        footer.setText("LSPosed önerilen kapsam: Xiaomi Telefon/Kişiler, Gelen Arama ve YellowPage. "
            +"Arama otomatik engellenmez. Otomatik çevrimiçi sorgu yalnızca yukarıdaki açık rızayla etkinleşir.");
        footer.setTextColor(Color.DKGRAY);
        footer.setTextSize(13);
        parent.addView(footer);
        setContentView(scroll);
    }
}
