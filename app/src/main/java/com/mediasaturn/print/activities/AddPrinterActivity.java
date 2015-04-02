package com.mediasaturn.print.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.mediasaturn.print.model.Printer;
import com.mediasaturn.print.services.Cups;
import com.mediasaturn.print.utils.PrinterUtils;

import org.apache.commons.lang3.StringUtils;
import org.cups.android.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mediasaturn.print.utils.PrinterUtils.VALID;
import static com.mediasaturn.print.utils.PrinterUtils.filterPrintersBySeedWordAndPCL;
import static com.mediasaturn.print.utils.PrinterUtils.getKeyByValue;
import static com.mediasaturn.print.utils.PrinterUtils.printerProtocols;


public class AddPrinterActivity extends Activity {
    private static final String TAG = "AddPrinterActivity";


    /** User inputs */
    private EditText printerNameInput;
    private Spinner printerProtocolSpinner;
    private EditText printerHostInput;
    private EditText printerShareInput;
    private Spinner printerProducerSpinner;

    private EditText printerModelInput;
    private Button printerModelButton;

    private Button addPrinterButton;
    static final List<String> printerProducers = Arrays.asList("HP","Generic");

    private ProgressDialog progressCircle = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_printer);
        progressCircle = new ProgressDialog(this);
        progressCircle.setMessage("Please wait while adding printer.");
        progressCircle.setTitle("Add printer");

        renderUI();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_printer, menu);
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

    public void renderUI(){

        printerNameInput = (EditText) findViewById(R.id.printerNameInput);
        printerNameInput.setHint("PrinterName");

        printerProtocolSpinner = (Spinner) findViewById(R.id.printerProtocolSpinner);
        ArrayAdapter<String> protocolSpinnerAdapter = new ArrayAdapter<String>(AddPrinterActivity.this,android.R.layout.simple_spinner_item,new ArrayList<String>(printerProtocols.keySet()));
        printerProtocolSpinner.setAdapter(protocolSpinnerAdapter);

        printerHostInput = (EditText) findViewById(R.id.printerHostInput);
        printerHostInput.setHint(R.string.printer_host_placeholder);

        printerShareInput = (EditText) findViewById(R.id.printerShareInput);
        printerShareInput.setHint(R.string.printer_share_placeholder);


        ArrayAdapter<String> printerProducerAdapter = new ArrayAdapter<String>(AddPrinterActivity.this,android.R.layout.simple_spinner_item, printerProducers);
        printerProducerSpinner = (Spinner) findViewById(R.id.printerModelSpinner);
        printerProducerSpinner.setAdapter(printerProducerAdapter);


        printerModelInput = (EditText) findViewById(R.id.printerModelInput);
        printerModelInput.setHint("Enter a keyword, to filter the printers");

        printerModelButton = (Button) findViewById(R.id.printerModelButton);
        printerModelButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                printerModelInput.setEnabled(true);
                progressCircle = ProgressDialog.show(AddPrinterActivity.this,"Add printer","Please wait while the printer models are being fetched.This might take a while");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //TODO refactore so that this list is kept in MainActivity(for state purposes), and fast retrieval.
                        PrinterUtils.printerTypes = Cups.getPrinterModels(AddPrinterActivity.this);

                        String printerProducer = printerProducers.get(printerProducerSpinner.getSelectedItemPosition());
                        List<String> printersByProducer = PrinterUtils.filterPrintersByProducer(printerProducer);
                        String seedWord = printerModelInput.getText().toString();

                        final List<CharSequence> values = new ArrayList<CharSequence>();
                        final AlertDialog.Builder builder = new AlertDialog.Builder(AddPrinterActivity.this);
                        builder.setTitle(R.string.select_printer_model);

                        if(StringUtils.isEmpty(seedWord)){
                            values.addAll(printersByProducer);
                        }else{
                            for (String s: printersByProducer){
                                if (filterPrintersBySeedWordAndPCL(s, seedWord.toLowerCase())){
                                    Log.d(TAG, "Found model: " + s);
                                    values.add(s);
                                }
                            }
                        }
                        if (values.size() == 0){
                            builder.setMessage(R.string.no_models_found);
                            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface d, int s)
                                {
                                    d.dismiss();
                                }
                            });
                        }
                        else{
                            builder.setItems(values.toArray(new CharSequence[0]), new DialogInterface.OnClickListener()
                            {
                                public void onClick(DialogInterface dialog, int which)
                                {
                                    printerModelInput.setText(values.get(which));
                                }
                            });
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                AlertDialog alert = builder.create();
                                alert.setOwnerActivity(AddPrinterActivity.this);
                                alert.show();
                                progressCircle.hide();
                                progressCircle.setMessage("Please wait while adding printer");
                            }
                        });
                    }
                }).start();
            }
        });


        addPrinterButton = (Button) findViewById(R.id.addPrinterButton);
        addPrinterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               final Printer printer = PrinterUtils.buildPrinter(printerNameInput.getText().toString(),
                                                            printerProtocols.get(printerProtocolSpinner.getSelectedItem()),
                                                            printerHostInput.getText().toString(),
                                                            printerShareInput.getText().toString(),
                                                            Cups.getManufacturerByModelName(printerModelInput.getText().toString()),
                                                            printerModelInput.getText().toString());

                String printerStatus = PrinterUtils.validatePrinter(printer);

                if (!printerStatus.equals(PrinterUtils.VALID)){
                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(AddPrinterActivity.this);
                    alertDialogBuilder.setTitle(R.string.error);
                    alertDialogBuilder.setMessage(printerStatus);
                    alertDialogBuilder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int s) {
                            d.dismiss();
                        }
                    });
                    AlertDialog alert = alertDialogBuilder.create();
                    alert.setOwnerActivity(AddPrinterActivity.this);
                    alert.show();
                    return;
                }
                progressCircle.show();
                new Thread(new Runnable(){
                    public void run(){
                        Cups.addPrinter(AddPrinterActivity.this, printer);
                        Cups.updatePrintersInfo(AddPrinterActivity.this);
                        runOnUiThread(new Runnable()
                        {
                            public void run()
                            {
                                progressCircle.dismiss();
                                Toast.makeText(AddPrinterActivity.this, R.string.printer_added_successfully, Toast.LENGTH_LONG).show();
                                finish();
                            }
                        });
                    }
                }).start();
            }
        });


        if(getIntent() != null && getIntent().getExtras() != null) {
            String scannedPrinter = (String) getIntent().getExtras().get("qrPrinter");
            if (!StringUtils.isEmpty(scannedPrinter)) {
                Printer printer = PrinterUtils.parseToPrinter(scannedPrinter);
                String validationResult = PrinterUtils.validatePrinter(printer);
                if (validationResult.equals(VALID)) {
                    populatePrinterFields(printer);
                } else {
                    Toast.makeText(AddPrinterActivity.this, validationResult, Toast.LENGTH_LONG).show();
                    AddPrinterActivity.this.finish();
                }
            }
        }
    }

    /**
     * Populates the fields in the AddPrinterActivity with the values obtained from scanning a QR code printer.
     * @param printer The QR code scanned printer.
     */
    private void populatePrinterFields(Printer printer){
        printerNameInput.setText(printer.getName());
        printerHostInput.setText(printer.getHost());
        printerProtocolSpinner.setSelection(new ArrayList<String>(printerProtocols.keySet()).indexOf(getKeyByValue(printerProtocols, printer.getProtocol())));
        printerShareInput.setText(printer.getShare());
        printerProducerSpinner.setSelection(printerProducers.indexOf(printer.getManufacturer()));
        printerModelInput.setText(printer.getModel());
    }

}
