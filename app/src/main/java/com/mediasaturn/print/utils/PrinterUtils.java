package com.mediasaturn.print.utils;

import android.util.Log;

import com.mediasaturn.print.model.Printer;
import com.mediasaturn.print.services.Cups;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by dragos.triteanu on 3/6/2015.
 */
public class PrinterUtils {
    private static final String TAG = "PrinterUtils";
    public static final String VALID = "OK";

    public static final String LPD_PROTOCOL = "lpd://";
    public static final String IPP_PROTOCOL = "ipp://";
    public static final String HTTP_PROTOCOL = "http://";

    public static Map<String,String> printerTypes = new HashMap<String,String>();

    public static final Map<String,String> printerProtocols = new HashMap<String, String>();
    static{
        printerProtocols.put("Printer connected to computer","lpd://");
        printerProtocols.put("Printer is wireless","ipp://");
    }



    public static List<String> filterPrintersByProducer(String printerProducer) {
        List<String> printerByProducer = new ArrayList<String>();
        Set<String> printerModels = printerTypes.keySet();
        for(String printer: printerModels){
            if(printer != null && printer.toLowerCase().startsWith(printerProducer.toLowerCase())){
                printerByProducer.add(printer);
            }
        }
        return printerByProducer;
    }

    //TODO add filtering for PCL
    public static boolean filterPrintersBySeedWordAndPCL(String printerName, String seedWord) {
        String printerNameLowercased = printerName.toLowerCase();
        return (printerNameLowercased.indexOf(seedWord) != -1);
    }

    /**
     * Parses a scanned printer url in order to produce a {@link com.mediasaturn.print.model.Printer}.
     * An example URL can be seen here:
     * 'somePrinter/protocol/hostName/shareName/Generic PDF Printer'
     * @param scannedPrinter
     * @return
     */
    public static Printer parseToPrinter(String scannedPrinter){

        if(scannedPrinter.indexOf(HTTP_PROTOCOL) != -1){
            scannedPrinter = scannedPrinter.substring(HTTP_PROTOCOL.length());
        }

        String printerDetails[] = scannedPrinter.split("#");

        if(printerDetails.length != 3){
            return null;
        }
        if(StringUtils.isEmpty(printerDetails[0]) || StringUtils.isEmpty(printerDetails[1]) || StringUtils.isEmpty(printerDetails[2])){
            return null;
        }



        Printer newPrinter = new Printer();
        newPrinter.setName(printerDetails[0]);

        populatePrinterWithSplitAddress(newPrinter,printerDetails[1]);

        newPrinter.setManufacturer(Cups.getManufacturerByModelName(printerDetails[2]));
        newPrinter.setModel(printerDetails[2]);

        return newPrinter;
    }

    public static String validatePrinter(Printer printer){
        String invalidQR = "Invalid QR code.";
        if(printer == null){
            return invalidQR;
        }
        if(StringUtils.isEmpty(printer.getName())){
            Log.d(TAG,"Scanned printer name is empty");
            return invalidQR +"Printer name is empty";
        }
        if(StringUtils.isEmpty(printer.getHost())){
            Log.d(TAG,"Scanned printer host is empty");
            return invalidQR +"Printer host is empty";
        }
        if(StringUtils.isEmpty(printer.getProtocol())){
            Log.d(TAG,"Scanned printer protocol is empty");
            return invalidQR +"Printer protocol is empty";
        }
        if(StringUtils.isEmpty(printer.getShare())){
            Log.d(TAG,"Scanned printer share is empty");
            return invalidQR +"Printer share is empty";
        }
        if(StringUtils.isEmpty(printer.getManufacturer())){
            Log.d(TAG,"Scanned printer manufacturer is empty");
            return invalidQR +"Printer manufacturer is empty";
        }
        if(StringUtils.isEmpty(printer.getModel())){
            Log.d(TAG,"Scanned printer model is empty");
            return invalidQR +"Printer model is empty";
        }
        if(PrinterUtils.printerTypes.get(printer.getModel()) == null){
            Log.d(TAG,"Scanned printer model does not exist.Probably wrong model name in QR code");
            return  invalidQR + "Printer model does not exist";
        }

        return VALID;
    }

    /**
     * Convenience method for building an {@link com.mediasaturn.print.model.Printer}.
     * @return
     */
    public static Printer buildPrinter(String printerName,String printerProtocol,String printerHost,String printerShare,String printerProducer,String printerModel ){
        Printer printer = new Printer();
        printer.setName(printerName);
        printer.setProtocol(printerProtocol);
        printer.setHost(printerHost);
        printer.setShare(printerShare);
        printer.setManufacturer(printerProducer);
        printer.setModel(printerModel);
        return printer;
    }


    public static void populatePrinterWithSplitAddress(Printer printer, String address){
        String protocol = "";
        String host = "";
        String share = "";
        if(address.indexOf(LPD_PROTOCOL) != -1){
            protocol = address.substring(0,LPD_PROTOCOL.length());
            address = address.substring(LPD_PROTOCOL.length());
        }
        if(address.indexOf(IPP_PROTOCOL) != -1){
            protocol = address.substring(0,IPP_PROTOCOL.length() - 1);
            address = address.substring(IPP_PROTOCOL.length());
        }

        String hostAndShare[] = address.split("/");

       if(hostAndShare.length == 2) {

           host = hostAndShare[0];
           share = hostAndShare[1];

           printer.setProtocol(protocol);
           printer.setHost(host);
           printer.setShare(share);
       }

       }

    public static Object getKeyByValue(Map map, Object value) {
        for (Object o : map.keySet()) {
            if (map.get(o).equals(value)) {
                return o;
            }
        }
        return null;
    }

}
