package HTTPFormat;

/**
 * Redes Integradas de Telecomunicações II
 * MIEEC 2020/2021
 *
 * HTTPQuery.java
 *
 * Class that stores all information about a HTTP request
 * Incomplete Version
 *
 */


import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URL;
import java.util.StringTokenizer;


public class HTTPQuery {
    public String method;   // stores the HTTP Method of the request
    public String url_txt;  // stores the url of the request
    public String version;  // stores the HTTP version of the request
    public Headers headers; // stores the HTTP headers of the request
    public String text;     //store possible contents in an HTTP request (for example POST contents)
    private final Log log;  // log object
    private final String id_str;    // id_string for logging purposes
    public int local_port;  // local HTTP server port
    
    
    /** 
     * Creates a new instance of HTTPQuery
     * @param log   log object
     * @param id    log id
     * @param local_port local HTTP server port
     */
    public HTTPQuery (Log log, String id, int local_port) {
        // initializes everything to null
        this.headers= new Headers (log);
        this.log= log;
        this.id_str= id;
        this.local_port= local_port;
        this.url_txt= null;
        this.method= null;
        this.version= null;
        this.text= null;
    }
    
    /** 
     * Creates a clone of an instance of HTTPQuery
     * @param q     existing query to be cloned
     * @param id    new log id 
     */
    public HTTPQuery (HTTPQuery q, String id) {
        // initializes everything to null
        headers= new Headers (q.log);
        this.log= q.log;
        this.id_str= id;
        this.local_port= q.local_port;
    }
    
    /**
     * Is a direct proxy
     * @return true if is a direct proxy
     */
    public boolean is_direct_proxy_request() {
        return ((url_txt!=null) && url_txt.startsWith("http"));
    }
    
    /**
     * Is a reverse proxy
     * @return true if is a reverse proxy
     */
    public boolean is_reverse_proxy_request() {
        return ((url_txt!=null) && url_txt.startsWith("/"));
    }
    
    /**
     * It is not a valid URL request
     * @return true is invalid
     */
    public boolean is_invalid_request() {
        return !is_direct_proxy_request() && !is_reverse_proxy_request();
    }
    
    /**
     * Logs a message to the screen prepending the log id
     * @param in_window write in window or only in the command line
     * @param s message to be written
     */
    public void Log (boolean in_window, String s) {
        if (in_window)
            log.Log (id_str+ "  " + s );
        else
            System.out.print (id_str + "  " + s);
    }

    /**
     * Get a header property value
     * @param hdrName   header name
     * @return          header value
     */
    public String getProperty(String hdrName) {
        return headers.getProperty(hdrName);
    }
    
    /**
     * Set a header property value
     * @param hdrName   header name
     * @param hdrVal    header value
     */
    public void setProperty(String hdrName, String hdrVal) {
        headers.setProperty(hdrName, hdrVal);
    }

    /**
     * Remove a header property name
     * @param hdrName   header name
     * @return true if successful
     */
    public boolean removeProperty(String hdrName) {
        return headers.removeProperty(hdrName);
    }
    
    /** Parses a new HTTP query from an input steam
     * @param bin   input stream
     * @param echo  if true, echoes the received message to the screen
     * @return HTTPReplyCode.OK when successful, or HTTPReplyCode.BADREQ in case of error
     * @throws java.io.IOException 
     */
    public int parse_Query (BufferedReader bin, boolean echo) throws IOException {
        // Get first line
        String request = bin.readLine( );  	// Reads the first line
        if (request == null) {
            if (echo) Log (true, "Invalid request Connection closed\n");
            return -1;
        }
        Log( true, "Request: " + request + "\n");
        StringTokenizer st= new StringTokenizer(request);
        if (st.countTokens() != 3) return -1;  // Invalid request
        method= st.nextToken();    // USES HTTP syntax
        url_txt= st.nextToken();    // for requesting files
        version= st.nextToken();
        // It does not read the other header fields! 
        // read the remaining headers
        // check if the Content-Length size is different than zero. If true read the body of the request (that can contain form data)

        headers.parse_Headers(bin, echo);
        
        // Parse request data
        int clength= 0;
        try {
            String len= headers.getProperty ("Content-Length");
            if (len != null)
                clength= Integer.parseInt (len);
            else if (!bin.ready ())
                clength= 0;
        } catch (NumberFormatException e) {
            if (echo) Log (true, "Bad request\n");
            return HTTPReplyCode.BADREQ;
        }
        if (clength>0) {
            // Length is not 0 - read data to string
            String str= new String ();
            char [] cbuf= new char [clength];

            int n, cnt= 0;
            while ((cnt<clength) && ((n= bin.read (cbuf)) > 0)) {
                str= str + new String (cbuf);
                cnt += n;
            }
            if (cnt != clength) {
                Log (false, "Read request with "+cnt+" data bytes and Content-Length = "+clength+" bytes\n");
                return HTTPReplyCode.BADREQ;
            }
            text= str;
            if (echo)
                Log (true, "Contents('"+text+"')\n");
        }

        return HTTPReplyCode.OK;
    }    
}
