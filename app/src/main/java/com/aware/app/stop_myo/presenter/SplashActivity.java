package com.aware.app.stop_myo.presenter;

import android.Manifest;
import android.accounts.Account;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncRequest;
import android.os.Bundle;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.app.stop_myo.model.Provider;
import com.aware.ui.PermissionsHandler;

import java.util.ArrayList;

public class SplashActivity extends AppCompatActivity {

    public static final String STUDY_URL = "https://api.awareframework.com/index.php/webservice/index/2292/lzntsFObLz62";
    private static final String PACKAGE_NAME = "com.aware.app.stop_myo";

    @Override
    protected void onResume() {
        super.onResume();

        // List of required permission
        ArrayList<String> REQUIRED_PERMISSIONS = new ArrayList<>();
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        REQUIRED_PERMISSIONS.add(Manifest.permission.WRITE_SYNC_SETTINGS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.READ_SYNC_SETTINGS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.INTERNET);
        REQUIRED_PERMISSIONS.add(Manifest.permission.GET_ACCOUNTS);
        REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH);
        REQUIRED_PERMISSIONS.add(Manifest.permission.BLUETOOTH_ADMIN);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);

        // flag to check permissions
        boolean permissions_ok = true;
        for (String p : REQUIRED_PERMISSIONS) {
            if (PermissionChecker.checkSelfPermission(this, p) != PermissionChecker.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }

        // 1st: Check for permissions
        if (permissions_ok) {
            Intent aware = new Intent(getApplicationContext(), Aware.class);
            startService(aware);

            if (Aware.isStudy(this)) {
                // Open MainActivity when all conditions are ok
                Intent main = new Intent(this, MainActivity.class);
                startActivity(main);
                finish();

            } else {
                Aware.joinStudy(this, STUDY_URL);
                IntentFilter joinFilter = new IntentFilter(Aware.ACTION_JOINED_STUDY);
                registerReceiver(joinObserver, joinFilter);
            }

        } else {

            Intent permissions = new Intent(this, PermissionsHandler.class);
            permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(permissions);
        }
    }

    // Receiver waiting for study joined state
    private JoinObserver joinObserver = new JoinObserver();
    private class JoinObserver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase(Aware.ACTION_JOINED_STUDY)) {

                Account aware_account = Aware.getAWAREAccount(getApplicationContext());
                String authority = Provider.getAuthority(getApplicationContext());
                long frequency = Long.parseLong(Aware.getSetting(getApplicationContext(), Aware_Preferences.FREQUENCY_WEBSERVICE)) * 60;
                ContentResolver.setIsSyncable(aware_account, authority, 1);
                ContentResolver.setSyncAutomatically(aware_account, authority, true);
                SyncRequest request = new SyncRequest.Builder()
                        .syncPeriodic(frequency, frequency / 3)
                        .setSyncAdapter(aware_account, authority)
                        .setExtras(new Bundle()).build();
                ContentResolver.requestSync(request);

                // Setting up Aware preferences
                Aware.isBatteryOptimizationIgnored(getApplicationContext(), PACKAGE_NAME);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.DEBUG_DB_SLOW, true);
                Aware.setSetting(getApplicationContext(), Aware_Preferences.WEBSERVICE_SILENT, true);

                // Open MainActivity when all conditions are ok
                Intent main = new Intent(getApplicationContext(), MainActivity.class);
                startActivity(main);
                unregisterReceiver(joinObserver);
                finish();
            }
        }
    }
}