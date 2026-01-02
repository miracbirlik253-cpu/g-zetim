# Android Uygulaması - Gözetim

Bu klasör Android uygulamasının kaynak kodlarını içerir.

## 🏗️ Proje Yapısı

```
android-app/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/gozetim/
│   │   │   │   ├── MainActivity.java
│   │   │   │   ├── services/
│   │   │   │   │   ├── MonitoringService.java
│   │   │   │   │   ├── LocationService.java
│   │   │   │   │   └── AppBlockerService.java
│   │   │   │   ├── receivers/
│   │   │   │   │   ├── DeviceAdminReceiver.java
│   │   │   │   │   └── BootReceiver.java
│   │   │   │   ├── utils/
│   │   │   │   │   ├── FirebaseHelper.java
│   │   │   │   │   ├── PermissionHelper.java
│   │   │   │   │   └── AppUsageHelper.java
│   │   │   │   └── models/
│   │   │   │       ├── AppUsage.java
│   │   │   │       ├── DeviceInfo.java
│   │   │   │       └── LocationData.java
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   │   └── drawable/
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle
│   └── build.gradle
└── README.md (bu dosya)
```

## 🚀 Kurulum Adımları

### 1. Android Studio Kurulumu

1. [Android Studio](https://developer.android.com/studio) indirin ve kurun
2. Android SDK'yı yükleyin (API Level 26+)

### 2. Proje Oluşturma

**ÖNEMLİ**: Android Studio ile yeni bir proje oluşturmanız gerekiyor:

```bash
# Android Studio'yu açın
# File > New > New Project
# "Empty Activity" seçin
# 
# Application name: Gozetim
# Package name: com.gozetim.app
# Save location: d:/projeler/GOZETIM/android-app
# Language: Java
# Minimum SDK: API 26 (Android 8.0)
```

### 3. Gerekli Bağımlılıklar

`app/build.gradle` dosyasına ekleyin:

```gradle
dependencies {
    implementation 'androidx.appcompat:appcompat:1.6.1'
    implementation 'com.google.android.material:material:1.11.0'
    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
    
    // Firebase
    implementation platform('com.google.firebase:firebase-bom:32.7.0')
    implementation 'com.google.firebase:firebase-database'
    implementation 'com.google.firebase:firebase-auth'
    implementation 'com.google.firebase:firebase-messaging'
    
    // WorkManager (Arka plan işlemleri)
    implementation 'androidx.work:work-runtime:2.9.0'
    
    // Location
    implementation 'com.google.android.gms:play-services-location:21.1.0'
}
```

### 4. AndroidManifest.xml İzinleri

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />
```

### 5. Firebase Bağlantısı

1. Android Studio'da: `Tools > Firebase`
2. `Realtime Database` seçin
3. `Connect to Firebase` butonuna tıklayın
4. Web panelde kullandığınız Firebase projesini seçin
5. `Add Firebase Realtime Database to your app` butonuna tıklayın

### 6. Kaynak Kodları

Bu klasördeki kaynak kod dosyalarını Android Studio projenize kopyalayın:

- `MainActivity.java` → `app/src/main/java/com/gozetim/`
- `services/` klasörünü → `app/src/main/java/com/gozetim/services/`
- `receivers/` klasörünü → `app/src/main/java/com/gozetim/receivers/`
- vb.

## 📱 Özellikler

### 1. Uygulama Kullanım Takibi
- `UsageStatsManager` kullanarak tüm uygulamaların kullanım sürelerini izler
- Her 15 dakikada bir Firebase'e gönderir

### 2. Uygulama Engelleme
- Engellenen uygulamalar açıldığında overlay gösterir
- Kullanıcıyı ana ekrana geri yönlendirir

### 3. Konum Takibi
- GPS ve Network kullanarak konum alır
- Her 15 dakikada bir Firebase'e gönderir
- Arka planda çalışır

### 4. Cihaz Bilgileri
- Model, Android sürümü, pil durumu
- Gerçek zamanlı olarak güncellenir

### 5. Silme Koruması
- Uygulama silinmeye çalışıldığında Firebase'e bildirim gönderir
- Device Admin kullanarak koruma sağlar

## 🔧 Geliştirme

### Debug Modu

```bash
# USB Debugging açın
# Android Studio'da Run > Debug 'app'
```

### APK Oluşturma

```bash
# Build > Build Bundle(s) / APK(s) > Build APK(s)
# APK: app/build/outputs/apk/debug/app-debug.apk
```

### Release APK (İmzalı)

```bash
# Build > Generate Signed Bundle / APK
# Keystore oluşturun
# Release APK oluşturun
```

## 🐛 Sorun Giderme

### İzin Hataları

Uygulama ilk açılışta şu izinleri istemelidir:
1. **Usage Stats**: Settings > Apps > Special Access > Usage Access
2. **Overlay**: Settings > Apps > Special Access > Display over other apps
3. **Location**: Uygulama içinden izin iste
4. **Device Admin**: Settings > Security > Device Administrators

### Firebase Bağlantı Hatası

- `google-services.json` dosyasının `app/` klasöründe olduğundan emin olun
- Firebase Console'da Android uygulaması eklenmiş olmalı
- Package name doğru olmalı: `com.gozetim.app`

### Arka Plan Servisleri Çalışmıyor

Android 8.0+ için:
- Foreground Service kullanın
- WorkManager ile periyodik görevler tanımlayın
- Battery Optimization'dan muaf tutun

## 📊 Veri Yapısı (Firebase)

```json
{
  "users": {
    "userId": {
      "email": "user@example.com",
      "devices": {
        "deviceId": {
          "name": "Samsung Galaxy S21",
          "model": "SM-G991B",
          "androidVersion": "13",
          "batteryLevel": 78,
          "isOnline": true,
          "lastSeen": 1234567890,
          "apps": {
            "com.instagram.android": {
              "name": "Instagram",
              "usageToday": 145,
              "usageWeek": 890,
              "status": "limited",
              "limit": 120
            }
          },
          "location": {
            "lat": 41.0082,
            "lng": 28.9784,
            "address": "Sultanahmet, İstanbul",
            "timestamp": 1234567890,
            "accuracy": 15
          }
        }
      }
    }
  }
}
```

## 🔐 Güvenlik

- Firebase Security Rules kullanın
- Kullanıcı sadece kendi cihazlarına erişebilmeli
- API anahtarlarını güvende tutun
- ProGuard ile kod karıştırma yapın

## 📝 Notlar

- Minimum Android sürümü: 8.0 (API 26)
- Hedef Android sürümü: 14 (API 34)
- Java 8+ gereklidir

## 🚀 Sonraki Adımlar

1. Android Studio'da projeyi oluşturun
2. Kaynak kodları kopyalayın
3. Firebase bağlantısını yapın
4. Test edin
5. APK oluşturun
6. Telefona yükleyin

---

**Yardım için**: Android geliştirme konusunda yardıma ihtiyacınız varsa, kaynak kodları ve detaylı açıklamalar için bana sorun!
