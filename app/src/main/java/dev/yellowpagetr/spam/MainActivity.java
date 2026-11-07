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
        DailySpamSyncJob.schedule(this);
        // Online automatic lookups are suspended in favor of local daily feeds.
        OpenSpamSettings.setEnabled(this,false);
        int p=(int)(18*getResources().getDisplayMetrics().density);
        ScrollView scroll=new ScrollView(this);
        LinearLayout parent=new LinearLayout(this);
        parent.setPadding(p,p,p,p);
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setGravity(Gravity.TOP);
        parent.setBackgroundColor(Color.WHITE);
        scroll.addView(parent);

        TextView title=new TextView(this);
        title.setText("YellowPage TR · v1.2 — Xiaomi YellowPage");
        title.setTextColor(Color.BLACK);
        title.setTextSize(23);
        parent.addView(title);

        TextView explanation=new TextView(this);
        explanation.setText("Xiaomi'nin telefona yerleşik YellowPage ve gelen arama ekranına ek veri kaynağı. "
            +"Telefon yalnızca güncellenmiş çevrimdışı listeden uyarı üretir. "
            +"OpenSpam toplu liste için özel izin gerektirir; hiçbir gelen çağrı numarası internete gönderilmez.");
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
            else if(DailyOfflineList.contains(this,raw))
                answer.setText("Yerel listede var: olası spam. Yanlış pozitif olabilir; otomatik engelleme yok.");
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

        TextView dailyTitle=new TextView(this);
        dailyTitle.setText("Günlük Türkiye spam listesi");
        dailyTitle.setTextColor(Color.BLACK);
        dailyTitle.setTextSize(19);
        dailyTitle.setPadding(0,p,0,p/2);
        parent.addView(dailyTitle);

        TextView dailyInfo=new TextView(this);
        dailyInfo.setTextColor(Color.DKGRAY);
        dailyInfo.setTextSize(14);
        long last=DailyOfflineList.updatedAt(this);
        dailyInfo.setText(last==0 ?
            "İlk güncelleme planlandı; paket içindeki 201 numaralık liste kullanılacak." :
            "Son başarılı güncelleme: "+
            new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm",
                java.util.Locale.forLanguageTag("tr-TR")).format(new java.util.Date(last))+
            " · "+DailyOfflineList.count(this)+" numara");
        parent.addView(dailyInfo);

        TextView dailySource=new TextView(this);
        dailySource.setTextColor(Color.DKGRAY);
        dailySource.setTextSize(13);
        dailySource.setText("Kaynak: Turkish-Spam-Numbers (GPL-3.0), OpenSpam değil. "+
            "Yalnızca liste indirilir; arayan numarası gönderilmez. "+
            "OpenSpam tam liste erişimi onaylandığında ayrıca bağlanabilir.");
        parent.addView(dailySource);

        Button dailyButton=new Button(this);
        dailyButton.setText("Çevrimdışı listeyi şimdi güncelle");
        dailyButton.setOnClickListener(v->{
            dailyButton.setEnabled(false);
            dailyInfo.setText("Liste güvenli bağlantıdan güncelleniyor...");
            new Thread(()->{
                boolean ok=DailyOfflineList.refresh(this);
                runOnUiThread(()->{
                    dailyButton.setEnabled(true);
                    if(!ok) dailyInfo.setText("Güncelleme olmadı; mevcut liste korunuyor.");
                    else dailyInfo.setText("Liste yenilendi: "+
                        DailyOfflineList.count(this)+" numara · "+
                        new java.text.SimpleDateFormat("dd.MM.yyyy HH:mm",
                            java.util.Locale.forLanguageTag("tr-TR"))
                            .format(new java.util.Date(DailyOfflineList.updatedAt(this))));
                });
            },"DailyOfflineListManual").start();
        });
        parent.addView(dailyButton);

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
                            +"Bu sorgu yalnızca 24 saatlik önbelleğe kaydedilir; tam veritabanı değildir. "
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
            +"Arama otomatik engellenmez. Gelen aramalarda OpenSpam'e sorgu gönderilmez. "+
            "OpenSpam tam liste senkronu için ayrıca yetkili veri erişimi gerekir.");
        footer.setTextColor(Color.DKGRAY);
        footer.setTextSize(13);
        parent.addView(footer);
        setContentView(scroll);
    }
}
