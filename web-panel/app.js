// ==================== SUPABASE CONFIGURATION ====================
// Kullanıcı kendi Supabase projesini oluşturmalı
const supabaseConfig = {
    url: "https://hsqttindsdvoappumvlx.supabase.co",
    key: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImhzcXR0aW5kc2R2b2FwcHVtdmx4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjczNjM2NTQsImV4cCI6MjA4MjkzOTY1NH0.gWXRQa_65LJRae2c1yBAnlixPyYwQl-af53_myzEimo"
};

// Supabase istemcisini oluştur
// Eğer kullanıcı config'i henüz girmemişse null olacak, demo modu çalışacak
let supabase = null;
if (supabaseConfig.url !== "YOUR_SUPABASE_URL") {
    supabase = window.supabase.createClient(supabaseConfig.url, supabaseConfig.key);
}

// Demo mode - Supabase ayarlanana kadar true
const DEMO_MODE = false;

// ==================== STATE MANAGEMENT ====================
let currentUser = null;
let currentDevice = null;
let installedApps = [];
let appUsageData = [];
let locationHistory = [];
let realtimeSubscriptions = [];

// ==================== INITIALIZATION ====================
document.addEventListener('DOMContentLoaded', () => {
    initializeApp();
    setupEventListeners();

    // Eğer Supabase ayarlı değilse veya demo modundaysak demo verisi yükle
    if (DEMO_MODE || !supabase) {
        loadDemoData();
    }
});

async function initializeApp() {
    // Check if user is logged in
    if (supabase) {
        const { data: { session } } = await supabase.auth.getSession();
        if (session) {
            currentUser = session.user;
            showDashboard();
            setupRealtimeSubscription();
            loadRealData();
        } else {
            // Local storage fallback for demo user
            const savedUser = localStorage.getItem('currentUser');
            if (savedUser) {
                currentUser = JSON.parse(savedUser);
                showDashboard();
            } else {
                showLogin();
            }
        }
    } else {
        // Demo mode fallback
        const savedUser = localStorage.getItem('currentUser');
        if (savedUser) {
            currentUser = JSON.parse(savedUser);
            showDashboard();
        } else {
            showLogin();
        }
    }
}

// ==================== AUTHENTICATION ====================
function setupEventListeners() {
    // Login form
    const loginForm = document.getElementById('loginForm');
    if (loginForm) {
        loginForm.addEventListener('submit', handleLogin);
    }

    // Register button
    const registerBtn = document.getElementById('registerBtn');
    if (registerBtn) {
        registerBtn.addEventListener('click', handleRegister);
    }

    // Logout button
    const logoutBtn = document.getElementById('logoutBtn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', handleLogout);
    }

    // Navigation tabs
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const tab = item.getAttribute('data-tab');
            switchTab(tab);
        });
    });

    // Filter tabs
    const filterTabs = document.querySelectorAll('.filter-tab');
    filterTabs.forEach(tab => {
        tab.addEventListener('click', () => {
            filterTabs.forEach(t => t.classList.remove('active'));
            tab.classList.add('active');
            filterApps(tab.getAttribute('data-filter'));
        });
    });

    // App search
    const appSearch = document.getElementById('appSearch');
    if (appSearch) {
        appSearch.addEventListener('input', (e) => {
            searchApps(e.target.value);
        });
    }

    // Refresh location
    const refreshLocation = document.getElementById('refreshLocation');
    if (refreshLocation) {
        refreshLocation.addEventListener('click', updateLocation);
    }

    // Modal controls
    setupModalControls();

    // Settings toggles
    setupSettingsToggles();

    // Dangerous actions
    setupDangerousActions();
}

async function handleLogin(e) {
    e.preventDefault();

    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;

    // Demo login
    if (DEMO_MODE && email === 'demo@gozetim.com' && password === 'demo123') {
        currentUser = {
            id: 'demo-user',
            email: email,
            name: 'Demo Kullanıcı'
        };

        localStorage.setItem('currentUser', JSON.stringify(currentUser));
        showDashboard();
        loadDemoData();
        return;
    }

    // Supabase Login
    if (supabase) {
        const { data, error } = await supabase.auth.signInWithPassword({
            email: email,
            password: password,
        });

        if (error) {
            alert('Giriş başarısız: ' + error.message);
        } else {
            currentUser = data.user;
            showDashboard();
            loadRealData();
            setupRealtimeSubscription();
        }
    } else {
        alert('Supabase yapılandırması eksik! Lütfen app.js dosyasını düzenleyin veya Demo hesabını kullanın: demo@gozetim.com / demo123');
    }
}

