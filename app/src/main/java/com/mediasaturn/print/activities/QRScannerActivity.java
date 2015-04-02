package com.mediasaturn.print.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.google.zxing.Result;

import me.dm7.barcodescanner.zbar.ZBarScannerView;

/**
 * Created by dragos.triteanu on 3/30/2015.
 */
public class QRScannerActivity extends Activity implements ZBarScannerView.ResultHandler {
    private ZBarScannerView mScannerView;
    private static final String TAG = "ScannerActivity";

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mScannerView = new ZBarScannerView(this);    // Programmatically initialize the scanner view
        setContentView(mScannerView);                // Set the scanner view as the content view
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this); // Register ourselves as a handler for scan results.
        mScannerView.startCamera();          // Start camera on resume
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();           // Stop camera on pause
    }

    @Override
    public void handleResult(me.dm7.barcodescanner.zbar.Result result) {
        String printerUrl = result.getContents();
        Log.d(TAG, "Received following contents from QR code: "+result.getContents());
        Intent intent = new Intent(QRScannerActivity.this,AddPrinterActivity.class);
        intent.putExtra("qrPrinter",printerUrl);
        QRScannerActivity.this.startActivity(intent);
        QRScannerActivity.this.finish();
    }
}
