# 📱 Gözetim - Ebeveyn Kontrol Sistemi (Supabase Edition)

## ✅ Proje Tamamlandı!

Tebrikler! Gözetim Ebeveyn Kontrol Sistemi başarıyla **Supabase** altyapısına taşındı.

---

## 🏗️ Yeni Mimari (Supabase)

Bu versiyonda Google Firebase yerine açık kaynaklı **PostgreSQL** tabanlı **Supabase** kullanıldı.

```
┌─────────────────────┐
│   Web Panel         │
│   (Controller)      │
│                     │
│  - Supabase JS SDK  │
│  - Realtime Subs.   │
│  - Dashboard        │
└──────────┬──────────┘
           │
           │ REST API &
           │ Realtime
           │ (PostgreSQL)
           │
┌──────────▼──────────┐
│  Android App        │
│  (Monitored)        │
│                     │
│  - Retrofit Client  │
│  - REST API Calls   │
│  - Usage Tracker    │
└─────────────────────┘
```

## 📂 Proje Yapısı

```
GOZETIM/
├── web-panel/
│   ├── app.js                 # ✅ Updated for Supabase
│   ├── index.html             # ✅ Supabase CDN Added
│   └── styles.css
│
├── android-app/
│   ├── network/               # ✅ NEW: Network Layer
│   │   ├── SupabaseService.java
│   │   └── models/            # POJOs (Device, App, Location)
│   ├── utils/
│   │   └── SupabaseHelper.java # ✅ Replaces FirebaseHelper
│   ├── services/
│   │   ├── MonitoringService.java # ✅ Updated
│   │   ├── LocationService.java   # ✅ Updated
│   │   └── AppBlockerService.java # ✅ Updated
│   └── MainActivity.java          # ✅ Updated
│
└── ...
```

## 🚀 Avantajlar

1.  **İlişkisel Veritabanı (SQL)**: Veriler artık düzgün tablolar (Users, Devices, Apps) halinde tutuluyor.
2.  **Daha Hızlı Sorgular**: Karmaşık sorgular SQL ile çok daha kolay.
3.  **Açık Kaynak**: Vendor lock-in yok.
4.  **REST API**: Android tarafında standart Retrofit kullanıldı, yönetimi kolay.

---

## 🏃‍♂️ Kurulum

Detaylı kurulum için **`KURULUM_REHBERI.md`** dosyasına bakın.

1. Supabase projesi oluşturun.
2. SQL kodlarını çalıştırın.
3. API anahtarlarını hem `app.js` hem de `SupabaseHelper.java` dosyalarına girin.
4. Başlatın!

---

**Versiyon**: 2.0.0 (Supabase Migration)
**Durum**: ✅ Tamamlandı