function handleRegister() {
    alert('Kayıt özelliği yakında eklenecek. Şimdilik demo hesabı kullanın:\ndemo@gozetim.com / demo123');
}

async function handleLogout() {
    if (confirm('Çıkış yapmak istediğinizden emin misiniz?')) {
        if (supabase) {
            await supabase.auth.signOut();
        }
        localStorage.removeItem('currentUser');
        currentUser = null;

        // Unsubscribe from realtime
        if (realtimeSubscriptions) {
            supabase.removeChannel(realtimeSubscriptions);
        }

        showLogin();
    }
}

function showLogin() {
    document.getElementById('loginScreen').classList.add('active');
    document.getElementById('dashboardScreen').classList.remove('active');
}

function showDashboard() {
    document.getElementById('loginScreen').classList.remove('active');
    document.getElementById('dashboardScreen').classList.add('active');

    // Load dashboard data
    loadOverviewData();
}

// ==================== REALTIME DATA ====================
function setupRealtimeSubscription() {
    if (!supabase || !currentUser) return;

    // Installed Apps değişikliklerini dinle
    const appsChannel = supabase.channel('table-db-changes')
        .on(
            'postgres_changes',
            {
                event: '*',
                schema: 'public',
                table: 'installed_apps',
            },
            (payload) => {
                console.log('App change received!', payload);
                loadAppsData(); // Verileri yeniden çek ve güncelle
            }
        )
        .subscribe();

    realtimeSubscriptions = appsChannel;
}

async function loadRealData() {
    if (!supabase) return;

    // Cihazları çek
    const { data: devices, error: deviceError } = await supabase
        .from('devices')
        .select('*')
        .eq('user_id', currentUser.id)
        .limit(1);

    if (devices && devices.length > 0) {
        currentDevice = devices[0];

        // Uygulamaları çek
        const { data: apps, error: appsError } = await supabase
            .from('installed_apps')
            .select('*')
            .eq('device_id', currentDevice.device_id);

        if (apps) {
            installedApps = apps.map(app => ({
                name: app.app_name,
                package: app.package_name,
                icon: app.icon_char || '📱',
                usageToday: app.usage_today_minutes || 0,
                usageWeek: app.usage_week_minutes || 0,
                status: app.status,
                limit: app.daily_limit_minutes
            }));

            loadOverviewData();
            loadAppsData();
        }

        // Konum geçmişini çek
        const { data: locations, error: locError } = await supabase
            .from('location_history')
            .select('*')
            .eq('device_id', currentDevice.device_id)
            .order('timestamp', { ascending: false })
            .limit(10);

        if (locations) {
            locationHistory = locations.map(loc => ({
                lat: loc.latitude,
                lng: loc.longitude,
                address: loc.address,
                timestamp: new Date(loc.timestamp).getTime(),
                accuracy: loc.accuracy
            }));
            loadLocationData();
        }
    }
}

// ==================== TAB SWITCHING ====================
function switchTab(tabName) {
    // Update nav items
    document.querySelectorAll('.nav-item').forEach(item => {
        item.classList.remove('active');
        if (item.getAttribute('data-tab') === tabName) {
            item.classList.add('active');
        }
    });

    // Update tab content
    document.querySelectorAll('.tab-content').forEach(content => {
        content.classList.remove('active');
    });

    const targetTab = document.getElementById(tabName + 'Tab');
    if (targetTab) {
        targetTab.classList.add('active');

        // Load tab-specific data
        switch (tabName) {
            case 'overview':
                loadOverviewData();
                break;
            case 'apps':
                loadAppsData();
                break;
            case 'location':
                loadLocationData();
                break;
            case 'settings':
                loadSettingsData();
                break;
        }
    }
}

