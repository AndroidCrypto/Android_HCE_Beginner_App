package de.androidcrypto.android_hce_beginner_app;

import static de.androidcrypto.android_hce_beginner_app.Utils.bytesToHexNpe;
import static de.androidcrypto.android_hce_beginner_app.Utils.doVibrate;
import static de.androidcrypto.android_hce_beginner_app.Utils.hexStringToByteArray;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.cardemulation.CardEmulation;
import android.nfc.tech.IsoDep;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ReadFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ReadFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "ReadFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ReadFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ReceiveFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ReadFragment newInstance(String param1, String param2) {
        ReadFragment fragment = new ReadFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    private TextView readResult;
    private View loadingLayout;
    private String outputString = ""; // used for the UI output
    private NfcAdapter mNfcAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this.getContext());
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        readResult = getView().findViewById(R.id.tvReadResult);
        loadingLayout = getView().findViewById(R.id.loading_layout);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_read, container, false);
    }

    // This method is running in another thread when a card is discovered
    // This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {
        System.out.println("NFC tag discovered");
        playSinglePing();

        setLoadingLayoutVisibility(true);
        outputString = "";

        requireActivity().runOnUiThread(() -> {
            readResult.setBackgroundColor(getResources().getColor(R.color.white));
            readResult.setText("");
        });

        IsoDep isoDep = IsoDep.get(tag);
        if (isoDep == null) {
            Log.e(TAG, "isoDep is NULL, aborted");
            writeToUiAppend("The tag is not readable with IsoDep class, sorry");
            writeToUiFinal(readResult);
            setLoadingLayoutVisibility(false);
            returnOnNotSuccess();
            return;
        } else {
            Log.i(TAG, "isoDep is available");
        }

        byte[] tagId = isoDep.getTag().getId();
        writeToUiAppend("TagId: " + bytesToHexNpe(tagId));

        try {
            isoDep.connect();
            byte[] command, response;

            String aidString = "F22334455667";
            byte[] aid = Utils.hexStringToByteArray(aidString);
            command = selectApdu(aid);
            response = isoDep.transceive(command);
            writeToUiAppend("selectApdu with AID: " + bytesToHexNpe(command));
            if (response == null) {
                writeToUiAppend("selectApdu with AID fails (null), aborted");
                return;
            } else {
                writeToUiAppend("response length: " + response.length + " data: " + bytesToHexNpe(response));
                Log.i(TAG, "response: " + bytesToHexNpe(response));
            }

            // asking for data in file 01
            int fileNumber01 = 1;
            command = getDataApdu(fileNumber01);
            response = isoDep.transceive(command);
            writeToUiAppend("getDataApdu with file01: " + bytesToHexNpe(command));
            if (response == null) {
                writeToUiAppend("getDataApdu with file01 fails (null)");
            } else {
                writeToUiAppend("response length: " + response.length + " data: " + bytesToHexNpe(response));
            }
            // verify response
            if (checkResponse(response)) {
                writeToUiAppend(new String(returnDataBytes(response), StandardCharsets.UTF_8));
                Log.i(TAG, "response: " + bytesToHexNpe(returnDataBytes(response)));
            } else {
                writeToUiAppend("The tag returned NOT OK");
                Log.i(TAG, "The tag returned NOT OK");
            }

            // read previous content of file 02
            int fileNumber02 = 2;
            command = getDataApdu(fileNumber02);
            response = isoDep.transceive(command);
            writeToUiAppend("getDataApdu with file02: " + bytesToHexNpe(command));
            if (response == null) {
                writeToUiAppend("getDataApdu with file02 fails (null)");
            } else {
                writeToUiAppend("response length: " + response.length + " data: " + bytesToHexNpe(response));
            }
            // verify response
            if (checkResponse(response)) {
                writeToUiAppend(new String(returnDataBytes(response), StandardCharsets.UTF_8));
                Log.i(TAG, "response: " + bytesToHexNpe(returnDataBytes(response)));
            } else {
                writeToUiAppend("The tag returned NOT OK");
                Log.i(TAG, "The tag returned NOT OK");
            }

            // write data to fileNumber 02
            byte[] dataToWrite = "New Content in fileNumber 02".getBytes(StandardCharsets.UTF_8);
            command = putDataApdu(fileNumber02, dataToWrite);
            response = isoDep.transceive(command);
            writeToUiAppend("putDataApdu with file02: " + bytesToHexNpe(command));
            if (response == null) {
                writeToUiAppend("putDataApdu with file02 fails (null)");
            } else {
                writeToUiAppend("response length: " + response.length + " data: " + bytesToHexNpe(response));
            }
            // verify response
            if (checkResponse(response)) {
                writeToUiAppend("SUCCESS");
                Log.i(TAG, "response: " + bytesToHexNpe(returnDataBytes(response)));
            } else {
                writeToUiAppend("The tag returned NOT OK");
                Log.i(TAG, "The tag returned NOT OK");
            }

            // read updated content
            command = getDataApdu(fileNumber02);
            response = isoDep.transceive(command);
            writeToUiAppend("getDataApdu with file02: " + bytesToHexNpe(command));
            if (response == null) {
                writeToUiAppend("getDataApdu with file02 fails (null)");
            } else {
                writeToUiAppend("response length: " + response.length + " data: " + bytesToHexNpe(response));
            }
            // verify response
            if (checkResponse(response)) {
                writeToUiAppend(new String(returnDataBytes(response), StandardCharsets.UTF_8));
                Log.i(TAG, "response: " + bytesToHexNpe(returnDataBytes(response)));
            } else {
                writeToUiAppend("The tag returned NOT OK");
                Log.i(TAG, "The tag returned NOT OK");
            }

            isoDep.close();
        } catch (IOException e) {
            writeToUiAppend("IOException on connection: " + e.getMessage());
            Log.e(TAG, "IOException on connection: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            writeToUiAppend("Exception on connection: " + e.getMessage());
            Log.e(TAG, "Exception on connection: " + e.getMessage());
            e.printStackTrace();
        }

        writeToUiFinal(readResult);
        playDoublePing();
        setLoadingLayoutVisibility(false);
        doVibrate(getActivity());
    }

    private void returnOnNotSuccess() {
        writeToUiAppend("=== Return on Not Success ===");
        writeToUiFinal(readResult);
        playDoublePing();
        setLoadingLayoutVisibility(false);
        doVibrate(getActivity());
        mNfcAdapter.disableReaderMode(this.getActivity());
    }

    private byte[] selectApdu(byte[] aid) {
        byte[] commandApdu = new byte[6 + aid.length];
        commandApdu[0] = (byte) 0x00;  // CLA
        commandApdu[1] = (byte) 0xA4;  // INS
        commandApdu[2] = (byte) 0x04;  // P1
        commandApdu[3] = (byte) 0x00;  // P2
        commandApdu[4] = (byte) (aid.length & 0x0FF);       // Lc
        System.arraycopy(aid, 0, commandApdu, 5, aid.length);
        commandApdu[commandApdu.length - 1] = (byte) 0x00;  // Le
        return commandApdu;
    }

    /**
     * getDataApdu is asking for data in file
      * @param file is the identifier on the (emulated) tag
     * @return
     */
    private byte[] getDataApdu(byte[] file) {
        byte[] commandApdu = new byte[6 + file.length];
        commandApdu[0] = (byte) 0x00;  // CLA
        commandApdu[1] = (byte) 0xCA;  // INS
        commandApdu[2] = (byte) 0x00;  // P1
        commandApdu[3] = (byte) 0x00;  // P2
        commandApdu[4] = (byte) (file.length & 0x0FF);       // Lc
        System.arraycopy(file, 0, commandApdu, 5, file.length);
        commandApdu[commandApdu.length - 1] = (byte) 0x00;  // Le
        return commandApdu;
    }

    private byte[] getDataApdu(int file) {
        byte[] commandApdu = new byte[6 + 1]; // 6 + byte length
        commandApdu[0] = (byte) 0x00;  // CLA
        commandApdu[1] = (byte) 0xCA;  // INS
        commandApdu[2] = (byte) 0x00;  // P1
        commandApdu[3] = (byte) 0x00;  // P2
        commandApdu[4] = (byte) 0x01;  // Lc
        commandApdu[5] = (byte) (file & 0x0FF);
        commandApdu[commandApdu.length - 1] = (byte) 0x00;  // Le
        return commandApdu;
    }

    private byte[] putDataApdu(int fileNumber, byte[] dataToWrite) {
        byte[] commandApdu = new byte[6 + 1 + dataToWrite.length]; // 6 + fileNumber + dataToWrite
        commandApdu[0] = (byte) 0x00;  // CLA
        commandApdu[1] = (byte) 0xDA;  // INS
        commandApdu[2] = (byte) 0x00;  // P1
        commandApdu[3] = (byte) 0x00;  // P2
        commandApdu[4] = (byte) ((dataToWrite.length + 1) & 0x0FF);       // Lc
        commandApdu[5] = (byte) (fileNumber & 0x0FF); // file number
        System.arraycopy(dataToWrite, 0, commandApdu, 6, dataToWrite.length); // dataToWrite
        commandApdu[commandApdu.length - 1] = (byte) 0x00;  // Le
        return commandApdu;
    }

    /**
     * checks if the response has an 0x'9000' at the end means success
     * and the method returns true.
     * if any other trailing bytes show up the method returns false
     *
     * @param data
     * @return
     */
    private boolean checkResponse(@NonNull byte[] data) {
        // simple sanity check
        if (data.length < 2) {
            return false;
        } // not ok
        int status = ((0xff & data[data.length - 2]) << 8) | (0xff & data[data.length - 1]);
        if (status == 0x9000) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Return the data without the attached status bytes
     * @param data
     * @return
     */
    private byte[] returnDataBytes(byte[] data) {
        if (data == null) return null;
        if (data.length < 3) return null;
        return Arrays.copyOfRange(data, 0, (data.length - 2));
    }

    /**
     * Sound files downloaded from Material Design Sounds
     * https://m2.material.io/design/sound/sound-resources.html
     */
    private void playSinglePing() {
        MediaPlayer mp = MediaPlayer.create(getContext(), R.raw.notification_decorative_02);
        mp.start();
    }

    private void playDoublePing() {
        MediaPlayer mp = MediaPlayer.create(getContext(), R.raw.notification_decorative_01);
        mp.start();
    }

    private void writeToUiAppend(String message) {
        //System.out.println(message);
        outputString = outputString + message + "\n";
    }

    private void writeToUiFinal(final TextView textView) {
        if (textView == (TextView) readResult) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textView.setText(outputString);
                    System.out.println(outputString); // print the data to console
                }
            });
        }
    }

    /**
     * shows a progress bar as long as the reading lasts
     *
     * @param isVisible
     */

    private void setLoadingLayoutVisibility(boolean isVisible) {
        getActivity().runOnUiThread(() -> {
            if (isVisible) {
                loadingLayout.setVisibility(View.VISIBLE);
            } else {
                loadingLayout.setVisibility(View.GONE);
            }
        });
    }

    private void showWirelessSettings() {
        Toast.makeText(this.getContext(), "You need to enable NFC", Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mNfcAdapter != null) {

            if (!mNfcAdapter.isEnabled())
                showWirelessSettings();

            Bundle options = new Bundle();
            // Work around for some broken Nfc firmware implementations that poll the card too fast
            options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250);

            // Enable ReaderMode for NfcA types of card and disable platform sounds
            // the option NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK is NOT set
            // to get the data of the tag after reading
            mNfcAdapter.enableReaderMode(this.getActivity(),
                    this,
                    NfcAdapter.FLAG_READER_NFC_A |
                            NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                    options);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableReaderMode(this.getActivity());
    }

}