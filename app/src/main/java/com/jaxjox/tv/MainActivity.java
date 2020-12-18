package com.jaxjox.tv;

import android.app.Activity;
import android.os.Bundle;

import com.jaxjox.tv.remote.R;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        System.out.println("MainActivity  OnCreate()....");

    }

    @Override
    protected void onStart() {
        super.onStart();
        finish();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (getIntent().getAction().equals(NfcAdapter.ACTION_NDEF_DISCOVERED)) {
//            Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
//            if (rawMsgs != null) {
//                NdefMessage[] msgs = new NdefMessage[rawMsgs.length];
//                for (int i = 0; i < rawMsgs.length; i++) {
//                    msgs[i] = (NdefMessage) rawMsgs[i];
//                }
//                runNFCTagData(msgs[0].getRecords()[0].getPayload());
//                startActivity(new Intent(this, AlarmList.class));
//            } else {
////                Toast.makeText(this, "getResources().getString(R.string.nfc_ndef_not_found)", Toast.LENGTH_LONG).show();
//            }
//        }
    }
}