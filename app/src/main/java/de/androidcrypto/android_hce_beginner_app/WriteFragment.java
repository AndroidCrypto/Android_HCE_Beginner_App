package de.androidcrypto.android_hce_beginner_app;

import static de.androidcrypto.android_hce_beginner_app.Utils.doVibrate;

import android.content.Intent;
import android.media.MediaPlayer;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link WriteFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class WriteFragment extends Fragment implements NfcAdapter.ReaderCallback {

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";
    private static final String TAG = "WriteFragment";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private com.google.android.material.textfield.TextInputEditText dataToSend, resultNfcWriting;
    private SwitchMaterial addTimestampToData;
    private AutoCompleteTextView autoCompleteTextView;
    private com.google.android.material.textfield.TextInputLayout dataToSendLayout;
    private RadioButton rbNoAuth, rbDefaultAuth, rbCustomAuth;
    private View loadingLayout;
    private NfcAdapter mNfcAdapter;
    private NfcA nfcA;
    private boolean isTagUltralight = false;
    private int pageToWrite;
    private String outputString = ""; // used for the UI output

    public WriteFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment SendFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static WriteFragment newInstance(String param1, String param2) {
        WriteFragment fragment = new WriteFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_write, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        dataToSend = getView().findViewById(R.id.etWriteData);
        dataToSendLayout = getView().findViewById(R.id.etWriteDataLayout);
        resultNfcWriting = getView().findViewById(R.id.etMainResult);
        addTimestampToData = getView().findViewById(R.id.swMainAddTimestampSwitch);
        addTimestampToData.setChecked(false);
        rbNoAuth = getView().findViewById(R.id.rbNoAuth);
        rbDefaultAuth = getView().findViewById(R.id.rbDefaultAuth);
        rbCustomAuth = getView().findViewById(R.id.rbCustomAuth);
        loadingLayout = getView().findViewById(R.id.loading_layout);

        //mNfcAdapter = NfcAdapter.getDefaultAdapter(getView().getContext());

        Intent intent = new Intent(view.getContext(), MyHostApduService.class);
        intent.putExtra("ndefMessage", "abc");
        Toast.makeText(view.getContext(), "This message is send as NDEF message: " + "abc", Toast.LENGTH_SHORT).show();
        requireActivity().startService(intent);


        addTimestampToData.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                // format YYMMDD HH:mm:SS
                // e.g.   240829 11:39:42
                // timestamp is 15 chars long + 1 trailing " " = 16 characters = 4 PAGES
                if (b) {
                    dataToSendLayout.setEnabled(false);
                } else {
                    dataToSendLayout.setEnabled(true);
                    dataToSendLayout.setCounterMaxLength(16);
                    setEditTextMaxLength(dataToSend, 16);
                }
            }
        });

    }

    private void setEditTextMaxLength(EditText et, int maxLength) {
        et.setFilters(new InputFilter[]{new InputFilter.LengthFilter(maxLength)});
    }

    /**
     * section for NFC
     */

    @Override
    public void onTagDiscovered(Tag tag) {
        System.out.println("NFC tag discovered");
        playSinglePing();

        boolean success;
        boolean authSuccess = false;

        setLoadingLayoutVisibility(true);
        outputString = "";

        requireActivity().runOnUiThread(() -> {
            resultNfcWriting.setBackgroundColor(getResources().getColor(R.color.white));
            resultNfcWriting.setText("");
        });
/*
        // you should have checked that this device is capable of working with Mifare Ultralight tags, otherwise you receive an exception
        nfcA = NfcA.get(tag);

        if (nfcA == null) {
            writeToUiAppend("The tag is not readable with NfcA classes, sorry");
            writeToUiFinal(resultNfcWriting);
            setLoadingLayoutVisibility(false);
            returnOnNotSuccess();
            return;
        }

        // get card details
        byte[] tagId = nfcA.getTag().getId();
        String[] techList = nfcA.getTag().getTechList();
        StringBuilder sb = new StringBuilder();
        sb.append("Technical Data of the Tag").append("\n");
        sb.append("Tag ID: ").append(bytesToHexNpe(tagId)).append("\n");
        sb.append("Tech-List:").append("\n");
        sb.append("Tag TechList: ").append(Arrays.toString(techList)).append("\n");
        if (identifyUltralightFamily(nfcA)) {
            sb.append("The Tag seems to be a MIFARE Ultralight Family tag").append("\n");
            isTagUltralight = true;
        } else {
            sb.append("The Tag IS NOT a MIFARE Ultralight tag").append("\n");
            sb.append("** End of Processing **").append("\n");
            isTagUltralight = false;
        }
        writeToUiAppend(sb.toString());

        // stop processing if not an Ultralight Family tag
        if (!isTagUltralight) {
            returnOnNotSuccess();
            return;
        }

        // you should have checked that this device is capable of working with Mifare Ultralight tags, otherwise you receive an exception

        String sendData = dataToSend.getText().toString();
        if (addTimestampToData.isChecked()) sendData = getTimestampShort() + " ";
        if (TextUtils.isEmpty(sendData)) {
            writeToUiAppend("Please enter some data to write on tag. Aborted");
            writeToUiFinal(resultNfcWriting);
            return;
        }
        if (sendData.length() > 16) sendData = sendData.substring(0, 16);

        try {
            nfcA.connect();
            writeToUiAppend("This is an Ultralight C tag with 48 pages = 192 bytes memory");

            if (rbNoAuth.isChecked()) {
                writeToUiAppend("No Authentication requested");
                authSuccess = true;
            } else if (rbDefaultAuth.isChecked()) {
                writeToUiAppend("Authentication with Default Key requested");
                //authSuccess = doAuthenticateUltralightCDefault();
                //byte[] defaultKey = "BREAKMEIFYOUCAN!".getBytes(StandardCharsets.UTF_8);
                //authSuccess = authenticateUltralightC(nfcA, defaultKey);
                authSuccess = authenticateUltralightC(nfcA, defaultAuthKey);
                writeToUiAppend("authenticateUltralightC with defaultAuthKey success: " + authSuccess);
            } else {
                writeToUiAppend("Authentication with Custom Key requested");
                authSuccess = authenticateUltralightC(nfcA, customAuthKey);
                //authSuccess = doAuthenticateUltralightCCustom();
                //authSuccess = authenticateUltralightC(nfcA, customAuthKey);
                writeToUiAppend("authenticateUltralightC with customAuthKey success: " + authSuccess);
            }

            if (!authSuccess) {
                writeToUiAppend("The authentication was not successful, operation aborted.");
                returnOnNotSuccess();
                return;
            }

            // get page to write
            String choiceString = autoCompleteTextView.getText().toString();
            pageToWrite = Integer.parseInt(choiceString);

            byte[] dtw = new byte[16];
            System.arraycopy(sendData.getBytes(StandardCharsets.UTF_8), 0, dtw, 0, sendData.getBytes(StandardCharsets.UTF_8).length); // this is an array filled up with 0x00
            writeToUiAppend(printData("data to write", dtw));
            // split dtw (16 bytes long) into 4 byte arrays for page 1 to 4
            byte[] page1 = Arrays.copyOfRange(dtw, 0, 4);
            byte[] page2 = Arrays.copyOfRange(dtw, 4, 8);
            byte[] page3 = Arrays.copyOfRange(dtw, 8, 12);
            byte[] page4 = Arrays.copyOfRange(dtw, 12, 16);

            // write to tag
            success = writePageMifareUltralightC(nfcA, pageToWrite, page1);
            writeToUiAppend("Tried to write data to tag on page " + pageToWrite + ", success ? : " + success);
            success = writePageMifareUltralightC(nfcA, pageToWrite + 1, page2);
            writeToUiAppend("Tried to write data to tag on page " + (pageToWrite + 1) + ", success ? : " + success);
            success = writePageMifareUltralightC(nfcA, pageToWrite + 2, page3);
            writeToUiAppend("Tried to write data to tag on page " + (pageToWrite + 2) + ", success ? : " + success);
            success = writePageMifareUltralightC(nfcA, pageToWrite + 3, page4);
            writeToUiAppend("Tried to write data to tag on page " + (pageToWrite + 3) + ", success ? : " + success);
            nfcA.close();
        } catch (IOException e) {
            writeToUiAppend("IOException on connection: " + e.getMessage());
            e.printStackTrace();
        }
*/
        writeToUiFinal(resultNfcWriting);
        playDoublePing();
        setLoadingLayoutVisibility(false);
        doVibrate(getActivity());
        reconnect(nfcA);

    }

    private void returnOnNotSuccess() {
        writeToUiAppend("=== Return on Not Success ===");
        writeToUiFinal(resultNfcWriting);
        playDoublePing();
        setLoadingLayoutVisibility(false);
        doVibrate(getActivity());
        mNfcAdapter.disableReaderMode(this.getActivity());
    }

    private void reconnect(NfcA nfcA) {
        // this is just an advice - if an error occurs - close the connection and reconnect the tag
        // https://stackoverflow.com/a/37047375/8166854
        try {
            nfcA.close();
            Log.d(TAG, "Close NfcA");
        } catch (Exception e) {
            Log.e(TAG, "Exception on Close NfcA: " + e.getMessage());
        }
        try {
            Log.d(TAG, "Reconnect NfcA");
            nfcA.connect();
        } catch (Exception e) {
            Log.e(TAG, "Exception on Reconnect NfcA: " + e.getMessage());
        }
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
        outputString = outputString + message + "\n";
    }

    private void writeToUiFinal(final TextView textView) {
        if (textView == (TextView) resultNfcWriting) {
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

    private void showMessage(String message) {
        getActivity().runOnUiThread(() -> {
            Toast.makeText(getContext(),
                    message,
                    Toast.LENGTH_SHORT).show();
            resultNfcWriting.setText(message);
        });
    }

    private void showWirelessSettings() {
        Toast.makeText(getView().getContext(), "You need to enable NFC", Toast.LENGTH_SHORT).show();
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
            mNfcAdapter.enableReaderMode(getActivity(),
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
            mNfcAdapter.disableReaderMode(getActivity());
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}