package com.aware.app.stop_myo.model;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import com.aware.Aware;
import com.aware.Aware_Preferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import androidx.annotation.Nullable;
import eu.darken.myolib.BaseMyo;
import eu.darken.myolib.Myo;
import eu.darken.myolib.MyoCmds;
import eu.darken.myolib.MyoConnector;
import eu.darken.myolib.msgs.MyoMsg;
import eu.darken.myolib.processor.emg.EmgData;
import eu.darken.myolib.processor.emg.EmgProcessor;
import eu.darken.myolib.processor.imu.ImuData;
import eu.darken.myolib.processor.imu.ImuProcessor;

public class MyoHandler extends Service implements
        MyoConnector.ScannerCallback,
        BaseMyo.ConnectionListener,
        Myo.ReadDeviceNameCallback,
        Myo.BatteryCallback,
        EmgProcessor.EmgDataListener,
        ImuProcessor.ImuDataListener {

    private Context context;

    public MyoHandler() {
    }

    public MyoHandler(Context context) {
        this.context = context;
    }

    // Myo variables
    private Myo myo;
    private MyoConnector connector;
    private EmgProcessor emgProcessor;
    private ImuProcessor imuProcessor;
    private boolean connected;

    // Bluetooth variables for MAC connection
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothDevice bt;

    // JSON objects variables
    private JSONObject myoDataObject;
    private JSONArray accArray, gyroArray, orientArray, emgArray, batteryArray, labelsArray;
    private long first_insert;

    // Sampling frequency and connection variables
    private static final int SAMPLING_BUFFER_SIZE = 600;                  // maximum buffer size (e.g. 600 for 5 mins of sampling)
    private static final int SAMPLING_IMU_FREQUENCY = 500;                // in milliseconds (e.g. once per 500 ms)
    private static final int SAMPLING_EMG_FREQUENCY = 500;                // in milliseconds (e.g. once per 500 ms)
    private static final int SAMPLING_BATTERY_LVL_READ_FREQUENCY = 60000; // in milliseconds (e.g. once per 60 sec)
    private static final int CONNECTION_TIMEOUT = 100000;                 // in milliseconds (e.g. 100 sec)

    // Sampling keys for JSON handling
    private static final String SAMPLE_KEY_TIMESTAMP = "timestamp";
    private static final String SAMPLE_KEY_MYO_DATA = "myo_data";
    private static final String SAMPLE_KEY_MAC_ADDRESS = "mac";
    private static final String SAMPLE_KEY_DEVICE_NAME = "device_name";
    private static final String SAMPLE_KEY_BATTERY = "battery";
    private static final String SAMPLE_KEY_BATTERY_LEVEL = "battery_level";
    private static final String SAMPLE_KEY_EXTENSION_OF = "extension_of";
    private static final String SAMPLE_KEY_LABELS = "labels";
    private static final String SAMPLE_KEY_LABEL = "label";
    public static final String SAMPLE_KEY_LABEL_START = "start";
    public static final String SAMPLE_KEY_LABEL_END = "end";
    private static final String SAMPLE_KEY_IMU = "IMU";
    private static final String SAMPLE_KEY_ACCELEROMETER = "accelerometer";
    private static final String SAMPLE_KEY_GYROSCOPE = "gyroscope";
    private static final String SAMPLE_KEY_ORIENTATION = "orientation";
    private static final String SAMPLE_KEY_IMU_X = "x";
    private static final String SAMPLE_KEY_IMU_Y = "y";
    private static final String SAMPLE_KEY_IMU_Z = "z";
    private static final String SAMPLE_KEY_EMG = "EMG";
    private static final String SAMPLE_KEY_EMG_0 = "emg0";
    private static final String SAMPLE_KEY_EMG_1 = "emg1";
    private static final String SAMPLE_KEY_EMG_2 = "emg2";
    private static final String SAMPLE_KEY_EMG_3 = "emg3";
    private static final String SAMPLE_KEY_EMG_4 = "emg4";
    private static final String SAMPLE_KEY_EMG_5 = "emg5";
    private static final String SAMPLE_KEY_EMG_6 = "emg6";
    private static final String SAMPLE_KEY_EMG_7 = "emg7";

    // Broadcast receiver flags
    public static final String MYO_INTENT = "MYO_INTENT";
    public static final String MYO_CONNECTION_STATE = "MYO_CONNECTION_STATE";
    public static final String MYO_LABEL = "MYO_LABEL";
    public static final String MYO_PROPERTIES_MAC = "MYO_PROPERTIES_MAC";
    public static final String MYO_PROPERTIES_NAME = "MYO_PROPERTIES_NAME";
    public static final String MYO_BATTERY = "MYO_BATTERY";
    public final static String STATE_SCANNING = "STATE_SCANNING";
    public final static String STATE_CONNECTING = "STATE_CONNECTING";
    public final static String STATE_CONNECTED = "STATE_CONNECTED";
    public final static String STATE_DISCONNECTED = "STATE_DISCONNECTED";
    public final static String STATE_SLEEP_MODE = "STATE_SLEEP_MODE";
    public final static String STATE_MAC_WRONG = "STATE_MAC_WRONG";
    public final static String STATE_CONNECTION_TIMEOUT = "STATE_CONNECTION_TIMEOUT";
    public final static String STATE_EMPTY_AUTOSCAN = "STATE_EMPTY_AUTOSCAN";
    public final static String STATE_PROPERTIES_READ = "STATE_PROPERTIES_READ";
    public final static String STATE_BATTERY_READ = "STATE_BATTERY_READ";
    public final static String STATE_LABEL_ADDED = "STATE_LABEL_ADDED";


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        connector = null;
        myo = null;
        emgProcessor = null;
        imuProcessor = null;
        connected = false;

        // TODO:
//        myoDataObject = new JSONObject();
//        accArray = new JSONArray();
//        gyroArray = new JSONArray();
//        orientArray = new JSONArray();
//        emgArray = new JSONArray();
//        batteryArray = new JSONArray();
//        labelsArray = new JSONArray();
//        first_insert = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Insert existing data on disconnect if smth not inserted exists
        if (accArray.length()!=0 || gyroArray.length()!=0 || orientArray.length()!=0 || emgArray.length()!=0 || labelsArray.length()!=0) {
            insertData();
        }

        // Remove all the existing values
        removeValues();
        myoDataObject = null;
        accArray = null;
        gyroArray = null;
        orientArray = null;
        emgArray = null;
        batteryArray = null;
        labelsArray = null;
    }

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
            Intent scanning = new Intent(MYO_INTENT);
            scanning.putExtra(MYO_CONNECTION_STATE, STATE_SCANNING);
            context.sendBroadcast(scanning);

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
            Intent connecting = new Intent(MYO_INTENT);
            connecting.putExtra(MYO_CONNECTION_STATE, STATE_CONNECTING);
            context.sendBroadcast(connecting);

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
                            Intent connection_timeout = new Intent(MYO_INTENT);
                            connection_timeout.putExtra(MYO_CONNECTION_STATE, STATE_CONNECTION_TIMEOUT);
                            context.sendBroadcast(connection_timeout);

                            //Removing existing values
                            removeValues();
                        }
                    }
                };
                handler.postDelayed(r, CONNECTION_TIMEOUT);

            } catch (IllegalArgumentException e) {
                // Update UI: Wrong MAC address format
                Intent mac_wrong = new Intent(MYO_INTENT);
                mac_wrong.putExtra(MYO_CONNECTION_STATE, STATE_MAC_WRONG);
                context.sendBroadcast(mac_wrong);

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
                    Intent disconnected = new Intent(MYO_INTENT);
                    disconnected.putExtra(MYO_CONNECTION_STATE, STATE_DISCONNECTED);
                    context.sendBroadcast(disconnected);

                    // Insert existing data on disconnect if smth not inserted exists
                    if (accArray.length()!=0 || gyroArray.length()!=0 || orientArray.length()!=0 || emgArray.length()!=0 || labelsArray.length()!=0) {
                        insertData();
                    }
                }
            });
        }
    }

    // Sends the broadcast to UI about the current connection state
    public void getConnectionState() {
        BaseMyo.ConnectionState state;
        if (myo != null) {
            state = myo.getConnectionState();
        } else {
            state = BaseMyo.ConnectionState.DISCONNECTED;
        }

        Intent connection_state = new Intent(MYO_INTENT);
        connection_state.putExtra(MYO_CONNECTION_STATE, state);
        context.sendBroadcast(connection_state);
    }

    // Sets Myo to deep sleep mode and disconnects
    public void goSleepMode() {
        if (myo != null) {
            myo.writeDeepSleep(null);

            // Removing existing values
            removeValues();

            // Update UI: Disconnected
            Intent sleep_mode = new Intent(MYO_INTENT);
            sleep_mode.putExtra(MYO_CONNECTION_STATE, STATE_SLEEP_MODE);
            context.sendBroadcast(sleep_mode);

            // Insert existing data on disconnect if smth not inserted exists
            if (accArray.length()!=0 || gyroArray.length()!=0 || orientArray.length()!=0 || emgArray.length()!=0 || labelsArray.length()!=0) {
                insertData();
            }
        }
    }

    // Record label during sampling
    public void addLabel(String label) {
        try {
            // JSON: record label object
            JSONObject labelObj = new JSONObject();
            labelObj.put(SAMPLE_KEY_TIMESTAMP, System.currentTimeMillis());
            labelObj.put(SAMPLE_KEY_LABEL, label);
            labelsArray.put(labelObj);
            labelObj = null;

            // Update UI: Label recorded
            Intent label_added = new Intent(MYO_INTENT);
            label_added.putExtra(MYO_CONNECTION_STATE, STATE_LABEL_ADDED);
            label_added.putExtra(MYO_LABEL, label);
            context.sendBroadcast(label_added);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // Insert data to db once the EMG array length equals to variable SAMPLE_BUFFER_SIZE
    // or when the disconnect is called
    private synchronized void insertData() {
        // Handler for getting access to AWARE from the separate threads
        Handler handler = new Handler(Looper.getMainLooper());
        Runnable r = new Runnable() {
            public void run() {
                try {
            /*
             With the battery-friendly purpose, the buffer size is set for 600 by default (5 mins of sampling).
             Every 5 mins the data is recorded to db. If the record is the continuation of sampling (2nd entry, etc.),
             it will have the JSON Object "extension_of" that refers to very first session entry.
             */
                    ContentValues values = new ContentValues();
                    if (first_insert == 0) {
                        first_insert = System.currentTimeMillis();
                        values.put(Provider.Myo_Data.TIMESTAMP, first_insert);
                    } else {
                        values.put(Provider.Myo_Data.TIMESTAMP, System.currentTimeMillis());
                        myoDataObject.put(SAMPLE_KEY_EXTENSION_OF, first_insert);
                    }

                    // JSON: Building a result object
                    myoDataObject.put(SAMPLE_KEY_BATTERY, batteryArray);

                    JSONObject imuObject = new JSONObject();
                    imuObject.put(SAMPLE_KEY_ACCELEROMETER, accArray);
                    imuObject.put(SAMPLE_KEY_GYROSCOPE, gyroArray);
                    imuObject.put(SAMPLE_KEY_ORIENTATION, orientArray);

                    final JSONObject result = new JSONObject();
                    result.put(SAMPLE_KEY_MYO_DATA, myoDataObject);
                    result.put(SAMPLE_KEY_LABELS, labelsArray);
                    result.put(SAMPLE_KEY_IMU, imuObject);
                    result.put(SAMPLE_KEY_EMG, emgArray);

                    // JSON: Inserting result object to db
                    values.put(Provider.Myo_Data.DEVICE_ID, Aware.getSetting(context, Aware_Preferences.DEVICE_ID));
                    values.put(Provider.Myo_Data.DATA, result.toString());
                    context.getContentResolver().insert(Provider.Myo_Data.CONTENT_URI, values);

                    // Empty sampling arrays for further data collection
                    accArray = new JSONArray();
                    gyroArray = new JSONArray();
                    orientArray = new JSONArray();
                    emgArray = new JSONArray();
                    batteryArray = new JSONArray();
                    labelsArray = new JSONArray();

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        handler.postDelayed(r, 0);
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

        first_insert = 0;
    }

    // Myo autoscanner callback
    @Override
    public void onScanFinished(List<Myo> myos) {
        if (myos.size() == 0) {
            //Remove already existing values
            removeValues();

            // Update UI: No Myos found on autoscanning
            Intent empty_autoscan = new Intent(MYO_INTENT);
            empty_autoscan.putExtra(MYO_CONNECTION_STATE, STATE_EMPTY_AUTOSCAN);
            context.sendBroadcast(empty_autoscan);

        } else {
            // Update UI: Connecting
            Intent connecting = new Intent(MYO_INTENT);
            connecting.putExtra(MYO_CONNECTION_STATE, STATE_CONNECTING);
            context.sendBroadcast(connecting);

            // Connect to found Myo
            myo = myos.get(0);
            myo.addConnectionListener(MyoHandler.this);
            myo.connect();

            // Remove values and reset UI if not connected after 100 seconds
            final Handler handler = new Handler(Looper.getMainLooper());
            final Runnable r = new Runnable() {
                public void run() {
                    if (myo!=null && !connected) {
                        // Update UI: Connection timetout
                        Intent connection_timeout = new Intent(MYO_INTENT);
                        connection_timeout.putExtra(MYO_CONNECTION_STATE, STATE_CONNECTION_TIMEOUT);
                        context.sendBroadcast(connection_timeout);

                        //Removing existing values
                        removeValues();
                    }
                }
            };
            handler.postDelayed(r, CONNECTION_TIMEOUT);
        }
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
                    // Update UI: Connected
                    Intent connected = new Intent(MYO_INTENT);
                    connected.putExtra(MYO_CONNECTION_STATE, STATE_CONNECTED);
                    context.sendBroadcast(connected);

                    // TODO:
                    myoDataObject = new JSONObject();
                    accArray = new JSONArray();
                    gyroArray = new JSONArray();
                    orientArray = new JSONArray();
                    emgArray = new JSONArray();
                    batteryArray = new JSONArray();
                    labelsArray = new JSONArray();
                    first_insert = 0;

                    // Setting up Imu and EMG sensors
                    imuProcessor = new ImuProcessor();
                    emgProcessor = new EmgProcessor();
                    imuProcessor.addListener(MyoHandler.this);
                    emgProcessor.addListener(MyoHandler.this);
                    myo.addProcessor(imuProcessor);
                    myo.addProcessor(emgProcessor);

                    // Read device name on connect and record it to JSON object
                    myo.readDeviceName(MyoHandler.this);
                }
            });
        }

        if (state == BaseMyo.ConnectionState.DISCONNECTED) {
            // Removing existing values
            removeValues();

            // Update UI: Disconnected
            Intent disconnected = new Intent(MYO_INTENT);
            disconnected.putExtra(MYO_CONNECTION_STATE, STATE_DISCONNECTED);
            context.sendBroadcast(disconnected);

            // Insert existing data on disconnect if smth not inserted exists
            if (accArray.length()!=0 || gyroArray.length()!=0 || orientArray.length()!=0 || emgArray.length()!=0 || labelsArray.length()!=0) {
               insertData();
            }
        }

    }

    //Myo device name reader
    @Override
    public void onDeviceNameRead(Myo myo, MyoMsg msg, String deviceName) {
        // Update UI: Device name and mac properties read
        Intent properties_read = new Intent(MYO_INTENT);
        properties_read.putExtra(MYO_CONNECTION_STATE, STATE_PROPERTIES_READ);
        properties_read.putExtra(MYO_PROPERTIES_NAME, deviceName);
        properties_read.putExtra(MYO_PROPERTIES_MAC, myo.getDeviceAddress());
        context.sendBroadcast(properties_read);

        // JSON: record device properties
        try {
            myoDataObject.put(SAMPLE_KEY_DEVICE_NAME, deviceName);
            myoDataObject.put(SAMPLE_KEY_MAC_ADDRESS, myo.getDeviceAddress());
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //Myo battery level listener
    @Override
    public void onBatteryLevelRead(Myo myo, MyoMsg msg, int batteryLevel) {
        // Update UI: Battery level read
        Intent battery_read = new Intent(MYO_INTENT);
        battery_read.putExtra(MYO_CONNECTION_STATE, STATE_BATTERY_READ);
        battery_read.putExtra(MYO_BATTERY, batteryLevel);
        context.sendBroadcast(battery_read);

        // JSON: record battery level
        try {
            JSONObject batteryObj = new JSONObject();
            batteryObj.put(SAMPLE_KEY_TIMESTAMP, mLastBatteryUpdate);
            batteryObj.put(SAMPLE_KEY_BATTERY_LEVEL, batteryLevel);
            batteryArray.put(batteryObj);
            batteryObj = null;

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    // EMG data listener
    private long mLastEmgUpdate = 0;
    @Override
    public void onNewEmgData(EmgData emgData) {
        // Check for EMG updates twice per second
        if (System.currentTimeMillis() - mLastEmgUpdate > SAMPLING_EMG_FREQUENCY) {
            mLastEmgUpdate = System.currentTimeMillis();

            // JSON: record EMG sample
            try {
                JSONObject emgObj = new JSONObject();
                emgObj.put(SAMPLE_KEY_TIMESTAMP, emgData.getTimestamp());
                emgObj.put(SAMPLE_KEY_EMG_0, emgData.getData()[0]);
                emgObj.put(SAMPLE_KEY_EMG_1, emgData.getData()[1]);
                emgObj.put(SAMPLE_KEY_EMG_2, emgData.getData()[2]);
                emgObj.put(SAMPLE_KEY_EMG_3, emgData.getData()[3]);
                emgObj.put(SAMPLE_KEY_EMG_4, emgData.getData()[4]);
                emgObj.put(SAMPLE_KEY_EMG_5, emgData.getData()[5]);
                emgObj.put(SAMPLE_KEY_EMG_6, emgData.getData()[6]);
                emgObj.put(SAMPLE_KEY_EMG_7, emgData.getData()[7]);
                emgArray.put(emgObj);
                emgObj = null;

                if (emgArray.length() == SAMPLING_BUFFER_SIZE) insertData();

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    // IMU data (accelerometer, gyroscope, orientation) listener
    private long mLastImuUpdate = 0;
    private long mLastBatteryUpdate = 0;
    @Override
    public void onNewImuData(ImuData imuData) {
        // Check for sensor updates twice per second
        if (System.currentTimeMillis() - mLastImuUpdate > SAMPLING_IMU_FREQUENCY) {
            mLastImuUpdate = System.currentTimeMillis();

            // JSON: record IMU sample
            try {
                JSONObject accObj = new JSONObject();
                accObj.put(SAMPLE_KEY_TIMESTAMP, imuData.getTimeStamp())
                        .put(SAMPLE_KEY_IMU_X, imuData.getAccelerometerData()[0])
                        .put(SAMPLE_KEY_IMU_Y, imuData.getAccelerometerData()[1])
                        .put(SAMPLE_KEY_IMU_Z, imuData.getAccelerometerData()[2]);
                accArray.put(accObj);
                accObj = null;

                JSONObject gyroObj = new JSONObject();
                gyroObj.put(SAMPLE_KEY_TIMESTAMP, imuData.getTimeStamp())
                        .put(SAMPLE_KEY_IMU_X, imuData.getGyroData()[0])
                        .put(SAMPLE_KEY_IMU_Y, imuData.getGyroData()[1])
                        .put(SAMPLE_KEY_IMU_Z, imuData.getGyroData()[2]);
                gyroArray.put(gyroObj);
                gyroObj = null;

                JSONObject orientObj = new JSONObject();
                orientObj.put(SAMPLE_KEY_TIMESTAMP, imuData.getTimeStamp())
                        .put(SAMPLE_KEY_IMU_X, imuData.getOrientationData()[0])
                        .put(SAMPLE_KEY_IMU_Y, imuData.getOrientationData()[1])
                        .put(SAMPLE_KEY_IMU_Z, imuData.getOrientationData()[2]);
                orientArray.put(orientObj);
                orientObj = null;

            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        // Check for battery level once per minute
        if (System.currentTimeMillis() - mLastBatteryUpdate > SAMPLING_BATTERY_LVL_READ_FREQUENCY) {
            mLastBatteryUpdate = System.currentTimeMillis();
            myo.readBatteryLevel(MyoHandler.this);
        }
    }
}
