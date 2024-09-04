package de.androidcrypto.android_hce_beginner_app;

import static de.androidcrypto.android_hce_beginner_app.Utils.doVibrate;

import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.cardemulation.CardEmulation;
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

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.io.IOException;
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
    private TextView readSpan;
    private TextView readSpanLegend;
    private RadioButton rbNoAuth, rbDefaultAuth, rbCustomAuth;
    private RadioButton rbNoCounterIncrease, rbCounterIncrease;
    private View loadingLayout;
    private String outputString = ""; // used for the UI output
    private NfcAdapter mNfcAdapter;
    private NfcA nfcA;
    private boolean isTagUltralight = false;


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
        readSpan = getView().findViewById(R.id.tvSpan);
        readSpanLegend = getView().findViewById(R.id.tvSpanLegend);
        rbNoAuth = getView().findViewById(R.id.rbNoAuth);
        rbDefaultAuth = getView().findViewById(R.id.rbDefaultAuth);
        rbCustomAuth = getView().findViewById(R.id.rbCustomAuth);
        rbNoCounterIncrease = getView().findViewById(R.id.rbNoCounterIncrease);
        rbCounterIncrease = getView().findViewById(R.id.rbCounterIncrease);
        loadingLayout = getView().findViewById(R.id.loading_layout);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_read, container, false);
    }

    // This method is running in another thread when a card is discovered
    // !!!! This method cannot cannot direct interact with the UI Thread
    // Use `runOnUiThread` method to change the UI from this method
    @Override
    public void onTagDiscovered(Tag tag) {
        // Read and or write to Tag here to the appropriate Tag Technology type class
        // in this example the card should be an NfcA Technology Type

        System.out.println("NFC tag discovered");
        playSinglePing();

        boolean success;
        boolean authSuccess = false;

        setLoadingLayoutVisibility(true);
        outputString = "";

        requireActivity().runOnUiThread(() -> {
            readResult.setBackgroundColor(getResources().getColor(R.color.white));
            readResult.setText("");
        });
/*
        // you should have checked that this device is capable of working with Mifare Ultralight tags, otherwise you receive an exception
        nfcA = NfcA.get(tag);

        if (nfcA == null) {
            writeToUiAppend("The tag is not readable with NfcA classes, sorry");
            writeToUiFinal(readResult);
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

        // go through all sectors
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

            // increase the counter value if requested
            if (rbNoCounterIncrease.isChecked()) {
                writeToUiAppend("No Counter Increase requested");
            } else {
                writeToUiAppend("Counter Increase requested");
                if (!authSuccess) {
                    writeToUiAppend("Previous Auth was not successful or not done, skipped");
                } else {
                    success = increaseCounterValueByOne(nfcA);
                    writeToUiAppend("Status of increaseCounterValueByOne command to page 41: " + success);
                }
            }

            // get the current counter
            int counterValue = getCounterValue(nfcA);
            writeToUiAppend("Current Counter Value: " + counterValue);

            // read complete memory with colored data
            byte[] memoryContent = readCompleteContent(nfcA);
            String memoryDumpString = HexDumpOwn.prettyPrint(memoryContent);

            SpannableString spanString = new SpannableString(memoryDumpString);
            // UID = RED
            spanString.setSpan(new BackgroundColorSpan(Color.RED), 10, 18, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            spanString.setSpan(new BackgroundColorSpan(Color.RED), 22, 33, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            // Lock Bytes = CYAN
            spanString.setSpan(new BackgroundColorSpan(Color.CYAN), 50, 55, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            spanString.setSpan(new BackgroundColorSpan(Color.CYAN), (20 * 34) + 10, (20 * 34) + 15, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            // OTP = YELLOW
            spanString.setSpan(new BackgroundColorSpan(Color.YELLOW), (1 * 34) + 22, (1 * 34) + 33, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            // User Memory = GREEN
            for (int i = 0; i < 18; i++) {
                spanString.setSpan(new BackgroundColorSpan(Color.GREEN), ((i + 2) * 34) + 10, ((i + 2) * 34) + 33, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            }
            // Counter = Magenta
            spanString.setSpan(new BackgroundColorSpan(Color.MAGENTA), (20 * 34) + 22, (20 * 34) + 27, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            // Authentication Configuration
            spanString.setSpan(new BackgroundColorSpan(Color.LTGRAY), (21 * 34) + 10, (21 * 34) + 33, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            // Authentication Keys
            spanString.setSpan(new BackgroundColorSpan(Color.GRAY), (22 * 34) + 10, (22 * 34) + 33, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            spanString.setSpan(new BackgroundColorSpan(Color.GRAY), (23 * 34) + 10, (23 * 34) + 33, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

            StringBuilder sbL = new StringBuilder();
            sbL.append("Colored Data Legend").append("\n");
            sbL.append("Tag UID").append("\n");
            sbL.append("Lock Bytes").append("\n");
            sbL.append("One Time Programming Area").append("\n");
            sbL.append("User Memory Area").append("\n");
            sbL.append("16-Bit Counter").append("\n");
            sbL.append("Authentication Configuration").append("\n");
            sbL.append("Authentication Keys").append("\n");

            SpannableString spanLegendString = new SpannableString(sbL.toString());
            spanLegendString.setSpan(new BackgroundColorSpan(Color.RED), 20, 27, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            spanLegendString.setSpan(new BackgroundColorSpan(Color.CYAN), 28, 38, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            spanLegendString.setSpan(new BackgroundColorSpan(Color.YELLOW), 39, 64, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            spanLegendString.setSpan(new BackgroundColorSpan(Color.GREEN), 65, 81, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            spanLegendString.setSpan(new BackgroundColorSpan(Color.MAGENTA), 82, 96, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            spanLegendString.setSpan(new BackgroundColorSpan(Color.LTGRAY), 97, 125, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            spanLegendString.setSpan(new BackgroundColorSpan(Color.GRAY), 126, 145, Spannable.SPAN_INCLUSIVE_INCLUSIVE);

            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    readSpan.setText(spanString);
                    readSpanLegend.setText(spanLegendString);
                }
            });

            nfcA.close();
        } catch (IOException e) {
            writeToUiAppend("IOException on connection: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            writeToUiAppend("Exception on connection: " + e.getMessage());
            e.printStackTrace();
        }
*/
        writeToUiFinal(readResult);
        playDoublePing();
        setLoadingLayoutVisibility(false);
        doVibrate(getActivity());
        reconnect(nfcA);
    }

    private void returnOnNotSuccess() {
        writeToUiAppend("=== Return on Not Success ===");
        writeToUiFinal(readResult);
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