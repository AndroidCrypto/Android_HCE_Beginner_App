package de.androidcrypto.android_hce_beginner_app;

import android.app.Service;
import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.Arrays;

public class KHostApduService extends HostApduService {

    // Original source (Kotlin): https://github.com/underwindfall/NFCAndroid/blob/master/app/src/main/java/com/qifan/nfcbank/cardEmulation/KHostApduService.kt
    // Translated to Java by Chat GPT 3

    private static final String TAG = "HostApduService";

    private static final byte[] APDU_SELECT = new byte[]{
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xA4, // INS	- Instruction - Instruction code
            (byte) 0x04, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x00, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x07, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xD2,
            (byte) 0x76,
            (byte) 0x00,
            (byte) 0x00,
            (byte) 0x85,
            (byte) 0x01,
            (byte) 0x01, // NDEF Tag Application name
            (byte) 0x00 // Le field	- Maximum number of bytes expected in the data field of the response to the command
    };

    private static final byte[] CAPABILITY_CONTAINER_OK = new byte[]{
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xa4, // INS	- Instruction - Instruction code
            (byte) 0x00, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x0c, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x02, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xe1,
            (byte) 0x03 // file identifier of the CC file
    };

    private static final byte[] READ_CAPABILITY_CONTAINER = new byte[]{
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xb0, // INS	- Instruction - Instruction code
            (byte) 0x00, // P1	- Parameter 1 - Instruction parameter 1
            (byte) 0x00, // P2	- Parameter 2 - Instruction parameter 2
            (byte) 0x0f // Lc field	- Number of bytes present in the data field of the command
    };

    private boolean READ_CAPABILITY_CONTAINER_CHECK = false;

    private static final byte[] READ_CAPABILITY_CONTAINER_RESPONSE = new byte[]{
            (byte) 0x00, (byte) 0x11, // CCLEN length of the CC file
            (byte) 0x20, // Mapping Version 2.0
            (byte) 0xFF, (byte) 0xFF, // MLe maximum
            (byte) 0xFF, (byte) 0xFF, // MLc maximum
            (byte) 0x04, // T field of the NDEF File Control TLV
            (byte) 0x06, // L field of the NDEF File Control TLV
            (byte) 0xE1, (byte) 0x04, // File Identifier of NDEF file
            (byte) 0xFF, (byte) 0xFE, // Maximum NDEF file size of 65534 bytes
            (byte) 0x00, // Read access without any security
            (byte) 0xFF, // Write access without any security
            (byte) 0x90, (byte) 0x00 // A_OKAY
    };

    private static final byte[] NDEF_SELECT_OK = new byte[]{
            (byte) 0x00, // CLA	- Class - Class of instruction
            (byte) 0xa4, // Instruction byte (INS) for Select command
            (byte) 0x00, // Parameter byte (P1), select by identifier
            (byte) 0x0c, // Parameter byte (P1), select by identifier
            (byte) 0x02, // Lc field	- Number of bytes present in the data field of the command
            (byte) 0xE1,
            (byte) 0x04 // file identifier of the NDEF file retrieved from the CC file
    };

    private static final byte[] NDEF_READ_BINARY = new byte[]{
            (byte) 0x00, // Class byte (CLA)
            (byte) 0xb0 // Instruction byte (INS) for ReadBinary command
    };

    private static final byte[] NDEF_READ_BINARY_NLEN = new byte[]{
            (byte) 0x00, // Class byte (CLA)
            (byte) 0xb0, // Instruction byte (INS) for ReadBinary command
            (byte) 0x00,
            (byte) 0x00, // Parameter byte (P1, P2), offset inside the CC file
            (byte) 0x02 // Le field
    };

    private static final byte[] A_OKAY = new byte[]{
            (byte) 0x90, // SW1	Status byte 1 - Command processing status
            (byte) 0x00 // SW2	Status byte 2 - Command processing qualifier
    };

    private static final byte[] A_ERROR = new byte[]{
            (byte) 0x6A, // SW1	Status byte 1 - Command processing status
            (byte) 0x82 // SW2	Status byte 2 - Command processing qualifier
    };

    private static final byte[] NDEF_ID = new byte[]{(byte) 0xE1, (byte) 0x04};

