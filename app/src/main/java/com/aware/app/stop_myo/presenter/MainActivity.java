package com.aware.app.stop_myo.presenter;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.aware.app.stop_myo.R;
import com.aware.app.stop_myo.model.MyoHandler;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = LayoutInflater.from(this).inflate(R.layout.activity_main, null);
        setContentView(view);

        final MyoHandler myoHandler = new MyoHandler(this, view);

        Button autoconnect = findViewById(R.id.btn_autoconnect);
        Button connect_mac = findViewById(R.id.btn_connect_mac);
        Button disconnect = findViewById(R.id.btn_disconnect);
        final EditText mac = findViewById(R.id.et_mac);

        autoconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myoHandler.connectMyo();
            }
        });

        connect_mac.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myoHandler.connectMacMyo(mac.getText().toString());
            }
        });

        disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                myoHandler.disconnectMyo();
            }
        });


    }
}
