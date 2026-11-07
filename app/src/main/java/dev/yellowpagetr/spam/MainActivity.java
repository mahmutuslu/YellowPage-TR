package dev.yellowpagetr.spam;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ScrollView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Install-and-forget status screen. All incoming call labels are displayed
 * by Xiaomi's original Phone/YellowPage UI through LSPosed.
 * This screen is never required to activate the daily updater.
 */
public final class MainActivity extends Activity {
    private TextView status;
    private TextView count;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        DailySpamSyncJob.schedule(this);
        int padding=(int)(20*getResources().getDisplayMetrics().density);
        ScrollView scroll=new ScrollView(this);
        LinearLayout parent=new LinearLayout(this);
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setPadding(padding,padding,padding,padding);
        parent.setBackgroundColor(Color.WHITE);
        scroll.addView(parent);
        TextView heading=new TextView(this);
        heading.setText("YellowPage TR · v1.3");
        heading.setTextColor(Color.BLACK);
        heading.setTextSize(24);
        parent.addView(heading);

        TextView explanation=new TextView(this);
        explanation.setText("Xiaomi'nin orijinal YellowPage ve gelen arama ekranı için "
            +"Türkçe spam uyarıları. Kurulumdan sonra günlük numara listesi "
            +"arka planda otomatik yenilenir. API anahtarı, hesap veya "
            +"numara girişi gerekmez.");
        explanation.setTextSize(16);
        explanation.setTextColor(Color.DKGRAY);
        explanation.setPadding(0,padding,0,padding);
        parent.addView(explanation);

        status=new TextView(this);
        status.setTextColor(Color.BLACK);
        status.setTextSize(17);
        parent.addView(status);

        count=new TextView(this);
        count.setTextColor(Color.DKGRAY);
        count.setTextSize(15);
        count.setPadding(0,padding/2,0,padding/2);
        parent.addView(count);

        TextView details=new TextView(this);
        details.setText("Veri kaynağı: Turkish-Spam-Numbers (GPL-3.0). "
            +"Güncelleme yaklaşık 24 saatte bir, Android'in uygun gördüğü "
            +"saatte yapılır. İndirilen liste doğrulanmadan kullanılmaz; "
            +"başarısız güncellemede mevcut liste korunur. "
            +"Arayan telefon numarası internete gönderilmez. "
            +"Kişi adları değiştirilmez, aramalar engellenmez. "
            +"Bu liste Google Telefon'un kapsamlı spam veritabanıyla aynı değildir.");
        details.setTextColor(Color.DKGRAY);
        details.setTextSize(14);
        details.setPadding(0,padding,0,padding);
        parent.addView(details);

        Button refresh=new Button(this);
        refresh.setText("İsteğe bağlı: listeyi şimdi yenile");
        refresh.setOnClickListener(v->{
            refresh.setEnabled(false);
            status.setText("Güncelleme deneniyor...");
            new Thread(()->{
                boolean ok=DailyOfflineList.refresh(getApplicationContext());
                runOnUiThread(()->{
                    refresh.setEnabled(true);
                    updateStatus();
                    if(!ok) status.append("\nSon deneme başarısız: önceki liste korundu.");
                });
            },"YellowPageManualRefresh").start();
        });
        parent.addView(refresh);

        TextView scope=new TextView(this);
        scope.setText("LSPosed önerilen kapsam: com.android.contacts, "
            +"com.android.incallui ve com.miui.yellowpage. "
            +"Yalnızca önerilen uygulamaların seçili olması yeterlidir.");
        scope.setTextColor(Color.DKGRAY);
        scope.setTextSize(13);
        scope.setPadding(0,padding,0,padding);
        parent.addView(scope);
        setContentView(scroll);
        updateStatus();
    }
    @Override protected void onResume() {
        super.onResume();
        updateStatus();
    }
    private void updateStatus() {
        if(status==null || count==null)return;
        long last=DailyOfflineList.updatedAt(this);
        if(last<=0) {
            status.setText("Otomatik güncelleme planlandı. İlk indirme bekleniyor.");
        } else {
            String stamp=new SimpleDateFormat("dd.MM.yyyy HH:mm",
                Locale.forLanguageTag("tr-TR")).format(new Date(last));
            status.setText("Son başarılı liste güncellemesi: "+stamp);
        }
        count.setText("Telefondaki spam listesi: "+DailyOfflineList.count(this)+
            " numara"+(last==0?" (paket içi başlangıç listesi)":""));
    }
}
