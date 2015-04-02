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
import java.util.List;
import java.util.Map;

import static com.mediasaturn.print.activities.AddPrinterActivity.printerProducers;
import static com.mediasaturn.print.utils.PrinterUtils.filterPrintersBySeedWordAndPCL;
import static com.mediasaturn.print.utils.PrinterUtils.getKeyByValue;
import static com.mediasaturn.print.utils.PrinterUtils.printerProtocols;

public class EditPrinterActivity extends Activity {
    private static final String TAG = "EditPrinterActivity";

    private static final int THREAD_SAFETY_SLEEP = 10000;

    Button printerModelButton ;
    Button editPrinterButton;

    /**
     * User inputs.
     */
    EditText printerNameInput;
    Spinner printerProtocolSpinner;
    EditText printerHostInput;
    Spinner printerProducerSpinner;
    EditText printerModelInput;

    EditText printerShareInput;

    Printer printer;

    private ProgressDialog progressCircle = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_printer);

        printer = (Printer) this.getIntent().getExtras().getSerializable("printerToEdit");
        progressCircle = new ProgressDialog(this);
        progressCircle.setTitle(getString(R.string.editPrinterTitle));

        renderUI(printer);

    }

    private void renderUI(final Printer printer) {
        progressCircle = new ProgressDialog(this);

        printerNameInput = (EditText) findViewById(R.id.printerNameInput);
        printerNameInput.setText(printer.getName());
        printerNameInput.setEnabled(false);

        printerProtocolSpinner = (Spinner) findViewById(R.id.printerProtocolSpinner);
        final ArrayAdapter<String> printerProtocolAdapter = new ArrayAdapter<String>(EditPrinterActivity.this,android.R.layout.simple_spinner_item, new ArrayList<String>(printerProtocols.keySet()));
        printerProtocolSpinner.setAdapter(printerProtocolAdapter);

        printerProtocolSpinner.setSelection(new ArrayList<String>(printerProtocols.keySet()).indexOf(getKeyByValue(printerProtocols, printer.getProtocol())));

        printerHostInput = (EditText) findViewById(R.id.printerHostInput);
        printerHostInput.setHint(getString(R.string.printer_host_placeholder));
        printerHostInput.setText(printer.getHost());

        printerShareInput = (EditText) findViewById(R.id.printerShareInput);
        printerShareInput.setHint(getString(R.string.printer_share_placeholder));
        printerShareInput.setText(printer.getShare());

        printerProducerSpinner = (Spinner) findViewById(R.id.printerModelSpinner);
        printerProducerSpinner.setAdapter(new ArrayAdapter<String>(EditPrinterActivity.this, android.R.layout.simple_spinner_item, (String[]) printerProducers.toArray()));
        printerProducerSpinner.setSelection(printerProducers.indexOf(Cups.getManufacturerByModelName(printer.getModel())));


        printerModelInput = (EditText) findViewById(R.id.printerModelInput);
        printerModelInput.setText(printer.getModel());
        printerModelInput.setHint(getString(R.string.printer_model_placeholder));

        printerModelButton = (Button) findViewById(R.id.printerModelButton);
        printerModelButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                progressCircle = ProgressDialog.show(EditPrinterActivity.this,"Edit printer","Please wait while the printer models are being fetched.This might take a while");
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        //TODO refactor so that this list is kept in MainActivity(for state purposes), and fast retrieval.
                        PrinterUtils.printerTypes = Cups.getPrinterModels(EditPrinterActivity.this);

                        String printerProducer = printerProducers.get(printerProducerSpinner.getSelectedItemPosition());
                        List<String> printersByProducer = PrinterUtils.filterPrintersByProducer(printerProducer);
                        String seedWord = printerModelInput.getText().toString();

                        final List<CharSequence> values = new ArrayList<CharSequence>();
                        final AlertDialog.Builder builder = new AlertDialog.Builder(EditPrinterActivity.this);
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
                                alert.setOwnerActivity(EditPrinterActivity.this);
                                alert.show();
                                progressCircle.hide();
                                progressCircle.setMessage("Please wait while adding printer");
                            }
                        });
                    }
                }).start();
            }
        });


        editPrinterButton = (Button) findViewById(R.id.editPrinterButton);
        editPrinterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressCircle.setTitle(getString(R.string.editPrinterTitle));
                progressCircle.setMessage(getString(R.string.editingPrinterDefinitionMessage));
                progressCircle.show();
                new Thread(new Runnable(){
                    public void run(){
                        Printer newPrinter = PrinterUtils.buildPrinter(printerNameInput.getText().toString(),
                                printerProtocols.get(printerProtocolSpinner.getSelectedItem()),
                                printerHostInput.getText().toString(),
                                printerShareInput.getText().toString(),
                                (String) printerProducerSpinner.getSelectedItem(),
                                printerModelInput.getText().toString());

                        Cups.editPrinter(EditPrinterActivity.this, newPrinter);
                        Cups.updatePrintersInfo(EditPrinterActivity.this);

                        runOnUiThread(new Runnable(){
                            public void run()
                            {
                                progressCircle.dismiss();
                                Toast.makeText(EditPrinterActivity.this, getString(R.string.printerUpdatedSuccessfullyMessage), Toast.LENGTH_LONG).show();
                                finish();
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
        getMenuInflater().inflate(R.menu.menu_edit_printer, menu);
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


}
