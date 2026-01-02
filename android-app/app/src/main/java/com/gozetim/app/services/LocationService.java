package com.gozetim.app.services;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;

import androidx.core.app.ActivityCompat;

import com.gozetim.app.utils.SupabaseHelper;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Konum takibi yapan servis
 */
public class LocationService {

    private Context context;
    private SupabaseHelper supabaseHelper;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Geocoder geocoder;

    public LocationService(Context context, SupabaseHelper supabaseHelper) {
        this.context = context;
        this.supabaseHelper = supabaseHelper;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        this.geocoder = new Geocoder(context, new Locale("tr", "TR"));

        setupLocationCallback();
    }

    /**
     * Konum callback'ini ayarla
     */
    private void setupLocationCallback() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }

                for (Location location : locationResult.getLocations()) {
                    processLocation(location);
                }
            }
        };
    }

    /**
     * Konum güncellemelerini başlat
     */
    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        LocationRequest locationRequest = new LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                15 * 60 * 1000 // 15 dakika
        )
                .setMinUpdateIntervalMillis(10 * 60 * 1000) // Minimum 10 dakika
                .build();

        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                Looper.getMainLooper());
    }

    /**
     * Tek seferlik konum güncelle
     */
    public void updateLocation() {
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        processLocation(location);
                    }
                });
    }

    /**
     * Konumu işle ve Supabase'e gönder
     */
    private void processLocation(Location location) {
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        float accuracy = location.getAccuracy();

        // Adresi al (ters geocoding)
        String address = getAddressFromLocation(latitude, longitude);

        // Supabase'e gönder
        supabaseHelper.updateLocation(latitude, longitude, address, accuracy);
    }

    /**
     * Koordinatlardan adres al
     */
    private String getAddressFromLocation(double latitude, double longitude) {
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);

            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);

                StringBuilder addressString = new StringBuilder();

                // Mahalle/Semt
                if (address.getSubLocality() != null) {
                    addressString.append(address.getSubLocality()).append(", ");
                }

                // İlçe
                if (address.getSubAdminArea() != null) {
                    addressString.append(address.getSubAdminArea()).append(", ");
                }

                // İl
                if (address.getAdminArea() != null) {
                    addressString.append(address.getAdminArea());
                }

                return addressString.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Adres bulunamadı";
    }

    /**
     * Konum güncellemelerini durdur
     */
    public void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
    }
}
