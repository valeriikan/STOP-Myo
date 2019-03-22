package com.aware.app.stop_myo.presenter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.aware.app.stop_myo.model.MyoHandler;
import com.aware.app.stop_myo.views.MainView;

import eu.darken.myolib.BaseMyo;

public class MainActivity extends AppCompatActivity {

    MainView mainView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize Myo handling service
        startService(new Intent(this, MyoHandler.class));

        // Registering broadcast receiver to update UI
        IntentFilter filter = new IntentFilter(MyoHandler.MYO_INTENT);
        registerReceiver(receiver, filter);

        // Initialize UI
        MyoHandler myoHandler = new MyoHandler(this);
        mainView = new MainView(this, myoHandler);

        // Get the broadcast telling the current state of connection
        myoHandler.getConnectionState();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(receiver);
    }

    // Receiver for updating UI based on Myo connection state
    public BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            // Library-based connection states are used only in activity's onCreate
            if (intent.getExtras().get(MyoHandler.MYO_CONNECTION_STATE)
                    .equals(BaseMyo.ConnectionState.CONNECTING)) mainView.onConnecting();

            if (intent.getExtras().get(MyoHandler.MYO_CONNECTION_STATE)
                    .equals(BaseMyo.ConnectionState.CONNECTED)) mainView.onConnected();

            if (intent.getExtras().get(MyoHandler.MYO_CONNECTION_STATE)
                    .equals(BaseMyo.ConnectionState.DISCONNECTING)) mainView.onDisconnecting();

            if (intent.getExtras().get(MyoHandler.MYO_CONNECTION_STATE)
                    .equals(BaseMyo.ConnectionState.DISCONNECTED)) mainView.onDisconnected();


            // Own connection state covers other regular functionalities
            if (intent.getExtras().get(MyoHandler.MYO_CONNECTION_STATE)
                    .equals(MyoHandler.STATE_SCANNING)) mainView.onScanning();

            if (intent.getExtras().get(MyoHandler.MYO_CONNECTION_STATE)
                    .equals(MyoHandler.STATE_CONNECTING)) mainView.onConnecting();

            if (intent.getExtras().get(MyoHandler.MYO_CONNECTION_STATE)
                    .equals(MyoHandler.STATE_CONNECTED)) mainView.onConnected();

            if (intent.getExtras().get(MyoHandler.MYO_CONNECTION_STATE)
                    .equals(MyoHandler.STATE_DISCONNECTED)) mainView.onDisconnected();

            if (intent.getExtras().get(MyoHandler.MYO_CONNECTION_STATE)
                    .equals(MyoHandler.STATE_SLEEP_MODE)) mainView.onSleepMode();

            if (intent.getExtras().get(MyoHandler.MYO_CONNECTION_STATE)
                    .equals(MyoHandler.STATE_MAC_WRONG)) mainView.onMacWrong();

            if (intent.getExtras().get(MyoHandler.MYO_CONNECTION_STATE)
                    .equals(MyoHandler.STATE_CONNECTION_TIMEOUT)) mainView.onConnectionTimeout();

            if (intent.getExtras().get(MyoHandler.MYO_CONNECTION_STATE)
                    .equals(MyoHandler.STATE_EMPTY_AUTOSCAN)) mainView.onEmptyAutoscan();

            if (intent.getExtras().get(MyoHandler.MYO_CONNECTION_STATE)
                    .equals(MyoHandler.STATE_PROPERTIES_READ)) {
                String name = (String) intent.getExtras().get(MyoHandler.MYO_PROPERTIES_NAME);
                String mac = (String) intent.getExtras().get(MyoHandler.MYO_PROPERTIES_MAC);
                mainView.onPropertiesRead(name, mac);
            }

            if (intent.getExtras().get(MyoHandler.MYO_CONNECTION_STATE)
                    .equals(MyoHandler.STATE_BATTERY_READ)) {
                int level = (int) intent.getExtras().get(MyoHandler.MYO_BATTERY);
                mainView.onBatteryRead(level);
            }

            if (intent.getExtras().get(MyoHandler.MYO_CONNECTION_STATE)
                    .equals(MyoHandler.STATE_LABEL_ADDED)) {
                String label = (String) intent.getExtras().get(MyoHandler.MYO_LABEL);
                mainView.onLabelAdded(label);
            }
        }
    };
}
