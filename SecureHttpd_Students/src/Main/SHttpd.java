/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**Esta a espera de ligaç~oes de clientes TCP
   Associada a um socket servidor TCP
   Aceitar Ligaçoes de Clientes**/

package Main;

import static Main.guiHttpd.server_name;
import java.net.ServerSocket;
/**
 *   /** HTTP server thread 
 * @author pedroamaral */
 

class SHttpd extends Thread {
        guiHttpd root;
        ServerSocket ss;
        boolean is_SSL;
        volatile boolean active;
        
        SHttpd ( guiHttpd root, ServerSocket ss, boolean is_SSL ) {
            this.root= root;
            this.ss= ss;
            this.is_SSL = is_SSL;
        }
        
        public void wake_up () {
            this.interrupt ();
        }
        
        public void stop_thread () {
            active= false;
            this.interrupt ();
        }
        
        @Override
        public void run () {
            System.out.println (
                    "\n******************** "+guiHttpd.server_name+ (is_SSL ? "HTTPS" : "HTTP")+ " started ********************\n");
            active= true;
            while ( active ) {
                try {
                    
                    httpThread conn = new httpThread ( root, ss, ss.accept (), is_SSL);//O objeto e criado quando o cliente se liga
                    conn.start ( );
                    root.thread_started ();
                } catch (java.io.IOException e) {
                    root.Log ("IO exception: "+ e + "\n");
                    active= false;
                }
            }
        }
} // end of class SHttpd
