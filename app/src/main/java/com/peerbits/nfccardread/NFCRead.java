package com.peerbits.nfccardread;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import java.io.IOException;

public class NFCRead extends Activity {

    public static final String TAG = NFCRead.class.getSimpleName();
    private TextView num_1;
    private TextView num_2;
    private NfcAdapter mNfcAdapter;

    private ImageView ivBack;
    int team_1 = 0;
    int team_2 = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nfc_read);
        initViews();
        ivBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }

    private void initViews() {
        this.num_1 = findViewById(R.id.num_1);
        this.num_2 = findViewById(R.id.num_2);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        ivBack = findViewById(R.id.ivBack);
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] nfcIntentFilter = new IntentFilter[]{techDetected, tagDetected, ndefDetected};

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        if (mNfcAdapter != null)
            mNfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mNfcAdapter != null)
            mNfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        patchTag(tag);
        if (tag != null) {
            readFromNFC(tag, intent);
        }
    }


    public Tag patchTag(Tag oTag) {
        if (oTag == null)
            return null;

        String[] sTechList = oTag.getTechList();

        Parcel oParcel, nParcel;

        oParcel = Parcel.obtain();
        oTag.writeToParcel(oParcel, 0);
        oParcel.setDataPosition(0);

        int len = oParcel.readInt();
        byte[] id = null;
        if (len >= 0) {
            id = new byte[len];
            oParcel.readByteArray(id);
        }
        int[] oTechList = new int[oParcel.readInt()];
        oParcel.readIntArray(oTechList);
        Bundle[] oTechExtras = oParcel.createTypedArray(Bundle.CREATOR);
        int serviceHandle = oParcel.readInt();
        int isMock = oParcel.readInt();
        IBinder tagService;
        if (isMock == 0) {
            tagService = oParcel.readStrongBinder();
        } else {
            tagService = null;
        }
        oParcel.recycle();

        int nfca_idx = -1;
        int mc_idx = -1;

        for (int idx = 0; idx < sTechList.length; idx++) {
            if (sTechList[idx] == NfcA.class.getName()) {
                nfca_idx = idx;
            } else if (sTechList[idx] == MifareClassic.class.getName()) {
                mc_idx = idx;
            }
        }

        if (nfca_idx >= 0 && mc_idx >= 0 && oTechExtras[mc_idx] == null) {
            oTechExtras[mc_idx] = oTechExtras[nfca_idx];
        } else {
            return oTag;
        }

        nParcel = Parcel.obtain();
        nParcel.writeInt(id.length);
        nParcel.writeByteArray(id);
        nParcel.writeInt(oTechList.length);
        nParcel.writeIntArray(oTechList);
        nParcel.writeTypedArray(oTechExtras, 0);
        nParcel.writeInt(serviceHandle);
        nParcel.writeInt(isMock);
        if (isMock == 0) {
            nParcel.writeStrongBinder(tagService);
        }
        nParcel.setDataPosition(0);
        Tag nTag = Tag.CREATOR.createFromParcel(nParcel);
        nParcel.recycle();

        return nTag;
    }

    private void readFromNFC(Tag tag, Intent intent) {

        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();

                if (ndefMessage != null) {
                    Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

                    if (messages != null) {
                        NdefMessage[] ndefMessages = new NdefMessage[messages.length];
                        for (int i = 0; i < messages.length; i++) {
                            ndefMessages[i] = (NdefMessage) messages[i];
                        }
                        NdefRecord record = ndefMessages[0].getRecords()[0];

                        byte[] payload = record.getPayload();
                        String text = new String(payload);

                        //GAME START
                        if (text.equals("1")) {
                            team_1++;
                            String str_num_1 = Integer.toString(team_1);
                            String str_num_2 = Integer.toString(team_2);
                            num_1.setText(str_num_1);
                            // matchball
                            if(team_1 == 3 & team_2 != 2 & team_2 != 3) {
                                team_1 = 0;
                                team_2 = 0;

                                str_num_1 = Integer.toString(team_1);
                                str_num_2 = Integer.toString(team_2);
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "Команда 1 победила", Toast.LENGTH_SHORT);
                                toast.show();

                                num_1.setText(str_num_1);
                                num_2.setText(str_num_2);
                            }
                            // Meaning for stop game
                            if(team_1 >=3 & team_1 - team_2 == 2){
                                team_1 = 0;
                                team_2 = 0;
                                str_num_1 = Integer.toString(team_1);
                                str_num_2 = Integer.toString(team_2);
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "Команда 1 победила", Toast.LENGTH_SHORT);
                                toast.show();
                                num_1.setText(str_num_1);
                                num_2.setText(str_num_2);

                            }


                        }

                        // Meaning for score second team
                        if (text.equals("2")) {
                            team_2++;
                            String str_num_2 = Integer.toString(team_2);
                            String str_num_1 = Integer.toString(team_1);

                            num_2.setText(str_num_2);
                            if (team_2 == 3 & team_1 != 2 & team_1 != 3) {
                                team_1 = 0;
                                team_2 = 0;
                                str_num_1 = Integer.toString(team_1);
                                str_num_2 = Integer.toString(team_2);
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "Команда 2 победила", Toast.LENGTH_SHORT);
                                toast.show();
                                num_1.setText(str_num_1);
                                num_2.setText(str_num_2);
                            }
                            if(team_2 >= 3 & team_2 - team_1 == 2) {
                                team_1 = 0;
                                team_2 = 0;
                                str_num_1 = Integer.toString(team_1);
                                str_num_2 = Integer.toString(team_2);
                                Toast toast = Toast.makeText(getApplicationContext(),
                                        "Команда 2 победила", Toast.LENGTH_SHORT);
                                toast.show();
                                num_1.setText(str_num_1);
                                num_2.setText(str_num_2);
                            }
                        }
                        Log.e("tag", "vahid  -->" + text);

                        ndef.close();




                    }

                } else {
                    Toast.makeText(this, "Not able to read from NFC, Please try again...", Toast.LENGTH_LONG).show();

                }
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        NdefMessage ndefMessage = ndef.getNdefMessage();

                        if (ndefMessage != null) {
                            String message = new String(ndefMessage.getRecords()[0].getPayload());
                            Log.d(TAG, "NFC found.. " + "readFromNFC: " + message);
//                            Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                            //This message dont work in our case
                            num_1.setText(message);
                            ndef.close();
                        } else {
                            Toast.makeText(this, "Not able to read from NFC, Please try again...", Toast.LENGTH_LONG).show();

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, "NFC is not readable", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }





    }
}
