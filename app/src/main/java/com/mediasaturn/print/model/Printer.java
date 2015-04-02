package com.mediasaturn.print.model;

import java.io.Serializable;

/**
 * Class that models a printer, along with it's settings in CUPS.
 * Created by dragos.triteanu on 3/6/2015.
 */
public class Printer implements Serializable {

    /**
     * The name of the printer
     */
    private String name;

    /**
     * The hostname or IP address of the printer, or the computer it is connected to.
     */
    private String host;

    /**
     * The share printer name or print queue.
     */
    private String share;

    /**
     * The protocol for accessing the printer.This can be either LPD(lpd://) or
     * IPP(ipp://).
     */
    private String protocol;

    /**
     * The printer model.
     */
    private String model;

    /**
     * The printer manufacturer.
     */
    private String manufacturer;

    public Printer(){}

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getManufacturer() {
        return manufacturer;
    }

    public void setManufacturer(String manufacturer) {
        this.manufacturer = manufacturer;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getShare() {
        return share;
    }

    public void setShare(String share) {
        this.share = share;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }
}
