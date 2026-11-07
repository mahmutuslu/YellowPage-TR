# YellowPage TR v1.3.0 — İlk herkese açık sürüm

**Xiaomi'nin orijinal Telefon / YellowPage uygulamasına yönelik LSPosed modülü.** Ayrı çevirici veya arama engelleyici değildir.

## Öne çıkan özellikler

- Xiaomi **InCallUI** üzerinde, yerel spam listesiyle eşleşen gelen çağrılar için **Olası spam (TR)** etiketi.
- API anahtarı veya kullanıcı hesabı gerektirmez. Kurulduktan sonra yaklaşık 24 saatte bir **otomatik çevrimdışı liste yenilemesi** planlanır.
- Açık kaynaklı [Turkish-Spam-Numbers](https://github.com/symbuzzer/Turkish-Spam-Numbers) (GPL-3.0) verisiyle birlikte gelen **201 Türkiye numarası**; indirilebilir güncel CSV.
- Ağ kesilirse son geçerli liste, hiç güncellenemediyse paket içi başlangıç listesi kullanılır.
- Arayanın numarası internete gönderilmez; kişi adları değiştirilmez; aramalar engellenmez.
- LSPosed **Önerilen Kapsam:** `com.android.contacts`, `com.android.incallui`, `com.miui.yellowpage`.

## Kurulum

1. Bu sürümün **YellowPage_TR_v1.3.apk** dosyasını indirip telefona kur.
2. LSPosed'de modülü etkinleştir ve üç önerilen kapsam uygulamasının **seçili** olduğunu doğrula. Önerilen uygulamalar her LSPosed sürümünde otomatik açılmayabilir.
3. Telefonu bir kez yeniden başlat. Veritabanı güncellemeleri Android tarafından arka planda planlanır.
4. İstersen modülü açarak son başarılı güncelleme zamanını ve telefonundaki liste boyutunu kontrol edebilirsin.

## Uyum ve test durumu

- Geliştirme ve tanılama cihazı: **Xiaomi 15 Ultra**, HyperOS 4, Android 17, LSPosed.
- **Doğrulanan:** modülün Telefon/Kişiler sürecine 13 kanca yüklemesi; Android JobScheduler günlük görevinin kaydedilmesi; ilk spam listesi güncellemesi için yerel zaman kaydı; v0.9'da Xiaomi gelen arama ekranının kendi içinde `YellowPage TR (TEST)` etiketinin görünmesi.
- **Henüz doğrulanmayan:** v1.3 ile gerçek spam numarasından gelen bir çağrıda `Olası spam (TR)` etiketinin gösterilmesi; başka ROM'larda ve cihazlarda çalışma.

## Önemli sınırlamalar

- Bu sürümde **OpenSpam tam veritabanı yoktur**: OpenSpam toplu veri dışa aktarımı için ayrı yetkilendirme gerekir. API anahtarı veya arayan numarasını servise gönderen fonksiyonlar bulunmaz.
- Bu veri seti Google Telefon'un spam sistemine eşdeğer değildir; yalnızca listelenmiş numaralara tam eşleşme uygular. Topluluk bildirimi yanlış pozitif olabilir.
- Bu paket **GitHub Actions debug anahtarıyla imzalanmıştır**; farklı bir debug imzasıyla kurulu eski sürümün üstüne güncelleme yapılamayabilir. Böyle bir durumda Android, önceki uygulamanın kaldırılmasını isteyebilir. LSPosed ayarlarını ve kullanıcı verilerini yedekleyin.
- Otomatik job'ın çalışma zamanı Android pil/ağ politikaları nedeniyle değişebilir.
- Bu bir **deneysel LSPosed entegrasyonunun yayın sürümüdür**; sistem modifikasyonu yaptığınız için dikkatli kullanın.

Kaynak ve lisans: [Turkish-Spam-Numbers](https://github.com/symbuzzer/Turkish-Spam-Numbers) (GPL-3.0), bu depodaki `LICENSES/GPL-3.0.txt` ve `LICENSES/NOTICE.txt`.
