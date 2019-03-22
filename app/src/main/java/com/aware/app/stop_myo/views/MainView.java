package com.aware.app.stop_myo.views;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.aware.app.stop_myo.R;
import com.aware.app.stop_myo.model.MyoHandler;
import com.aware.app.stop_myo.presenter.MainActivity;


public class MainView extends View {

    MainActivity activity;

    LinearLayout layout_on_disconnected, layout_on_connected;
    ProgressBar progress_bar_connecting;
    TextView tv_connection_state, tv_device_name, tv_mac_address, tv_battery_level;
    EditText et_mac, et_custom_label;
    Button btn_autoconnect, btn_connect_mac, btn_disconnect, btn_start_label, btn_end_label, btn_custom_label;

    public MainView(Context context, final MyoHandler myoHandler) {
        super(context);

        activity = (MainActivity) context;
        View view = LayoutInflater.from(activity).inflate(R.layout.activity_main, null);
        activity.setContentView(view);

        // Initializing views
        layout_on_disconnected = view.findViewById(R.id.layout_on_disconnected);
        layout_on_connected = view.findViewById(R.id.layout_on_connected);
        tv_connection_state = view.findViewById(R.id.tv_connection_state);
        tv_device_name = view.findViewById(R.id.tv_device_name);
        tv_mac_address = view.findViewById(R.id.tv_mac_address);
        tv_battery_level = view.findViewById(R.id.tv_battery_level);
        progress_bar_connecting = view.findViewById(R.id.progress_bar_connecting);
        et_mac = view.findViewById(R.id.et_mac);
        et_custom_label = view.findViewById(R.id.et_custom_label);
        btn_autoconnect = view.findViewById(R.id.btn_autoconnect);
        btn_connect_mac = view.findViewById(R.id.btn_connect_mac);
        btn_disconnect = view.findViewById(R.id.btn_disconnect);
        btn_start_label = view.findViewById(R.id.btn_start_label);
        btn_end_label = view.findViewById(R.id.btn_end_label);
        btn_custom_label = view.findViewById(R.id.btn_custom_label);

        btn_autoconnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                myoHandler.connectMyo();
            }
        });

        btn_connect_mac.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                myoHandler.connectMacMyo(et_mac.getText().toString());
            }
        });

        btn_disconnect.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                myoHandler.disconnectMyo();
            }
        });

        btn_start_label.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                myoHandler.addLabel(MyoHandler.SAMPLE_KEY_LABEL_START);

            }
        });

        btn_end_label.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                myoHandler.addLabel(MyoHandler.SAMPLE_KEY_LABEL_END);
            }
        });

        btn_custom_label.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                myoHandler.addLabel(et_custom_label.getText().toString());
            }
        });

        tv_battery_level.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                myoHandler.goSleepMode();
                return true;
            }
        });
    }


    // Methods to update UI based on Myo connection state
    public void onScanning() {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onScanning");
                tv_connection_state.setText("SCANNING FOR MYO");

                progress_bar_connecting.setVisibility(VISIBLE);
                layout_on_connected.setVisibility(GONE);
                layout_on_disconnected.setVisibility(GONE);
            }
        }));
    }

    public void onConnecting() {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onConnecting");
                tv_connection_state.setText("CONNECTING TO MYO");

                progress_bar_connecting.setVisibility(VISIBLE);
                layout_on_connected.setVisibility(GONE);
                layout_on_disconnected.setVisibility(GONE);
            }
        }));
    }

    public void onConnected() {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onConnected");
                tv_connection_state.setText("CONNECTED TO MYO");

                layout_on_connected.setVisibility(VISIBLE);
                layout_on_disconnected.setVisibility(GONE);
                progress_bar_connecting.setVisibility(GONE);
            }
        }));
    }

    public void onDisconnecting() {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onDisconnecting");
                tv_connection_state.setText("DISCONNECTING FROM MYO");

                progress_bar_connecting.setVisibility(VISIBLE);
                layout_on_connected.setVisibility(GONE);
                layout_on_disconnected.setVisibility(GONE);
            }
        }));
    }

    public void onDisconnected() {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onDisconnected");
                tv_connection_state.setText("DISCONNECTED FROM MYO");

                layout_on_disconnected.setVisibility(VISIBLE);
                layout_on_connected.setVisibility(GONE);
                progress_bar_connecting.setVisibility(GONE);
            }
        }));
    }

    public void onSleepMode() {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onDisconnecting");
                tv_connection_state.setText("SLEEP MODE ACTIVATE\nDISCONNECTED FROM MYO");

                layout_on_disconnected.setVisibility(VISIBLE);
                layout_on_connected.setVisibility(GONE);
                progress_bar_connecting.setVisibility(GONE);
            }
        }));
    }

    public void onMacWrong() {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onMacWrong");
                tv_connection_state.setText("WRONG MAC FORMAT\nPLEASE TRY AGAIN");

                layout_on_disconnected.setVisibility(VISIBLE);
                layout_on_connected.setVisibility(GONE);
                progress_bar_connecting.setVisibility(GONE);
            }
        }));
    }

    public void onConnectionTimeout() {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onConnectionTimeout");
                tv_connection_state.setText("CONNECTION ERROR\nPLEASE TRY AGAIN");

                layout_on_disconnected.setVisibility(VISIBLE);
                layout_on_connected.setVisibility(GONE);
                progress_bar_connecting.setVisibility(GONE);
            }
        }));
    }

    public void onEmptyAutoscan(){
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onEmptyAutoscan");
                tv_connection_state.setText("NO MYOS HAVE BEEN FOUND\nPLEASE TRY AGAIN");

                layout_on_disconnected.setVisibility(VISIBLE);
                layout_on_connected.setVisibility(GONE);
                progress_bar_connecting.setVisibility(GONE);
            }
        }));
    }

    public void onPropertiesRead(final String name, final String mac) {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onPropertiesRead");
                tv_device_name.setText("Device name: " + name);
                tv_mac_address.setText("MAC: " + mac);
            }
        }));
    }

    public void onBatteryRead(final int level) {
        activity.runOnUiThread((new Runnable() {
            @Override
            public void run() {
                Log.d("STOP_TAG", "onBatteryRead");
                tv_battery_level.setText("Battery: " + level + "%");
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
