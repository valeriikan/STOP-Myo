package com.aware.app.stop_myo.model;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.view.View;

import com.aware.Aware;
import com.aware.Aware_Preferences;
import com.aware.app.stop_myo.views.MainView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import eu.darken.myolib.BaseMyo;
import eu.darken.myolib.Myo;
import eu.darken.myolib.MyoCmds;
import eu.darken.myolib.MyoConnector;
import eu.darken.myolib.msgs.MyoMsg;
import eu.darken.myolib.processor.emg.EmgData;
import eu.darken.myolib.processor.emg.EmgProcessor;
import eu.darken.myolib.processor.imu.ImuData;
import eu.darken.myolib.processor.imu.ImuProcessor;

public class MyoHandler implements
        BaseMyo.ConnectionListener,
        EmgProcessor.EmgDataListener,
        ImuProcessor.ImuDataListener,
        Myo.BatteryCallback,
        Myo.ReadDeviceNameCallback,
        MyoConnector.ScannerCallback {

    private Context context;
    private MainView mainView;

    public MyoHandler(Context context, View view) {
        this.context = context;
        mainView = new MainView(context, view);
        mainView.onDisconnected();
    }

    // Myo variables
    private MyoConnector connector = null;
    private Myo myo = null;
    private EmgProcessor emgProcessor = null;
    private ImuProcessor imuProcessor = null;
    private boolean connected = false;

    // Bluetooth variables for MAC connection
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bt;

    // JSON objects variables
    private JSONObject myoDataObject;
    private JSONArray accArray, gyroArray, orientArray, emgArray, batteryArray, exerciseArray;

    // Sampling frequency and connection variables
    private static final int SAMPLING_BUFFER_SIZE = 240;                  // maximum buffer size
    private static final int SAMPLING_IMU_FREQUENCY = 500;                // in milliseconds (e.g. once per 500 ms)
    private static final int SAMPLING_EMG_FREQUENCY = 500;                // in milliseconds (e.g. once per 500 ms)
    private static final int SAMPLING_BATTERY_LVL_READ_FREQUENCY = 60000; // in milliseconds (e.g. once per 60 sec)
    private static final int CONNECTION_TIMEOUT = 100000;                 // in milliseconds (e.g. 100 sec)
    private static final int NOTIFICATION_ID = 1;

    // Sampling keys for JSON handling
    private static final String SAMPLE_KEY_TIMESTAMP = "timestamp";
    private static final String SAMPLE_KEY_IMU_X = "x";
    private static final String SAMPLE_KEY_IMU_Y = "y";
    private static final String SAMPLE_KEY_IMU_Z = "z";
    private static final String SAMPLE_KEY_EMG_0 = "emg0";
    private static final String SAMPLE_KEY_EMG_1 = "emg1";
    private static final String SAMPLE_KEY_EMG_2 = "emg2";
    private static final String SAMPLE_KEY_EMG_3 = "emg3";
    private static final String SAMPLE_KEY_EMG_4 = "emg4";
    private static final String SAMPLE_KEY_EMG_5 = "emg5";
    private static final String SAMPLE_KEY_EMG_6 = "emg6";
    private static final String SAMPLE_KEY_EMG_7 = "emg7";
    private static final String SAMPLE_KEY_BATTERY_LEVEL = "battery_level";
    private static final String SAMPLE_KEY_BATTERY = "battery";
    private static final String SAMPLE_KEY_MAC_ADDRESS = "mac";
    private static final String SAMPLE_KEY_LABEL = "label";
    private static final String SAMPLE_KEY_MYO_DATA = "myo_data";
    private static final String SAMPLE_KEY_IMU = "IMU";
    private static final String SAMPLE_KEY_EMG = "EMG";
    private static final String SAMPLE_KEY_ACCELEROMETER = "accelerometer";
    private static final String SAMPLE_KEY_GYROSCOPE = "gyroscope";
    private static final String SAMPLE_KEY_ORIENTATION = "orientation";
    private static final String SAMPLE_KEY_EXERCISE = "exercises";
    private static final String SAMPLE_KEY_EXERCISE_START = "start_timestamp";
    private static final String SAMPLE_KEY_EXERCISE_END = "end_timestamp";


    // Connecting to Myo via autoscanning
    public void connectMyo() {
        if (bluetoothAdapter == null) bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            // Request to activate bluetooth
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBT.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(enableBT);

        } else {
            // Update UI: Scanning
            mainView.onScanning();

            // Try to connect with autoscanning
            // Callback either connects to found myo or offers to try again
            if (connector == null) connector = new MyoConnector(context);
            connector.scan(10000, MyoHandler.this);
        }
    }

    // Connecting to Myo via Mac
    public void connectMacMyo(String mac) {
        if (bluetoothAdapter == null) bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter != null && !bluetoothAdapter.isEnabled()) {
            // Request to activate bluetooth
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            enableBT.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(enableBT);

        } else {
            // Update UI: Connecting
            mainView.onConnecting();

            try {
                // Initialize bluetooth device with retrieved MAC address
                bt = bluetoothAdapter.getRemoteDevice(mac);

                // Connect to it
                myo = new Myo(context, bt);
                myo.addConnectionListener(MyoHandler.this);
                myo.connect();

                // Perform actions if not connected after 100 seconds
                final Handler handler = new Handler();
                final Runnable r = new Runnable() {
                    public void run() {
                        if (myo!=null && !connected) {
                            // UI Update: Connection timeout
                            mainView.onConnectionTimeout();

                            //Removing existing values
                            removeValues();
                        }
                    }
                };
                handler.postDelayed(r, CONNECTION_TIMEOUT);

            } catch (IllegalArgumentException e) {
                // Update UI: Wrong MAC address format
                mainView.onMacWrong();

                //Removing existing values
                removeValues();
            }
        }
    }

    // Disconnecting from Myo
    public void disconnectMyo() {
        if (myo != null) {
            //Applying settings to disconnected Myo
            myo.setConnectionSpeed(BaseMyo.ConnectionSpeed.BALANCED);
            myo.writeSleepMode(MyoCmds.SleepMode.NORMAL, null);
            myo.writeMode(MyoCmds.EmgMode.NONE, MyoCmds.ImuMode.NONE, MyoCmds.ClassifierMode.DISABLED, new Myo.MyoCommandCallback() {
                @Override
                public void onCommandDone(Myo myo, MyoMsg msg) {
                    // Disconnecting from Myo
                    myo.disconnect();

                    //Removing existing values
                    removeValues();

                    // Update UI: Disconnected
                    mainView.onDisconnected();

                    // Insert existing data on disconnect if smth not inserted exists
                    if (accArray.length()!=0 || gyroArray.length()!=0 || orientArray.length()!=0 || emgArray.length()!=0 || exerciseArray.length()!=0) {
                        // Insert data to db; Handler for getting access to AWARE from the Myo callback
                        Handler handler = new Handler(Looper.getMainLooper());
                        Runnable r = new Runnable() {
                            public void run() {
                                //TODO
//                                insertData();
                            }
                        };
                        handler.postDelayed(r, 0);
                    }
                }
            });
        }
    }

    // Inset data to db once the EMG array length equals to variable SAMPLE_BUFFER_SIZE
    private void insertData() {
        try {
            // Building JSON object
            myoDataObject.put(SAMPLE_KEY_BATTERY, batteryArray);

            JSONObject imuObject = new JSONObject();
            imuObject.put(SAMPLE_KEY_ACCELEROMETER, accArray);
            imuObject.put(SAMPLE_KEY_GYROSCOPE, gyroArray);
            imuObject.put(SAMPLE_KEY_ORIENTATION, orientArray);

            final JSONObject result = new JSONObject();
            result.put(SAMPLE_KEY_MYO_DATA, myoDataObject);
            result.put(SAMPLE_KEY_EXERCISE, exerciseArray);
            result.put(SAMPLE_KEY_IMU, imuObject);
            result.put(SAMPLE_KEY_EMG, emgArray);

            // Inserting to db
            ContentValues values = new ContentValues();
            values.put(Provider.Myo_Data.TIMESTAMP, System.currentTimeMillis());
            values.put(Provider.Myo_Data.DEVICE_ID, Aware.getSetting(context, Aware_Preferences.DEVICE_ID));
            values.put(Provider.Myo_Data.DATA, result.toString());
            context.getContentResolver().insert(Provider.Myo_Data.CONTENT_URI, values);

            // Empty sampling arrays for further data collection
            accArray = new JSONArray();
            gyroArray = new JSONArray();
            orientArray = new JSONArray();
            emgArray = new JSONArray();
            batteryArray = new JSONArray();
            exerciseArray = new JSONArray();

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Removing values when Myo is detached
    private void removeValues() {
        if (myo != null) {
            if (emgProcessor != null) {
                myo.removeProcessor(emgProcessor);
                emgProcessor = null;
            }
            if (imuProcessor != null) {
                myo.removeProcessor(imuProcessor);
                imuProcessor = null;
            }
            myo.removeConnectionListener(this);
            myo = null;
        }

        if (connector != null) connector = null;
        if (bluetoothAdapter != null) bluetoothAdapter = null;
        if (bt != null) bt = null;
        if (connected) connected = false;
    }

    //Myo connection state listener
    @Override
    public void onConnectionStateChanged(BaseMyo baseMyo, BaseMyo.ConnectionState state) {

        if (state == BaseMyo.ConnectionState.CONNECTED) {

            /* Applying settings to connected Myo
               First set does not make changes
               Do not remove that, since the second set will not be applied */
            myo.setConnectionSpeed(BaseMyo.ConnectionSpeed.HIGH);
            myo.setConnectionSpeed(BaseMyo.ConnectionSpeed.HIGH);
            myo.writeSleepMode(MyoCmds.SleepMode.NEVER, null);
            myo.writeSleepMode(MyoCmds.SleepMode.NEVER, null);
            myo.writeUnlock(MyoCmds.UnlockType.HOLD, null);
            myo.writeUnlock(MyoCmds.UnlockType.HOLD, null);
            myo.writeMode(MyoCmds.EmgMode.FILTERED, MyoCmds.ImuMode.RAW, MyoCmds.ClassifierMode.DISABLED, null);
            myo.writeMode(MyoCmds.EmgMode.FILTERED, MyoCmds.ImuMode.RAW, MyoCmds.ClassifierMode.DISABLED, null);

            // Second set makes actual changes
            myo.setConnectionSpeed(BaseMyo.ConnectionSpeed.HIGH);
            myo.writeSleepMode(MyoCmds.SleepMode.NEVER, null);
            myo.writeUnlock(MyoCmds.UnlockType.HOLD, new Myo.MyoCommandCallback() {
                @Override
                public void onCommandDone(Myo myo, MyoMsg msg) {
                    myo.writeVibrate(MyoCmds.VibrateType.LONG, null);
                }
            });
            myo.writeMode(MyoCmds.EmgMode.FILTERED, MyoCmds.ImuMode.RAW, MyoCmds.ClassifierMode.DISABLED, new Myo.MyoCommandCallback() {
                @Override
                public void onCommandDone(Myo mMyo, MyoMsg msg) {
                    connected = true;

                    // Setting up Imu and EMG sensors
                    imuProcessor = new ImuProcessor();
                    emgProcessor = new EmgProcessor();
                    imuProcessor.addListener(MyoHandler.this);
                    emgProcessor.addListener(MyoHandler.this);
                    myo.addProcessor(imuProcessor);
                    myo.addProcessor(emgProcessor);

                    // Update UI: Connected
                    mainView.onConnected();

                    // Read device name on connect and record it to JSON object
                    myo.readDeviceName(MyoHandler.this);
                    // TODO
//                    try {
//                        myoDataObject.put(SAMPLE_KEY_MAC_ADDRESS, myo.getDeviceAddress());;
//                    } catch (JSONException e) {
//                        e.printStackTrace();
//                    }
                }
            });
        }

        if (state == BaseMyo.ConnectionState.DISCONNECTED) {
            // Removing existing values
            removeValues();

            // Update UI: Disconnected
            mainView.onDisconnected();

            // Insert existing data on disconnect if smth not inserted exists
            if (accArray.length()!=0 || gyroArray.length()!=0 || orientArray.length()!=0 || emgArray.length()!=0 || exerciseArray.length()!=0) {
//               TODO
//               insertData();
            }
        }

    }

    //Myo battery level listener
    @Override
    public void onBatteryLevelRead(Myo myo, MyoMsg msg, int batteryLevel) {
        mainView.onBatteryRead(batteryLevel);
//        try {
//            // TODO: UI update on BATTERY SAMPLE
////            // Update notification on every battery sampling
////            nBuilder.setContentText(getString(R.string.notification_state_connected_charge_1) + batteryLevel + getString(R.string.notification_state_connected_charge_2))
////                    .setStyle(new NotificationCompat.BigTextStyle().bigText(getString(R.string.notification_state_connected_charge_1) + batteryLevel + getString(R.string.notification_state_connected_charge_2)))
////                    .setProgress(0,0,false)
////                    .mActions.clear();
////            nBuilder.addAction(0, getString(R.string.notification_action_disconnect), disconnectIntent);
////            nBuilder.addAction(0, "Start", startExerciseIntent);
////            nBuilder.addAction(0, "End", endExerciseIntent);
////            startForeground(NOTIFICATION_ID, nBuilder.getNotification());
////            nBuilder.setStyle(null);
//
//            // Record battery level to JSON object
////            JSONObject batteryObj = new JSONObject();
////            batteryObj.put(SAMPLE_KEY_TIMESTAMP, mLastBatteryUpdate);
////            batteryObj.put(SAMPLE_KEY_BATTERY_LEVEL, batteryLevel);
////            batteryArray.put(batteryObj);
////            batteryObj = null;
//
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }

    //Myo device name reader
    @Override
    public void onDeviceNameRead(Myo myo, MyoMsg msg, String deviceName) {
        mainView.onPropertiesRead(deviceName, myo.getDeviceAddress());

        //TODO
//        try {
//            // Insert device name to JSON when read is executed
//            myoDataObject.put(SAMPLE_KEY_LABEL, deviceName);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
    }


    @Override
    public void onNewEmgData(EmgData emgData) {
        // Check for Emg updates twice per second
        if (System.currentTimeMillis() - mLastEmgUpdate > SAMPLING_EMG_FREQUENCY) {
            mLastEmgUpdate = System.currentTimeMillis();

            //TODO
//            try {
//                JSONObject emgObj = new JSONObject();
//                emgObj.put(SAMPLE_KEY_TIMESTAMP, emgData.getTimestamp());
//                emgObj.put(SAMPLE_KEY_EMG_0, emgData.getData()[0]);
//                emgObj.put(SAMPLE_KEY_EMG_1, emgData.getData()[1]);
//                emgObj.put(SAMPLE_KEY_EMG_2, emgData.getData()[2]);
//                emgObj.put(SAMPLE_KEY_EMG_3, emgData.getData()[3]);
//                emgObj.put(SAMPLE_KEY_EMG_4, emgData.getData()[4]);
//                emgObj.put(SAMPLE_KEY_EMG_5, emgData.getData()[5]);
//                emgObj.put(SAMPLE_KEY_EMG_6, emgData.getData()[6]);
//                emgObj.put(SAMPLE_KEY_EMG_7, emgData.getData()[7]);
//
//                emgArray.put(emgObj);
//                emgObj = null;
//
////                if (emgArray.length() == SAMPLING_BUFFER_SIZE) insertData();
//
//            } catch (JSONException e) {
//                e.printStackTrace();;
//            }
        }
    }

    //Myo IMU data (accelerometer, gyroscope, orientation) listener
    private long mLastImuUpdate = 0;
    private long mLastBatteryUpdate = 0;
    @Override
    public void onNewImuData(ImuData imuData) {
        // Check for sensor updates twice per second
        if (System.currentTimeMillis() - mLastImuUpdate > SAMPLING_IMU_FREQUENCY) {
            mLastImuUpdate = System.currentTimeMillis();

            //TODO
//            try {
//                JSONObject accObj = new JSONObject();
//                accObj.put(SAMPLE_KEY_TIMESTAMP, imuData.getTimeStamp())
//                        .put(SAMPLE_KEY_IMU_X, imuData.getAccelerometerData()[0])
//                        .put(SAMPLE_KEY_IMU_Y, imuData.getAccelerometerData()[1])
//                        .put(SAMPLE_KEY_IMU_Z, imuData.getAccelerometerData()[2]);
//                accArray.put(accObj);
//                accObj = null;
//
//                JSONObject gyroObj = new JSONObject();
//                gyroObj.put(SAMPLE_KEY_TIMESTAMP, imuData.getTimeStamp())
//                        .put(SAMPLE_KEY_IMU_X, imuData.getGyroData()[0])
//                        .put(SAMPLE_KEY_IMU_Y, imuData.getGyroData()[1])
//                        .put(SAMPLE_KEY_IMU_Z, imuData.getGyroData()[2]);
//                gyroArray.put(gyroObj);
//                gyroObj = null;
//
//                JSONObject orientObj = new JSONObject();
//                orientObj.put(SAMPLE_KEY_TIMESTAMP, imuData.getTimeStamp())
//                        .put(SAMPLE_KEY_IMU_X, imuData.getOrientationData()[0])
//                        .put(SAMPLE_KEY_IMU_Y, imuData.getOrientationData()[1])
//                        .put(SAMPLE_KEY_IMU_Z, imuData.getOrientationData()[2]);
//                orientArray.put(orientObj);
//                orientObj = null;
//
//            } catch (JSONException e) {
//                e.printStackTrace();
//            }
        }

        // Check for battery level once per minute
        if (System.currentTimeMillis() - mLastBatteryUpdate > SAMPLING_BATTERY_LVL_READ_FREQUENCY) {
            mLastBatteryUpdate = System.currentTimeMillis();
            myo.readBatteryLevel(MyoHandler.this);
        }
    }

    //Myo EMG data listener
    private long mLastEmgUpdate = 0;
    @Override
    public void onScanFinished(List<Myo> myos) {
        // offer either to scan again or to connect straight wih MAC if no Myos were found
        if (myos.size() == 0) {
            //Remove already existing values
            removeValues();

            // Update UI: No Myos found on scanning
            mainView.onEmptyAutoscan();

        } else {
            // Update UI: Connecting
            mainView.onConnecting();

            // Connect to found Myo
            myo = myos.get(0);
            myo.addConnectionListener(MyoHandler.this);
            myo.connect();

            // Remove values and set notification to default state if not connected after 100 seconds
            final Handler handler = new Handler(Looper.getMainLooper());
            final Runnable r = new Runnable() {
                public void run() {
                    if (myo!=null && !connected) {
                        // Update UI: Connection timetout
                        mainView.onConnectionTimeout();

                        //Removing existing values
                        removeValues();
                    }
                }
            };
            handler.postDelayed(r, CONNECTION_TIMEOUT);
        }
    }
}
