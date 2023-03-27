package Main;

/**
 * Redes Integradas de Telecomunicações II MIEEC 2020/2021
 *
 * httpThread.java
 *
 * Class that handles client's requests. It must handle HTTP GET,and POST
 * client requests INCOMPLETE VERSION
 *
 */
import HTTPFormat.*;
import java.io.*;
import java.net.*;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class httpThread extends Thread {

    public static final int NO_AUTH = 10;
    public static final int AUTH_HEADER = 20;
    public static final int COOKIE_NEEDED = 30 ;
    public static final int SEND_AUTHR = 40 ;
    public static final String CRYPTO="SHA-256";
    
    guiHttpd root;
    ServerSocket ss;
    Socket client;
    DateFormat httpformat;
    boolean is_SSL;

    /**
     * Creates a new instance of httpThread
     * @param root
     * @param ss
     * @param client
     * @param is_SSL
     */
    public httpThread(guiHttpd root, ServerSocket ss, Socket client,boolean is_SSL) {
        this.root = root;
        this.ss = ss;
        this.client = client;
        this.is_SSL=is_SSL;
        httpformat = new SimpleDateFormat("EE, d MMM yyyy HH:mm:ss zz", Locale.UK);
        httpformat.setTimeZone(TimeZone.getTimeZone("GMT"));
        setPriority(NORM_PRIORITY - 1);
    }

   /**
     * Test if q has valid authentication headers
     * @param q query object
     * @return  NO_AUTH if no authentication needed, AUTH_HEADER if Authentication is valid and no cookie support, COOKIE_NEEDED otherwise
     */
    private int authentication_valid(HTTPQuery q) {
        // Test Authenticate header
        if (root.user_passwd().length()==0)  // No password is defined
            return httpThread.NO_AUTH;
        
        
        // TO DO:

        // Check if the Authentication header is present and if cokkie Checkbox
        // is selected or not 
        
            if (root.cookie_passwd()) {
                // if cookie is selected return this.COOKIE_NEEDED
                return this.COOKIE_NEEDED;
            }
            else if (q.getProperty("Authorization")!=null) {
                // If cookie not selected Decode the string, validating if the first token is "Basic"

                // Convert the base 64 encoded string to plain text using:
                String [] userpassBase64=q.getProperty("Authorization").split(" ");
                String userpassword = new String(Base64.getDecoder().decode(userpassBase64[1]));
                System.out.println(userpassword);
                // Compare to the string obtained with root.passwd() if ok return this.AUTH_HEADER
                if (userpassword.equals(root.user_passwd())) {
                    return httpThread.AUTH_HEADER;
                }

            }
        
        return httpThread.SEND_AUTHR;
        
    }
    
    /**
     * The type for unguessable files
     */
    String guessMime(String fn) {
        String lcname = fn.toLowerCase();
        int extenStartsAt = lcname.lastIndexOf('.');
        if (extenStartsAt < 0) {
            if (fn.equalsIgnoreCase("makefile")) {
                return "text/plain";
            }
            return "unknown/unknown";
        }
        String exten = lcname.substring(extenStartsAt);
        // System.out.println("Ext: "+exten);
        if (exten.equalsIgnoreCase(".htm")) {
            return "text/html";
        } else if (exten.equalsIgnoreCase(".html")) {
            return "text/html";
        } else if (exten.equalsIgnoreCase(".gif")) {
            return "image/gif";
        } else if (exten.equalsIgnoreCase(".jpg")) {
            return "image/jpeg";
        } else {
            return "application/octet-stream";
        }
    }
    
    /**
     * Logs a message to the screen prepending the log id
     * @param in_window write in window or only in the command line
     * @param s message to be written
     */
    public void Log(boolean in_window, String s) {
        if (in_window) {
            root.Log("" + client.getInetAddress().getHostAddress() + ";"
                    + client.getPort() + "  " + s);
        } else {
            System.out.print("" + client.getInetAddress().getHostAddress()
                    + ";" + client.getPort() + "  " + s);
        }
    }
    
    /**
     
     */
    public void redirectClient(HTTPAnswer response, HTTPQuery receivedhttp) {
        response.set_code(HTTPReplyCode.TMPREDIRECT);
        response.set_version(receivedhttp.version);
        if (receivedhttp.getProperty("Host").indexOf(":2") != -1) {
            response.redirectToHttps("https://" + receivedhttp.getProperty("Host").substring(0, receivedhttp.getProperty("Host").indexOf(":")) + ":" + root.getPortHTTPS());
        } else {
            response.redirectToHttps("https://" + receivedhttp.getProperty("Host") + ":" + root.getPortHTTPS());
        }
    }
    
    public void send_Unauthorized(HTTPAnswer response,  HTTPQuery receivedhttp) {
        response.set_code(HTTPReplyCode.UNAUTHORIZED);
        response.set_version(receivedhttp.version);
        response.set_basic_headers(receivedhttp.version);
        response.headers.setProperty("WWW-Authenticate", "Basic \"RIT2 Server Demo domain\"");
    }
    
    public void send_NotModified(HTTPAnswer response, HTTPQuery receivedhttp) {
        response.set_code(HTTPReplyCode.NOTMODIFIED);
        response.set_version(receivedhttp.version);
        response.set_basic_headers(receivedhttp.version);
    }
    
    public boolean cookie_authentication(String information){
        String [] aux =information.split("&");
        String[] userarray=aux[0].split("=");
        String[] passwordarray=aux[1].split("=");        
        if(userarray.length==1 || passwordarray.length==1)
            return false;
        String user=userarray[1];
        String password=passwordarray[1];
        String userpassword = user+":"+password;
        if (userpassword.equals(root.user_passwd())) {return true;}else return false;
         
    }
    
    public void set_cookie(HTTPAnswer response) {
        

        MessageDigest messageDigest= null;
        try {
            messageDigest = MessageDigest.getInstance(CRYPTO);
        } catch (NoSuchAlgorithmException ex) {
            Logger.getLogger(httpThread.class.getName()).log(Level.SEVERE, null, ex);
        }
    
        String [] aux=root.user_passwd().split(":");
        String cookieName =aux[0] ;
        
        messageDigest.update(cookieName.getBytes());

        String cookieNameHash = new String(messageDigest.digest());
                
        cookieNameHash = Base64.getEncoder().encodeToString(cookieNameHash.getBytes());
        
        String value =aux[1] ;
        
        messageDigest.update(value.getBytes());
        
        String valueHash = new String(messageDigest.digest());
        
        valueHash= Base64.getEncoder().encodeToString(valueHash.getBytes());
        
        Calendar c = Calendar.getInstance();
        Date currentDate = new Date();//EEE, dd-MMM-yy HH:mm:ss zzz
        c.setTime(currentDate);
        c.add(Calendar.MINUTE, 90);
        Date currentDatePlusOne = c.getTime();
        
       
        
//        currentDate=new Date(currentDate.getTime() + (hoursInMillis.)); // Adds 1 hours
        String date =httpformat.format(currentDatePlusOne) ;
        
        response.headers.setProperty("Set-Cookie", cookieNameHash + ".=." + valueHash + "; expires=" + date);//tenho de ver
    }
    public int findRightNameValue(String[] aux, String cookieNameroot, String valueroot){
        
        String namevalues = cookieNameroot + ".=." + valueroot;
        for (int i = 0; i < aux.length; i++) {
            System.out.println(aux[i]);
            System.out.println(namevalues);
            if (aux[i].equals(namevalues)) {
                
                return i;
            }
        }

        return -1;
    
    }
    public boolean compare_cookie(HTTPQuery q){
        
        if (q.getProperty("Cookie")!=null) {
            String headerValue = q.getProperty("Cookie");

            
            String[] aux = headerValue.split("; ");
            
            String[] auxfromroot = root.user_passwd().split(":");
            String cookieNameroot = auxfromroot[0];
            String valueroot = auxfromroot[1];
            
            MessageDigest messageDigest = null;
            try {
                messageDigest = MessageDigest.getInstance(CRYPTO);
            } catch (NoSuchAlgorithmException ex) {
                Logger.getLogger(httpThread.class.getName()).log(Level.SEVERE, null, ex);
            }
            messageDigest.update(cookieNameroot.getBytes());

            String cookieNameHash = new String(messageDigest.digest());
                
            cookieNameHash = Base64.getEncoder().encodeToString(cookieNameHash.getBytes());
            
            messageDigest.update(valueroot.getBytes());
        
            String valueHash = new String(messageDigest.digest());
        
            valueHash= Base64.getEncoder().encodeToString(valueHash.getBytes());
            
            int len = findRightNameValue(aux,cookieNameHash,valueHash);
            if(len>=0) return true;
            
////            int len=aux.length-1;
//            String[] values = aux[len].split(" .=. ");
//            
//            String nameCookie = values[0];
//            String valueCookie = values[1];
//            String auuux = values[2];
//
//            
//
//            
//
//            if (nameCookie.equals(cookieNameHash) && valueCookie.equals(valueHash)) {
//                return true;
//            }
        }


        return false;
    }
                                

    /**
     * Main thread that handles the requests
     */
    @Override
    public void run() {

        HTTPAnswer response = null;   // HTTP answer object
        HTTPQuery receivedhttp = null; //HTTP request object
        PrintStream pout = null;//enviar informaçao para o socket
        
        try {
            InputStream in = client.getInputStream();//para ler informaçao do socket
            BufferedReader bin = new BufferedReader( //ideal para ler strings
                    new InputStreamReader(in, "8859_1"));
            OutputStream out = client.getOutputStream();//
            pout = new PrintStream(out, false, "8859_1");
            try{
                client.setSoTimeout(root.getKeepAlive());

                do {
                    
                    //create an object to store the http request
                    receivedhttp = new HTTPQuery(root, client.getInetAddress().getHostAddress() + ":"
                            + client.getPort(), root.server.getLocalPort());
                    

                    //read the input http request
                    int result = receivedhttp.parse_Query(bin, true);
                    // receivedhttp is the HTTP query object with all request headers

                    // create a new answer object
                    response = new HTTPAnswer(root,
                            client.getInetAddress().getHostAddress() + ":" + client.getPort(),
                            guiHttpd.server_name + " - " + InetAddress.getLocalHost().getHostName() + "-" + root.server.getLocalPort());
                    // set BADREQ if the query has errors
                    if ((result != HTTPReplyCode.OK) || receivedhttp.is_invalid_request()) {
                        // Initialize the reply code and a few headers associated
                        response.set_error(HTTPReplyCode.BADREQ, receivedhttp.version);
                    } else {
                        
                        // Get file with contents
                        String filename = root.getRaizHtml() + receivedhttp.url_txt + (receivedhttp.url_txt.equals("/") ? "index.htm" : "");
                        System.out.println("Filename= " + filename);
                        File f = new File(filename);
                        
                        if (receivedhttp.method.equals("POST")) {
                            String information = receivedhttp.text;

                            if (!cookie_authentication(information)) {
                                response.unauthorized_page(receivedhttp.version);
                            } else {
                                response.set_code(HTTPReplyCode.OK);
                                response.set_version(receivedhttp.version);
                                response.set_file(f, guessMime(filename));
                                //set cookie 
                                set_cookie(response);
                            }

                        }else if (f.exists() && f.isFile()) {
                            if (ss.getLocalPort() == root.getPortHTTP()) {
                                // Redirect to Https
                                redirectClient(response,receivedhttp);
                                
                            }else if (authentication_valid(receivedhttp) == COOKIE_NEEDED && !compare_cookie(receivedhttp)) {   
                                
                                response.cookie_authentication_page(response.server_name,receivedhttp.version,receivedhttp.url_txt,receivedhttp.headers.getProperty("User-Agent"),root.getIp(),ss.getLocalPort(),"Welcome");
                                     
                            }else if (authentication_valid(receivedhttp) == SEND_AUTHR) {
                                
                                send_Unauthorized(response,receivedhttp);
                            
                            } else if (receivedhttp.getProperty("If-Modified-Since") != null) {

                                if (httpformat.parse(receivedhttp.getProperty("If-Modified-Since")).compareTo(httpformat.parse(Long.toString(f.lastModified()))) >= 0) {
                                    
                                    send_NotModified(response, receivedhttp);

                                }
                            
                            } else {
                                // Define reply contents
                                
                                response.set_code(HTTPReplyCode.OK);
                                response.set_version(receivedhttp.version);
                                response.set_file(f, guessMime(filename));
                            }

                            // NOTICE that only the first line of the reply is sent!
                            // No additional headers are defined you should add the needed headers using the HTTPAnswer class!
                        } else {
                            response.set_error(HTTPReplyCode.NOTFOUND, receivedhttp.version);
                            //System.out.println("File not found");

                            // NOTICE that some code is missing in HTTPAnswer!
                        }

                        // Send reply
                        response.send_Answer(pout, true, true);

                    }
                    if (receivedhttp.version.equals("HTTP/1.0") || receivedhttp.getProperty("Connection").equals("close") || root.getKeepAlive() != 0) {
                        in.close();
                        pout.close();
                        out.close();
                        break;
                    }

                } while (true);

                /*step 1
            criar um ciclo aqui para reutilizar a ligaçao- ao fim de x tempo sem receber nada, 
            fechamos o ciclo (socket setsotimeout-define o tempo maximo em
            milisegundos que o socket esta aberto)
            /** assim usamos o try catch e saimos do ciclo, ao sair desligamos a ligaçao
            tambem temos de fechar se a versao for 1.0
            ou se enviar um determinado cabeçalho tiver um valor de close, 
            temos de fechar a ligaçao, mas se for keep alive, mantemos
                 */
            } catch (SocketTimeoutException s) {
                in.close();
                pout.close();
                out.close();
            } catch (ParseException ex) {
                Logger.getLogger(httpThread.class.getName()).log(Level.SEVERE, null, ex);
            } 
//            catch (ParseException ex) {
//                Logger.getLogger(httpThread.class.getName()).log(Level.SEVERE, null, ex);
//            }
        } catch (IOException e) {
            if (root.active()) {
                System.out.println("I/O error " + e);
            }
        }
        
        finally {
            try {
                client.close();
            } catch (Exception e) {
                // Ignore
                System.out.println("Error closing client" + e);
            }
            root.thread_ended();
            Log(true, "Closed TCP connection\n");
        }
    }

}
