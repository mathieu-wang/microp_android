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

import java.util.HashMap;

/**
 * This class includes a small subset of standard GATT attributes for demonstration purposes.
 */
public class GattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static final String ACC_SERVICE_UUID =     "00366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    public static final String ROLL_CHAR_UUID =       "01366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    public static final String PITCH_CHAR_UUID =      "02366e80-cf3a-11e1-9ab4-0002a5d5c51b";

    public static final String TEMP_SERVICE_UUID =    "03366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    public static final String TEMP_CHAR_UUID =       "04366e80-cf3a-11e1-9ab4-0002a5d5c51b";

    public static final String DOUBLETAP_SERVICE_UUID =    "05366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    public static final String DOUBLETAP_CHAR_UUID =       "06366e80-cf3a-11e1-9ab4-0002a5d5c51b";

    public static final String LED_SERVICE_UUID =         "07366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    public static final String LED_SPEED_CHAR_UUID =      "08366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    public static final String LED_INTENSITY_CHAR_UUID =  "09366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    public static final String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    static {
        // Accelerometer Service
        attributes.put(ACC_SERVICE_UUID, "Accelerometer Service");
        attributes.put(ROLL_CHAR_UUID, "Roll Angle");
        attributes.put(PITCH_CHAR_UUID, "Pitch Angle");

        // Temperature Service
        attributes.put(TEMP_SERVICE_UUID, "Temperature Service");
        attributes.put(TEMP_CHAR_UUID, "Temperature");

        // Doubletap Service
        attributes.put(DOUBLETAP_SERVICE_UUID, "Double Tap Service");
        attributes.put(DOUBLETAP_CHAR_UUID, "Double Tap Notification");

        // LED Service
        attributes.put(LED_SERVICE_UUID, "LED Service");
        attributes.put(LED_SPEED_CHAR_UUID, "LED Speed");
        attributes.put(LED_INTENSITY_CHAR_UUID, "LED Intensity");

    }

    public static String lookup(String uuid) {
        return attributes.get(uuid);
    }
}