// ==================== DEMO DATA ====================
function loadDemoData() {
    // Sample installed apps
    installedApps = [
        { name: 'Instagram', package: 'com.instagram.android', icon: '📷', usageToday: 145, usageWeek: 890, usageMonth: 3200, status: 'limited', limit: 120 },
        { name: 'TikTok', package: 'com.zhiliaoapp.musically', icon: '🎵', usageToday: 0, usageWeek: 0, usageMonth: 0, status: 'blocked' },
        { name: 'WhatsApp', package: 'com.whatsapp', icon: '💬', usageToday: 67, usageWeek: 420, usageMonth: 1850, status: 'allowed' },
        { name: 'YouTube', package: 'com.google.android.youtube', icon: '▶️', usageToday: 89, usageWeek: 540, usageMonth: 2100, status: 'limited', limit: 90 },
        { name: 'Chrome', package: 'com.android.chrome', icon: '🌐', usageToday: 45, usageWeek: 280, usageMonth: 1200, status: 'allowed' },
        { name: 'Spotify', package: 'com.spotify.music', icon: '🎧', usageToday: 120, usageWeek: 780, usageMonth: 3400, status: 'allowed' },
        { name: 'Twitter', package: 'com.twitter.android', icon: '🐦', usageToday: 34, usageWeek: 210, usageMonth: 890, status: 'limited', limit: 60 },
        { name: 'Telegram', package: 'org.telegram.messenger', icon: '✈️', usageToday: 23, usageWeek: 150, usageMonth: 620, status: 'allowed' },
        { name: 'Netflix', package: 'com.netflix.mediaclient', icon: '🎬', usageToday: 0, usageWeek: 0, usageMonth: 0, status: 'blocked' },
        { name: 'Gmail', package: 'com.google.android.gm', icon: '📧', usageToday: 12, usageWeek: 85, usageMonth: 340, status: 'allowed' },
    ];

    // Sample location history
    locationHistory = [
        { lat: 41.0082, lng: 28.9784, address: 'Sultanahmet, İstanbul', timestamp: Date.now() - 300000, accuracy: 15 },
        { lat: 41.0055, lng: 28.9769, address: 'Eminönü, İstanbul', timestamp: Date.now() - 3600000, accuracy: 20 },
        { lat: 41.0123, lng: 28.9745, address: 'Sirkeci, İstanbul', timestamp: Date.now() - 7200000, accuracy: 18 },
    ];

    // Sample device info
    currentDevice = {
        name: 'Samsung Galaxy S21',
        model: 'SM-G991B',
        androidVersion: '13',
        appVersion: '1.0.0',
        batteryLevel: 78,
        isOnline: true,
        lastSeen: Date.now()
    };
}

// ==================== OVERVIEW TAB ====================
function loadOverviewData() {
    // Update stats
    const totalScreenTime = installedApps.reduce((sum, app) => sum + app.usageToday, 0);
    document.getElementById('screenTime').textContent = formatMinutes(totalScreenTime);
    document.getElementById('appsUsed').textContent = installedApps.filter(app => app.usageToday > 0).length;

    const blockedCount = installedApps.filter(app => app.status === 'blocked').length;
    const limitedCount = installedApps.filter(app => app.status === 'limited').length;
    document.getElementById('activeRules').textContent = blockedCount + limitedCount;

    // Load top apps
    loadTopApps();

    // Load activity timeline
    loadActivityTimeline();

    // Load usage chart
    loadUsageChart();
}

function loadTopApps() {
    const topAppsContainer = document.getElementById('topApps');
    const topApps = [...installedApps]
        .sort((a, b) => b.usageToday - a.usageToday)
        .slice(0, 5);

    topAppsContainer.innerHTML = topApps.map(app => `
        <div class="app-item">
            <div class="app-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                ${app.icon}
            </div>
            <div class="app-info">
                <div class="app-name">${app.name}</div>
                <div class="app-usage">Bugün <span class="app-time">${formatMinutes(app.usageToday)}</span> kullanıldı</div>
            </div>
        </div>
    `).join('');
}

