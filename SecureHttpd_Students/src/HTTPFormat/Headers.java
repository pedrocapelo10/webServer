package HTTPFormat;

/**
 * Redes Integradas de Telecomunicações II
 * MIEEC 2020/2021
 *
 * Headers.java
 *
 * Auxiliary class to handle indexed list of HTTP headers with repetitions
 *
 */

import java.util.*;
import java.io.*;

/**
 *
 * @author pfa@fct.unl.pt
 */
public class Headers {

    public Properties param;                            // Single value list
    public HashMap<String,ArrayList<String>> dup_param; // Multiple value list
    private final Log log;                              // Log object

    /**
     * Creates an empty list of headers
     * @param log log object
     */
    public Headers(Log log) {
        param = new Properties();
        dup_param = new HashMap<>();
        this.log = log;
    }
    
    /**
     * Clones an existing list of headers
     * @param hdr 
     */
    public Headers(Headers hdr) {
        param = (Properties)hdr.param.clone();
        dup_param= new HashMap<>(hdr.dup_param);
        this.log = hdr.log;
    }
    
    /**
     * Clears the contents of the list
     */
    public void clear() {
        param.clear();
        dup_param.clear();
    }
    
    /**
     * Store a header value; it stores more than one value per property
     * @param hdrName   header name
     * @param hdrVal    header value
     */
    public void setProperty(String hdrName, String hdrVal) {
        String storedhdrVal= param.getProperty(hdrName);
        if (storedhdrVal != null) {
            ArrayList<String> lst= dup_param.get(hdrName);
            if (lst == null) {
                lst= new ArrayList<>();
                lst.add(hdrName+": "+storedhdrVal+"\r\n"); // It is both in param and dup_param
                dup_param.put(hdrName, lst);
            } 
            lst.add(hdrName+": "+hdrVal+"\r\n");
        } else {
            param.setProperty(hdrName, hdrVal);
        }       
    }

    /**
     * Returns the value of a property (returns the last one)
     * @param hdrName   header name
     * @return  the last header value
     */
    public String getProperty(String hdrName) {
        return param.getProperty(hdrName);
    }

    /**
     * Returns a list with all values of a header, if more than one exist
     * @param hdrName   header name
     * @return an array list of values
     */
    public ArrayList<String> getDupProperty(String hdrName) {
        return dup_param.get(hdrName);
    }

    /**
     * Removes all the values of a header
     * @param hdrName   header name
     * @return true if a header was removed, false otherwise
     */
    public boolean removeProperty(String hdrName) {
        if (param.containsKey(hdrName)) {
            param.remove(hdrName);
            if (dup_param.containsKey(hdrName))
                dup_param.remove(hdrName);
            return true;
        } else
            return false;
    }
    
    /**
     * Returns an enumeration of all header names
     * @return an enumeration object
     */
    public Enumeration getAllPropertyNames() {
        return param.keys();
    }
    
    /**
     * Parses an input stream for a sequence of headers, until an empty line is reached
     * @param in    input stream
     * @param echo  if true, echoes the headers received on screen
     * @return  true if headers were read successfully 
     * @throws IOException when a socket error occurs
     */
    public boolean parse_Headers(BufferedReader in, boolean echo) throws IOException {
        // Get other header parameters
        String req;
        while (((req = in.readLine()) != null) && (req.length() != 0)) {
            int ix;
            if (echo) log.Log("hdr(" + req + ")\n");
            if ((ix = req.indexOf(':')) != -1) {
                String hdrName = req.substring(0, ix);
                String hdrVal = req.substring(ix + 1).trim();//esta a tirar os espaços em branco-cuidado
                setProperty(hdrName, hdrVal);         
            } else {
                if (echo) log.Log("Invalid header\n");
                return false;
            }
        }
        return true;
    }

    /**
     * write the headers list to an output stream
     * @param pout  output stream
     * @param echo  if true, echoes the headers sent on screen
     * @throws IOException when a socket error occurs
     */
    public void write_Headers(PrintStream pout, boolean echo) throws IOException {
        for (String hdrName : param.stringPropertyNames()) {
            ArrayList<String> lst= dup_param.get(hdrName);
            if (lst == null) {
                // Single value parameter
                String val = param.getProperty(hdrName);
                pout.print(hdrName + ": " + val + "\r\n");
                if (echo) 
                    log.Log(hdrName + ": " + val + "\n");
            } else {
                // Duplicate value parameter
                for (String hdr : lst) {
                    pout.print(hdr);
                    if (echo) 
                        log.Log(hdr);         
                }                
            }
        }
    }
    
}
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
