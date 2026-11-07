# YellowPage TR v1.3 — API anahtarsız Xiaomi YellowPage spam uyarıları

Xiaomi 15 Ultra / HyperOS 4 için deneysel **LSPosed** modülü. Xiaomi'nin orijinal Telefon, YellowPage ve gelen arama ekranları kullanılmaya devam eder. Modül bağımsız bir çevirici, Google Telefon veya çağrı engelleyici değildir.

## Kur ve unut

1. GitHub Actions'tan `YellowPage-TR-LSPosed-v1.3-debug-APK` indirip yükle.
2. LSPosed > YellowPage TR > **Önerilen kapsam**'ta `com.android.contacts`, `com.android.incallui`, `com.miui.yellowpage` seçili olsun. Modül scope öneriyor; LSPosed'in mevcut kullanıcı kapsamını kendiliğinden değiştirmesi garanti edilmez.
3. Telefonu **bir kez yeniden başlat**. Sonrasında açılışta ve paket güncellemesinde arka plan yenileme işi otomatik planlanır; uygulamayı açmak, API anahtarı girmek, numara yazmak gerekmez.
4. Android JobScheduler internet bağlantısı varken yaklaşık 24 saatte bir güncelleme dener. İşletim sistemi, pil ve ağ durumuna göre zamanlamayı erteleyebilir. Uygulamayı isteğe bağlı açarsan son güncellemeyi ve yerel kayıt sayısını görürsün.

## Kaynak ve veri

- **Türkiye spam listesi:** `symbuzzer/Turkish-Spam-Numbers` GPL-3.0 CSV (doğrudan, anahtarsız HTTPS)
  - https://github.com/symbuzzer/Turkish-Spam-Numbers
  - `https://raw.githubusercontent.com/symbuzzer/Turkish-Spam-Numbers/main/SpamBlocker.csv`
- **İlk paket içi liste:** 201 Türkiye numarası. Geçerli bir yeni CSV doğrulanınca yerel liste değişir; güncelleme başarısız olursa son sağlam liste korunur. **Günlük kontrol**, kaynağın her gün yeni numara eklediği anlamına gelmez.
- Liste yalnızca numaraya tam eşleşme yapar, herhangi bir numara aralığını spam olarak genellemez. Yanlış pozitif olasılığı vardır.
- **OpenSpam tam listesi bu sürümde YOKTUR.** OpenSpam toplu dışa aktarımı ayrıca yetkilendirilmiştir. Standart bireysel API sorguları ve API anahtarı ekranı v1.3'ten kaldırılmıştır.
- Bu açık küçük Türkiye veri kaynağı **Google Telefon'un milyonlarca kullanıcı raporlu spam veritabanının eşdeğeri değildir**. Ek, lisanslı ve Türkiye'yi kapsayan veri kaynakları bulunursa aynı yerel güncelleyiciye bağlanabilir.

## Teknik

- `BootSyncReceiver` Android'in `BOOT_COMPLETED` veya `MY_PACKAGE_REPLACED` olayında senkronizasyonu planlar. `DailySpamSyncJob` `JobScheduler` ile ağ erişimi olan uygun bir zamanda listeyi alır.
- `DailyOfflineList` CSV başlığını, kayıt biçimini, minimum/maksimum sayıyı ve en çok 1 MB dosya boyutunu doğrular, veriyi özel uygulama depolamasına atomik olarak kaydeder. Uygulama var olan listeyi kullanır; gelen çağrıda HTTPS yapılmaz.
- `RiskCacheProvider.call("hasLocalRisk")` yalnızca Xiaomi Telefon, InCallUI ve YellowPage'in kimliği doğrulanmış UID'lerine `boolean` yanıt verir. Telefon numaraları veya arama günlükleri saklanmaz, günlüklerde numara yazılmaz.
- `HookEntry` Xamarin değil **Xiaomi'nin gerçek InCallUI** `CallCardInfo.createCallCardInfo` yolundaki `markTitle` ve `extraInfo` alanlarını doldurur. Orijinal kişi adı ve daha önceki Xiaomi etiketi korunur, arama engelleme yapılmaz.
- v0.9'da `YellowPage TR (TEST)` etiketi gerçek gelen arama ekranında **görsel olarak doğrulandı**. v1.3'ün yenilenen listeyi gerçek spam gelen aramada gösterme davranışı telefonda ayrıca doğrulanmalıdır.
- Kayıtlar: `YellowPageTR/1.3`. Kapsam doğru olsa bile her romda aynı Xiaomi metotları bulunmayabilir.

## Lisans / güvenlik

GPL-3.0 upstream listesinin lisansı: `LICENSES/`. Yalnızca kullanıcıya şüpheli spam etiketi gösterir, çağrıları otomatik reddetmez. Acil aramalar için kullanılmamalıdır.
