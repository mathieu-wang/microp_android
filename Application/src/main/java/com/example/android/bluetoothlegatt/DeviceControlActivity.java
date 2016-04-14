/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.bluetoothlegatt;

import android.app.Activity;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * For a given BLE device, this Activity provides the user interface to connect, display data,
 * and display GATT services and characteristics supported by the device.  The Activity
 * communicates with {@code BluetoothLeService}, which in turn interacts with the
 * Bluetooth LE API.
 */
public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private Map<String, Map<String, BluetoothGattCharacteristic>> mGattCharacteristics
            = new HashMap<>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private double lastTemp;
    private double lastRoll;
    private double lastPitch;

    private boolean setDoubleTap;

    // Code to manage Service lifecycle.
    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            // Automatically connects to the device upon successful start-up initialization.
            mBluetoothLeService.connect(mDeviceAddress);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    // Handles various events fired by the Service.
    // ACTION_GATT_CONNECTED: connected to a GATT server.
    // ACTION_GATT_DISCONNECTED: disconnected from a GATT server.
    // ACTION_GATT_SERVICES_DISCOVERED: discovered GATT services.
    // ACTION_DATA_AVAILABLE: received data from the device.  This can be a result of read
    //                        or notification operations.
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                loadGattServices(mBluetoothLeService.getSupportedGattServices());
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String uuid = intent.getStringExtra(BluetoothLeService.CHAR_DATA);
                double value = intent.getFloatExtra(BluetoothLeService.VALUE_DATA, 30);
                switch (uuid) {
                    case GattAttributes.TEMP_CHAR_UUID:
                        if (5 < value && value < 100) {
                            lastTemp = value;
                        }
                        break;
                    case GattAttributes.ROLL_CHAR_UUID:
                        if (0 < value && value < 180) {
                            lastRoll = value;
                        }
                        break;
                    case GattAttributes.PITCH_CHAR_UUID:
                        if (0 < value && value < 180) {
                            lastPitch = value;
                        }
                        break;
                }
            }
        }
    };

    private final BroadcastReceiver mGattNotifReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                String uuid = intent.getStringExtra(BluetoothLeService.CHAR_DATA);
                if (GattAttributes.DOUBLETAP_CHAR_UUID.equals(uuid)) {
                    Intent myAct = new Intent(context, DeviceControlActivity.class);
                    myAct.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
                    context.startActivity(myAct);
                }
            }
        }
    };

    private void clearUI() {
        //TODO: clear ui
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mConnectionState = (TextView) findViewById(R.id.connection_state);

        getActionBar().setTitle(mDeviceName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        // the LED controls
        SeekBar speedSelector = (SeekBar) findViewById(R.id.speedSlider);
        speedSelector.setOnSeekBarChangeListener(speedSelectorListener);

        SeekBar intensitySelector = (SeekBar) findViewById(R.id.intensitySlider);
        intensitySelector.setOnSeekBarChangeListener(intensitySelectorListener);

        // the board values graphs
        GraphView tempGraph = (GraphView)findViewById(R.id.tempGraph);
        tempData = new LineGraphSeries<DataPoint>();
        tempGraph.addSeries(tempData);
        tempGraph.getViewport().setXAxisBoundsManual(true);
        tempGraph.getViewport().setYAxisBoundsManual(true);
        tempGraph.getViewport().setMinX(0);
        tempGraph.getViewport().setMaxX(60);
        tempGraph.getViewport().setMinY(30);
        tempGraph.getViewport().setMaxY(40);

        GraphView pitchGraph = (GraphView)findViewById(R.id.pitchGraph);
        pitchData = new LineGraphSeries<DataPoint>();
        pitchGraph.addSeries(pitchData);
        pitchGraph.getViewport().setXAxisBoundsManual(true);
        pitchGraph.getViewport().setYAxisBoundsManual(true);
        pitchGraph.getViewport().setMinX(0);
        pitchGraph.getViewport().setMaxX(60);
        pitchGraph.getViewport().setMinY(0);
        pitchGraph.getViewport().setMaxY(180);

        GraphView rollGraph = (GraphView)findViewById(R.id.rollGraph);
        rollData = new LineGraphSeries<DataPoint>();
        rollGraph.addSeries(rollData);
        rollGraph.getViewport().setXAxisBoundsManual(true);
        rollGraph.getViewport().setYAxisBoundsManual(true);
        rollGraph.getViewport().setMinX(0);
        rollGraph.getViewport().setMaxX(60);
        rollGraph.getViewport().setMinY(0);
        rollGraph.getViewport().setMaxY(180);
    }

    private SeekBar.OnSeekBarChangeListener speedSelectorListener =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    writeLedSpeed(progress);
                    updateSpeedTextSize(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            };

    private int[] speeds = {R.id.answerNegative10,R.id.answerNegative9,R.id.answerNegative8,R.id.answerNegative7,
            R.id.answerNegative6,R.id.answerNegative5,R.id.answerNegative4,R.id.answerNegative3,R.id.answerNegative2,
            R.id.answerNegative1,R.id.answer0,R.id.answer1,R.id.answer2,R.id.answer3,R.id.answer4,R.id.answer5,
            R.id.answer6,R.id.answer7,R.id.answer8,R.id.answer9,R.id.answer10,};

    private void updateSpeedTextSize(int currentSpeed) {
        TextView selectedSpeed;

        for (int i = 0; i < speeds.length; i++) {
            selectedSpeed = (TextView) findViewById(speeds[i]);
            selectedSpeed.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        }

        selectedSpeed = (TextView) findViewById(speeds[currentSpeed]);
        selectedSpeed.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
    }

    private SeekBar.OnSeekBarChangeListener intensitySelectorListener =
            new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    writeLedIntensity(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            };

    //TODO: move this to the top
    private final Handler mHandler = new Handler();
    private Runnable graphUpdateThread;
    private Runnable tempValueThread;
    private Runnable rollValueThread;
    private Runnable pitchValueThread;
    private Runnable doubleTapMonitorThread;
    private LineGraphSeries<DataPoint> tempData;
    private LineGraphSeries<DataPoint> pitchData;
    private LineGraphSeries<DataPoint> rollData;
    private double graphLastXValue = 5d;

    private void wakeUp() {
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
                        WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        try {
            this.unregisterReceiver(mGattNotifReceiver);
        } catch (IllegalArgumentException e) {
            System.out.println("mGattNotifReceiver not registered. Ignored");
        }

        wakeUp();

        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }

        // Threads updates graph
        graphUpdateThread = new Runnable() {
            @Override
            public void run() {
                if (mBluetoothLeService != null
                        && mGattCharacteristics.containsKey(GattAttributes.DOUBLETAP_SERVICE_UUID)
                        && !setDoubleTap) {
                    setDoubleTap();
                    setDoubleTap = true;
                }

                graphLastXValue += 1d;
                if (lastTemp != 0) {
                    ((TextView)findViewById(R.id.tempValue)).setText(String.format ("%.2f", lastTemp));
                    tempData.appendData(new DataPoint(graphLastXValue, lastTemp), true, 60);
                }
                if (lastRoll != 0) {
                    ((TextView)findViewById(R.id.rollValue)).setText(String.format ("%.2f", lastRoll));
                    rollData.appendData(new DataPoint(graphLastXValue, lastRoll), true, 60);
                }
                if (lastPitch != 0) {
                    ((TextView)findViewById(R.id.pitchValue)).setText(String.format ("%.2f", lastPitch));
                    pitchData.appendData(new DataPoint(graphLastXValue, lastPitch), true, 60);
                }
                mHandler.postDelayed(this, 200);
            }
        };
        mHandler.postDelayed(graphUpdateThread, 0);

        tempValueThread = new Runnable() {
            @Override
            public void run() {
                readTemperature();
                mHandler.postDelayed(this, 1000);
            }
        };
        mHandler.postDelayed(tempValueThread, 50);

        rollValueThread = new Runnable() {
            @Override
            public void run() {
                readRoll();
                mHandler.postDelayed(this, 500);
            }
        };
        mHandler.postDelayed(rollValueThread, 100);

        pitchValueThread = new Runnable() {
            @Override
            public void run() {
                readPitch();
                mHandler.postDelayed(this, 500);
            }
        };
        mHandler.postDelayed(pitchValueThread, 300);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mHandler.removeCallbacks(tempValueThread);
        mHandler.removeCallbacks(rollValueThread);
        mHandler.removeCallbacks(pitchValueThread);
        unregisterReceiver(mGattUpdateReceiver);
        registerReceiver(mGattNotifReceiver, makeGattNotifIntentFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }

    private void readTemperature() {
        readCharacteristic(GattAttributes.TEMP_SERVICE_UUID, GattAttributes.TEMP_CHAR_UUID);
    }

    private void readRoll() {
        readCharacteristic(GattAttributes.ACC_SERVICE_UUID, GattAttributes.ROLL_CHAR_UUID);
    }

    private void readPitch() {
        readCharacteristic(GattAttributes.ACC_SERVICE_UUID, GattAttributes.PITCH_CHAR_UUID);
    }

    private void setDoubleTap() {
        setCharacteristicNotification(GattAttributes.DOUBLETAP_SERVICE_UUID,
                GattAttributes.DOUBLETAP_CHAR_UUID);
    }


    private void readCharacteristic(String serviceUuid, String characteristicsUuid) {
        if (mBluetoothLeService == null || !mGattCharacteristics.containsKey(serviceUuid)
                || !mGattCharacteristics.get(serviceUuid).containsKey(characteristicsUuid)) {
            return;
        }
        final BluetoothGattCharacteristic characteristic =
                mGattCharacteristics.get(serviceUuid).get(characteristicsUuid);
        mBluetoothLeService.readCharacteristic(characteristic);
    }

    private void setCharacteristicNotification(String serviceUuid, String characteristicsUuid) {
        if (mBluetoothLeService == null || !mGattCharacteristics.containsKey(serviceUuid)
                || !mGattCharacteristics.get(serviceUuid).containsKey(characteristicsUuid)) {
            return;
        }
        final BluetoothGattCharacteristic characteristic =
                mGattCharacteristics.get(serviceUuid).get(characteristicsUuid);
        mBluetoothLeService.setCharacteristicNotification(characteristic, true);
    }


    private void writeLedIntensity(int intensity) {
        writeCharacteristic(GattAttributes.LED_SERVICE_UUID,
                GattAttributes.LED_INTENSITY_CHAR_UUID, intensity);
    }

    private void writeLedSpeed(int speed) {
        writeCharacteristic(GattAttributes.LED_SERVICE_UUID,
                GattAttributes.LED_SPEED_CHAR_UUID, speed);
    }

    private void writeCharacteristic(String serviceUuid, String characteristicsUuid, int data) {
        if (mBluetoothLeService == null || !mGattCharacteristics.containsKey(serviceUuid)
                || !mGattCharacteristics.get(serviceUuid).containsKey(characteristicsUuid)) {
            return;
        }
        final BluetoothGattCharacteristic characteristic =
                mGattCharacteristics.get(serviceUuid).get(characteristicsUuid);
        characteristic.setValue(intToByteArray(data));
        mBluetoothLeService.writeCharacteristic(characteristic);
    }

    public byte[] intToByteArray(int value) {
        byte lowByte = (byte)(value & 0xFF);
        byte[] byteArray = new byte[1];
        byteArray[0] = lowByte;
        return byteArray;
    }

    // Demonstrates how to iterate through the supported GATT Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the ExpandableListView
    // on the UI.
    private void loadGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid;

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            if (GattAttributes.lookup(uuid) == null) {
                continue;
            }

            HashMap<String, BluetoothGattCharacteristic> characteristicMap = new HashMap<>();
            mGattCharacteristics.put(uuid, characteristicMap);

            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid().toString();
                if (GattAttributes.lookup(uuid) == null) {
                    continue;
                }
                characteristicMap.put(uuid, gattCharacteristic);
            }
        }
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private static IntentFilter makeGattNotifIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
