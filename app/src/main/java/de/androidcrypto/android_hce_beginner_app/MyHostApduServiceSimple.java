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

import android.content.Context;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.widget.Toast;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;


/**
 * This is a sample APDU Service which demonstrates how to interface with the card emulation support
 * added in Android 4.4, KitKat.
 *
 * <p>This sample replies to any requests sent with the string "Hello World". In real-world
 * situations, you would need to modify this code to implement your desired communication
 * protocol.
 *
 * <p>This sample will be invoked for any terminals selecting AIDs of 0xF11111111, 0xF22222222, or
 * 0xF33333333. See src/main/res/xml/aid_list.xml for more details.
 *
 * <p class="note">Note: This is a low-level interface. Unlike the NdefMessage many developers
 * are familiar with for implementing Android Beam in apps, card emulation only provides a
 * byte-array based communication channel. It is left to developers to implement higher level
 * protocol support as needed.
 */
public class MyHostApduServiceSimple extends HostApduService {
    private static final String TAG = "HceBeginnerApp";
    private static final String BEGINNER_APP_AID = "F22334455667";
    // AID for our loyalty card service.
    private static final String SAMPLE_LOYALTY_CARD_AID = "F222222222";
    // ISO-DEP command HEADER for selecting an AID.
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String SELECT_APDU_HEADER = "00A40400";
    // Format: [Class | Instruction | Parameter 1 | Parameter 2]
    private static final String GET_DATA_APDU_HEADER = "00CA0000";
    // "OK" status word sent in response to SELECT AID command (0x9000)
    private static final byte[] SELECT_OK_SW = hexStringToByteArray("9000");
    // "UNKNOWN" status word sent in response to invalid APDU command (0x0000)
    private static final byte[] UNKNOWN_CMD_SW = hexStringToByteArray("0000");
    private static final byte[] SELECT_APDU = buildSelectApdu(SAMPLE_LOYALTY_CARD_AID);
    private static final byte[] GET_DATA_APDU = buildGetDataApdu();

    // Commands for each step


    private static final byte[] SELECT_APDU_TagInfo = hexStringToByteArray("00A4040007D276000085010100");
    private static final byte[] SELECT_APDU_TagInfo_OLD = {
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xA4, // INS	- Instruction - Instruction code
            (byte) 0x04, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x00, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x07, // Lc field	- Number of bytes present in the data field of the command
            //(byte)0xF0, (byte)0x39, (byte)0x41, (byte)0x48, (byte)0x14, (byte)0x81, (byte)0x00, // NDEF Tag Application name
            (byte) 0xD2, (byte) 0x76, (byte) 0x00, (byte) 0x00, (byte) 0x85, (byte) 0x01, (byte) 0x01, // NDEF Tag Application name
            (byte) 0x00  // Le field	- Maximum number of bytes expected in the data field of the response to the command
    };

    private static final byte[] GET_COMPATIBILITY_CONTAINER_APDU_OLD = hexStringToByteArray("00A4000C02E103");
    private static final byte[] GET_CAPABILITY_CONTAINER_APDU = {
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xa4, // INS	- Instruction - Instruction code
            (byte) 0x00, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x0c, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x02, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xe1, (byte) 0x03 // file identifier of the CC file
    };

    private static final byte[] READ_CAPABILITY_CONTAINER_APDU = {
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xb0, // INS	- Instruction - Instruction code
            (byte) 0x00, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x00, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x0f  // Lc field	- Number of bytes present in the data field of the command
    };

    // In the scenario that we have done a CC read, the same byte[] match
    // for ReadBinary would trigger and we don't want that in succession
    private boolean READ_CAPABILITY_CONTAINER_CHECK = false;

    private static final byte[] READ_CAPABILITY_CONTAINER_RESPONSE = {
            (byte) 0x00, (byte) 0x0F, // CCLEN length of the CC file
            (byte) 0x20, // Mapping Version 2.0
            (byte) 0xFF, (byte) 0xFF, // MLe maximum 59 bytes R-APDU data size
            (byte) 0xFF, (byte) 0xFF, // MLc maximum 52 bytes C-APDU data size
            (byte) 0x04, // T field of the NDEF File Control TLV
            (byte) 0x06, // L field of the NDEF File Control TLV
            (byte) 0xE1, (byte) 0x04, // File Identifier of NDEF file
            (byte) 0xFF, (byte) 0xFE, // Maximum NDEF file size of 50 bytes
            (byte) 0x00, // Read access without any security
            (byte) 0xFF, // Write access without any security
            (byte) 0x90, (byte) 0x00 // A_OKAY
    };

    private static final byte[] READ_CAPABILITY_CONTAINER_RESPONSE_ORG = {
            (byte) 0x00, (byte) 0x0F, // CCLEN length of the CC file
            (byte) 0x20, // Mapping Version 2.0
            (byte) 0x00, (byte) 0x3B, // MLe maximum 59 bytes R-APDU data size
            (byte) 0x00, (byte) 0x34, // MLc maximum 52 bytes C-APDU data size
            (byte) 0x04, // T field of the NDEF File Control TLV
            (byte) 0x06, // L field of the NDEF File Control TLV
            (byte) 0xE1, (byte) 0x04, // File Identifier of NDEF file
            (byte) 0x00, (byte) 0x32, // Maximum NDEF file size of 50 bytes
            (byte) 0x00, // Read access without any security
            (byte) 0x00, // Write access without any security
            (byte) 0x90, (byte) 0x00 // A_OKAY
    };

