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
public class SampleGattAttributes {
    private static HashMap<String, String> attributes = new HashMap();
    public static String ACC_SERVICE_UUID =     "02366e80-cf3a-11e1-9ab4-0002a5d5c51b";
    public static String ROLL_CHAR_UUID =       "e23e78a0-cf4a-11e1-8ffc-0002a5d5c51b";
    public static String PITCH_CHAR_UUID =      "340a1b80-cf4b-11e1-ac36-0002a5d5c51b";

    public static String TEMP_SERVICE_UUID =    "42821a40-e477-11e2-82d0-0002a5d5c51b";
    public static String TEMP_CHAR_UUID =       "a32e5520-e477-11e2-a9e3-0002a5d5c51b";

//    public static String DOUBLETAP_SERVICE_UUID =    "08366e80-cf3a-11e1-9ab4-0002a5d5c51b";
//    public static String DOUBLETAP_CHAR_UUID =       "09366e80-cf3a-11e1-9ab4-0002a5d5c51b";
//
//    public static String LED_SERVICE_UUID =         "0b366e80-cf3a-11e1-9ab4-0002a5d5c51b";
//    public static String LED_SPEED_CHAR_UUID =      "0c366e80-cf3a-11e1-9ab4-0002a5d5c51b";
//    public static String LED_INTENSITY_CHAR_UUID =  "cd20c480-e48b-11e2-840b-0002a5d5c51b";

    static {
        // Accelerometer Service
        attributes.put(ACC_SERVICE_UUID, "Accelerometer Service");
        attributes.put(ROLL_CHAR_UUID, "Roll Angle");
        attributes.put(PITCH_CHAR_UUID, "Pitch Angle");

        // Temperature Service
        attributes.put(TEMP_SERVICE_UUID, "Temperature Service");
        attributes.put(TEMP_CHAR_UUID, "Temperature");

//        // Doubletap Service
//        attributes.put(DOUBLETAP_SERVICE_UUID, "Double Tap Service");
//        attributes.put(DOUBLETAP_CHAR_UUID, "Doble Tap Notification");
//
//        // LED Service
//        attributes.put(LED_SERVICE_UUID, "Accelerometer Service");
//        attributes.put(LED_SPEED_CHAR_UUID, "LED Speed");
//        attributes.put(LED_INTENSITY_CHAR_UUID, "LED Intensity");

    }

    public static String lookup(String uuid, String defaultName) {
        String name = attributes.get(uuid);
        return name == null ? defaultName : name;
    }
}