function loadActivityTimeline() {
    const timelineContainer = document.getElementById('activityTimeline');

    const activities = [
        { icon: '🚫', bg: '#ef4444', title: 'TikTok engellendi', description: 'Uygulama açılmaya çalışıldı', time: '5 dakika önce' },
        { icon: '⏰', bg: '#f59e0b', title: 'Instagram limit aşıldı', description: 'Günlük 2 saat limiti doldu', time: '23 dakika önce' },
        { icon: '✅', bg: '#10b981', title: 'Cihaz çevrimiçi', description: 'Bağlantı yeniden kuruldu', time: '1 saat önce' },
        { icon: '📍', bg: '#3b82f6', title: 'Konum güncellendi', description: 'Sultanahmet, İstanbul', time: '2 saat önce' },
    ];

    timelineContainer.innerHTML = activities.map(activity => `
        <div class="activity-item">
            <div class="activity-icon" style="background: ${activity.bg};">
                ${activity.icon}
            </div>
            <div class="activity-content">
                <div class="activity-title">${activity.title}</div>
                <div class="activity-description">${activity.description}</div>
            </div>
            <div class="activity-time">${activity.time}</div>
        </div>
    `).join('');
}

function loadUsageChart() {
    const ctx = document.getElementById('usageChart');
    if (!ctx) return;

    // Sample data for last 7 days
    const labels = ['Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt', 'Paz'];
    const data = [245, 289, 198, 312, 267, 423, 356];

    new Chart(ctx, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Ekran Süresi (dakika)',
                data: data,
                borderColor: '#667eea',
                backgroundColor: 'rgba(102, 126, 234, 0.1)',
                tension: 0.4,
                fill: true,
                pointBackgroundColor: '#667eea',
                pointBorderColor: '#fff',
                pointBorderWidth: 2,
                pointRadius: 5,
                pointHoverRadius: 7
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: {
                        color: 'rgba(255, 255, 255, 0.05)'
                    },
                    ticks: {
                        color: '#94a3b8'
                    }
                },
                x: {
                    grid: {
                        display: false
                    },
                    ticks: {
                        color: '#94a3b8'
                    }
                }
            }
        }
    });
}

// ==================== APPS TAB ====================
function loadAppsData() {
    renderApps(installedApps);
}

function renderApps(apps) {
    const appsGrid = document.getElementById('appsGrid');

    appsGrid.innerHTML = apps.map(app => `
        <div class="app-card" data-package="${app.package}">
            <div class="app-card-header">
                <div class="app-card-icon" style="background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);">
                    ${app.icon}
                </div>
                <div class="app-card-info">
                    <div class="app-card-name">${app.name}</div>
                    <div class="app-card-package">${app.package}</div>
                </div>
            </div>
            <div class="app-card-stats">
                <div class="app-stat">
                    <div class="app-stat-value">${formatMinutes(app.usageToday)}</div>
                    <div class="app-stat-label">Bugün</div>
                </div>
                <div class="app-stat">
                    <div class="app-stat-value">${formatMinutes(app.usageWeek)}</div>
                    <div class="app-stat-label">Bu Hafta</div>
                </div>
                <div class="app-stat">
                    <div class="app-stat-value">${app.limit ? formatMinutes(app.limit) : '-'}</div>
                    <div class="app-stat-label">Limit</div>
                </div>
            </div>
            <span class="app-card-status ${app.status}">
                ${app.status === 'allowed' ? 'İzin Verildi' : app.status === 'limited' ? 'Sınırlı' : 'Engellendi'}
            </span>
        </div>
    `).join('');

    // Add click handlers
    document.querySelectorAll('.app-card').forEach(card => {
        card.addEventListener('click', () => {
            const packageName = card.getAttribute('data-package');
            openAppModal(packageName);
        });
    });
}

function filterApps(filter) {
    let filtered = installedApps;

    if (filter !== 'all') {
        filtered = installedApps.filter(app => app.status === filter);
    }

    renderApps(filtered);
}

function searchApps(query) {
    const filtered = installedApps.filter(app =>
        app.name.toLowerCase().includes(query.toLowerCase()) ||
        app.package.toLowerCase().includes(query.toLowerCase())
    );

    renderApps(filtered);
}

// ==================== APP MODAL ====================
function setupModalControls() {
    const modal = document.getElementById('appModal');
    const closeBtn = modal.querySelector('.modal-close');
    const cancelBtn = modal.querySelector('.modal-cancel');
    const overlay = modal.querySelector('.modal-overlay');

    [closeBtn, cancelBtn, overlay].forEach(el => {
        el.addEventListener('click', () => {
            modal.classList.remove('active');
        });
    });

    // Status radio buttons
    const statusRadios = document.querySelectorAll('input[name="appStatus"]');
    statusRadios.forEach(radio => {
        radio.addEventListener('change', (e) => {
            const timeLimitSection = document.getElementById('timeLimitSection');
            if (e.target.value === 'limited') {
                timeLimitSection.style.display = 'block';
            } else {
                timeLimitSection.style.display = 'none';
            }
        });
    });

    // Save button
    const saveBtn = document.getElementById('saveAppSettings');
    saveBtn.addEventListener('click', saveAppSettings);
}

