package com.aware.app.stop_myo.views;

import android.content.Context;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aware.app.stop_myo.R;
import com.aware.app.stop_myo.presenter.MainActivity;

public class MainView extends View {

    View view;
    MainActivity activity;

    TextView connection_state;
    LinearLayout onDisconnected_layout;
    LinearLayout onConnected_layout;
    ProgressBar connecting_progress;
    TextView device_name, mac_address, battery_level;

    public MainView(Context context, View v) {
        super(context);

        view = v;
        activity = (MainActivity) context;

        connection_state = view.findViewById(R.id.connection_state);
        onDisconnected_layout = view.findViewById(R.id.onDisconnected_layout);
        onConnected_layout = view.findViewById(R.id.onConnected_layout);
        connecting_progress = view.findViewById(R.id.connecting_progress);
        device_name = view.findViewById(R.id.device_name);
        mac_address = view.findViewById(R.id.mac_address);
        battery_level = view.findViewById(R.id.battery_level);
    }

    public void onScanning() {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onScanning");
                connection_state.setText("SCANNING FOR MYO");

                connecting_progress.setVisibility(VISIBLE);
                onConnected_layout.setVisibility(GONE);
                onDisconnected_layout.setVisibility(GONE);
            }
        }));
    }

    public void onConnecting() {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onConnecting");
                connection_state.setText("CONNECTING TO MYO");

                connecting_progress.setVisibility(VISIBLE);
                onConnected_layout.setVisibility(GONE);
                onDisconnected_layout.setVisibility(GONE);
            }
        }));
    }

    public void onConnected() {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onConnected");
                connection_state.setText("CONNECTED TO MYO");

                onConnected_layout.setVisibility(VISIBLE);
                onDisconnected_layout.setVisibility(GONE);
                connecting_progress.setVisibility(GONE);
            }
        }));
    }

    public void onDisconnected() {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onDisconnected");
                connection_state.setText("DISCONNECTED FROM MYO");

                onDisconnected_layout.setVisibility(VISIBLE);
                onConnected_layout.setVisibility(GONE);
                connecting_progress.setVisibility(GONE);
            }
        }));
    }

    public void onMacWrong() {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onMacWrong");
                connection_state.setText("WRONG MAC FORMAT\nPLEASE TRY AGAIN");

                onDisconnected_layout.setVisibility(VISIBLE);
                onConnected_layout.setVisibility(GONE);
                connecting_progress.setVisibility(GONE);
            }
        }));
    }

    public void onConnectionTimeout() {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onConnectionTimeout");
                connection_state.setText("CONNECTION ERROR\nPLEASE TRY AGAIN");

                onDisconnected_layout.setVisibility(VISIBLE);
                onConnected_layout.setVisibility(GONE);
                connecting_progress.setVisibility(GONE);
            }
        }));
    }

    public void onEmptyAutoscan(){
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onEmptyAutoscan");
                connection_state.setText("NO MYOS HAVE BEEN FOUND\nPLEASE TRY AGAIN");

                onDisconnected_layout.setVisibility(VISIBLE);
                onConnected_layout.setVisibility(GONE);
                connecting_progress.setVisibility(GONE);
            }
        }));
    }

    public void onPropertiesRead(final String name, final String mac) {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onPropertiesRead");
                device_name.setText("Device name: " + name);
                mac_address.setText("MAC: " + mac);
            }
        }));
    }

    public void onBatteryRead(final int level) {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onBatteryRead");
                battery_level.setText("Battery: " + level + "%");
            }
        }));
    }

    public void onLabelAdded(final String label) {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onLabelAdded");
                String str = label + " label added";
                Toast.makeText(activity, str, Toast.LENGTH_SHORT).show();
            }
        }));
    }
}
