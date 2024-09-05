package de.androidcrypto.android_hce_beginner_app;/*
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

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MyHostApduServiceSimple extends HostApduService {
    private static final String TAG = "HceBeginnerApp";
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final byte[] SELECT_APPLICATION_APDU = hexStringToByteArray("00a4040006f2233445566700");
    private static final String GET_DATA_APDU_HEADER = "00CA0000";
    private static final String PUT_DATA_APDU_HEADER = "00DA0000";
    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = hexStringToByteArray("9000");
    // "UNKNOWN" status word sent in response to invalid APDU command (0x0000)
    private static final byte[] UNKNOWN_CMD_SW = hexStringToByteArray("0000");

    private byte[] fileContent01 = "HCE Beginner App 1".getBytes(StandardCharsets.UTF_8);
    private byte[] fileContent02 = "HCE Beginner App 2".getBytes(StandardCharsets.UTF_8);
    private byte[] fileContentUnknown = "HCE Beginner App Unknown".getBytes(StandardCharsets.UTF_8);

    /**
     * Called if the connection to the NFC card is lost, in order to let the application know the
     * cause for the disconnection (either a lost link, or another AID being selected by the
     * reader).
     *
     * @param reason Either DEACTIVATION_LINK_LOSS or DEACTIVATION_DESELECTED
     */
    @Override
    public void onDeactivated(int reason) {
    }

    /**
     * This method will be called when a command APDU has been received from a remote device. A
     * response APDU can be provided directly by returning a byte-array in this method. In general
     * response APDUs must be sent as quickly as possible, given the fact that the user is likely
     * holding his device over an NFC reader when this method is called.
     *
     * <p class="note">If there are multiple services that have registered for the same AIDs in
     * their meta-data entry, you will only get called if the user has explicitly selected your
     * service, either as a default or just for the next tap.
     *
     * <p class="note">This method is running on the main thread of your application. If you
     * cannot return a response APDU immediately, return null and use the {@link
     * #sendResponseApdu(byte[])} method later.
     *
     * @param commandApdu The APDU that received from the remote device
     * @param extras      A bundle containing extra data. May be null.
     * @return a byte-array containing the response APDU, or null if no response APDU can be sent
     * at this point.
     */

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        // The following flow is based on Appendix E "Example of Mapping Version 2.0 Command Flow"
        // in the NFC Forum specification
        Log.i(TAG, "Received APDU: " + byteArrayToHexString(commandApdu));

        // First command: Application select (Section 5.5.2 in NFC Forum spec)
        if (Arrays.equals(SELECT_APPLICATION_APDU, commandApdu)) {
            Log.i(TAG, "This is: 01 SELECT_APPLICATION_APDU");
            return SELECT_OK_SW;

        // Second command: GetData command, here returning any data and not from file01
        } else if (arraysStartWith(commandApdu, hexStringToByteArray(GET_DATA_APDU_HEADER))) {
            Log.i(TAG, "This is: 02 GET_DATA_APDU");

            // get the file number from commandApdu
            int fileNumber;
            byte[] fileContent;
            if (commandApdu.length == 7) {
                fileNumber = (int) commandApdu[5];
                if (fileNumber == 1) {
                    fileContent = fileContent01.clone();
                } else if (fileNumber == 2) {
                    fileContent = fileContent02.clone();
                } else {
                    fileContent = fileContentUnknown.clone();
                }
            } else {
                fileContent = "Unknown Request".getBytes(StandardCharsets.UTF_8);
            }
            byte[] response = new byte[fileContent.length + SELECT_OK_SW.length];
            System.arraycopy(fileContent, 0, response, 0, fileContent.length);
            System.arraycopy(SELECT_OK_SW, 0, response, fileContent.length, SELECT_OK_SW.length);
            Log.i(TAG, "GET_DATA_APDU Our Response: " + byteArrayToHexString(response));
            return response;
        // write data to the emulated tag
        } else if (arraysStartWith(commandApdu, hexStringToByteArray(PUT_DATA_APDU_HEADER))) {
            Log.i(TAG, "This is: 03 PUT_DATA_APDU");
            // get the data length and file number from commandApdu
            int dataLength;
            int fileNumber;
            byte[] fileContent;
            if (commandApdu.length >= 7) {
                dataLength = (int) commandApdu[4];
                fileNumber = (int) commandApdu[5];
                // System.out.println("commandApdu l: " + commandApdu.length + " data l: " + dataLength + " fnr: " + fileNumber);
                // get the data
                if (commandApdu.length != (6 + dataLength)) {
                    return UNKNOWN_CMD_SW;
                }
                fileContent = Arrays.copyOfRange(commandApdu, 6, (5 + dataLength));
                Log.i(TAG,  "fileNr: " + fileNumber +  " content: " + new String(fileContent, StandardCharsets.UTF_8));
                if (fileNumber == 1) {
                    fileContent01 = fileContent.clone();
                } else if (fileNumber == 2) {
                    fileContent02 = fileContent.clone();
                } else {
                    fileContentUnknown = fileContent.clone();
                }
            } else {
                fileContent = "Unknown Request".getBytes(StandardCharsets.UTF_8);
            }
            return SELECT_OK_SW;

            // We're doing something outside our scope
        } else
            Log.wtf(TAG, "processCommandApdu() | I don't know what's going on!!!.");
        //return "Can I help you?".getBytes();
        return UNKNOWN_CMD_SW;
    }

    boolean arraysStartWith(byte[] completeArray, byte[] compareArray) {
        int n = compareArray.length;
        // Log.d(TAG, "completeArray length: " + completeArray.length + " data: " + ByteArrayToHexString(completeArray));
        // Log.d(TAG, "compareArray  length: " + compareArray.length + " data: " + ByteArrayToHexString(compareArray));
        return ByteBuffer.wrap(completeArray, 0, n).equals(ByteBuffer.wrap(compareArray, 0, n));
    }

    /**
     * Utility method to convert a byte array to a hexadecimal string.
     *
     * @param bytes Bytes to convert
     * @return String, containing hexadecimal representation.
     */
    public static String byteArrayToHexString(byte[] bytes) {
        final char[] hexArray = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        char[] hexChars = new char[bytes.length * 2]; // Each byte has two hex characters (nibbles)
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF; // Cast bytes[j] to int, treating as unsigned value
            hexChars[j * 2] = hexArray[v >>> 4]; // Select hex character from upper nibble
            hexChars[j * 2 + 1] = hexArray[v & 0x0F]; // Select hex character from lower nibble
        }
        return new String(hexChars);
    }

    /**
     * Utility method to convert a hexadecimal string to a byte string.
     *
     * <p>Behavior with input strings containing non-hexadecimal characters is undefined.
     *
     * @param s String containing hexadecimal characters to convert
     * @return Byte array generated from input
     * @throws IllegalArgumentException if input length is incorrect
     */
    public static byte[] hexStringToByteArray(String s) throws IllegalArgumentException {
        int len = s.length();
        if (len % 2 == 1) {
            throw new IllegalArgumentException("Hex string must have even number of characters");
        }
        byte[] data = new byte[len / 2]; // Allocate 1 byte per 2 hex characters
        for (int i = 0; i < len; i += 2) {
            // Convert each character into a integer (base-16), then bit-shift into place
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }
}