function openAppModal(packageName) {
    const app = installedApps.find(a => a.package === packageName);
    if (!app) return;

    const modal = document.getElementById('appModal');

    // Fill modal data
    document.getElementById('modalAppName').textContent = app.name;
    document.getElementById('modalAppIcon').textContent = app.icon;
    document.getElementById('modalAppIcon').style.background = 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)';
    document.getElementById('modalAppPackage').textContent = app.package;
    document.getElementById('modalAppUsage').textContent = `Bugün ${formatMinutes(app.usageToday)} kullanıldı`;

    // Set status
    document.querySelector(`input[name="appStatus"][value="${app.status}"]`).checked = true;

    // Show/hide time limit
    const timeLimitSection = document.getElementById('timeLimitSection');
    if (app.status === 'limited') {
        timeLimitSection.style.display = 'block';
        if (app.limit) {
            const hours = Math.floor(app.limit / 60);
            const minutes = app.limit % 60;
            document.getElementById('timeLimitHours').value = hours;
            document.getElementById('timeLimitMinutes').value = minutes;
        }
    } else {
        timeLimitSection.style.display = 'none';
    }

    // Update usage stats
    document.getElementById('modalUsageToday').textContent = formatMinutes(app.usageToday);
    document.getElementById('modalUsageWeek').textContent = formatMinutes(app.usageWeek);
    document.getElementById('modalUsageMonth').textContent = formatMinutes(app.usageMonth);

    // Store current app package
    modal.setAttribute('data-current-package', packageName);

    // Show modal
    modal.classList.add('active');
}

async function saveAppSettings() {
    const modal = document.getElementById('appModal');
    const packageName = modal.getAttribute('data-current-package');
    const app = installedApps.find(a => a.package === packageName);

    if (!app) return;

    // Get selected status
    const selectedStatus = document.querySelector('input[name="appStatus"]:checked').value;
    app.status = selectedStatus;

    // Get time limit if limited
    let newLimit = null;
    if (selectedStatus === 'limited') {
        const hours = parseInt(document.getElementById('timeLimitHours').value) || 0;
        const minutes = parseInt(document.getElementById('timeLimitMinutes').value) || 0;
        newLimit = hours * 60 + minutes;
        app.limit = newLimit;
    } else {
        app.limit = null;
    }

    // Save to Supabase (if connected)
    if (supabase && currentDevice) {
        const { error } = await supabase
            .from('installed_apps')
            .update({
                status: selectedStatus,
                daily_limit_minutes: newLimit
            })
            .eq('device_id', currentDevice.device_id)
            .eq('package_name', packageName);

        if (error) {
            console.error("Error updating app:", error);
            showNotification('Kaydedilirken hata oluştu: ' + error.message, 'danger');
            return;
        }
    } else if (DEMO_MODE) {
        localStorage.setItem('installedApps', JSON.stringify(installedApps));
    }

    // Close modal and refresh
    modal.classList.remove('active');
    loadAppsData();

    // Show success message
    showNotification('Ayarlar kaydedildi!', 'success');
}

// ==================== LOCATION TAB ====================
function loadLocationData() {
    if (locationHistory.length > 0) {
        const latest = locationHistory[0];

        document.getElementById('currentAddress').textContent = latest.address;
        document.getElementById('currentCoords').textContent = `${latest.lat.toFixed(6)}, ${latest.lng.toFixed(6)}`;
        document.getElementById('lastUpdate').textContent = formatTimestamp(latest.timestamp);
        document.getElementById('accuracy').textContent = `${latest.accuracy} metre`;

        // Load history
        const historyContainer = document.getElementById('locationHistory');
        historyContainer.innerHTML = locationHistory.map(loc => `
            <div class="history-item">
                <div class="history-item-time">${formatTimestamp(loc.timestamp)}</div>
                <div class="history-item-address">${loc.address}</div>
            </div>
        `).join('');
    }
}