    private static final byte[] NDEF_SELECT_APDU = {
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xa4, // Instruction byte (INS) for Select command
            (byte) 0x00, // Parameter byte (P1), select by identifier
            (byte) 0x0c, // Parameter byte (P1), select by identifier
            (byte) 0x02, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xE1, (byte) 0x04 // file identifier of the NDEF file retrieved from the CC file
    };

    private static final byte[] NDEF_READ_BINARY_NLEN_APDU = {
            (byte) 0x00, // Class byte (CLA)
            (byte) 0xb0, // Instruction byte (INS) for ReadBinary command
            (byte) 0x00, (byte) 0x00, // Parameter byte (P1, P2), offset inside the CC file
            (byte) 0x02  // Le field
    };

    private static final byte[] NDEF_READ_BINARY_GET_NDEF_APDU = {
            (byte) 0x00, // Class byte (CLA)
            (byte) 0xb0, // Instruction byte (INS) for ReadBinary command
            (byte) 0x00, (byte) 0x02, // Parameter byte (P1, P2), offset inside the CC file
            (byte) 0x13  //  Le field
    };

    private static final byte[] NDEF_READ_BINARY_GET_NDEF_APDU_ORG = {
            (byte) 0x00, // Class byte (CLA)
            (byte) 0xb0, // Instruction byte (INS) for ReadBinary command
            (byte) 0x00, (byte) 0x00, // Parameter byte (P1, P2), offset inside the CC file
            (byte) 0x0f  //  Le field
    };


    private byte[] NDEF_URI_BYTES = getNdefMessage("Hello world!");
    private byte[] NDEF_URI_LEN = BigInteger.valueOf(NDEF_URI_BYTES.length).toByteArray();

    private byte[] getNdefMessage(String ndefData) {
        if (ndefData.length() == 0) {
            return new byte[0];
        }
        NdefMessage ndefMessage;
        NdefRecord ndefRecord;
        ndefRecord = NdefRecord.createTextRecord("en", ndefData);
        ndefMessage = new NdefMessage(ndefRecord);
        return ndefMessage.toByteArray();
    }

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
/*
        if (commandApdu != null) {
            Log.i(TAG, "Return SELECT_OK_SW");
            return SELECT_OK_SW;
        }

 */
        /*
        else {
            Log.i(TAG, "Return UNKNOWN_CMD_SW");
            return UNKNOWN_CMD_SW;
        }

         */

        // see https://github.com/underwindfall/NFCAndroid/blob/master/app/src/main/java/com/qifan/nfcbank/cardEmulation/KHostApduService.kt
        /*
        if (commandApdu.sliceArray(0..1).contentEquals(NDEF_READ_BINARY)) {
            // do nothing
        }
         */

        // First command: Application select (Section 5.5.2 in NFC Forum spec)
        if (Arrays.equals(SELECT_APDU_TagInfo, commandApdu)) {
            Log.i(TAG, "This is: 01 SELECT_APDU_TagInfo");
            return SELECT_OK_SW;

        // Second command: GetData command, here returning any data and not from file01
        } else if (arraysStartWith(commandApdu, hexStringToByteArray(GET_DATA_APDU_HEADER))) {
            Log.i(TAG, "This is: 02 GET_DATA_APDU");
            byte[] dataToReturn = "HCE Beginner App".getBytes(StandardCharsets.UTF_8);
            byte[] response = new byte[dataToReturn.length + SELECT_OK_SW.length];
            System.arraycopy(dataToReturn, 0, response, 0, dataToReturn.length);
            System.arraycopy(SELECT_OK_SW, 0, response, dataToReturn.length, SELECT_OK_SW.length);
            Log.i(TAG, "GET_DATA_APDU Our Response: " + byteArrayToHexString(response));
            return response;

            // We're doing something outside our scope
        } else
            Log.wtf(TAG, "processCommandApdu() | I don't know what's going on!!!.");
        //return "Can I help you?".getBytes();
        return UNKNOWN_CMD_SW;
    }


    /**
     * Build APDU for SELECT AID command. This command indicates which service a reader is
     * interested in communicating with. See ISO 7816-4.
     *
     * @param aid Application ID (AID) to select
     * @return APDU for SELECT AID command
     */
    public static byte[] buildSelectApdu(String aid) {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return hexStringToByteArray(SELECT_APDU_HEADER + String.format("%02X",
                aid.length() / 2) + aid);
    }

    /**
     * Build APDU for GET_DATA command. See ISO 7816-4.
     *
     * @return APDU for SELECT AID command
     */
    public static byte[] buildGetDataApdu() {
        // Format: [CLASS | INSTRUCTION | PARAMETER 1 | PARAMETER 2 | LENGTH | DATA]
        return hexStringToByteArray(GET_DATA_APDU_HEADER + "0FFF");
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