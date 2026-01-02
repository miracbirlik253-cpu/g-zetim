# Gözetim - Ebeveyn Kontrol Sistemi

Modern ve güçlü bir ebeveyn kontrol/parental control uygulaması.

## 📱 Özellikler

### Web Kontrol Paneli
- ✅ Modern, dark mode arayüz
- ✅ Gerçek zamanlı cihaz takibi
- ✅ Uygulama kullanım istatistikleri
- ✅ Uygulama engelleme ve zaman limitleri
- ✅ Konum takibi
- ✅ Uzaktan cihaz kilitleme
- ✅ Detaylı raporlama

### Android Uygulaması
- ✅ Arka planda çalışma
- ✅ Uygulama kullanım takibi
- ✅ Uygulama engelleme
- ✅ Konum paylaşımı
- ✅ Cihaz bilgilerini gönderme
- ✅ Silme koruması (bildirim)

## 🚀 Kurulum

### Web Panel Kurulumu

1. **Firebase Projesi Oluşturun**
   - [Firebase Console](https://console.firebase.google.com/) adresine gidin
   - Yeni proje oluşturun
   - Authentication, Realtime Database ve Cloud Messaging'i etkinleştirin

2. **Firebase Config Güncelleyin**
   ```javascript
   // web-panel/app.js dosyasındaki firebaseConfig'i güncelleyin
   const firebaseConfig = {
       apiKey: "YOUR_API_KEY",
       authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
       // ... diğer ayarlar
   };
   ```

3. **Web Paneli Çalıştırın**
   ```bash
   cd web-panel
   # Basit bir HTTP server başlatın
   python -m http.server 8000
   # veya
   npx serve
   ```

4. **Tarayıcıda Açın**
   - `http://localhost:8000` adresine gidin
   - Demo hesap: `demo@gozetim.com` / `demo123`

### Android Uygulaması Kurulumu

1. **Android Studio'da Açın**
   ```bash
   cd android-app
   # Android Studio ile açın
   ```

2. **Firebase Bağlantısı**
   - Tools > Firebase > Realtime Database
   - "Connect to Firebase" butonuna tıklayın
   - Projenizi seçin

3. **İzinleri Verin**
   Uygulama ilk açılışta şu izinleri isteyecek:
   - Usage Stats Access (Uygulama kullanımı)
   - Location (Konum)
   - Device Admin (Cihaz yönetimi)
   - Overlay Permission (Uygulama engelleme)

4. **Uygulamayı Yükleyin**
   - Build > Build Bundle(s) / APK(s) > Build APK(s)
   - APK'yı telefona yükleyin

## 📖 Kullanım

### İlk Kurulum

1. **Web panelde hesap oluşturun**
2. **Android uygulamasını yükleyin**
3. **Uygulamada aynı hesapla giriş yapın**
4. **Gerekli izinleri verin**
5. **Web panelden cihazı görüntüleyin**

### Uygulama Engelleme

1. Web panelde "Uygulamalar" sekmesine gidin
2. Engellemek istediğiniz uygulamaya tıklayın
3. "Engelle" seçeneğini seçin
4. Kaydet butonuna tıklayın

### Zaman Limiti Koyma

1. Uygulamaya tıklayın
2. "Sınırla" seçeneğini seçin
3. Günlük limit belirleyin (örn: 2 saat)
4. Kaydedin

### Konum Takibi

1. "Konum" sekmesine gidin
2. "Konumu Yenile" butonuna tıklayın
3. Haritada güncel konumu görün

## 🔒 Güvenlik

- Tüm veriler Firebase üzerinden şifrelenir
- Sadece yetkili kullanıcılar cihaza erişebilir
- İki faktörlü doğrulama desteği (opsiyonel)
- Uygulama silme bildirimi

## ⚠️ Önemli Notlar

### Kısıtlamalar

1. **Uygulama Silme**: Kullanıcı uygulamayı silebilir, ancak web panele bildirim gider
2. **Fabrika Ayarları**: Cihaz fabrika ayarlarına döndürülürse tüm korumalar kaldırılır
3. **Root/Jailbreak**: Root edilmiş cihazlarda korumalar aşılabilir
4. **Android Sürümü**: Android 8.0+ gereklidir

### Yasal Uyarı

Bu uygulama ebeveyn kontrolü amacıyla tasarlanmıştır. Kullanmadan önce:
- Cihaz sahibinin rızasını alın
- Yerel yasalara uygun kullanın
- Gizlilik haklarına saygı gösterin

## 🛠️ Teknik Detaylar

### Kullanılan Teknolojiler

**Web Panel:**
- HTML5, CSS3, JavaScript
- Chart.js (Grafikler)
- Firebase SDK

**Android App:**
- Java/Kotlin
- Firebase Realtime Database
- WorkManager (Arka plan işlemleri)
- UsageStatsManager (Uygulama kullanımı)
- LocationManager (Konum)
- DeviceAdminReceiver (Cihaz yönetimi)

### Mimari

```
┌─────────────────┐
│   Web Panel     │
│  (Controller)   │
└────────┬────────┘
         │
         │ Firebase
         │ Realtime DB
         │
┌────────▼────────┐
│  Android App    │
│  (Monitored)    │
└─────────────────┘
```

## 📝 Yapılacaklar (TODO)

- [ ] iOS uygulaması (TestFlight)
- [ ] Ekran görüntüsü alma
- [ ] Arama/SMS logları
- [ ] Sosyal medya içerik filtreleme
- [ ] Yapay zeka destekli tehdit tespiti
- [ ] Çoklu cihaz desteği
- [ ] Aile profilleri

## 🤝 Katkıda Bulunma

Bu proje açık kaynak değildir ve kişisel kullanım içindir.

## 📄 Lisans

Tüm hakları saklıdır © 2026

## 📞 Destek

Sorularınız için: support@gozetim.com

---

**Not**: Bu uygulama demo amaçlıdır. Üretim ortamında kullanmadan önce güvenlik testlerinden geçirin.
