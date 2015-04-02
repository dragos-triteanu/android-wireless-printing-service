package com.mediasaturn.print.activities;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.mediasaturn.print.init.Installer;
import com.mediasaturn.print.model.Printer;
import com.mediasaturn.print.services.Cups;
import com.mediasaturn.print.services.CupsPrintService;
import com.mediasaturn.print.utils.PrinterUtils;

import org.cups.android.R;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Semaphore;

public class MainActivityRemade extends Activity {
    private static final String TAG = "MainActivity";

    private boolean destroyed = false;
    private boolean jobsThreadStarted = false;
    private boolean jobsThreadPaused = false;
    private String[] printJobs = new String[0];
    private Semaphore printJobsUpdate = new Semaphore(0);

    Button addPrinterButton;
    Button enablePrinterSharingSettings;
    Button importQRPrinterButton;

    private ProgressDialog progressCircle = null;

    private TextView installingNotice = null;
    private TextView turnOnPrintingServiceText = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.spinner_item);
        destroyed = false;
        jobsThreadStarted = false;

        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        showInitializationUI();
    }

    @Override
    synchronized protected void onDestroy()
    {
        Log.d(TAG, "onDestroy");
        destroyed = true;
        jobsThreadStarted = false;
        super.onDestroy();
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (!Installer.unpacking)
            renderUiOnUnThread();
        jobsThreadPaused = false;
    }

    @Override protected void onStop(){
        super.onStop();
        jobsThreadPaused = true;
    }

    @Override
    protected void onResume(){
        renderUiOnUnThread();
        super.onResume();
    }

    public void renderUiOnUnThread(){
        runOnUiThread(new Runnable(){
            public void run(){
                showInitializationUI();
                renderUI();
            }
        });
    }


    void showInitializationUI(){

        setContentView(R.layout.spinner_item);
        installingNotice = (TextView) findViewById(R.id.installingNotice);
        installingNotice.setText(R.string.init);
        installingNotice.setTextSize(20);
        installingNotice.setPadding(20, 20, 20, 50);

        progressCircle = new ProgressDialog(this);
        progressCircle.setMessage(getResources().getString(R.string.please_wait));
    }

    public void renderUI(){
        //TODO make visible if service is running
        boolean isServiceRunning = isServiceRunning(CupsPrintService.class);

        setContentView(R.layout.activity_main_activity_remade);

        addPrinterButton = (Button) findViewById(R.id.addPrinterButton);
        addPrinterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivityRemade.this.startActivity(new Intent(MainActivityRemade.this, AddPrinterActivity.class));
            }
        });

        enablePrinterSharingSettings = (Button) findViewById(R.id.enablePrinterOptionsButton);
        enablePrinterSharingSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MainActivityRemade.this.startActivity(new Intent(Settings.ACTION_PRINT_SETTINGS));
            }
        });

        String[] printers = Cups.getPrinters(this);
        List<String> printerList = Arrays.asList(printers);

        ListView listView = (ListView) findViewById(R.id.printerListView);
        listView.setAdapter(new PrinterListAdapter(this, printerList));


        importQRPrinterButton = (Button) findViewById(R.id.importQRPrinter);
        importQRPrinterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressCircle.setMessage("Preparing QR scanner");
                progressCircle.show();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        PrinterUtils.printerTypes = Cups.getPrinterModels(MainActivityRemade.this);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressCircle.dismiss();
                                MainActivityRemade.this.startActivity(new Intent(MainActivityRemade.this,QRScannerActivity.class));
                            }
                        });
                    }
                }).start();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main_activity_remade, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private boolean isServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    /***
     * Adapter class that creates a row of the printer list ListView.
     * The class also adds action listeners for removeButton click, and for row click.
     */
    public class PrinterListAdapter extends ArrayAdapter<String> {
        private static final String TAG = "PrinterListAdapter";

        private List<String> printerList;
        private Context context;


        public PrinterListAdapter(Context context, List<String> objects) {
            super(context, R.layout.printer_list_item, objects);
            this.printerList = objects;
            this.context = context;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View rowView = inflater.inflate(R.layout.printer_list_item, parent, false);

            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    progressCircle.setMessage("Fetching printer information.Please wait");
                    progressCircle.setTitle("Edit Printer");
                    progressCircle.show();

                    final TextView printerLabel = (TextView) v.findViewById(R.id.printerName);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String clickedPrinterName = printerLabel.getText().toString();
                            Printer printerToEdit = Cups.getPrinterByPrinterName(MainActivityRemade.this, clickedPrinterName);
                            final Intent intent = new Intent(MainActivityRemade.this,EditPrinterActivity.class);
                            intent.putExtra("printerToEdit",printerToEdit);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressCircle.hide();
                                    MainActivityRemade.this.startActivity(intent);
                                }
                            });
                        }
                    }).start();
                }
            });

            final TextView printerNameText = (TextView) rowView.findViewById(R.id.printerName);
            Button deleteButton = (Button) rowView.findViewById(R.id.deletePrinterButton);

            printerNameText.setText(printerList.get(position));
            deleteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    AlertDialog.Builder alertBuilder = new AlertDialog.Builder(context);
                    alertBuilder.setTitle("Remove printer").setMessage("Are You sure you want to remove printer '"+printerNameText.getText()+"'?");

                    alertBuilder.setPositiveButton(R.string.ok,new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            progressCircle.show();
                            Cups.deletePrinter(context, printerList.get(printerList.indexOf(printerNameText.getText())));
                            Cups.updatePrintersInfo(context);
                            progressCircle.hide();
                            dialog.dismiss();
                            context.startActivity(new Intent(context, MainActivityRemade.class));
                        }
                    });



                    alertBuilder.setNegativeButton(R.string.cancel,new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    alertBuilder.create().show();
                }
            });

            return rowView;
        }
    }

}
