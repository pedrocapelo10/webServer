package HTTPFormat;

/**
 * Redes Integradas de Telecomunicações II
 * MIEEC 2020/2021
 *
 * HTTPAnswer.java
 *
 * Class that stores all information about a HTTP reply
 * INCOMPLETE VERSION
 *
 */


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class HTTPAnswer {

    /**
     * Answer code and version information  
     */
    public HTTPReplyCode code;
    /**
     * Headers data
     */
    public Headers headers;                 // List of header fields

    private final int MAXBUF_SIZE= 8 * 1024;// Default buffer size
    private final DateFormat httpformat;    // to read/write HTTP time format

    /**
     * Reply contents
     * They are stored either in a text buffer or in a file
     */
    public String text; // buffer with reply contents (dynamic HTML generated in the server).  
    public File file;   // file used if text == null for file (static content responses).
    public Date date;   // Date when the answer was received
    public String server_name;  // Proxy server name
    private final Log log;      // Log object
    private final String id_str;  // Thread id - for logging purposes

    /**
     * Creates a new instance of HTTPAnswer
     * @param log           log object
     * @param id_str        log id
     * @param server_name   local HTTP server name
     */
    public HTTPAnswer(Log log, String id_str, String server_name) {
        this.code = new HTTPReplyCode();
        this.server_name= server_name;
        this.id_str = id_str;
        this.log = log;
        this.text = null;
        this.file = null;
        this.headers = new Headers(log);
        this.date = new Date();
                                
        this.httpformat = new SimpleDateFormat ("EE, d MMM yyyy HH:mm:ss zz", Locale.UK);
        this.httpformat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }


    /**
     * Resets the answer object to an empty state
     */
    public void clear() {
        this.code = new HTTPReplyCode();
        this.text = null;
        this.file = null;     
        this.headers = new Headers(log);
        this.date = new Date();    }
    
    /**
     * Logs a message to the screen prepending the log id
     * @param in_window write in window or only in the command line
     * @param s message to be written
     */
    public void Log(boolean in_window, String s) {
        if (in_window) {
            log.Log(id_str + "  " + s);
        } else {
            System.out.print(id_str + "  " + s);
        }
    }
   
    /**
     * return a string with the log id
     * @return the log id
     */
    public String id_str() {
        return id_str;
    }
    
    /**
     * Method to set the HTTP reply code of the answer
     * @param _code the HTTP reply code number
     */
    public void set_code(int _code) {
        code.set_code(_code);
    }
   
    /** 
     * Method to set the HTTP version of the answer
     * @param v HTTP version srting
     */
    public void set_version(String v) {
        code.set_version(v);
    }
    
    /** 
     * Set the "Date" header field with the local date in HTTP format 
     */
    void set_Date() {
        headers.removeProperty("Date"); // Removes any existing date
        headers.setProperty("Date", httpformat.format(new Date()));
    }
    
    
    /**
     * Method to set an HTTP header of the answer
     * @param name  header name
     * @param value header value
     */
    public void set_header(String name, String value) {
        headers.setProperty(name, value);
    }

    /**
     * Initialize a set of common HTTP headers
     * @param version   string with HTTP version
     */
    private void set_common_headers (String version) {
        this.set_version((version != null) ? version : "HTTP/1.0");
        set_Date();
        headers.removeProperty("Server");
        headers.setProperty ("Server", server_name);
    }
    /** 
     * @param location */
    public void redirectToHttps(String location){
        
        headers.setProperty("Location",location);
        set_common_headers(code.get_version());
        
    }
    
    /**
     * @param version   string with HTTP version
     */
    public void set_basic_headers(String version){
        set_common_headers(version);
        
    }
    
   /** Sets the headers needed in a reply with a locally generated HTML string
     * and fill the text property with the input String object 
     * containing the HTML to send
     * @param _text */
    public void set_text_headers(String _text) {
        set_common_headers (code.get_version());
        text = _text; // sets text property with the HTML string.
        headers.setProperty ("Content-Type", "text/html");
        headers.setProperty ("Content-Length", ""+text.length());
        headers.setProperty ("Content-Encoding", "iso-8859-1");
    }
    
    /** Sets the headers needed in a reply with a locally generated HTML string
     * and fill the text property with the input String object 
     * containing the HTML to send
     * @param _f  file object to send
     * @param mime_enc  Mime type
     */
    
     public void set_file(File _f, String mime_enc) {
        file = _f;
        if (_f != null){
            headers.setProperty("Content-Length", Long.toString(_f.length()));
            //set other needed headers type last modified
            headers.setProperty("Content-Type", mime_enc);
            headers.setProperty("Last-Modified",httpformat.format(_f.lastModified()));
        }
            
    }
    
    
    /**
     * Returns the current value of the answer code
     * @return the reply code number
     */
    public int get_code() {
        return code.get_code();
    }

    /**
     * Returns the current value of the answer code
     * @return the version string including the "HTTP/"
     */
    public String get_version() {
        return code.get_version();
    }

    /**
     * Returns a string with the first line of the answer
     * @return 
     */
    public String get_first_line() {
        return code.toString();
    }

    /**
     * Returns an iterator over all header names
     * @return 
     */
    public Enumeration getAllHeaderNames() {
        return headers.getAllPropertyNames();
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

    /**
     * Returns the value received in the "Age" header
     * @return 
     */
    public int age_hdr () {
        String txt= headers.getProperty ("Age");
        if (txt==null)
            return 0;
        try {
            return Integer.parseInt (txt.trim ());
        } catch (NumberFormatException e) {
            System.out.println ("Invalid value in age field: "+e+"\n");
            return 0;
        }
    }
    
    
    /** Sends the HTTP reply to the client using 'pout' text device
     * @param pout      output stream
     * @param send_data indicates if data is present in the response or only headers
     * @param echo      if true, echoes the sent message to the screen
     * @throws java.io.IOException */
    public void send_Answer(PrintStream pout, boolean send_data, boolean echo) throws IOException {
        if (code.get_code_txt() == null) {
            code.set_code(HTTPReplyCode.BADREQ);
        }
        if (echo) {
            Log(true, "Answer: " + code.toString() + "\n");
        }
        pout.print(code.toString() + "\r\n"); // sends the first line of the HTTP response Version Code and reason \r\n 
        /**
         * ENVIAR TODOS OS CABEÇALHOS-step 1 
         * Send all headers in the headers object 
         * 
         */
        for(Map.Entry me : headers.param.entrySet()){
            
            pout.print(me.getKey()+":"+me.getValue() + "\r\n");
           
        }
        
        
        //Log(true, "HTTPAnswer does not yet send headers\n");

        pout.print("\r\n");

        if (send_data) {

            if (text != null) {
                pout.print(text);
            } else if (file != null)  {
                try (FileInputStream fin = new FileInputStream(file)) {
                    if (fin == null) {
                        Log(true, "Internal error sending answer data\n");
                        return;
                    }

                    /*
                    *nao se deve ler tudo de uma vez-temos de ler x bytes de cada vez devido ao tamanho do buffer
                    byte [] data = new byte [fin.available()];
                    fin.read( data );  // Read the entire file to buffer 'data'
                    // IMPORTANT - Please modify this code to send a file chunk-by-chunk
                    //             to avoid having CRASHES with BIG FILES
                    Log(true, "HTTPAnswer may fail for large files - please modify it\n");
                    pout.write(data);
                    */
                    
                    byte[] buffer = new byte[MAXBUF_SIZE];
                    int bytes_read= 1;
                    while  ((bytes_read = fin.read(buffer)) != -1) {
                        pout.write(buffer, 0, bytes_read);
                    }

                    fin.close();
                }
            } else if (code.get_code() != HTTPReplyCode.NOTMODIFIED) {
                Log(true, "Internal server error sending answer\n");
            }
        }

        pout.flush();
        if (echo) {
            Log(false, "\n");
        }
    }    
    
    

    /** 
     * Prepares an HTTP answer with an error code
     * @param _code     Error reply code
     * @param version   HTTP version string
     */
    public void set_error(int _code, String version) {
        clear();
        set_version(version==null ? "HTTP/1.0" : version);
        set_Date();
        code.set_code(_code);
        if (code.get_code_txt() == null) {
            code.set_code(HTTPReplyCode.BADREQ);
        }

        //if (!code.get_version().equalsIgnoreCase("HTTP/1.0")) {
        headers.setProperty("Connection", "close");
        //}
        // Prepares a web page with an error description
        String txt = "<HTML>\r\n";
        txt = txt + "<HEAD><TITLE>Error " + code.get_code() + " -- " + code.get_code_txt()
                + "</TITLE></HEAD>\r\n";
        txt = txt + "<H1> Error " + code.get_code() + " : " + code.get_code_txt() + " </H1>\r\n";
        txt = txt + "  by " + headers.getProperty("Server") + "\r\n";
        txt = txt + "</HTML>\r\n";

        // Set the header properties
        set_text_headers(txt);
    }

    /**
     * Prepares an HTTP answer with an unauthorized error code
     * @param version       HTTP version string    
     */
    public void unauthorized_page ( String version) {
        // Set page
        String txt= "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>";
        txt= txt+"<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\\\r\n";
        txt=txt+"\"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\r\n";
        txt=txt+"<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en\">\r\n";
        txt=txt+"<head>\r\n";
        txt=txt+"<title>Authentication required!</title>\r\n";
        txt=txt+"<meta http-equiv=\"Content-Type\" content=\"text/html; charset=iso-8859-1\" />\r\n";
        txt=txt+"<link rev=\"made\" href=\"mailto:lflb@uninova.pt\" />\r\n";
        txt=txt+"<style type=\"text/css\">\r\n";
        txt=txt+"<!--\r\n";
        txt=txt+"body { color: #000000; background-color: #FFFFFF; }\r\n";
        txt=txt+"a:link { color: #0000CC; }\r\n";
        txt=txt+"-->\r\n";
        txt=txt+"</style>\r\n";
        txt=txt+"</head>\r\n\r\n";
        txt=txt+"<body>\r\n";
        txt=txt+"<h1>Authentication required!</h1>\r\n";
        txt=txt+"<dl>\r\n<dd>\r\n\r\n\r\n";
        txt=txt+"This server could not verify that you are authorized to access\r\n";
        txt=txt+"the requested URL.\r\n";
        txt=txt+"You either supplied\r\n";
        txt=txt+"the wrong credentials (e.g., bad password), or your\r\n";
        txt=txt+"browser doesn't understand how to supply the credentials required.\r\n\r\n";
        txt=txt+"</dd></dl><dl><dd>\r\n\r\n\r\n";
        txt=txt+"In case you are allowed to request the document, please\r\n";
        txt=txt+"check your user-id and password and try again.\r\n\r\n";
        txt=txt+"</dd></dl><dl><dd>\r\n";
        txt=txt+"If you think this is a server error, please contact\r\n";
        txt=txt+"the <a href=\"mailto:pfa@fct.unl.pt\">webmaster</a>\r\n\r\n";
        txt=txt+"</dd></dl>\r\n\r\n";
        txt=txt+"</body>\r\n</html>\r\n";
        
        this.set_text_headers(txt);
    }
 /** Generates the HTML for the cookie authentication page
     * places it in the text property to be sent in send_answer().
     * @param server_name
     * @param version
     * @param URL
     * @param UserAgent
     * @param ip
     * @param port
     * @param comment */
     public void cookie_authentication_page(String server_name, String version, String URL, String UserAgent, String ip, int port, String comment) {
        String html= "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">\r\n<html>\r\n<head>\r\n";
        html += "<meta content=\"text/html; charset=ISO-8859-1\" http-equiv=\"content-type\">\r\n";
        html += "<title>"+URL+"</title>\r\n</head>\r\n<body>\r\n";
        html += "<h1 align=\"center\">P&aacute;gina de LOGIN do servidor</h1>\r\n";
        html += "<h1 align=\"center\"><font color=\"#800000\">RIT II</font> <font color=\"#c0c0c0\">2020/2021</font></h1>\r\n";
        html += "<h3 align=\"center\">1&ordm; Trabalho de laborat&oacute;rio</h3>\r\n";
        html += "<p align=\"left\">Ligou a partir de <font color=\"#ff0000\">" + ip + "</font>:";
        html += "<font color=\"#ff0000\">" + port + "</font> num browser do tipo <font color=\"#ff0000\">" + UserAgent + "</font>.</p>\r\n";
        if (comment.length()>0)
            html += "<p><font color=\"#ff0000\">"+comment+"</font></p>\r\n";
        html += "<form method=\"post\" action=\""+URL+"\">\r\n<h3>\r\nLOGIN</h3>";
        html += "<p>Username <input name=\"Username\" size =\"10\" type=\"text\"></p>\r\n";
        html += "<p>Password <input name=\"Password\" size=\"10\" type=\"text\"></p>\r\n";
        html += "<p><input value=\"Submeter\" name=\"BotaoSubmeter\" type=\"submit\"></p>\r\n";
        html += "<p align=\"left\">&nbsp;</p>\r\n";
        html += "<p align=\"left\"><font face=\"Times New Roman\">&copy; Pedro Amaral 2021</font></p>\r\n</body>\r\n</html>\r\n";

        DateFormat httpformat=
                new SimpleDateFormat ("EE, d MMM yyyy HH:mm:ss zz", Locale.UK);
        httpformat.setTimeZone (TimeZone.getTimeZone ("GMT"));
        html=html+"<small>"+httpformat.format (new Date ())+"</small>\n";
        html=html+"<br />\n";
        html=html+"<small>"+server_name+"</small>\n";
        html=html+"</address>\n</dd>\n</dl>\n</body>\n</html>\n";
        set_text_headers(html);
    }
    
    
    /** Parses a new HTTP query
     * @param bin       input stream
     * @param out_file  output file where the answer will be stored
     * @param echo      if true, echoes the received message to the screen
     * @return HTTPReplyCode.NOTDEFINED in case of error, the reply code otherwise
     * @throws java.io.IOException 
     */
    public int parse_Answer (BufferedReader bin, File out_file, boolean echo) 
                                                throws IOException {
        // Update receiving date
        date = new Date();
        // Get first line
        String request = bin.readLine( );  	// Reads the first line
        if (request == null) {
            if (echo) Log (true, "Invalid request Connection closed\n");
            return HTTPReplyCode.NOTDEFINED;
        }
        if (echo)
            Log (true, "Answer: " + request + "\n");
        if (request.length ()==0) {
            if (echo) Log (true, "Empty answer\n");
            return HTTPReplyCode.NOTDEFINED;
        }
        StringTokenizer st = new StringTokenizer ( request );
        if (st.countTokens () < 3) {
            if (echo) Log (true, "Invalid answer\n");
            return HTTPReplyCode.NOTDEFINED;
        }
        
        // Get version
        String v= st.nextToken ();
        if (!v.startsWith ("HTTP/")) {
            if (echo) Log (true, "Invalid answer\n");
            return HTTPReplyCode.NOTDEFINED;
        }
        
        // Get answer code
        int n;
        try {
            n= Integer.parseInt (st.nextToken ());
        } catch (NumberFormatException e) {
            Log (true, "Invalid answer code\n");
            return HTTPReplyCode.NOTDEFINED;
        }
        String code_txt= st.nextToken ();
        while (st.hasMoreTokens ())
            code_txt= code_txt+" "+st.nextToken ();
        code= new HTTPReplyCode(n, v);

        // It does not read the other header fields! 
        // Put here the code to read the remaining headers
        Log(true, "HTTPAnswer does not yet parse headers\n");

        // check if the Content-Length size is different than zero. If true read the body of the request (that can contain form data)
        // Here is a buffer we use for reading from the server
        String cl= headers.getProperty("Content-Length");
        int clength= -1; 
        if (cl != null) {
            try {
                clength= Integer.parseInt(cl);
            } catch (NumberFormatException e) {
                Log(true, "Invalid number on Content-Length:"+e);
                return HTTPReplyCode.NOTDEFINED;
            }
        }

        try (FileOutputStream to_file = new FileOutputStream(out_file)) {
            OutputStreamWriter fosr= new OutputStreamWriter (to_file, "8859_1");
            // Parse the answer contents and store them in a file
            try (BufferedWriter fos = new BufferedWriter (fosr)) {
                // Parse the answer contents and store them in a file
                char[] buffer = new char[MAXBUF_SIZE];
                int bytes_read= 1;
                // Now read the rest of the bytes and write to the file
                while ( ((clength > 0) || (clength == -1)) && (bytes_read>0)) {
                    if (clength == -1)
                        bytes_read= bin.read(buffer);
                    else
                        bytes_read= bin.read(buffer,0,Math.min(MAXBUF_SIZE, clength));
                    if (bytes_read  != -1) {
                        fos.write(buffer, 0, bytes_read);
                        if (clength > 0)
                            clength -= bytes_read;
                    }
                }
                fos.close();
            }
        }
        
        // Test if file was well received
        if (clength == 0 || clength == -1) {
            file= out_file;
            return code.get_code();
        } else
            return HTTPReplyCode.NOTDEFINED;
    }      
}