    //private NdefMessage NDEF_URI = new NdefMessage(createTextRecord("en", "Ciao, come va?", NDEF_ID));
    private NdefMessage NDEF_URI = getNdefMessage("Hello world!");
    private byte[] NDEF_URI_BYTES = NDEF_URI.toByteArray();
    private byte[] NDEF_URI_LEN = fillByteArrayToFixedDimension(
            BigInteger.valueOf(NDEF_URI_BYTES.length).toByteArray(),
            2
    );

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.hasExtra("ndefMessage")) {
            //NDEF_URI = new NdefMessage(createTextRecord("en", intent.getStringExtra("ndefMessage"), NDEF_ID));

            NDEF_URI = getNdefMessage(intent.getStringExtra("ndefMessage"));

            NDEF_URI_BYTES = NDEF_URI.toByteArray();
            NDEF_URI_LEN = fillByteArrayToFixedDimension(
                    BigInteger.valueOf(NDEF_URI_BYTES.length).toByteArray(),
                    2
            );
        }

        Log.i(TAG, "onStartCommand() | NDEF " + NDEF_URI);

        return Service.START_STICKY;
    }

    // extra
    private NdefMessage getNdefMessage(String ndefData) {
        if (ndefData.length() == 0) {
            return null;
        }
        NdefMessage ndefMessage;
        NdefRecord ndefRecord;
        ndefRecord = NdefRecord.createTextRecord("en", ndefData);
        ndefMessage = new NdefMessage(ndefRecord);
        return ndefMessage;
    }

    private byte[] getNdefMessageOrg(String ndefData) {
        if (ndefData.length() == 0) {
            return new byte[0];
        }
        NdefMessage ndefMessage;
        NdefRecord ndefRecord;
        ndefRecord = NdefRecord.createTextRecord("en", ndefData);
        ndefMessage = new NdefMessage(ndefRecord);
        return ndefMessage.toByteArray();
    }

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
        Log.i(TAG, "processCommandApdu() | incoming commandApdu: " + toHex(commandApdu));

        if (Arrays.equals(APDU_SELECT, commandApdu)) {
            Log.i(TAG, "APDU_SELECT triggered. Our Response: " + toHex(A_OKAY));
            return A_OKAY;
        }

        if (Arrays.equals(CAPABILITY_CONTAINER_OK, commandApdu)) {
            Log.i(TAG, "CAPABILITY_CONTAINER_OK triggered. Our Response: " + toHex(A_OKAY));
            return A_OKAY;
        }

        if (Arrays.equals(READ_CAPABILITY_CONTAINER, commandApdu) && !READ_CAPABILITY_CONTAINER_CHECK) {
            Log.i(TAG, "READ_CAPABILITY_CONTAINER triggered. Our Response: " + toHex(READ_CAPABILITY_CONTAINER_RESPONSE));
            READ_CAPABILITY_CONTAINER_CHECK = true;
            return READ_CAPABILITY_CONTAINER_RESPONSE;
        }

        if (Arrays.equals(NDEF_SELECT_OK, commandApdu)) {
            Log.i(TAG, "NDEF_SELECT_OK triggered. Our Response: " + toHex(A_OKAY));
            return A_OKAY;
        }

        if (Arrays.equals(NDEF_READ_BINARY_NLEN, commandApdu)) {
            byte[] response = new byte[NDEF_URI_LEN.length + A_OKAY.length];
            System.arraycopy(NDEF_URI_LEN, 0, response, 0, NDEF_URI_LEN.length);
            System.arraycopy(A_OKAY, 0, response, NDEF_URI_LEN.length, A_OKAY.length);

            Log.i(TAG, "NDEF_READ_BINARY_NLEN triggered. Our Response: " + toHex(response));

            READ_CAPABILITY_CONTAINER_CHECK = false;
            return response;
        }

        if (Arrays.equals(Arrays.copyOfRange(commandApdu, 0, 2), NDEF_READ_BINARY)) {
            int offset = Integer.parseInt(toHex(Arrays.copyOfRange(commandApdu, 2, 4)), 16);
            int length = Integer.parseInt(toHex(Arrays.copyOfRange(commandApdu, 4, 5)), 16);

            byte[] fullResponse = new byte[NDEF_URI_LEN.length + NDEF_URI_BYTES.length];
            System.arraycopy(NDEF_URI_LEN, 0, fullResponse, 0, NDEF_URI_LEN.length);
            System.arraycopy(NDEF_URI_BYTES, 0, fullResponse, NDEF_URI_LEN.length, NDEF_URI_BYTES.length);

            byte[] slicedResponse = Arrays.copyOfRange(fullResponse, offset, fullResponse.length);
            byte[] response = new byte[length + A_OKAY.length];
            System.arraycopy(slicedResponse, 0, response, 0, length);
            System.arraycopy(A_OKAY, 0, response, length, A_OKAY.length);

            Log.i(TAG, "NDEF_READ_BINARY triggered. Our Response: " + toHex(response));
            return response;
        }

        Log.i(TAG, "processCommandApdu() | I don't know what's going on!!!");
        return A_ERROR;
    }

    @Override
    public void onDeactivated(int reason) {
        Log.i(TAG, "onDeactivated() | reason: " + reason);
    }
/*
    private static byte[] createTextRecord(String language, String text, byte[] id) {
        byte[] languageBytes;
        byte[] textBytes;
        try {
            languageBytes = language.getBytes("US-ASCII");
            textBytes = text.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }

        int languageLength = languageBytes.length;
        int textLength = textBytes.length;
        int payloadLength = 1 + languageLength + textLength;

        byte[] payload = new byte[payloadLength];

        payload[0] = (byte) languageLength;
        System.arraycopy(languageBytes, 0, payload, 1, languageLength);
        System.arraycopy(textBytes, 0, payload, 1 + languageLength, textLength);

        NdefRecord recordNdef = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, id, payload);
        return new byte[][]{recordNdef.toByteArray()};
    }
*/
    private static byte[] fillByteArrayToFixedDimension(byte[] array, int length) {
        byte[] result = new byte[length];
        System.arraycopy(array, 0, result, length - array.length, array.length);
        return result;
    }

    private static String toHex(byte[] bytes) {
        StringBuilder result = new StringBuilder();
        for (byte b : bytes) {
            result.append(String.format("%02X", b));
        }
        return result.toString();
    }
}

