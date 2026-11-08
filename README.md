# YellowPage TR v2.0.0 — libxposed API 102

**Nihai kaynak ve APK dağıtımı:** [GitHub Releases v2.0.0](https://github.com/mahmutuslu/YellowPage-TR/releases/tag/v2.0.0)

Xiaomi 15 Ultra / HyperOS 4 ve uyumlu Xiaomi Telefon/YellowPage ortamları için bağımsız spam-etiket modülü. Bu uygulama **resmî Xiaomi bileşeni değildir** ve Google Telefon'un geniş spam veri tabanına sahip değildir.

## API 102 geçişi

- Eski `de.robv.android.xposed` API 82 / 93, stub JAR ve `assets/xposed_init` kaldırıldı.
- Gerçek `io.github.libxposed:api:102.0.0` ve `XposedModule.onPackageReady()` kullanılır.
- Hook'lar `hook(method).setId(...).intercept(chain -> ...)` üzerinden kurulur.
- Modern `META-INF/xposed/java_init.list`, `module.prop`, `scope.list` tanımları.
- **Önerilen kapsam:** `com.android.contacts`, `com.android.incallui`, `com.miui.yellowpage`. LSPosed'den bu üç uygulamanın gerçekten seçili olduğunu kontrol et.
- Paket kimliği (`dev.yellowpagetr.spam`) ve mevcut veri saklama alanı korunur; sürüm `2.0.0` (code 20) olur.
- Android minSdk 29 / targetSdk 32 korunmuştur. **Xposed API 102**, Android API 102 anlamına gelmez.

## Kurulum

1. Modülün önceki sürümü v1.3.0 veya daha eskiyse önce verilerini yedeklemek istersen yedekle. GitHub Actions debug imzası farklı olabilir: `INSTALL_FAILED_UPDATE_INCOMPATIBLE` olursa **yalnızca YellowPage TR APK'sını** kaldırıp bu sürümü kur; Xiaomi'nin kendi YellowPage ve Telefon uygulamalarını kaldırma.
2. **API 102'yi gerçekten destekleyen** LSPosed/framework sürümünü kullan.
3. [v2.0.0 APK](https://github.com/mahmutuslu/YellowPage-TR/releases/tag/v2.0.0) dosyasını yükle, LSPosed > YellowPage TR > Modülü etkinleştir.
4. Önerilen üç uygulamanın kapsamını seç: `com.android.contacts`, `com.android.incallui`, `com.miui.yellowpage`.
5. Telefonu bir kez yeniden başlat. Günlük ağ varlığında Android'in uygun gördüğü zamanda yenileme planlanır; numara veya API anahtarı girmek gerekmez.

## Özellikler ve sınırlar

- Xiaomi'nin **mevcut arama ekranında** listedeki arayanı `Olası spam (TR)` olarak işaretlemeyi dener; orijinal kişi adını ve mevcut Xiaomi etiketlerini korur.
- `symbuzzer/Turkish-Spam-Numbers` GPL-3.0 CSV akışı (doğrudan HTTPS, API anahtarsız) yaklaşık 24 saatte bir yeniden kontrol edilir. İlk paket içinde 201 Türkiye numarası vardır.
- Geçerli yeni liste başarısız olursa en son güvenilir liste kullanılır. Gelen çağrıdaki numara **internete gönderilmez**; eşleşme çevrimdışıdır ve yalnızca tam numara eşleşmesidir.
- **OpenSpam tam listesi bulunmaz**, yetki gerektiren API'den numara sorgulama yapılmaz.
- Kullanıcıyı korumak için çağrı engellenmez; olası yanlış pozitifler uyarı niteliğindedir. ROM güncellemeleri Xiaomi'nin özel sınıf/metot adlarını değiştirebilir.
- Derleme ve kaynak testlerinin geçmesi gerçek gelen spam aramasında etiketin görüldüğünü kanıtlamaz. v1.3.0'da bile bu davranış henüz canlı spam aramasında teyit edilmemişti.

## Doğrulama ve kaynak

GitHub Actions, Python regresyon testleri, Java JUnit normalizasyon testleri, APK `META-INF/xposed` kayıtları ve derlemeyi doğrular. Derleme: `gradle :app:testDebugUnitTest :app:assembleDebug`.

Veri kaynağı: [symbuzzer/Turkish-Spam-Numbers](https://github.com/symbuzzer/Turkish-Spam-Numbers), lisans belgeleri `LICENSES/` içinde. Bağımsız modül GPL-3.0 bildirimlerine uyar.