function updateLocation() {
    showNotification('Konum güncelleniyor...', 'info');

    // Simulate location update
    setTimeout(() => {
        showNotification('Konum güncellendi!', 'success');
        loadLocationData();
    }, 1500);
}

// ==================== SETTINGS TAB ====================
function loadSettingsData() {
    if (currentDevice) {
        document.getElementById('deviceName').textContent = currentDevice.name;
        document.getElementById('deviceModel').textContent = currentDevice.model;
        document.getElementById('androidVersion').textContent = currentDevice.androidVersion;
        document.getElementById('batteryLevel').textContent = `${currentDevice.batteryLevel}%`;
        document.getElementById('appVersion').textContent = currentDevice.appVersion;
    }
}

function setupSettingsToggles() {
    const toggles = document.querySelectorAll('.toggle input');
    toggles.forEach(toggle => {
        toggle.addEventListener('change', (e) => {
            const settingName = e.target.id;
            const isEnabled = e.target.checked;

            // Save to Firebase or local storage
            if (DEMO_MODE) {
                localStorage.setItem(settingName, isEnabled);
            }

            showNotification(`${settingName} ${isEnabled ? 'açıldı' : 'kapatıldı'}`, 'info');
        });
    });
}

function setupDangerousActions() {
    const lockDevice = document.getElementById('lockDevice');
    const clearData = document.getElementById('clearData');

    lockDevice.addEventListener('click', () => {
        if (confirm('Cihazı kilitlemek istediğinizden emin misiniz?')) {
            showNotification('Cihaz kilitleniyor...', 'warning');
            // Send lock command to device
        }
    });

    clearData.addEventListener('click', () => {
        if (confirm('TÜM VERİLERİ SİLMEK İSTEDİĞİNİZDEN EMİN MİSİNİZ? Bu işlem geri alınamaz!')) {
            if (confirm('Son kez soruyoruz: Emin misiniz?')) {
                showNotification('Veriler siliniyor...', 'danger');
                // Clear all data
            }
        }
    });
}

// ==================== UTILITY FUNCTIONS ====================
function formatMinutes(minutes) {
    if (minutes < 60) {
        return `${minutes}d`;
    }
    const hours = Math.floor(minutes / 60);
    const mins = minutes % 60;
    return `${hours}s ${mins}d`;
}

function formatTimestamp(timestamp) {
    const date = new Date(timestamp);
    const now = new Date();
    const diff = now - date;

    if (diff < 60000) {
        return 'Az önce';
    } else if (diff < 3600000) {
        return `${Math.floor(diff / 60000)} dakika önce`;
    } else if (diff < 86400000) {
        return `${Math.floor(diff / 3600000)} saat önce`;
    } else {
        return date.toLocaleDateString('tr-TR', {
            day: 'numeric',
            month: 'long',
            hour: '2-digit',
            minute: '2-digit'
        });
    }
}

function showNotification(message, type = 'info') {
    // Create notification element
    const notification = document.createElement('div');
    notification.className = `notification notification-${type}`;
    notification.textContent = message;
    notification.style.cssText = `
        position: fixed;
        top: 20px;
        right: 20px;
        padding: 1rem 1.5rem;
        background: ${type === 'success' ? '#10b981' : type === 'danger' ? '#ef4444' : type === 'warning' ? '#f59e0b' : '#3b82f6'};
        color: white;
        border-radius: 0.75rem;
        box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
        z-index: 10000;
        animation: slideInRight 0.3s ease-out;
    `;

    document.body.appendChild(notification);

    setTimeout(() => {
        notification.style.animation = 'slideOutRight 0.3s ease-out';
        setTimeout(() => {
            document.body.removeChild(notification);
        }, 300);
    }, 3000);
}

// Add notification animations
const style = document.createElement('style');
style.textContent = `
    @keyframes slideInRight {
        from {
            transform: translateX(400px);
            opacity: 0;
        }
        to {
            transform: translateX(0);
            opacity: 1;
        }
    }
    
    @keyframes slideOutRight {
        from {
            transform: translateX(0);
            opacity: 1;
        }
        to {
            transform: translateX(400px);
            opacity: 0;
        }
    }
`;
document.head.appendChild(style);
