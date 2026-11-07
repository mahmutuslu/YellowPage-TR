package dev.yellowpagetr.spam;
import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
public final class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        int p=(int)(20*getResources().getDisplayMetrics().density);
        LinearLayout parent=new LinearLayout(this);
        parent.setPadding(p,p,p,p);
        parent.setOrientation(LinearLayout.VERTICAL);
        parent.setGravity(Gravity.TOP);
        parent.setBackgroundColor(Color.WHITE);
        TextView title=new TextView(this);
        title.setText("YellowPage TR · v0.6");
        title.setTextColor(Color.BLACK);
        title.setTextSize(22);
        parent.addView(title);
        TextView explanation=new TextView(this);
        explanation.setText("Deneysel LSPosed modülü. Yerel listede "+SpamDb.size()+" numara var. İnternet izni yoktur. Xiaomi arama ekranındaki uyarı henüz doğrulanmamıştır.");
        explanation.setTextColor(Color.DKGRAY);
        explanation.setTextSize(16);
        explanation.setPadding(0,p,0,p);
        parent.addView(explanation);
        EditText number=new EditText(this);
        number.setSingleLine(true);
        number.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
        number.setHint("Örn. +90 ...");
        parent.addView(number);
        TextView answer=new TextView(this);
        answer.setTextColor(Color.BLACK);
        answer.setTextSize(17);
        answer.setPadding(0,p,0,0);
        parent.addView(answer);
        Button test=new Button(this);
        test.setText("Yalnızca yerel listede kontrol et");
        test.setOnClickListener(v->{
            String raw=number.getText().toString();
            if(SpamDb.normalize(raw)==null) answer.setText("Geçerli bir Türkiye numarası girin.");
            else if(SpamDb.contains(raw)) answer.setText("Listede var: olası spam. Yanlış pozitif olabilir. Otomatik engelleme yok.");
            else answer.setText("Bu sınırlı listede yok; güvenilir olduğu anlamına gelmez.");
        });
        parent.addView(test);
        setContentView(parent);
    }
}
