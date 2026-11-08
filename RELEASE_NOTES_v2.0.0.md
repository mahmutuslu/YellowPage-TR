# YellowPage TR v2.0.0 — Xposed API 102 final APK

**Android için modern libxposed (API 102) geçişi.** Önceki v1.3.0'un var olan spam listesi, otomatik JobScheduler güncellemesi, Xiaomi gelen çağrı ekranı etiketleme mantığı ve güvenli yerel `RiskCacheProvider` işlevleri korunur.

### Değişiklikler
- Eski `de.robv.android.xposed` arabirimi ve derleme stub'ları tamamen kaldırıldı.
- `io.github.libxposed:api:102.0.0`, `XposedModule.onPackageReady` ve kimlikli interceptor hook'ları kullanıldı.
- `META-INF/xposed/` altında API 102 bildirimi ve önerilen 3 uygulama kapsamı.
- 201 numaralık başlangıç Türkiye listesi, GPL-3.0 veri kaynağı, API anahtarsız günlük yenileme korundu.
- JUnit ve APK metadata kontrolü; loglarda telefon numarası yazılmaması ve arama sırasında ağ sorgusu yapılmaması korunur.
- Sürüm kodu 20, sürüm adı 2.0.0; uygulama kimliği değişmedi.

### Kurulum / önemli not
İndirilecek `YellowPage_TR_v2.0.0_API102.apk` dosyası **GitHub Actions debug anahtarıyla imzalıdır**, stable kullanıcı release sertifikasıyla değil. Önceki sürüm başka bir sertifikayla imzalandıysa doğrudan üzerine kurulamaz; verileri yedekleyip yalnız YellowPage TR modül APK'sını kaldırarak kurulumu gerçekleştir. Orijinal Xiaomi Telefon/YellowPage uygulamalarını kaldırma.

API 102 destekli bir Xposed framework gerekli. LSPosed kapsamına `com.android.contacts`, `com.android.incallui`, `com.miui.yellowpage` ekle ve telefonu yeniden başlat.

### Test sınırı
CI derlemesi, normalizasyon ve paket yapısı kontrol edilir. Xiaomi 15 Ultra/HyperOS 4 cihazında gerçek gelen spam aramasında `Olası spam (TR)` etiketinin görünmesi bu yayın için henüz canlı olarak doğrulanmamıştır. Sadece tam liste eşleşmelerini uyarır; çağrı engellemez. Bu sürümde **OpenSpam tam listesi yoktur**.
